import { Router } from "express";
import { db } from "../../config/database.js";
import { redis } from "../../config/redis.js";
import { importGvisionChannels } from "../../services/gvisionImport.js";

const router = Router();

async function clearChannelCache() {
  const keys = await redis.keys("channels:*");
  if (keys.length) await redis.del(...keys);
}

function mapChannel(row) {
  return {
    id: row.id,
    name: row.name,
    streamUrl: row.stream_url,
    category: row.category || "—",
    logoUrl: row.logo_url,
    isLive: row.is_live,
    active: row.active,
    viewerCount: row.viewer_count ?? 0,
    drmType: row.drm_type,
    drmKey: row.drm_key ? "••••" : null,
    hasDrm: Boolean(row.drm_key),
    userAgent: row.user_agent,
    referer: row.referer,
    sourceCategory: row.source_category,
    epgId: row.epg_id,
    createdAt: row.created_at,
  };
}

const SELECT_FIELDS = `
  id, name, stream_url, category, logo_url, is_live, active,
  viewer_count, drm_type, drm_key, user_agent, referer, source_category, epg_id, created_at
`;

// GET /api/admin/channels/categories
router.get("/categories/list", async (_req, res, next) => {
  try {
    const result = await db.query(
      `SELECT category, COUNT(*)::int AS count
       FROM channels
       WHERE active = true
       GROUP BY category
       ORDER BY count DESC, category ASC`,
    );
    res.json({ data: result.rows });
  } catch (err) {
    next(err);
  }
});

// POST /api/admin/channels/import — body: full gvision JSON
router.post("/import", async (req, res, next) => {
  try {
    const mode = req.body.mode === "append" ? "append" : "replace";
    const payload = req.body.data ?? req.body;

    const result = await importGvisionChannels(payload, { mode });
    res.json(result);
  } catch (err) {
    if (err.message?.includes("JSON") || err.message?.includes("channel")) {
      return res.status(400).json({ error: err.message });
    }
    next(err);
  }
});

// GET /api/admin/channels
router.get("/", async (req, res, next) => {
  try {
    const { search = "", category, page = 1, limit = 50, active } = req.query;
    const offset = (Math.max(1, parseInt(page, 10)) - 1) * parseInt(limit, 10);
    const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 50));

    const params = [];
    const conditions = ["1=1"];

    if (search) {
      params.push(`%${search}%`);
      conditions.push(`(name ILIKE $${params.length} OR category ILIKE $${params.length})`);
    }
    if (category && category !== "Semua") {
      params.push(category);
      conditions.push(`category = $${params.length}`);
    }
    if (active === "true") conditions.push("active = true");
    if (active === "false") conditions.push("active = false");

    const where = conditions.join(" AND ");

    const countResult = await db.query(
      `SELECT COUNT(*)::int AS c FROM channels WHERE ${where}`,
      params,
    );

    params.push(lim, offset);
    const result = await db.query(
      `SELECT ${SELECT_FIELDS}
       FROM channels
       WHERE ${where}
       ORDER BY id DESC
       LIMIT $${params.length - 1} OFFSET $${params.length}`,
      params,
    );

    res.json({
      data: result.rows.map(mapChannel),
      total: countResult.rows[0].c,
      page: parseInt(page, 10) || 1,
      limit: lim,
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/admin/channels/:id
router.get("/:id", async (req, res, next) => {
  try {
    const result = await db.query(
      `SELECT ${SELECT_FIELDS} FROM channels WHERE id = $1`,
      [req.params.id],
    );
    if (!result.rows.length) {
      return res.status(404).json({ error: "Channel tidak ditemukan" });
    }
    res.json(mapChannel(result.rows[0]));
  } catch (err) {
    next(err);
  }
});

// POST /api/admin/channels
router.post("/", async (req, res, next) => {
  try {
    const {
      name,
      streamUrl,
      category,
      logoUrl,
      drmType,
      drmKey,
      userAgent,
      referer,
      active = true,
      isLive = true,
    } = req.body;

    if (!name?.trim() || !streamUrl?.trim()) {
      return res.status(400).json({ error: "Nama dan stream URL wajib diisi" });
    }

    const result = await db.query(
      `INSERT INTO channels (
         name, stream_url, category, logo_url,
         drm_type, drm_key, user_agent, referer, active, is_live
       ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING ${SELECT_FIELDS}`,
      [
        name.trim().slice(0, 255),
        streamUrl.trim(),
        category || null,
        logoUrl || null,
        drmType || null,
        drmKey || null,
        userAgent || null,
        referer || null,
        Boolean(active),
        Boolean(isLive),
      ],
    );

    await clearChannelCache();
    res.status(201).json(mapChannel(result.rows[0]));
  } catch (err) {
    next(err);
  }
});

// PUT /api/admin/channels/:id
router.put("/:id", async (req, res, next) => {
  try {
    const {
      name,
      streamUrl,
      category,
      logoUrl,
      drmType,
      drmKey,
      userAgent,
      referer,
      active,
      isLive,
    } = req.body;

    const result = await db.query(
      `UPDATE channels SET
         name = COALESCE($1, name),
         stream_url = COALESCE($2, stream_url),
         category = COALESCE($3, category),
         logo_url = COALESCE($4, logo_url),
         drm_type = COALESCE($5, drm_type),
         drm_key = COALESCE($6, drm_key),
         user_agent = COALESCE($7, user_agent),
         referer = COALESCE($8, referer),
         active = COALESCE($9, active),
         is_live = COALESCE($10, is_live)
       WHERE id = $11
       RETURNING ${SELECT_FIELDS}`,
      [
        name?.trim()?.slice(0, 255),
        streamUrl?.trim(),
        category,
        logoUrl,
        drmType,
        drmKey,
        userAgent,
        referer,
        active !== undefined ? Boolean(active) : null,
        isLive !== undefined ? Boolean(isLive) : null,
        req.params.id,
      ],
    );

    if (!result.rows.length) {
      return res.status(404).json({ error: "Channel tidak ditemukan" });
    }

    await clearChannelCache();
    res.json(mapChannel(result.rows[0]));
  } catch (err) {
    next(err);
  }
});

// PATCH /api/admin/channels/:id/toggle
router.patch("/:id/toggle", async (req, res, next) => {
  try {
    const field = req.body.field === "live" ? "is_live" : "active";
    const result = await db.query(
      `UPDATE channels SET ${field} = NOT ${field}
       WHERE id = $1
       RETURNING ${SELECT_FIELDS}`,
      [req.params.id],
    );
    if (!result.rows.length) {
      return res.status(404).json({ error: "Channel tidak ditemukan" });
    }
    await clearChannelCache();
    res.json(mapChannel(result.rows[0]));
  } catch (err) {
    next(err);
  }
});

// DELETE /api/admin/channels/:id
router.delete("/:id", async (req, res, next) => {
  try {
    const result = await db.query(
      "DELETE FROM channels WHERE id = $1 RETURNING id",
      [req.params.id],
    );
    if (!result.rows.length) {
      return res.status(404).json({ error: "Channel tidak ditemukan" });
    }
    await clearChannelCache();
    res.json({ success: true, id: result.rows[0].id });
  } catch (err) {
    next(err);
  }
});

export default router;
