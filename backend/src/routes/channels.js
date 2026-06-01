import { Router } from "express";
import { db }     from "../config/database.js";
import { redis }  from "../config/redis.js";
import { authenticate, isAdmin } from "../middleware/auth.js";
import { generateStreamToken }   from "../services/streamToken.js";

const router = Router();
router.use(authenticate);

// GET /api/channels?category=Sport&page=1&limit=50
router.get("/", async (req, res, next) => {
  try {
    const { category, page = 1, limit = 50 } = req.query;
    const offset = (page - 1) * limit;

    // Cek cache Redis dulu
    const cacheKey = `channels:${category || "all"}:${page}`;
    const cached = await redis.get(cacheKey);
    if (cached) return res.json(JSON.parse(cached));

    let query = `SELECT id, name, category, logo_url, is_live, viewer_count, epg_id
                 FROM channels WHERE active = true`;
    const params = [];

    if (category) {
      params.push(category);
      query += ` AND category = $${params.length}`;
    }

    const countResult = await db.query(
      `SELECT COUNT(*) FROM channels WHERE active = true
       ${category ? "AND category = $1" : ""}`,
      category ? [category] : []
    );

    query += ` ORDER BY viewer_count DESC LIMIT $${params.length + 1} OFFSET $${params.length + 2}`;
    params.push(limit, offset);

    const result = await db.query(query, params);

    const payload = {
      data: result.rows,
      total: parseInt(countResult.rows[0].count),
      page: parseInt(page),
      limit: parseInt(limit),
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
    const result = await db.query(
      `SELECT id, name, category, logo_url, is_live, viewer_count
       FROM channels
       WHERE active = true AND (
         name ILIKE $1 OR category ILIKE $1
       )
       ORDER BY viewer_count DESC
       LIMIT 20`,
      [`%${q}%`]
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

    const channel = await db.query(
      "SELECT stream_url FROM channels WHERE id = $1 AND active = true",
      [req.params.id]
    );
    if (!channel.rows.length)
      return res.status(404).json({ error: "Channel tidak ditemukan" });

    const { streamUrl, token, expiresAt } = generateStreamToken(
      channel.rows[0].stream_url,
      req.user.id
    );

    res.json({ streamUrl, token, expiresAt });
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