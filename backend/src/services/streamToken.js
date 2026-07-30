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
  const secret = process.env.STREAM_SECRET || "default_stream_secret";
  const payload = `${userId}:${expiresAt}`;
  const token = crypto
    .createHmac("sha256", secret)
    .update(payload)
    .digest("hex");

  // Cloudfront & external CDNs reserve 'exp' and 'sig' for Cloudfront Signed URLs.
  // Appending internal HMAC 'exp' and 'sig' parameters to external CDN hosts causes Cloudfront WAF signature validation to fail with HTTP 403.
  const isExternalCdn = /cloudfront\.net|akamaized\.net|visionplus\.id|kaltura\.com|fastly\.net/i.test(cleanUrl);

  const streamUrlWithToken = isExternalCdn
    ? cleanUrl
    : `${cleanUrl}${cleanUrl.includes("?") ? "&" : "?"}uid=${userId}&exp=${expiresAt}&sig=${token}`;

  return { streamUrl: streamUrlWithToken, token, expiresAt };
}
