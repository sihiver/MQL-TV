import { Router } from "express";
import bcrypt from "bcryptjs";
import { db } from "../../config/database.js";

const router = Router();

const USER_FIELDS = `
  u.id, u.name, u.email, u.plan, u.role, u.banned, u.created_at,
  COUNT(d.id)::int AS devices
`;

const userGroupBy = "GROUP BY u.id";

function mapUser(row) {
  return {
    id: row.id,
    name: row.name,
    email: row.email,
    plan: row.plan,
    role: row.role,
    status: row.banned ? "banned" : "active",
    banned: row.banned,
    devices: row.devices ?? 0,
    joined: row.created_at
      ? new Date(row.created_at).toISOString().slice(0, 10)
      : "—",
    lastSeen: "—",
    revenue: 0,
  };
}

// GET /api/admin/users
router.get("/", async (req, res, next) => {
  try {
    const { search = "", filter = "Semua" } = req.query;
    const params = [];
    const conditions = ["1=1"];

    if (search) {
      params.push(`%${search}%`);
      conditions.push(`(u.name ILIKE $${params.length} OR u.email ILIKE $${params.length})`);
    }
    if (filter === "Banned") {
      conditions.push("u.banned = true");
    } else if (filter !== "Semua") {
      conditions.push("u.banned = false");
      params.push(filter.toLowerCase());
      conditions.push(`u.plan = $${params.length}`);
    }

    const where = conditions.join(" AND ");
    const result = await db.query(
      `SELECT ${USER_FIELDS}
       FROM users u
       LEFT JOIN devices d ON d.user_id = u.id
       WHERE ${where}
       ${userGroupBy}
       ORDER BY u.created_at DESC`,
      params,
    );

    res.json({ data: result.rows.map(mapUser), total: result.rows.length });
  } catch (err) {
    next(err);
  }
});

// GET /api/admin/users/:id
router.get("/:id", async (req, res, next) => {
  try {
    const result = await db.query(
      `SELECT ${USER_FIELDS}
       FROM users u
       LEFT JOIN devices d ON d.user_id = u.id
       WHERE u.id = $1
       ${userGroupBy}`,
      [req.params.id],
    );
    if (!result.rows.length) return res.status(404).json({ error: "User tidak ditemukan" });
    res.json(mapUser(result.rows[0]));
  } catch (err) {
    next(err);
  }
});

// POST /api/admin/users
router.post("/", async (req, res, next) => {
  try {
    const { name, email, password, plan = "free", role = "user" } = req.body;
    if (!name?.trim() || !email?.trim() || !password) {
      return res.status(400).json({ error: "Nama, email, dan password wajib diisi" });
    }

    const exists = await db.query("SELECT id FROM users WHERE email = $1", [email.trim()]);
    if (exists.rows.length) return res.status(409).json({ error: "Email sudah terdaftar" });

    const hash = await bcrypt.hash(password, 12);
    const result = await db.query(
      `INSERT INTO users (name, email, password_hash, plan, role, banned)
       VALUES ($1, $2, $3, $4, $5, false)
       RETURNING id, name, email, plan, role, banned, created_at`,
      [name.trim(), email.trim().toLowerCase(), hash, plan, role],
    );

    res.status(201).json(mapUser({ ...result.rows[0], devices: 0 }));
  } catch (err) {
    next(err);
  }
});

// PUT /api/admin/users/:id
router.put("/:id", async (req, res, next) => {
  try {
    const { name, email, plan, role, banned, password } = req.body;
    const id = req.params.id;

    const current = await db.query("SELECT * FROM users WHERE id = $1", [id]);
    if (!current.rows.length) return res.status(404).json({ error: "User tidak ditemukan" });

    if (email && email !== current.rows[0].email) {
      const dup = await db.query("SELECT id FROM users WHERE email = $1 AND id != $2", [
        email.trim().toLowerCase(),
        id,
      ]);
      if (dup.rows.length) return res.status(409).json({ error: "Email sudah dipakai user lain" });
    }

    const hash = password ? await bcrypt.hash(password, 12) : null;

    const result = await db.query(
      `UPDATE users SET
         name = COALESCE($1, name),
         email = COALESCE($2, email),
         plan = COALESCE($3, plan),
         role = COALESCE($4, role),
         banned = COALESCE($5, banned),
         password_hash = COALESCE($6, password_hash)
       WHERE id = $7
       RETURNING id, name, email, plan, role, banned, created_at`,
      [
        name?.trim(),
        email?.trim().toLowerCase(),
        plan,
        role,
        typeof banned === "boolean" ? banned : null,
        hash,
        id,
      ],
    );

    const devices = await db.query("SELECT COUNT(*)::int AS c FROM devices WHERE user_id = $1", [id]);
    res.json(mapUser({ ...result.rows[0], devices: devices.rows[0].c }));
  } catch (err) {
    next(err);
  }
});

// PATCH /api/admin/users/:id/ban — toggle atau set banned
router.patch("/:id/ban", async (req, res, next) => {
  try {
    const banned = req.body.banned ?? true;
    const result = await db.query(
      `UPDATE users SET banned = $1 WHERE id = $2
       RETURNING id, name, email, plan, role, banned, created_at`,
      [banned, req.params.id],
    );
    if (!result.rows.length) return res.status(404).json({ error: "User tidak ditemukan" });

    const devices = await db.query("SELECT COUNT(*)::int AS c FROM devices WHERE user_id = $1", [
      req.params.id,
    ]);
    res.json(mapUser({ ...result.rows[0], devices: devices.rows[0].c }));
  } catch (err) {
    next(err);
  }
});

// DELETE /api/admin/users/:id
router.delete("/:id", async (req, res, next) => {
  try {
    const result = await db.query("DELETE FROM users WHERE id = $1 RETURNING id", [req.params.id]);
    if (!result.rows.length) return res.status(404).json({ error: "User tidak ditemukan" });
    res.json({ success: true, id: result.rows[0].id });
  } catch (err) {
    next(err);
  }
});

export default router;
