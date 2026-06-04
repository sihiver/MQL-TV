import jwt from "jsonwebtoken";
import { redis } from "../config/redis.js";
import { isUserBanned } from "../services/userBan.js";

export const authenticate = async (req, res, next) => {
  const header = req.headers.authorization;
  if (!header?.startsWith("Bearer "))
    return res.status(401).json({ error: "Token tidak ditemukan" });

  const token = header.split(" ")[1];

  // Cek token di blacklist Redis (kalau sudah logout)
  const isBlacklisted = await redis.get(`blacklist:${token}`);
  if (isBlacklisted)
    return res.status(401).json({ error: "Token sudah tidak valid" });

  try {
    req.user = jwt.verify(token, process.env.JWT_SECRET);
  } catch {
    return res.status(401).json({ error: "Token tidak valid atau sudah kadaluarsa" });
  }

  if (await isUserBanned(req.user.id)) {
    return res.status(403).json({ error: "Akun ini telah diblokir" });
  }

  next();
};

export const isAdmin = (req, res, next) => {
  if (req.user.role !== "admin")
    return res.status(403).json({ error: "Akses ditolak" });
  next();
};

export const checkSubscription = (req, res, next) => {
  if (req.user.plan === "free" && req.user.channelCount > 10)
    return res.status(403).json({ error: "Upgrade ke Premium untuk akses penuh" });
  next();
};