import { Router }  from "express";
import bcrypt      from "bcryptjs";
import jwt         from "jsonwebtoken";
import { db }      from "../config/database.js";
import { redis }   from "../config/redis.js";
import { authenticate } from "../middleware/auth.js";

const router = Router();

// POST /api/auth/register
router.post("/register", async (req, res, next) => {
  try {
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
    const token = jwt.sign(
      { id: user.id, email: user.email, plan: user.plan, role: "user" },
      process.env.JWT_SECRET,
      { expiresIn: "1d" }
    );

    res.status(201).json({ token, user });
  } catch (err) { next(err); }
});

// POST /api/auth/login
router.post("/login", async (req, res, next) => {
  try {
    const { email, password } = req.body;

    const result = await db.query(
      "SELECT * FROM users WHERE email = $1", [email]
    );
    const user = result.rows[0];
    if (!user || !(await bcrypt.compare(password, user.password_hash)))
      return res.status(401).json({ error: "Email atau password salah" });

    if (user.banned)
      return res.status(403).json({ error: "Akun ini telah diblokir" });

    const token = jwt.sign(
      { id: user.id, email: user.email, plan: user.plan, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: "1d" }
    );
    const refreshToken = jwt.sign(
      { id: user.id },
      process.env.JWT_REFRESH_SECRET,
      { expiresIn: "30d" }
    );

    // Simpan refresh token di Redis
    await redis.setex(`refresh:${user.id}`, 60 * 60 * 24 * 30, refreshToken);

    res.json({ token, refreshToken, expiresIn: 86400 });
  } catch (err) { next(err); }
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
              COUNT(d.id) as devices
       FROM users u
       LEFT JOIN devices d ON d.user_id = u.id
       WHERE u.id = $1
       GROUP BY u.id`,
      [req.user.id]
    );
    res.json(result.rows[0]);
  } catch (err) { next(err); }
});

export default router;