import fs from "fs";
import path from "path";
import { db } from "../src/config/database.js";

async function runMigration() {
  const sqlPath = path.join(process.cwd(), "migrations", "011_refresh_tokens.sql");
  const sql = fs.readFileSync(sqlPath, "utf8");

  try {
    console.log("Menjalankan migrasi 011 (refresh_tokens persistent table)...");
    await db.query(sql);
    console.log("Migrasi 011 berhasil!");
    process.exit(0);
  } catch (err) {
    console.error("Gagal menjalankan migrasi 011:", err);
    process.exit(1);
  }
}

runMigration();
