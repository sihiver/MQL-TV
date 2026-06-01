import { Router } from "express";
import { db } from "../../config/database.js";

const router = Router();

const SLUG_RE = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

function mapPackage(row) {
  return {
    id: row.id,
    name: row.name,
    slug: row.slug,
    price: row.price ?? 0,
    maxDevices: row.max_devices ?? 1,
    description: row.description || "",
    features: row.features || "",
    active: row.active,
    sortOrder: row.sort_order ?? 0,
    subscriptionCount: row.subscription_count ?? 0,
    createdAt: row.created_at,
  };
}

const SELECT_BASE = `
  SELECT p.id, p.name, p.slug, p.price, p.max_devices, p.description, p.features,
         p.active, p.sort_order, p.created_at,
         COUNT(s.id)::int AS subscription_count
  FROM packages p
  LEFT JOIN subscriptions s ON LOWER(s.plan) = p.slug
`;

// GET /api/admin/packages
router.get("/", async (req, res, next) => {
  try {
    const { active, search = "" } = req.query;
    const params = [];
    const conditions = ["1=1"];

    if (active === "true") {
      conditions.push("p.active = true");
    } else if (active === "false") {
      conditions.push("p.active = false");
    }

    if (search) {
      params.push(`%${search}%`);
      conditions.push(
        `(p.name ILIKE $${params.length} OR p.slug ILIKE $${params.length})`,
      );
    }

    const result = await db.query(
      `${SELECT_BASE}
       WHERE ${conditions.join(" AND ")}
       GROUP BY p.id
       ORDER BY p.sort_order ASC, p.id ASC`,
      params,
    );

    res.json({ data: result.rows.map(mapPackage), total: result.rows.length });
  } catch (err) {
    next(err);
  }
});

// GET /api/admin/packages/:id
router.get("/:id", async (req, res, next) => {
  try {
    const result = await db.query(
      `${SELECT_BASE} WHERE p.id = $1 GROUP BY p.id`,
      [req.params.id],
    );
    if (!result.rows.length) {
      return res.status(404).json({ error: "Paket tidak ditemukan" });
    }
    res.json(mapPackage(result.rows[0]));
  } catch (err) {
    next(err);
  }
});

// POST /api/admin/packages
router.post("/", async (req, res, next) => {
  try {
    const {
      name,
      slug,
      price = 0,
      maxDevices = 1,
      description = "",
      features = "",
      active = true,
      sortOrder = 0,
    } = req.body;

    if (!name?.trim() || !slug?.trim()) {
      return res.status(400).json({ error: "Nama dan slug wajib diisi" });
    }

    const normalizedSlug = slug.trim().toLowerCase();
    if (!SLUG_RE.test(normalizedSlug)) {
      return res.status(400).json({
        error: "Slug hanya huruf kecil, angka, dan tanda hubung (contoh: premium-plus)",
      });
    }

    const dup = await db.query("SELECT id FROM packages WHERE slug = $1", [normalizedSlug]);
    if (dup.rows.length) {
      return res.status(409).json({ error: "Slug paket sudah digunakan" });
    }

    const result = await db.query(
      `INSERT INTO packages (name, slug, price, max_devices, description, features, active, sort_order)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING id, name, slug, price, max_devices, description, features, active, sort_order, created_at`,
      [
        name.trim(),
        normalizedSlug,
        Number(price) || 0,
        Number(maxDevices) || 1,
        description,
        features,
        Boolean(active),
        Number(sortOrder) || 0,
      ],
    );

    res.status(201).json(mapPackage({ ...result.rows[0], subscription_count: 0 }));
  } catch (err) {
    next(err);
  }
});

// PUT /api/admin/packages/:id
router.put("/:id", async (req, res, next) => {
  try {
    const {
      name,
      slug,
      price,
      maxDevices,
      description,
      features,
      active,
      sortOrder,
    } = req.body;

    const existing = await db.query("SELECT id, slug FROM packages WHERE id = $1", [
      req.params.id,
    ]);
    if (!existing.rows.length) {
      return res.status(404).json({ error: "Paket tidak ditemukan" });
    }

    let normalizedSlug;
    if (slug !== undefined) {
      normalizedSlug = slug.trim().toLowerCase();
      if (!SLUG_RE.test(normalizedSlug)) {
        return res.status(400).json({ error: "Format slug tidak valid" });
      }
      const dup = await db.query("SELECT id FROM packages WHERE slug = $1 AND id != $2", [
        normalizedSlug,
        req.params.id,
      ]);
      if (dup.rows.length) {
        return res.status(409).json({ error: "Slug paket sudah digunakan" });
      }
    }

    const result = await db.query(
      `UPDATE packages SET
         name = COALESCE($1, name),
         slug = COALESCE($2, slug),
         price = COALESCE($3, price),
         max_devices = COALESCE($4, max_devices),
         description = COALESCE($5, description),
         features = COALESCE($6, features),
         active = COALESCE($7, active),
         sort_order = COALESCE($8, sort_order)
       WHERE id = $9
       RETURNING id, name, slug, price, max_devices, description, features, active, sort_order, created_at`,
      [
        name?.trim(),
        normalizedSlug,
        price !== undefined ? Number(price) : null,
        maxDevices !== undefined ? Number(maxDevices) : null,
        description,
        features,
        active !== undefined ? Boolean(active) : null,
        sortOrder !== undefined ? Number(sortOrder) : null,
        req.params.id,
      ],
    );

    const row = await db.query(
      `${SELECT_BASE} WHERE p.id = $1 GROUP BY p.id`,
      [req.params.id],
    );
    res.json(mapPackage(row.rows[0]));
  } catch (err) {
    next(err);
  }
});

// DELETE /api/admin/packages/:id
router.delete("/:id", async (req, res, next) => {
  try {
    const pkg = await db.query("SELECT id, slug FROM packages WHERE id = $1", [req.params.id]);
    if (!pkg.rows.length) {
      return res.status(404).json({ error: "Paket tidak ditemukan" });
    }

    const used = await db.query(
      "SELECT COUNT(*)::int AS c FROM subscriptions WHERE LOWER(plan) = $1",
      [pkg.rows[0].slug],
    );
    if (used.rows[0].c > 0) {
      return res.status(409).json({
        error: `Paket dipakai ${used.rows[0].c} subscription. Nonaktifkan saja atau ubah subscription terlebih dahulu.`,
      });
    }

    await db.query("DELETE FROM packages WHERE id = $1", [req.params.id]);
    res.json({ success: true, id: Number(req.params.id) });
  } catch (err) {
    next(err);
  }
});

export default router;
