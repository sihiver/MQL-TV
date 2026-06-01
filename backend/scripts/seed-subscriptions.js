import "dotenv/config";
import pg from "pg";

const { Client } = pg;

async function seed() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  const users = await client.query(
    "SELECT id, email, plan FROM users WHERE role = 'user' ORDER BY id LIMIT 6",
  );

  if (!users.rows.length) {
    console.log("⚠️  Tidak ada user. Jalankan npm run db:seed-users dulu.");
    await client.end();
    return;
  }

  const now = new Date();
  const nextYear = new Date(now);
  nextYear.setFullYear(nextYear.getFullYear() + 1);
  const lastMonth = new Date(now);
  lastMonth.setMonth(lastMonth.getMonth() - 1);

  for (const u of users.rows) {
    const exists = await client.query(
      "SELECT id FROM subscriptions WHERE user_id = $1 LIMIT 1",
      [u.id],
    );
    if (exists.rows.length) {
      console.log(`⏭️  ${u.email} sudah punya subscription`);
      continue;
    }

    const isExpired = u.id % 4 === 0;
    await client.query(
      `INSERT INTO subscriptions (user_id, plan, status, started_at, expires_at, max_devices)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [
        u.id,
        u.plan || "basic",
        isExpired ? "expired" : "active",
        isExpired ? lastMonth.toISOString() : now.toISOString(),
        isExpired ? lastMonth.toISOString() : nextYear.toISOString(),
        u.plan === "premium" ? 3 : 1,
      ],
    );
    console.log(`✅ ${u.email} — ${isExpired ? "expired" : "active"}`);
  }

  await client.end();
  console.log("Selesai.");
}

seed().catch((e) => {
  console.error(e);
  process.exit(1);
});
