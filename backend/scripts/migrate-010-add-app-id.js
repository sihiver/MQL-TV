import fs from "fs";
import path from "path";
import { db } from "../src/config/database.js";

async function runMigration() {
  const sqlPath = path.join(process.cwd(), "migrations", "010_add_app_id_to_app_updates.sql");
  const sql = fs.readFileSync(sqlPath, "utf8");

  try {
    console.log("Menjalankan migrasi 010 (app_id untuk app_updates)...");
    await db.query(sql);
    console.log("Migrasi 010 berhasil!");
    process.exit(0);
  } catch (err) {
    console.error("Gagal menjalankan migrasi 010:", err);
    process.exit(1);
  }
}

runMigration();
