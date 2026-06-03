/** Parse "1h", "6h", "1d", "30m" → detik. */
export function parseDurationToSeconds(value) {
  const raw = String(value || "").trim().toLowerCase();
  const m = raw.match(/^(\d+)(h|d|m|s)$/);
  if (!m) return null;
  const n = parseInt(m[1], 10);
  switch (m[2]) {
    case "h":
      return n * 3600;
    case "d":
      return n * 86400;
    case "m":
      return n * 60;
    case "s":
      return n;
    default:
      return null;
  }
}
