import "dotenv/config";
import pg from "pg";

const { Client } = pg;

const DEFAULT_PACKAGES = [
  {
    name: "Premium",
    slug: "premium",
    price: 150_000,
    max_devices: 3,
    description: "Semua channel HD + catch-up",
    features: "HD,4K,Catch-up,EPG",
    sort_order: 1,
  },
  {
    name: "Basic",
    slug: "basic",
    price: 50_000,
    max_devices: 2,
    description: "Channel standar",
    features: "SD,EPG",
    sort_order: 2,
  },
  {
    name: "Free",
    slug: "free",
    price: 0,
    max_devices: 1,
    description: "Trial terbatas",
    features: "Trial",
    sort_order: 3,
  },
];

async function seed() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  for (const p of DEFAULT_PACKAGES) {
    await client.query(
      `INSERT INTO packages (name, slug, price, max_devices, description, features, sort_order)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       ON CONFLICT (slug) DO UPDATE SET
         name = EXCLUDED.name,
         price = EXCLUDED.price,
         max_devices = EXCLUDED.max_devices,
         description = EXCLUDED.description,
         features = EXCLUDED.features,
         sort_order = EXCLUDED.sort_order`,
      [p.name, p.slug, p.price, p.max_devices, p.description, p.features, p.sort_order],
    );
  }

  const { rows } = await client.query("SELECT slug, name, price FROM packages ORDER BY sort_order");
  console.log("✅ Seed paket:", rows.map((r) => `${r.slug} (${r.name}) Rp${r.price}`).join(", "));
  await client.end();
}

seed().catch((err) => {
  console.error("❌ Seed paket gagal:", err.message);
  process.exit(1);
});
