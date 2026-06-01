import "dotenv/config";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import pg from "pg";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const sqlPath = path.join(__dirname, "../migrations/004_package_channels.sql");
const { Client } = pg;

async function migrate() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();
  await client.query(fs.readFileSync(sqlPath, "utf8"));
  console.log("✅ Migrasi package_channels selesai");
  await client.end();
}

migrate().catch((err) => {
  console.error("❌", err.message);
  process.exit(1);
});
