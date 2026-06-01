import "dotenv/config";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import pg from "pg";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const sqlPath = path.join(__dirname, "../migrations/002_packages.sql");

const { Client } = pg;

async function migrate() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  const sql = fs.readFileSync(sqlPath, "utf8");
  await client.query(sql);

  const { rows } = await client.query(
    `SELECT column_name FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'packages'
     ORDER BY ordinal_position`,
  );

  console.log("✅ Migrasi packages selesai. Kolom:", rows.map((r) => r.column_name).join(", "));
  await client.end();
}

migrate().catch((err) => {
  console.error("❌ Migrasi packages gagal:", err.message);
  process.exit(1);
});
