import { db } from "../config/database.js";

/**
 * Mendapatkan slug paket user.
 * Prioritas:
 * 1. Subscription aktif (status = 'active' dan expires_at > NOW())
 * 2. Subscription terakhir (meskipun sudah expired), agar user tetap melihat channel paketnya
 * 3. Nilai plan di tabel users
 */
export async function getUserPlanSlug(userId) {
  // 1. Subscription aktif
  const activeSub = await db.query(
    `SELECT LOWER(s.plan) AS slug
     FROM subscriptions s
     WHERE s.user_id = $1 AND s.status = 'active' AND s.expires_at > NOW()
     ORDER BY s.expires_at DESC
     LIMIT 1`,
    [userId],
  );
  if (activeSub.rows[0]?.slug) {
    return activeSub.rows[0].slug;
  }

  // 2. Subscription terakhir (meskipun sudah expired)
  const lastSub = await db.query(
    `SELECT LOWER(s.plan) AS slug
     FROM subscriptions s
     WHERE s.user_id = $1
     ORDER BY s.expires_at DESC
     LIMIT 1`,
    [userId],
  );
  if (lastSub.rows[0]?.slug) {
    return lastSub.rows[0].slug;
  }

  // 3. Plan dari tabel users
  const userRow = await db.query(
    `SELECT LOWER(plan) AS slug FROM users WHERE id = $1`,
    [userId],
  );
  if (userRow.rows[0]?.slug && userRow.rows[0].slug !== "free") {
    return userRow.rows[0].slug;
  }

  return null;
}

export async function getPackageBySlug(slug) {
  if (!slug) return null;
  const result = await db.query(
    `SELECT id, slug, includes_all_channels, active
     FROM packages WHERE LOWER(slug) = LOWER($1)`,
    [slug],
  );
  return result.rows[0] || null;
}

/** Apakah channel boleh diakses oleh paket ini? */
export async function channelAllowedForPackage(packageId, channelId, includesAll) {
  if (includesAll) return true;
  if (!packageId) return true;
  const result = await db.query(
    `SELECT 1 FROM package_channels WHERE package_id = $1 AND channel_id = $2`,
    [packageId, channelId],
  );
  return result.rows.length > 0;
}

/** SQL JOIN + WHERE untuk list channel menurut paket user. */
export async function resolvePackageAccess(userId) {
  let planSlug = await getUserPlanSlug(userId);
  let pkg = planSlug ? await getPackageBySlug(planSlug) : null;

  // Jika paket tidak ditemukan atau tidak aktif, cari paket fallback aktif di database
  if (!pkg || !pkg.active) {
    const fallback = await db.query(
      `SELECT id, slug, includes_all_channels, active
       FROM packages
       WHERE active = true
       ORDER BY includes_all_channels DESC, sort_order ASC
       LIMIT 1`,
    );
    if (fallback.rows.length > 0) {
      pkg = fallback.rows[0];
      planSlug = pkg.slug;
    }
  }

  // Jika tidak ada paket sama sekali yang terkonfigurasi di packages,
  // tetap return includesAll: true agar channel list katalog tidak kosong.
  if (!pkg || !pkg.active) {
    return {
      planSlug: planSlug || "all",
      packageId: null,
      includesAll: true,
      hasPackage: true,
    };
  }

  return {
    planSlug,
    packageId: pkg.id,
    includesAll: pkg.includes_all_channels,
    hasPackage: true,
  };
}
