import "dotenv/config";
import pg from "pg";

const url = new URL(process.env.DATABASE_URL);
const dbName = url.pathname.slice(1);

url.pathname = "/postgres";

const { Client } = pg;

async function createDatabase() {
  const client = new Client({ connectionString: url.toString() });
  await client.connect();

  const exists = await client.query(
    "SELECT 1 FROM pg_database WHERE datname = $1",
    [dbName],
  );

  if (exists.rows.length) {
    console.log(`ℹ️  Database "${dbName}" sudah ada`);
  } else {
    await client.query(`CREATE DATABASE "${dbName}"`);
    console.log(`✅ Database "${dbName}" dibuat`);
  }

  await client.end();
}

createDatabase().catch((err) => {
  console.error("❌ Gagal membuat database:", err.message);
  process.exit(1);
});
