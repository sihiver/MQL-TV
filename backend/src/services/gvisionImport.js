import { db } from "../config/database.js";
import { redis } from "../config/redis.js";
import { parseIptvStreamUrl } from "../utils/iptvStreamUrl.js";

const BATCH_SIZE = 200;

/**
 * Flatten format gvision_channels.json → baris siap insert.
 */
export function parseGvisionJson(data) {
  if (!data || typeof data !== "object") {
    throw new Error("JSON tidak valid");
  }

  const rows = [];
  const categories = data.categories || [];

  for (const cat of categories) {
    const sourceCategory = cat.code || cat.name || "unknown";
    for (const ch of cat.channels || []) {
      if (!ch?.stream_url || !ch?.name) continue;

      const parsed = parseIptvStreamUrl(ch.stream_url);
      rows.push({
        name: String(ch.name).slice(0, 255),
        stream_url: parsed.url,
        category: String(ch.group || cat.name || "Lainnya").slice(0, 80),
        logo_url: ch.logo_url || null,
        drm_type: ch.drm_type || null,
        drm_key: ch.drm_key || null,
        user_agent: ch.user_agent || parsed.userAgent || null,
        referer: ch.referer || parsed.referer || null,
        source_category: String(sourceCategory).slice(0, 80),
      });
    }
  }

  if (!rows.length) {
    throw new Error("Tidak ada channel di JSON (format categories[].channels[])");
  }

  return {
    rows,
    meta: {
      source: data.source || null,
      channelCount: data.channel_count ?? rows.length,
      categoryCount: data.category_count ?? categories.length,
    },
  };
}

async function clearChannelCache() {
  const keys = await redis.keys("channels:*");
  if (keys.length) await redis.del(...keys);
}

/**
 * @param {object} data - parsed JSON
 * @param {{ mode?: 'replace'|'append' }} options
 */
export async function importGvisionChannels(data, { mode = "replace" } = {}) {
  const { rows, meta } = parseGvisionJson(data);

  const client = await db.connect();
  try {
    await client.query("BEGIN");

    if (mode === "replace") {
      await client.query("TRUNCATE channels RESTART IDENTITY CASCADE");
    }

    for (let i = 0; i < rows.length; i += BATCH_SIZE) {
      const batch = rows.slice(i, i + BATCH_SIZE);
      const values = [];
      const params = [];

      batch.forEach((ch, j) => {
        const b = j * 9;
        values.push(
          `($${b + 1},$${b + 2},$${b + 3},$${b + 4},$${b + 5},$${b + 6},$${b + 7},$${b + 8},$${b + 9}, true, true)`,
        );
        params.push(
          ch.name,
          ch.stream_url,
          ch.category,
          ch.logo_url,
          ch.drm_type,
          ch.drm_key,
          ch.user_agent,
          ch.referer,
          ch.source_category,
        );
      });

      await client.query(
        `INSERT INTO channels (
           name, stream_url, category, logo_url,
           drm_type, drm_key, user_agent, referer, source_category,
           active, is_live
         ) VALUES ${values.join(", ")}`,
        params,
      );
    }

    await client.query("COMMIT");
  } catch (err) {
    await client.query("ROLLBACK");
    throw err;
  } finally {
    client.release();
  }

  await clearChannelCache();

  const { rows: countRows } = await db.query("SELECT COUNT(*)::int AS c FROM channels");
  return {
    imported: rows.length,
    totalInDb: countRows[0].c,
    mode,
    meta,
  };
}
