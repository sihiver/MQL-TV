import axios from "axios";

const STREAM_URL_PATTERN = /^(https?|rtmp|rtsp):\/\//i;

export async function parseM3UFromUrl(url) {
  const { data } = await axios.get(url, { timeout: 15000, responseType: "text" });
  return parseM3UContent(typeof data === "string" ? data : String(data));
}

export function parseM3UContent(content) {
  const lines = String(content)
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean);

  const channels = [];

  for (let i = 0; i < lines.length; i++) {
    if (!lines[i].startsWith("#EXTINF")) continue;

    const meta = lines[i];
    let url = null;

    for (let j = i + 1; j < lines.length && j < i + 6; j++) {
      const line = lines[j];
      if (line.startsWith("#")) continue;
      if (STREAM_URL_PATTERN.test(line)) {
        url = line;
        break;
      }
    }

    if (!url) continue;

    const name =
      meta.match(/tvg-name="([^"]+)"/i)?.[1]?.trim() ||
      meta.match(/,(.+)$/)?.[1]?.trim() ||
      "Unknown";

    channels.push({
      name,
      logo: meta.match(/tvg-logo="([^"]+)"/i)?.[1] || null,
      epgId: meta.match(/tvg-id="([^"]+)"/i)?.[1] || null,
      category: meta.match(/group-title="([^"]+)"/i)?.[1] || "Lainnya",
      url,
    });
  }

  return channels;
}

// Generate M3U file untuk user (dengan token per URL)
export function generateM3U(channels, userId) {
  let output = "#EXTM3U\n";

  for (const ch of channels) {
    output += `#EXTINF:-1 tvg-id="${ch.epg_id || ""}" `;
    output += `tvg-logo="${ch.logo_url || ""}" `;
    output += `group-title="${ch.category}",${ch.name}\n`;
    output += `${ch.stream_url}?uid=${userId}&t=${Date.now()}\n`;
  }

  return output;
}
