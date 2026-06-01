import "dotenv/config";
import bcrypt from "bcryptjs";
import pg from "pg";

const { Client } = pg;

const users = [
  { name: "Ahmad Rizki", email: "ahmad@email.com", plan: "premium", role: "user", password: "password123" },
  { name: "Budi Santoso", email: "budi@email.com", plan: "basic", role: "user", password: "password123" },
  { name: "Citra Dewi", email: "citra@email.com", plan: "premium", role: "user", password: "password123" },
  { name: "Admin NusaVision", email: "admin@nusavision.id", plan: "premium", role: "admin", password: "admin123" },
];

async function seed() {
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  await client.connect();

  for (const u of users) {
    const exists = await client.query("SELECT id FROM users WHERE email = $1", [u.email]);
    if (exists.rows.length) {
      console.log(`⏭️  ${u.email} sudah ada`);
      continue;
    }
    const hash = await bcrypt.hash(u.password, 12);
    await client.query(
      `INSERT INTO users (name, email, password_hash, plan, role, banned)
       VALUES ($1, $2, $3, $4, $5, false)`,
      [u.name, u.email, hash, u.plan, u.role],
    );
    console.log(`✅ ${u.email}`);
  }

  await client.end();
  console.log("Selesai.");
}

seed().catch((e) => {
  console.error(e);
  process.exit(1);
});
