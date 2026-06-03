import { Router } from "express";
import { db } from "../config/database.js";
import { authenticate } from "../middleware/auth.js";

const router = Router();
router.use(authenticate);

router.get("/", async (req, res, next) => {
  try {
    const result = await db.query(
      `SELECT id, name, type, last_seen_at, created_at
       FROM devices WHERE user_id = $1 ORDER BY last_seen_at DESC`,
      [req.user.id],
    );
    res.json({ data: result.rows });
  } catch (err) {
    next(err);
  }
});

/** Daftar / perbarui heartbeat perangkat (untuk dashboard Perangkat Aktif). */
router.post("/register", async (req, res, next) => {
  try {
    const deviceKey = String(req.body.deviceKey || req.body.device_key || "").trim();
    const name = String(req.body.name || "Android TV").trim().slice(0, 100);
    const type = String(req.body.type || "tv").trim().slice(0, 50);

    if (!deviceKey) {
      return res.status(400).json({ error: "device_key wajib diisi" });
    }

    const result = await db.query(
      `INSERT INTO devices (user_id, name, type, device_key, last_seen_at)
       VALUES ($1, $2, $3, $4, NOW())
       ON CONFLICT (device_key) DO UPDATE SET
         user_id = EXCLUDED.user_id,
         name = EXCLUDED.name,
         type = EXCLUDED.type,
         last_seen_at = NOW()
       RETURNING id, name, type, last_seen_at`,
      [req.user.id, name, type, deviceKey],
    );

    res.json({ data: result.rows[0] });
  } catch (err) {
    next(err);
  }
});

export default router;
