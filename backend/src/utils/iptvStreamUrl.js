const DEFAULT_USER_AGENT =
  "Mozilla/5.0 (Linux; Android 10; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

/**
 * Format IPTV umum: https://host/path.m3u8|user-agent=...|referer=...
 */
export function parseIptvStreamUrl(raw) {
  if (!raw || typeof raw !== "string") {
    return { url: "", userAgent: null, referer: null };
  }

  let url = raw.trim();
  let userAgent = null;
  let referer = null;

  const pipeIndex = url.indexOf("|");
  if (pipeIndex !== -1) {
    const base = url.slice(0, pipeIndex).trim();
    const opts = url.slice(pipeIndex + 1);
    url = base;

    for (const segment of opts.split("|")) {
      const eq = segment.indexOf("=");
      if (eq === -1) continue;
      const key = segment.slice(0, eq).trim().toLowerCase().replace(/-/g, "_");
      const value = segment.slice(eq + 1).trim();
      if (!value) continue;
      if (key === "user_agent") userAgent = value;
      else if (key === "referer") referer = value;
    }
  }

  return { url, userAgent, referer };
}

export function resolveStreamHeaders(streamUrl, dbUserAgent, dbReferer) {
  const parsed = parseIptvStreamUrl(streamUrl);
  const url = parsed.url;
  let userAgent = dbUserAgent?.trim() || parsed.userAgent?.trim() || null;
  let referer = dbReferer?.trim() || parsed.referer?.trim() || null;

  if (!userAgent || userAgent === "Mozilla/5.0") {
    userAgent = DEFAULT_USER_AGENT;
  }

  // Referer hanya jika ada di DB / pipe — jangan tebak dari host CDN (bisa memicu 403).

  return { url, userAgent, referer: referer || null };
}
