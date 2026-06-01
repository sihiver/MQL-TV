import crypto from "crypto";

const STREAM_TTL_SEC = 4 * 60 * 60; // 4 jam

export function generateStreamToken(streamUrl, userId) {
  const expiresAt = Math.floor(Date.now() / 1000) + STREAM_TTL_SEC;
  const payload = `${userId}:${expiresAt}`;
  const token = crypto
    .createHmac("sha256", process.env.STREAM_SECRET)
    .update(payload)
    .digest("hex");

  const separator = streamUrl.includes("?") ? "&" : "?";
  const streamUrlWithToken = `${streamUrl}${separator}uid=${userId}&exp=${expiresAt}&sig=${token}`;

  return { streamUrl: streamUrlWithToken, token, expiresAt };
}
