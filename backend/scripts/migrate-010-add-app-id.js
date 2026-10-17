import "dotenv/config";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import pg from "pg";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const sqlPath = path.join(__dirname, "../migrations/010_add_app_id_to_app_updates.sql");

const { Client } = pg;

async function runMigration() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  const sql = fs.readFileSync(sqlPath, "utf8");
  console.log("Menjalankan migrasi 010 (app_id untuk app_updates)...");
  await client.query(sql);

  console.log("✅ Migrasi 010 berhasil!");
  await client.end();
}

runMigration().catch((err) => {
  console.error("❌ Gagal menjalankan migrasi 010:", err.message);
  process.exit(1);
});
