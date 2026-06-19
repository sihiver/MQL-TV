import "dotenv/config";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import pg from "pg";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const sql9Path = path.join(__dirname, "../migrations/009_app_updates.sql");
const sql10Path = path.join(__dirname, "../migrations/010_add_app_id_to_app_updates.sql");

const { Client } = pg;

async function migrate() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  const sql9 = fs.readFileSync(sql9Path, "utf8");
  await client.query(sql9);
  console.log("✅ Migrasi 009 app_updates berhasil.");

  const sql10 = fs.readFileSync(sql10Path, "utf8");
  await client.query(sql10);
  console.log("✅ Migrasi 010 add_app_id_to_app_updates berhasil.");

  await client.end();
}

migrate().catch((err) => {
  console.error("❌ Migrasi gagal:", err.message);
  process.exit(1);
});
