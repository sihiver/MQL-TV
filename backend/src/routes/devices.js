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

export default router;
