import { parseIptvStreamUrl } from "../utils/iptvStreamUrl.js";
import { importChannelRows } from "./channelImport.js";

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

/**
 * @param {object} data - parsed JSON
 * @param {{ mode?: 'replace'|'append' }} options
 */
export async function importGvisionChannels(data, { mode = "replace" } = {}) {
  const { rows, meta } = parseGvisionJson(data);
  return importChannelRows(rows, { mode, meta });
}
