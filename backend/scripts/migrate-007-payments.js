import "dotenv/config";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import pg from "pg";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const { Client } = pg;

async function migrate() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();
  await client.query(
    fs.readFileSync(path.join(__dirname, "../migrations/007_subscription_payments.sql"), "utf8"),
  );
  const { rows } = await client.query("SELECT COUNT(*)::int AS c FROM subscription_payments");
  console.log(`✅ Migrasi subscription_payments selesai (${rows[0].c} pembayaran)`);
  await client.end();
}

migrate().catch((e) => {
  console.error("❌", e.message);
  process.exit(1);
});
