import axios from "axios";
import { db } from "../config/database.js";
import { getEpgSourceUrl } from "./epgConfig.js";
import { parseXmltv, parseXmltvDateTime } from "./xmltvParser.js";
import { matchChannelsForSync } from "./epgMapping.js";

const BATCH_SIZE = 300;
const DAYS_FORWARD = 8;
const DAYS_BACK = 1;

async function insertProgrammeBatch(rows) {
  if (!rows.length) return;

  for (let i = 0; i < rows.length; i += BATCH_SIZE) {
    const batch = rows.slice(i, i + BATCH_SIZE);
    const values = [];
    const params = [];

    batch.forEach((p, j) => {
      const b = j * 6;
      values.push(`($${b + 1},$${b + 2},$${b + 3},$${b + 4},$${b + 5},$${b + 6})`);
      params.push(p.channelId, p.title, p.description, p.start, p.end, p.category);
    });

    await db.query(
      `INSERT INTO epg_programs (channel_id, title, description, start_time, end_time, category)
       VALUES ${values.join(", ")}`,
      params,
    );
  }
}

/**
 * Sinkron EPG dari epg.pw (XMLTV Indonesia).
 */
export async function syncEpgFromUrl(url = getEpgSourceUrl()) {
  const started = Date.now();

  const { data: xml } = await axios.get(url, {
    timeout: 120_000,
    responseType: "text",
    headers: { "User-Agent": "NusaVision-EPG-Sync/1.0" },
  });

  const { channels: xmltvChannels, programmes } = await parseXmltv(xml);
  const { mapping, matched, updatedEpgIds } = await matchChannelsForSync(xmltvChannels);

  const windowStart = new Date();
  windowStart.setDate(windowStart.getDate() - DAYS_BACK);
  const windowEnd = new Date();
  windowEnd.setDate(windowEnd.getDate() + DAYS_FORWARD);

  await db.query("DELETE FROM epg_programs");

  const toInsert = [];
  let skipped = 0;

  for (const p of programmes) {
    const channelIds = mapping.get(p.channel);
    if (!channelIds?.length) {
      skipped += 1;
      continue;
    }

    const start = parseXmltvDateTime(p.start);
    const end = parseXmltvDateTime(p.stop);
    if (!start || !end || !p.title) {
      skipped += 1;
      continue;
    }

    if (end < windowStart || start > windowEnd) {
      skipped += 1;
      continue;
    }

    const row = {
      title: p.title.slice(0, 200),
      description: (p.description || "").slice(0, 2000),
      start,
      end,
      category: (p.category || "").slice(0, 50) || null,
    };

    for (const channelId of channelIds) {
      toInsert.push({ channelId, ...row });
    }
  }

  await insertProgrammeBatch(toInsert);

  const meta = await db.query(
    `INSERT INTO epg_sync_meta (source_url, xmltv_channels, channels_matched, programmes_imported, programmes_skipped)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING synced_at`,
    [url, xmltvChannels.length, matched, toInsert.length, skipped],
  );

  return {
    sourceUrl: url,
    syncedAt: meta.rows[0].synced_at,
    durationMs: Date.now() - started,
    xmltvChannels: xmltvChannels.length,
    channelsMatched: matched,
    epgIdsUpdated: updatedEpgIds,
    programmesImported: toInsert.length,
    programmesSkipped: skipped,
  };
}

export async function getLastEpgSync() {
  const result = await db.query(
    `SELECT source_url, synced_at, xmltv_channels, channels_matched,
            programmes_imported, programmes_skipped
     FROM epg_sync_meta
     ORDER BY id DESC
     LIMIT 1`,
  );
  return result.rows[0] || null;
}
