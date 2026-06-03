import crypto from "crypto";
import { parseIptvStreamUrl } from "../utils/iptvStreamUrl.js";
import { getServerSettingsSync } from "./serverSettings.js";
import { parseDurationToSeconds } from "../utils/duration.js";

function streamTtlSec() {
  const sec = parseDurationToSeconds(getServerSettingsSync().streamExpiry);
  return sec && sec > 0 ? sec : 4 * 60 * 60;
}

export function generateStreamToken(streamUrl, userId) {
  const { url: cleanUrl } = parseIptvStreamUrl(streamUrl);
  const ttl = streamTtlSec();
  const expiresAt = Math.floor(Date.now() / 1000) + ttl;
  const payload = `${userId}:${expiresAt}`;
  const token = crypto
    .createHmac("sha256", process.env.STREAM_SECRET)
    .update(payload)
    .digest("hex");

  const separator = cleanUrl.includes("?") ? "&" : "?";
  const streamUrlWithToken = `${cleanUrl}${separator}uid=${userId}&exp=${expiresAt}&sig=${token}`;

  return { streamUrl: streamUrlWithToken, token, expiresAt };
}
