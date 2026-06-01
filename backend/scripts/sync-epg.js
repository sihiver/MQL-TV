import "dotenv/config";
import { syncEpgFromUrl, getEpgSourceUrl } from "../src/services/epgSync.js";

const url = process.argv[2] || getEpgSourceUrl();

console.log(`📡 Sinkron EPG dari ${url} …`);

syncEpgFromUrl(url)
  .then((r) => {
    console.log("✅ Selesai dalam", r.durationMs, "ms");
    console.log("   XMLTV channels:", r.xmltvChannels);
    console.log("   Cocok dengan DB:", r.channelsMatched, `(epg_id diperbarui: ${r.epgIdsUpdated})`);
    console.log("   Program diimpor:", r.programmesImported);
    console.log("   Dilewati:", r.programmesSkipped);
  })
  .catch((e) => {
    console.error("❌", e.message);
    process.exit(1);
  });
