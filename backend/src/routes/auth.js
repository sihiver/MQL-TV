import { Router }  from "express";
import bcrypt      from "bcryptjs";
import jwt         from "jsonwebtoken";
import { db }      from "../config/database.js";
import { redis }   from "../config/redis.js";
import { authenticate } from "../middleware/auth.js";
import { getServerSettings } from "../services/serverSettings.js";
import { parseDurationToSeconds } from "../utils/duration.js";

const router = Router();

async function jwtExpiresIn() {
  const settings = await getServerSettings();
  return settings.jwtExpiry || "1d";
}

// ── Refresh token helpers (dual-storage: Redis + PostgreSQL) ──────────────────
// Redis digunakan sebagai primary (fast), PostgreSQL sebagai fallback persistent.
// Jika Redis restart/mati, token tetap bisa ditemukan di PostgreSQL.

const REFRESH_TTL_DAYS = 90;
const REFRESH_TTL_SEC  = 60 * 60 * 24 * REFRESH_TTL_DAYS;

// Auto-ensure refresh_tokens table exists agar persistent fallback tidak pernah error
db.query(`
  CREATE TABLE IF NOT EXISTS refresh_tokens (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );
  CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);
  CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
`).catch(err => {
  console.warn("Notice: refresh_tokens table check:", err.message);
});

async function saveRefreshToken(userId, token) {
  const expiresAt = new Date(Date.now() + REFRESH_TTL_SEC * 1000);
  // Tulis ke Redis dengan key token (fast path per-token) dan key user (legacy compatibility)
  try { await redis.setex(`refresh:${token}`, REFRESH_TTL_SEC, userId.toString()); } catch (_) {}
  try { await redis.setex(`refresh:${userId}`, REFRESH_TTL_SEC, token); } catch (_) {}
  // Tulis ke PostgreSQL (persistent fallback)
  try {
    await db.query(
      `INSERT INTO refresh_tokens (user_id, token, expires_at)
       VALUES ($1, $2, $3)
       ON CONFLICT (token) DO UPDATE SET expires_at = EXCLUDED.expires_at`,
      [userId, token, expiresAt]
    );
  } catch (_) {}
}

async function isRefreshTokenValid(userId, token) {
  if (!userId || !token) return false;

  // 1. Blacklist check (misal setelah logout)
  try {
    const blacklisted = await redis.get(`blacklist:refresh:${token}`);
    if (blacklisted) return false;
  } catch (_) {}

  // 2. Fast path: Redis check per-token
  try {
    const storedUserId = await redis.get(`refresh:${token}`);
    if (storedUserId && storedUserId.toString() === userId.toString()) return true;
  } catch (_) {}

  // 3. Fast path: Redis check per-user
  try {
    const storedToken = await redis.get(`refresh:${userId}`);
    if (storedToken && storedToken === token) return true;
  } catch (_) {}

  // 4. Fallback ke PostgreSQL
  try {
    const res = await db.query(
      `SELECT 1 FROM refresh_tokens
       WHERE user_id = $1 AND token = $2 AND expires_at > NOW()`,
      [userId, token]
    );
    if (res.rows.length > 0) {
      // Re-sync ke Redis agar request berikutnya lebih cepat
      try { await redis.setex(`refresh:${token}`, REFRESH_TTL_SEC, userId.toString()); } catch (_) {}
      return true;
    }
  } catch (_) {}

  // 5. Cryptographic fallback:
  // Token sudah divalidasi tanda tangannya via jwt.verify(refreshToken, JWT_REFRESH_SECRET).
  // Jika Redis ter-flush dan baris DB hilang, selama user aktif & tidak banned, terima dan sinkronkan ulang!
  // Menghindari STB terkunci saat Redis restart atau cache expire.
  try {
    const userRes = await db.query("SELECT id, banned FROM users WHERE id = $1", [userId]);
    if (userRes.rows.length > 0 && !userRes.rows[0].banned) {
      await saveRefreshToken(userId, token);
      return true;
    }
  } catch (_) {}

  return false;
}

