import { getServerSettings } from "../services/serverSettings.js";

function requestPath(req) {
  return (req.originalUrl || req.url || "").split("?")[0];
}

function isAdminBypass(req) {
  if (req.headers["x-admin-key"]?.trim()) return true;
  const path = requestPath(req);
  if (path === "/api/auth/admin/login" || path.endsWith("/auth/admin/login")) return true;
  if (req.user?.role === "admin") return true;
  return false;
}

export async function maintenanceMiddleware(req, res, next) {
  try {
    const settings = await getServerSettings();
    if (!settings.maintenanceMode) return next();
    if (isAdminBypass(req)) return next();
    return res.status(503).json({
      error: "Server sedang maintenance. Coba lagi nanti.",
    });
  } catch (err) {
    next(err);
  }
}
