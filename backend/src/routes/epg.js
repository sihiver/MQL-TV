import { Router } from "express";
import { db } from "../config/database.js";
import { authenticate } from "../middleware/auth.js";

const router = Router();
router.use(authenticate);

router.get("/:channelId", async (req, res, next) => {
  try {
    const result = await db.query(
      `SELECT id, title, description, start_time, end_time, category
       FROM epg_programs
       WHERE channel_id = $1 AND start_time >= NOW() - INTERVAL '1 day'
       ORDER BY start_time
       LIMIT 50`,
      [req.params.channelId],
    );
    res.json({ data: result.rows });
  } catch (err) {
    next(err);
  }
});

export default router;
