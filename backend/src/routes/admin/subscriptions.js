import { Router } from "express";
import { db } from "../../config/database.js";
import { recordSubscriptionPayment } from "../../services/subscriptionPayments.js";

const router = Router();

function fmtDate(d) {
  if (!d) return "—";
  return new Date(d).toISOString().slice(0, 10);
}

function capitalizePlan(plan) {
  if (!plan) return "";
  const p = plan.toLowerCase();
  return p.charAt(0).toUpperCase() + p.slice(1);
}

function mapSubscription(row) {
  const planKey = (row.plan || "free").toLowerCase();
  return {
    id: row.id,
    userId: row.user_id,
    user: row.user_name || "—",
    userEmail: row.user_email || "",
    plan: row.package_name || capitalizePlan(row.plan),
    planKey,
    price: row.package_price ?? 0,
    status: row.status,
    start: fmtDate(row.started_at),
    end: fmtDate(row.expires_at),
    startedAt: row.started_at,
    expiresAt: row.expires_at,
    maxDevices: row.max_devices ?? 1,
    method: "Manual",
  };
}

async function resolvePackage(slug) {
  const key = (slug || "").toLowerCase();
  const result = await db.query(
    "SELECT slug, name, price, max_devices, active FROM packages WHERE slug = $1",
    [key],
  );
  return result.rows[0] || null;
}

const SELECT_JOIN = `
  SELECT s.id, s.user_id, s.plan, s.status, s.started_at, s.expires_at, s.max_devices,
         u.name AS user_name, u.email AS user_email,
         p.name AS package_name, p.price AS package_price
  FROM subscriptions s
  JOIN users u ON u.id = s.user_id
  LEFT JOIN packages p ON p.slug = LOWER(s.plan)
`;

// GET /api/admin/subscriptions
router.get("/", async (req, res, next) => {
  try {
    const { filter = "Semua", search = "" } = req.query;
    const params = [];
    const conditions = ["1=1"];

    if (search) {
      params.push(`%${search}%`);
      conditions.push(`(u.name ILIKE $${params.length} OR u.email ILIKE $${params.length})`);
    }

    const f = filter.toLowerCase();
    if (f === "active" || f === "expired") {
      params.push(f);
      conditions.push(`s.status = $${params.length}`);
    } else if (filter !== "Semua") {
      params.push(filter.toLowerCase());
      conditions.push(`LOWER(s.plan) = $${params.length}`);
    }

    const result = await db.query(
      `${SELECT_JOIN}
       WHERE ${conditions.join(" AND ")}
       ORDER BY s.id DESC`,
      params,
    );

    const data = result.rows.map(mapSubscription);
    const activeRevenue = data
      .filter((s) => s.status === "active")
      .reduce((a, s) => a + s.price, 0);

    res.json({ data, total: data.length, activeRevenue });
  } catch (err) {
    next(err);
  }
});

// GET /api/admin/subscriptions/:id
router.get("/:id", async (req, res, next) => {
  try {
    const result = await db.query(`${SELECT_JOIN} WHERE s.id = $1`, [req.params.id]);
    if (!result.rows.length) return res.status(404).json({ error: "Subscription tidak ditemukan" });
    res.json(mapSubscription(result.rows[0]));
  } catch (err) {
    next(err);
  }
});

