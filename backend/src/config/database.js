import pg from "pg";

const { Pool } = pg;

export const db = new Pool({
  connectionString: process.env.DATABASE_URL,
  max: 20,
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 5_000,
});

db.on("error", (err) => {
  console.error("PostgreSQL pool error:", err.message);
});

export async function testConnection() {
  const client = await db.connect();
  try {
    const { rows } = await client.query("SELECT NOW() AS now, current_database() AS db");
    return rows[0];
  } finally {
    client.release();
  }
}

export async function closePool() {
  await db.end();
}
