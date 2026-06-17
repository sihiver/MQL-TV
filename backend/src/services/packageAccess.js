import { db } from "../config/database.js";

/** Slug paket aktif user (subscription). */
export async function getUserPlanSlug(userId) {
  const result = await db.query(
    `SELECT LOWER(s.plan) AS slug
     FROM subscriptions s
     WHERE s.user_id = $1 AND s.status = 'active' AND s.expires_at > NOW()
     ORDER BY s.expires_at DESC
     LIMIT 1`,
    [userId],
  );
  return result.rows[0]?.slug || "basic";
}

export async function getPackageBySlug(slug) {
  const result = await db.query(
    `SELECT id, slug, includes_all_channels, active
     FROM packages WHERE slug = $1`,
    [slug],
  );
  return result.rows[0] || null;
}

/** Apakah channel boleh diakses oleh paket ini? */
export async function channelAllowedForPackage(packageId, channelId, includesAll) {
  if (includesAll) return true;
  const result = await db.query(
    `SELECT 1 FROM package_channels WHERE package_id = $1 AND channel_id = $2`,
    [packageId, channelId],
  );
  return result.rows.length > 0;
}

/** SQL JOIN + WHERE untuk list channel menurut paket user. */
export async function resolvePackageAccess(userId) {
  const planSlug = await getUserPlanSlug(userId);
  const pkg = await getPackageBySlug(planSlug);

  if (!pkg || !pkg.active) {
    return { planSlug, packageId: null, includesAll: false, hasPackage: false };
  }

  return {
    planSlug,
    packageId: pkg.id,
    includesAll: pkg.includes_all_channels,
    hasPackage: true,
  };
}