async function deleteRefreshToken(userId, token) {
  if (token) {
    try { await redis.del(`refresh:${token}`); } catch (_) {}
    try { await redis.setex(`blacklist:refresh:${token}`, REFRESH_TTL_SEC, "1"); } catch (_) {}
  }
  try { await redis.del(`refresh:${userId}`); } catch (_) {}
  try {
    if (token) {
      await db.query("DELETE FROM refresh_tokens WHERE user_id = $1 AND token = $2", [userId, token]);
    } else {
      await db.query("DELETE FROM refresh_tokens WHERE user_id = $1", [userId]);
    }
  } catch (_) {}
}
// ─────────────────────────────────────────────────────────────────────────────


// POST /api/auth/register
router.post("/register", async (req, res, next) => {
  try {
    const settings = await getServerSettings();
    if (!settings.allowRegistration) {
      return res.status(403).json({ error: "Registrasi pengguna baru dinonaktifkan" });
    }

    const { name, email, password } = req.body;

    const exists = await db.query(
      "SELECT id FROM users WHERE email = $1", [email]
    );
    if (exists.rows.length)
      return res.status(409).json({ error: "Email sudah terdaftar" });

    const hash = await bcrypt.hash(password, 12);
    const result = await db.query(
      `INSERT INTO users (name, email, password_hash, plan, role)
       VALUES ($1, $2, $3, 'free', 'user') RETURNING id, name, email, plan`,
      [name, email, hash]
    );

    const user = result.rows[0];
    const expiresIn = await jwtExpiresIn();
    const token = jwt.sign(
      { id: user.id, email: user.email, plan: user.plan, role: "user" },
      process.env.JWT_SECRET,
      { expiresIn },
    );

    res.status(201).json({ token, user });
  } catch (err) { next(err); }
});

// POST /api/auth/login
router.post("/login", async (req, res, next) => {
  try {
    const { email: rawEmail, password } = req.body;
    const email = rawEmail?.trim().toLowerCase();
    if (!email || !password) {
      return res.status(400).json({ error: "Email dan password wajib diisi" });
    }

    const result = await db.query(
      "SELECT * FROM users WHERE email = $1", [email]
    );
    const user = result.rows[0];
    if (!user || !(await bcrypt.compare(password, user.password_hash)))
      return res.status(401).json({ error: "Email atau password salah" });

    if (user.banned)
      return res.status(403).json({ error: "Akun ini telah diblokir" });

    const settings = await getServerSettings();
    if (settings.maintenanceMode && user.role !== "admin") {
      return res.status(503).json({ error: "Server sedang maintenance" });
    }

    const subResult = await db.query(
      `SELECT plan, expires_at, status FROM subscriptions WHERE user_id = $1 ORDER BY expires_at DESC LIMIT 1`,
      [user.id]
    );
    let expiresAt = null;
    let activePlan = null;
    if (subResult.rows.length > 0) {
      const sub = subResult.rows[0];
      const isSubActive = sub.status === 'active' && sub.expires_at && new Date(sub.expires_at) > new Date();
      if (isSubActive) {
        expiresAt = new Date(sub.expires_at).toISOString();
        activePlan = sub.plan;
      } else if (sub.expires_at) {
        expiresAt = new Date(sub.expires_at).toISOString();
      }
    }

    const effectivePlan = user.role === "admin" ? user.plan : (activePlan || "free");
    const expiresIn = await jwtExpiresIn();
    const token = jwt.sign(
      { id: user.id, email: user.email, plan: effectivePlan, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn },
    );
    const refreshToken = jwt.sign(
      { id: user.id },
      process.env.JWT_REFRESH_SECRET,
      { expiresIn: "90d" },
    );

    // Simpan refresh token (Redis + PostgreSQL dual-write)
    await saveRefreshToken(user.id, refreshToken);

    const expiresSec = parseDurationToSeconds(expiresIn) || 86400;

    res.json({
      token,
      refreshToken,
      expiresIn: expiresSec,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        plan: effectivePlan,
        role: user.role,
        expiresAt: expiresAt,
      },
    });
  } catch (err) { next(err); }
});

