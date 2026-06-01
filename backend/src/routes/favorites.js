import { Router } from "express";
import { db }     from "../config/database.js";
import { authenticate } from "../middleware/auth.js";

const router = Router();
router.use(authenticate);

// GET /api/favorites
router.get("/", async (req, res, next) => {
  try {
    const result = await db.query(
      `SELECT f.id, f.channel_id, f.created_at,
              c.name, c.category, c.logo_url, c.is_live, c.viewer_count
       FROM favorites f
       JOIN channels c ON c.id = f.channel_id
       WHERE f.user_id = $1
       ORDER BY f.created_at DESC`,
      [req.user.id]
    );
    res.json({ data: result.rows });
  } catch (err) { next(err); }
});

// POST /api/favorites/:channelId
router.post("/:channelId", async (req, res, next) => {
  try {
    const result = await db.query(
      `INSERT INTO favorites (user_id, channel_id)
       VALUES ($1, $2)
       ON CONFLICT (user_id, channel_id) DO NOTHING
       RETURNING id, channel_id, created_at`,
      [req.user.id, req.params.channelId]
    );
    if (!result.rows.length)
      return res.status(409).json({ error: "Sudah ada di favorit" });
    res.status(201).json(result.rows[0]);
  } catch (err) { next(err); }
});

// DELETE /api/favorites/:channelId
router.delete("/:channelId", async (req, res, next) => {
  try {
    await db.query(
      "DELETE FROM favorites WHERE user_id = $1 AND channel_id = $2",
      [req.user.id, req.params.channelId]
    );
    res.json({ success: true });
  } catch (err) { next(err); }
});

export default router;