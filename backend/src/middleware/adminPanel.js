import { authenticate, isAdmin } from "./auth.js";

/** Akses panel admin: header X-Admin-Key atau JWT role admin. */
export function adminPanelAuth(req, res, next) {
  const key = req.headers["x-admin-key"];
  const adminKey = process.env.ADMIN_API_KEY;

  if (adminKey && key === adminKey) {
    req.user = { id: 0, role: "admin", email: "admin@panel" };
    return next();
  }

  // Dev: izinkan tanpa key jika ADMIN_API_KEY belum dikonfigurasi
  if (!adminKey && process.env.NODE_ENV !== "production") {
    req.user = { id: 0, role: "admin", email: "admin@panel" };
    return next();
  }

  authenticate(req, res, () => isAdmin(req, res, next));
}
