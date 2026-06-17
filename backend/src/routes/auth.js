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

    const expiresIn = await jwtExpiresIn();
    const token = jwt.sign(
      { id: user.id, email: user.email, plan: user.plan, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn },
    );
    const refreshToken = jwt.sign(
      { id: user.id },
      process.env.JWT_REFRESH_SECRET,
      { expiresIn: "30d" },
    );

    // Simpan refresh token di Redis
    await redis.setex(`refresh:${user.id}`, 60 * 60 * 24 * 30, refreshToken);

    const expiresSec = parseDurationToSeconds(expiresIn) || 86400;

    const subResult = await db.query(
      `SELECT expires_at, status FROM subscriptions WHERE user_id = $1 ORDER BY expires_at DESC LIMIT 1`,
      [user.id]
    );
    let expiresAt = "2000-01-01T00:00:00.000Z";
    if (subResult.rows.length > 0) {
      const sub = subResult.rows[0];
      if (sub.status === 'active') {
        expiresAt = sub.expires_at ? new Date(sub.expires_at).toISOString() : "2000-01-01T00:00:00.000Z";
      } else {
        if (sub.expires_at) {
          const d = new Date(sub.expires_at);
          if (d <= new Date()) {
            expiresAt = d.toISOString();
          } else {
            expiresAt = new Date(Date.now() - 86400000).toISOString(); // yesterday
          }
        } else {
          expiresAt = "2000-01-01T00:00:00.000Z";
        }
      }
    }

    res.json({
      token,
      refreshToken,
      expiresIn: expiresSec,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        plan: user.plan,
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

    const stored = await redis.get(`refresh:${payload.id}`);
    if (!stored || stored !== refreshToken) {
      return res.status(401).json({ error: "Refresh token tidak valid" });
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

    res.json({
      token,
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
    // Blacklist token selama 1 hari
    await redis.setex(`blacklist:${token}`, 86400, "1");
    await redis.del(`refresh:${req.user.id}`);
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
              (SELECT status FROM subscriptions s WHERE s.user_id = u.id ORDER BY expires_at DESC LIMIT 1) as "subStatus"
       FROM users u
       LEFT JOIN devices d ON d.user_id = u.id
       WHERE u.id = $1
       GROUP BY u.id`,
      [req.user.id]
    );
    const user = result.rows[0];
    if (user) {
      let expiresAt = "2000-01-01T00:00:00.000Z";
      if (user.rawExpiresAt) {
        if (user.subStatus === 'active') {
          expiresAt = new Date(user.rawExpiresAt).toISOString();
        } else {
          const d = new Date(user.rawExpiresAt);
          if (d <= new Date()) {
            expiresAt = d.toISOString();
          } else {
            expiresAt = new Date(Date.now() - 86400000).toISOString(); // yesterday
          }
        }
      }
      user.expiresAt = expiresAt;
      delete user.rawExpiresAt;
      delete user.subStatus;
    }
    res.json(user);
  } catch (err) { next(err); }
});

export default router;