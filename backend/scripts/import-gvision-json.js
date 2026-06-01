import "dotenv/config";
import fs from "fs";
import path from "path";
import { importGvisionChannels } from "../src/services/gvisionImport.js";

const filePath = process.argv[2];
const mode = process.argv.includes("--append") ? "append" : "replace";

if (!filePath) {
  console.error("Usage: node scripts/import-gvision-json.js <path-to.json> [--append]");
  process.exit(1);
}

const abs = path.resolve(filePath);
if (!fs.existsSync(abs)) {
  console.error("❌ File tidak ditemukan:", abs);
  process.exit(1);
}

const raw = fs.readFileSync(abs, "utf8");
const data = JSON.parse(raw);

console.log(`📂 Membaca ${abs} …`);
importGvisionChannels(data, { mode })
  .then((r) => {
    console.log(`✅ Import selesai (${r.mode})`);
    console.log(`   Diimpor: ${r.imported} channel`);
    console.log(`   Total di DB: ${r.totalInDb}`);
    if (r.meta?.source) console.log(`   Sumber: ${r.meta.source}`);
    process.exit(0);
  })
  .catch((err) => {
    console.error("❌ Import gagal:", err.message);
    process.exit(1);
  });
