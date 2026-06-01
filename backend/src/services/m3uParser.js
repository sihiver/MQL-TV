import axios from "axios";

export async function parseM3UFromUrl(url) {
  const { data } = await axios.get(url, { timeout: 15000 });
  return parseM3UContent(data);
}

export function parseM3UContent(content) {
  const lines   = content.split("\n").map(l => l.trim());
  const channels = [];
  let i = 0;

  while (i < lines.length) {
    if (lines[i].startsWith("#EXTINF")) {
      const meta = lines[i];
      const url  = lines[i + 1];

      if (url && url.startsWith("http")) {
        channels.push({
          name    : meta.match(/,(.+)$/)?.[1]?.trim() ?? "Unknown",
          logo    : meta.match(/tvg-logo="([^"]+)"/)?.[1] ?? null,
          epgId   : meta.match(/tvg-id="([^"]+)"/)?.[1] ?? null,
          category: meta.match(/group-title="([^"]+)"/)?.[1] ?? "Lainnya",
          url,
        });
      }
      i += 2;
    } else {
      i++;
    }
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