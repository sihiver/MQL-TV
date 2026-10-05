import "dotenv/config";
import pg from "pg";

const { Client } = pg;

async function run() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  console.log("Memperbaiki tanggal expires_at pada tabel subscriptions...");

  // Kurangi 1 tahun jika tahun expires_at adalah 2027 dan started_at tahun 2026
  const updateDatesRes = await client.query(`
    UPDATE subscriptions
    SET expires_at = expires_at - INTERVAL '1 year'
    WHERE expires_at >= '2027-01-01' AND expires_at < '2028-01-01'
      AND started_at >= '2026-01-01' AND started_at < '2027-01-01'
    RETURNING id, user_id, started_at, expires_at, status
  `);

  console.log(`Updated ${updateDatesRes.rowCount} subscriptions dates.`);

  // Update status menjadi 'expired' jika tanggal expires_at sudah lewat
  const updateStatusRes = await client.query(`
    UPDATE subscriptions
    SET status = 'expired'
    WHERE status = 'active' AND expires_at <= NOW()
    RETURNING id, user_id, expires_at, status
  `);

  console.log(`Expired ${updateStatusRes.rowCount} subscriptions whose time has passed.`);

  const allRes = await client.query(`
    SELECT s.id, s.user_id, u.email, s.plan, s.status, s.started_at, s.expires_at, (s.expires_at <= NOW()) AS is_expired
    FROM subscriptions s
    JOIN users u ON u.id = s.user_id
    ORDER BY s.id ASC
  `);

  console.table(allRes.rows);

  await client.end();
}

run().catch((e) => {
  console.error("Gagal memperbaiki subscriptions:", e);
  process.exit(1);
});
