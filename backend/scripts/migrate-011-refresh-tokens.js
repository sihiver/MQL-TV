import "dotenv/config";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import pg from "pg";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const sqlPath = path.join(__dirname, "../migrations/011_refresh_tokens.sql");

const { Client } = pg;

async function migrate() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  const sql = fs.readFileSync(sqlPath, "utf8");
  console.log("Menjalankan migrasi 011 (refresh_tokens persistent table)...");
  await client.query(sql);

  console.log("✅ Migrasi 011 berhasil!");
  await client.end();
}

migrate().catch((err) => {
  console.error("❌ Gagal menjalankan migrasi 011:", err.message);
  process.exit(1);
});
