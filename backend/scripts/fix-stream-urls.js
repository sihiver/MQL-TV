/**
 * Normalisasi stream_url yang masih berisi |user-agent=...|referer=...
 */
import "dotenv/config";
import { db } from "../src/config/database.js";
import { parseIptvStreamUrl } from "../src/utils/iptvStreamUrl.js";

async function main() {
  const { rows } = await db.query(
    `SELECT id, stream_url, user_agent, referer FROM channels WHERE stream_url LIKE '%|%'`,
  );

  if (!rows.length) {
    console.log("Tidak ada stream_url dengan format pipe — selesai.");
    process.exit(0);
  }

  for (const row of rows) {
    const parsed = parseIptvStreamUrl(row.stream_url);
    const userAgent = row.user_agent || parsed.userAgent;
    const referer = row.referer || parsed.referer;

    await db.query(
      `UPDATE channels SET stream_url = $1, user_agent = COALESCE($2, user_agent), referer = COALESCE($3, referer) WHERE id = $4`,
      [parsed.url, userAgent, referer, row.id],
    );
    console.log(`#${row.id} → ${parsed.url}`);
  }

  console.log(`Diperbarui ${rows.length} channel.`);
  process.exit(0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
