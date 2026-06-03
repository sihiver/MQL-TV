import axios from "axios";
import { parseM3UContent } from "./m3uParser.js";
import { importChannelRows } from "./channelImport.js";
import { parseIptvStreamUrl } from "../utils/iptvStreamUrl.js";

export function parseM3uToRows(content) {
  const parsed = parseM3UContent(content);
  if (!parsed.length) {
    throw new Error("Tidak ada channel valid di file M3U (#EXTINF + URL stream)");
  }

  return parsed.map((ch) => {
    const parsedUrl = parseIptvStreamUrl(ch.url);
    return {
      name: String(ch.name).slice(0, 255),
      stream_url: parsedUrl.url,
      category: String(ch.category || "Lainnya").slice(0, 80),
      logo_url: ch.logo || null,
      drm_type: null,
      drm_key: null,
      user_agent: parsedUrl.userAgent || null,
      referer: parsedUrl.referer || null,
      source_category: "m3u",
      epg_id: ch.epgId || null,
    };
  });
}

export async function fetchM3uContent(url) {
  const { data } = await axios.get(url, {
    timeout: 30_000,
    responseType: "text",
    validateStatus: (s) => s >= 200 && s < 400,
  });
  const text = typeof data === "string" ? data : String(data);
  if (!text.includes("#EXTM3U") && !text.includes("#EXTINF")) {
    throw new Error("URL bukan playlist M3U valid");
  }
  return text;
}

/**
 * @param {{ content?: string, url?: string, mode?: 'replace'|'append' }} options
 */
export async function importM3uChannels({ content, url, mode = "replace" } = {}) {
  let text = content?.trim();
  if (!text && url) {
    text = await fetchM3uContent(url.trim());
  }
  if (!text) {
    throw new Error("Isi playlist M3U atau URL wajib diisi");
  }

  const rows = parseM3uToRows(text);
  const categories = new Set(rows.map((r) => r.category));

  return importChannelRows(rows, {
    mode,
    meta: {
      source: url || "m3u-upload",
      channelCount: rows.length,
      categoryCount: categories.size,
    },
  });
}