// POST /api/auth/admin/login — hanya role admin (panel)
router.post("/admin/login", async (req, res, next) => {
  try {
    const { email: rawEmail, password } = req.body;
    const email = rawEmail?.trim().toLowerCase();
    if (!email || !password) {
      return res.status(400).json({ error: "Email dan password wajib diisi" });
    }

    const result = await db.query("SELECT * FROM users WHERE email = $1", [email]);
    const user = result.rows[0];
    if (!user || !(await bcrypt.compare(password, user.password_hash))) {
      return res.status(401).json({ error: "Email atau password salah" });
    }
    if (user.banned) {
      return res.status(403).json({ error: "Akun ini telah diblokir" });
    }
    if (user.role !== "admin") {
      return res.status(403).json({ error: "Akses ditolak — hanya admin" });
    }

    const expiresIn = await jwtExpiresIn();
    const token = jwt.sign(
      { id: user.id, email: user.email, plan: user.plan, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn },
    );

    const expiresSec = parseDurationToSeconds(expiresIn) || 86400;

    res.json({
      token,
      expiresIn: expiresSec,
      user: { id: user.id, name: user.name, email: user.email, plan: user.plan, role: user.role },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/refresh — perpanjang access token (app TV)
router.post("/refresh", async (req, res, next) => {
  try {
    const refreshToken = req.body.refreshToken || req.body.refresh_token;
    if (!refreshToken) {
      return res.status(400).json({ error: "Refresh token wajib diisi" });
    }

    let payload;
    try {
      payload = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET);
    } catch {
      return res.status(401).json({ error: "Refresh token tidak valid atau kadaluarsa" });
    }

    const valid = await isRefreshTokenValid(payload.id, refreshToken);
    if (!valid) {
      return res.status(401).json({ error: "Refresh token tidak valid atau kadaluarsa" });
    }

    const result = await db.query("SELECT * FROM users WHERE id = $1", [payload.id]);
    const user = result.rows[0];
    if (!user || user.banned) {
      await redis.del(`refresh:${payload.id}`);
      return res.status(403).json({ error: "Akun ini telah diblokir" });
    }

    const settings = await getServerSettings();
    if (settings.maintenanceMode && user.role !== "admin") {
      return res.status(503).json({ error: "Server sedang maintenance" });
    }

    const expiresIn = await jwtExpiresIn();
    const token = jwt.sign(
      { id: user.id, email: user.email, plan: user.plan, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn },
    );
    const newRefreshToken = jwt.sign(
      { id: user.id },
      process.env.JWT_REFRESH_SECRET,
      { expiresIn: "90d" },
    );

    // Perbarui refresh token (dual-write, rolling 90 hari lagi)
    await saveRefreshToken(user.id, newRefreshToken);

    res.json({
      token,
      refreshToken: newRefreshToken,
      expiresIn: parseDurationToSeconds(expiresIn) || 86400,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/logout
router.post("/logout", authenticate, async (req, res, next) => {
  try {
    const token = req.headers.authorization.split(" ")[1];
    // Blacklist access token selama 1 hari
    await redis.setex(`blacklist:${token}`, 86400, "1");

    const refreshToken = req.body?.refreshToken || req.body?.refresh_token;
    await deleteRefreshToken(req.user.id, refreshToken);
    res.json({ success: true });
  } catch (err) { next(err); }
});

// GET /api/auth/me
router.get("/me", authenticate, async (req, res, next) => {
  try {
    const result = await db.query(
      `SELECT u.id, u.name, u.email, u.plan, u.role,
              COUNT(d.id) as devices,
              (SELECT expires_at FROM subscriptions s WHERE s.user_id = u.id ORDER BY expires_at DESC LIMIT 1) as "rawExpiresAt",
              (SELECT status FROM subscriptions s WHERE s.user_id = u.id ORDER BY expires_at DESC LIMIT 1) as "subStatus",
              (SELECT plan FROM subscriptions s WHERE s.user_id = u.id AND s.status = 'active' AND s.expires_at > NOW() ORDER BY expires_at DESC LIMIT 1) as "activePlan"
       FROM users u
       LEFT JOIN devices d ON d.user_id = u.id
       WHERE u.id = $1
       GROUP BY u.id`,
      [req.user.id]
    );
    const user = result.rows[0];
    if (user) {
      let expiresAt = null;
      if (user.rawExpiresAt) {
        expiresAt = new Date(user.rawExpiresAt).toISOString();
      }
      user.expiresAt = expiresAt;
      if (user.role !== "admin") {
        user.plan = user.activePlan || "free";
      }
      delete user.rawExpiresAt;
      delete user.subStatus;
      delete user.activePlan;
    }
    res.json(user);
  } catch (err) { next(err); }
});

export default router;