import { Router } from "express";
import { db }     from "../config/database.js";
import { redis }  from "../config/redis.js";
import { authenticate, isAdmin } from "../middleware/auth.js";
import { generateStreamToken }   from "../services/streamToken.js";
import {
  resolvePackageAccess,
  getPackageBySlug,
  channelAllowedForPackage,
} from "../services/packageAccess.js";

const router = Router();
router.use(authenticate);

// GET /api/channels?category=Sport&page=1&limit=50
router.get("/", async (req, res, next) => {
  try {
    const { category, page = 1, limit = 50 } = req.query;
    const offset = (page - 1) * limit;

    const access = await resolvePackageAccess(req.user.id);
    const cacheKey = `channels:${access.planSlug}:${category || "all"}:${page}`;
    const cached = await redis.get(cacheKey);
    if (cached) return res.json(JSON.parse(cached));

    const params = [];
    const conditions = ["c.active = true"];

    if (!access.hasPackage) {
      return res.json({ data: [], total: 0, page: parseInt(page, 10), limit: parseInt(limit, 10) });
    }

    if (!access.includesAll) {
      params.push(access.packageId);
      conditions.push(
        `c.id IN (SELECT channel_id FROM package_channels WHERE package_id = $${params.length})`,
      );
    }

    if (category) {
      params.push(category);
      conditions.push(`c.category = $${params.length}`);
    }

    const where = conditions.join(" AND ");

    const countResult = await db.query(
      `SELECT COUNT(*)::int AS count FROM channels c WHERE ${where}`,
      params,
    );

    params.push(limit, offset);
    const result = await db.query(
      `SELECT c.id, c.name, c.category, c.logo_url, c.is_live, c.viewer_count, c.epg_id
       FROM channels c
       WHERE ${where}
       ORDER BY c.viewer_count DESC
       LIMIT $${params.length - 1} OFFSET $${params.length}`,
      params,
    );

    const payload = {
      data: result.rows,
      total: countResult.rows[0].count,
      page: parseInt(page, 10),
      limit: parseInt(limit, 10),
      plan: access.planSlug,
    };

    // Cache 5 menit
    await redis.setex(cacheKey, 300, JSON.stringify(payload));
    res.json(payload);
  } catch (err) { next(err); }
});

// GET /api/channels/search?q=espn
router.get("/search", async (req, res, next) => {
  try {
    const { q } = req.query;
    const access = await resolvePackageAccess(req.user.id);
    if (!access.hasPackage) {
      return res.json({ data: [], total: 0 });
    }

    const params = [`%${q}%`];
    let packageFilter = "";
    if (!access.includesAll) {
      params.push(access.packageId);
      packageFilter = `AND c.id IN (SELECT channel_id FROM package_channels WHERE package_id = $${params.length})`;
    }

    const result = await db.query(
      `SELECT c.id, c.name, c.category, c.logo_url, c.is_live, c.viewer_count
       FROM channels c
       WHERE c.active = true ${packageFilter}
         AND (c.name ILIKE $1 OR c.category ILIKE $1)
       ORDER BY c.viewer_count DESC
       LIMIT 20`,
      params,
    );
    res.json({ data: result.rows, total: result.rows.length });
  } catch (err) { next(err); }
});

// GET /api/channels/:id/stream — generate stream URL + token
router.get("/:id/stream", async (req, res, next) => {
  try {
    // Cek subscription
    const sub = await db.query(
      `SELECT s.plan, s.expires_at FROM subscriptions s
       WHERE s.user_id = $1 AND s.status = 'active'
       AND s.expires_at > NOW()`,
      [req.user.id]
    );
    if (!sub.rows.length)
      return res.status(403).json({ error: "Subscription tidak aktif" });

    const planSlug = sub.rows[0].plan?.toLowerCase();
    const pkg = await getPackageBySlug(planSlug);

    const channel = await db.query(
      `SELECT id, stream_url, drm_type, drm_key, user_agent, referer
       FROM channels WHERE id = $1 AND active = true`,
      [req.params.id],
    );
    if (!channel.rows.length)
      return res.status(404).json({ error: "Channel tidak ditemukan" });

    if (pkg) {
      const allowed = await channelAllowedForPackage(
        pkg.id,
        channel.rows[0].id,
        pkg.includes_all_channels,
      );
      if (!allowed) {
        return res.status(403).json({ error: "Channel tidak termasuk paket langganan Anda" });
      }
    }

    const ch = channel.rows[0];
    const { streamUrl, token, expiresAt } = generateStreamToken(ch.stream_url, req.user.id);

    res.json({
      streamUrl,
      token,
      expiresAt,
      drmType: ch.drm_type,
      drmKey: ch.drm_key,
      userAgent: ch.user_agent,
      referer: ch.referer,
    });
  } catch (err) { next(err); }
});

// POST /api/channels (admin only)
router.post("/", isAdmin, async (req, res, next) => {
  try {
    const { name, url, category, logo_url, epg_id } = req.body;
    const result = await db.query(
      `INSERT INTO channels (name, stream_url, category, logo_url, epg_id, active)
       VALUES ($1, $2, $3, $4, $5, true) RETURNING id, name, created_at`,
      [name, url, category, logo_url, epg_id]
    );
    await redis.keys("channels:*").then(keys => keys.length && redis.del(keys));
    res.status(201).json(result.rows[0]);
  } catch (err) { next(err); }
});

export default router;