// POST /api/admin/subscriptions
router.post("/", async (req, res, next) => {
  try {
    const { userId, plan, status = "active", startedAt, expiresAt, maxDevices = 1 } = req.body;

    if (!userId || !plan || !expiresAt) {
      return res.status(400).json({ error: "User, plan, dan tanggal berakhir wajib diisi" });
    }

    const user = await db.query("SELECT id FROM users WHERE id = $1", [userId]);
    if (!user.rows.length) return res.status(404).json({ error: "User tidak ditemukan" });

    const pkg = await resolvePackage(plan);
    if (!pkg) {
      return res.status(400).json({ error: "Paket tidak ditemukan. Buat paket di menu Paket terlebih dahulu." });
    }
    if (!pkg.active) {
      return res.status(400).json({ error: "Paket tidak aktif" });
    }

    const devices = maxDevices ?? pkg.max_devices ?? 1;
    const start = startedAt || new Date().toISOString();
    const result = await db.query(
      `INSERT INTO subscriptions (user_id, plan, status, started_at, expires_at, max_devices)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id, user_id, plan, status, started_at, expires_at, max_devices`,
      [userId, pkg.slug, status, start, expiresAt, devices],
    );

    const row = await db.query(`${SELECT_JOIN} WHERE s.id = $1`, [result.rows[0].id]);
    const mapped = mapSubscription(row.rows[0]);

    await recordSubscriptionPayment(db, {
      userId,
      subscriptionId: mapped.id,
      plan: pkg.slug,
      amount: pkg.price ?? 0,
      paymentType: "new",
      method: req.body.method || "manual",
    });

    res.status(201).json(mapped);
  } catch (err) {
    next(err);
  }
});

// PUT /api/admin/subscriptions/:id
router.put("/:id", async (req, res, next) => {
  try {
    const { plan, status, startedAt, expiresAt, maxDevices } = req.body;

    let planSlug;
    if (plan !== undefined) {
      const pkg = await resolvePackage(plan);
      if (!pkg) {
        return res.status(400).json({ error: "Paket tidak ditemukan" });
      }
      planSlug = pkg.slug;
    }

    const result = await db.query(
      `UPDATE subscriptions SET
         plan = COALESCE($1, plan),
         status = COALESCE($2, status),
         started_at = COALESCE($3, started_at),
         expires_at = COALESCE($4, expires_at),
         max_devices = COALESCE($5, max_devices)
       WHERE id = $6
       RETURNING id`,
      [
        planSlug,
        status,
        startedAt,
        expiresAt,
        maxDevices,
        req.params.id,
      ],
    );

    if (!result.rows.length) return res.status(404).json({ error: "Subscription tidak ditemukan" });

    const row = await db.query(`${SELECT_JOIN} WHERE s.id = $1`, [req.params.id]);
    res.json(mapSubscription(row.rows[0]));
  } catch (err) {
    next(err);
  }
});

// PATCH /api/admin/subscriptions/:id/renew — perpanjang 30 hari
router.patch("/:id/renew", async (req, res, next) => {
  try {
    const months = req.body.months ?? 1;
    const days = months * 30;

    const current = await db.query(
      `SELECT s.expires_at, s.status, s.user_id, s.plan, p.price AS package_price
       FROM subscriptions s
       LEFT JOIN packages p ON p.slug = LOWER(s.plan)
       WHERE s.id = $1`,
      [req.params.id],
    );
    if (!current.rows.length) return res.status(404).json({ error: "Subscription tidak ditemukan" });

    const sub = current.rows[0];
    const base = new Date(sub.expires_at);
    const from = base > new Date() ? base : new Date();
    from.setDate(from.getDate() + days);

    const result = await db.query(
      `UPDATE subscriptions SET expires_at = $1, status = 'active' WHERE id = $2 RETURNING id`,
      [from.toISOString(), req.params.id],
    );

    const row = await db.query(`${SELECT_JOIN} WHERE s.id = $1`, [result.rows[0].id]);
    const mapped = mapSubscription(row.rows[0]);

    await recordSubscriptionPayment(db, {
      userId: sub.user_id,
      subscriptionId: mapped.id,
      plan: sub.plan,
      amount: (sub.package_price ?? 0) * months,
      paymentType: "renew",
      method: req.body.method || "manual",
      notes: months > 1 ? `Perpanjang ${months} bulan` : null,
    });

    res.json(mapped);
  } catch (err) {
    next(err);
  }
});

// DELETE /api/admin/subscriptions/:id
router.delete("/:id", async (req, res, next) => {
  try {
    const result = await db.query("DELETE FROM subscriptions WHERE id = $1 RETURNING id", [
      req.params.id,
    ]);
    if (!result.rows.length) return res.status(404).json({ error: "Subscription tidak ditemukan" });
    res.json({ success: true, id: result.rows[0].id });
  } catch (err) {
    next(err);
  }
});

export default router;
