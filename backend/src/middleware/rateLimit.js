import { getServerSettings } from "../services/serverSettings.js";
import { redis } from "../config/redis.js";

function clientIp(req) {
  const fwd = req.headers["x-forwarded-for"];
  if (typeof fwd === "string" && fwd.length) return fwd.split(",")[0].trim();
  return req.ip || req.socket?.remoteAddress || "unknown";
}

/** Rate limit per IP per menit (konfigurasi admin). */
export async function rateLimitMiddleware(req, res, next) {
  try {
    if (req.headers["x-admin-key"]?.trim()) return next();

    const settings = await getServerSettings();
    const limit = settings.rateLimit || 100;
    const ip = clientIp(req);
    const windowKey = Math.floor(Date.now() / 60_000);
    const key = `ratelimit:${ip}:${windowKey}`;

    const count = await redis.incr(key);
    if (count === 1) {
      await redis.expire(key, 60);
    }

    res.setHeader("X-RateLimit-Limit", String(limit));
    res.setHeader("X-RateLimit-Remaining", String(Math.max(0, limit - count)));

    if (count > limit) {
      return res.status(429).json({
        error: `Terlalu banyak permintaan. Maksimal ${limit} req/menit.`,
      });
    }
    next();
  } catch (err) {
    next(err);
  }
}
