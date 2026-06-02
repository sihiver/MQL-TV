import { Router } from "express";
import { db } from "../config/database.js";
import { authenticate } from "../middleware/auth.js";

const router = Router();
router.use(authenticate);

router.get("/", async (req, res, next) => {
  try {
    const subResult = await db.query(
      `SELECT plan, status, started_at, expires_at, max_devices
       FROM subscriptions
       WHERE user_id = $1 AND status = 'active' AND expires_at > NOW()
       ORDER BY expires_at DESC
       LIMIT 1`,
      [req.user.id],
    );

    if (!subResult.rows.length) {
      return res.json({
        plan: "free",
        status: "inactive",
        package_name: "Free",
        channel_count: 0,
      });
    }

    const sub = subResult.rows[0];
    const planSlug = (sub.plan || "free").toLowerCase();

    const pkgResult = await db.query(
      `SELECT p.name, p.includes_all_channels,
              CASE WHEN p.includes_all_channels THEN
                (SELECT COUNT(*)::int FROM channels WHERE active = true)
              ELSE
                (SELECT COUNT(*)::int FROM package_channels pc
                 INNER JOIN channels c ON c.id = pc.channel_id AND c.active = true
                 WHERE pc.package_id = p.id)
              END AS channel_count
       FROM packages p
       WHERE LOWER(p.slug) = $1 AND p.active = true
       LIMIT 1`,
      [planSlug],
    );

    const pkg = pkgResult.rows[0];
    res.json({
      ...sub,
      package_name: pkg?.name || sub.plan,
      channel_count: pkg?.channel_count ?? 0,
    });
  } catch (err) {
    next(err);
  }
});

export default router;
