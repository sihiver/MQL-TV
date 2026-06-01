import "dotenv/config";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import pg from "pg";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const sqlPath = path.join(__dirname, "../migrations/003_channels_extra.sql");

const { Client } = pg;

async function migrate() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();
  const sql = fs.readFileSync(sqlPath, "utf8");
  await client.query(sql);
  console.log("✅ Migrasi channels (DRM/meta) selesai");
  await client.end();
}

migrate().catch((err) => {
  console.error("❌ Migrasi gagal:", err.message);
  process.exit(1);
});
