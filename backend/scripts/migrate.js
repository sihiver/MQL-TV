import "dotenv/config";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import pg from "pg";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const sqlPath = path.join(__dirname, "../migrations/001_init.sql");

const { Client } = pg;

async function migrate() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  const sql = fs.readFileSync(sqlPath, "utf8");
  try {
    await client.query(sql);
  } catch (err) {
    if (err.message && err.message.includes("already exists")) {
      console.log("ℹ️  Tabel dasar sudah ada (skip 001_init.sql).");
    } else {
      throw err;
    }
  }

  const { rows } = await client.query(`
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = 'public' ORDER BY table_name
  `);

  console.log("✅ Migrasi selesai. Tabel:", rows.map((r) => r.table_name).join(", "));
  await client.end();
}

migrate().catch((err) => {
  console.error("❌ Migrasi gagal:", err.message);
  process.exit(1);
});
