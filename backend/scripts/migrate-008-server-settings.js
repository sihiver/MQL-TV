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
    fs.readFileSync(path.join(__dirname, "../migrations/008_server_settings.sql"), "utf8"),
  );
  console.log("✅ Migrasi server_settings selesai");
  await client.end();
}

migrate().catch((e) => {
  console.error("❌", e.message);
  process.exit(1);
});
