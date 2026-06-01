import { Router } from "express";
import { db } from "../config/database.js";
import { authenticate } from "../middleware/auth.js";

const router = Router();
router.use(authenticate);

router.get("/", async (req, res, next) => {
  try {
    const result = await db.query(
      `SELECT plan, status, started_at, expires_at, max_devices
       FROM subscriptions
       WHERE user_id = $1 AND status = 'active'
       ORDER BY expires_at DESC
       LIMIT 1`,
      [req.user.id],
    );
    res.json(result.rows[0] || { plan: "free", status: "active" });
  } catch (err) {
    next(err);
  }
});

export default router;
