import { Router } from "express";
import { db } from "../../config/database.js";
import { redis } from "../../config/redis.js";

const router = Router({ mergeParams: true });

async function clearChannelCache() {
  const keys = await redis.keys("channels:*");
  if (keys.length) await redis.del(...keys);
}

async function assertPackage(packageId) {
  const r = await db.query("SELECT id, name, includes_all_channels FROM packages WHERE id = $1", [
    packageId,
  ]);
  if (!r.rows.length) {
    const err = new Error("Paket tidak ditemukan");
    err.status = 404;
    throw err;
  }
  return r.rows[0];
}

function mapChannelRow(row) {
  return {
    id: row.id,
    name: row.name,
    category: row.category,
    logoUrl: row.logo_url,
    active: row.active,
    hasDrm: Boolean(row.drm_key),
  };
}

// GET /api/admin/packages/:packageId/channels
router.get("/", async (req, res, next) => {
  try {
    const packageId = req.params.packageId;
    await assertPackage(packageId);

    const { search = "", page = 1, limit = 50 } = req.query;
    const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 50));
    const offset = (Math.max(1, parseInt(page, 10)) - 1) * lim;

    const params = [packageId];
    const conditions = ["pc.package_id = $1"];

    if (search) {
      params.push(`%${search}%`);
      conditions.push(`(c.name ILIKE $${params.length} OR c.category ILIKE $${params.length})`);
    }

    const where = conditions.join(" AND ");

    const countResult = await db.query(
      `SELECT COUNT(*)::int AS c
       FROM package_channels pc
       JOIN channels c ON c.id = pc.channel_id
       WHERE ${where}`,
      params,
    );

    params.push(lim, offset);
    const result = await db.query(
      `SELECT c.id, c.name, c.category, c.logo_url, c.active, c.drm_key
       FROM package_channels pc
       JOIN channels c ON c.id = pc.channel_id
       WHERE ${where}
       ORDER BY c.name ASC
       LIMIT $${params.length - 1} OFFSET $${params.length}`,
      params,
    );

    res.json({
      data: result.rows.map(mapChannelRow),
      total: countResult.rows[0].c,
      page: parseInt(page, 10) || 1,
      limit: lim,
    });
  } catch (err) {
    if (err.status) return res.status(err.status).json({ error: err.message });
    next(err);
  }
});

// GET /api/admin/packages/:packageId/channels/available — channel belum di paket
router.get("/available", async (req, res, next) => {
  try {
    const packageId = req.params.packageId;
    await assertPackage(packageId);

    const { search = "", category, page = 1, limit = 30 } = req.query;
    const lim = Math.min(100, Math.max(1, parseInt(limit, 10) || 30));
    const offset = (Math.max(1, parseInt(page, 10)) - 1) * lim;

    const params = [packageId];
    const conditions = [
      "c.active = true",
      `c.id NOT IN (SELECT channel_id FROM package_channels WHERE package_id = $1)`,
    ];

    if (search) {
      params.push(`%${search}%`);
      conditions.push(`(c.name ILIKE $${params.length} OR c.category ILIKE $${params.length})`);
    }
    if (category) {
      params.push(category);
      conditions.push(`c.category = $${params.length}`);
    }

    const where = conditions.join(" AND ");
    const countResult = await db.query(
      `SELECT COUNT(*)::int AS c FROM channels c WHERE ${where}`,
      params,
    );

    params.push(lim, offset);
    const result = await db.query(
      `SELECT c.id, c.name, c.category, c.logo_url, c.active, c.drm_key
       FROM channels c
       WHERE ${where}
       ORDER BY c.name ASC
       LIMIT $${params.length - 1} OFFSET $${params.length}`,
      params,
    );

    res.json({
      data: result.rows.map(mapChannelRow),
      total: countResult.rows[0].c,
      page: parseInt(page, 10) || 1,
      limit: lim,
    });
  } catch (err) {
    if (err.status) return res.status(err.status).json({ error: err.message });
    next(err);
  }
});

// POST /api/admin/packages/:packageId/channels — { channelIds: [1,2,3] }
router.post("/", async (req, res, next) => {
  try {
    const packageId = req.params.packageId;
    const pkg = await assertPackage(packageId);

    if (pkg.includes_all_channels) {
      return res.status(400).json({
        error: 'Paket memakai mode "semua channel". Matikan opsi itu dulu jika ingin memilih channel manual.',
      });
    }

    const { channelIds } = req.body;
    if (!Array.isArray(channelIds) || !channelIds.length) {
      return res.status(400).json({ error: "channelIds wajib berupa array ID channel" });
    }

    const ids = [...new Set(channelIds.map((id) => parseInt(id, 10)).filter(Boolean))];
    const result = await db.query(
      `INSERT INTO package_channels (package_id, channel_id)
       SELECT $1, unnest($2::int[])
       ON CONFLICT DO NOTHING
       RETURNING channel_id`,
      [packageId, ids],
    );

    await clearChannelCache();
    res.json({ added: result.rows.length, requested: ids.length });
  } catch (err) {
    if (err.status) return res.status(err.status).json({ error: err.message });
    next(err);
  }
});

// POST /api/admin/packages/:packageId/channels/by-category — { categories: ["Nasional"] }
router.post("/by-category", async (req, res, next) => {
  try {
    const packageId = req.params.packageId;
    const pkg = await assertPackage(packageId);

    if (pkg.includes_all_channels) {
      return res.status(400).json({ error: 'Paket memakai mode "semua channel".' });
    }

    const { categories } = req.body;
    if (!Array.isArray(categories) || !categories.length) {
      return res.status(400).json({ error: "categories wajib berupa array nama kategori" });
    }

    const result = await db.query(
      `INSERT INTO package_channels (package_id, channel_id)
       SELECT $1, c.id FROM channels c
       WHERE c.active = true AND c.category = ANY($2::text[])
       ON CONFLICT DO NOTHING
       RETURNING channel_id`,
      [packageId, categories],
    );

    await clearChannelCache();
    res.json({ added: result.rows.length, categories });
  } catch (err) {
    if (err.status) return res.status(err.status).json({ error: err.message });
    next(err);
  }
});

// DELETE /api/admin/packages/:packageId/channels/:channelId
router.delete("/:channelId", async (req, res, next) => {
  try {
    const packageId = req.params.packageId;
    await assertPackage(packageId);

    const result = await db.query(
      `DELETE FROM package_channels
       WHERE package_id = $1 AND channel_id = $2
       RETURNING channel_id`,
      [packageId, req.params.channelId],
    );

    if (!result.rows.length) {
      return res.status(404).json({ error: "Channel tidak ada di paket ini" });
    }

    await clearChannelCache();
    res.json({ success: true, channelId: Number(req.params.channelId) });
  } catch (err) {
    if (err.status) return res.status(err.status).json({ error: err.message });
    next(err);
  }
});

// DELETE /api/admin/packages/:packageId/channels — kosongkan semua channel di paket
router.delete("/", async (req, res, next) => {
  try {
    const packageId = req.params.packageId;
    await assertPackage(packageId);

    const result = await db.query(
      "DELETE FROM package_channels WHERE package_id = $1 RETURNING channel_id",
      [packageId],
    );

    await clearChannelCache();
    res.json({ removed: result.rows.length });
  } catch (err) {
    if (err.status) return res.status(err.status).json({ error: err.message });
    next(err);
  }
});

export default router;
