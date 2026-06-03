import { db } from "../config/database.js";

function timeAgoId(date) {
  const sec = Math.floor((Date.now() - new Date(date).getTime()) / 1000);
  if (sec < 60) return `${sec} dtk lalu`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min} mnt lalu`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr} jam lalu`;
  const day = Math.floor(hr / 24);
  return `${day} hari lalu`;
}

function formatUptime(seconds) {
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (d > 0) return `${d}h ${h}j`;
  if (h > 0) return `${h}j ${m}m`;
  return `${m}m`;
}

/** Isi 12 titik terakhir; bulan/hari tanpa data = 0 */
function fillSeries(rows, key = "value", length = 12) {
  const map = new Map(rows.map((r) => [String(r.bucket), Number(r[key] || 0)]));
  const keys = rows.map((r) => String(r.bucket));
  if (keys.length >= length) {
    return keys.slice(-length).map((k) => map.get(k) ?? 0);
  }
  const out = Array(length).fill(0);
  keys.forEach((k, i) => {
    out[length - keys.length + i] = map.get(k) ?? 0;
  });
  return out;
}

export async function getDashboardStats() {
  const [
    totals,
    newUsersToday,
    activeStreams,
    revenueMonth,
    activeDevices,
    usersByMonth,
    viewsByDay,
    channelsByMonth,
    revenueByMonth,
    watchingNow,
  ] = await Promise.all([
    db.query(`
      SELECT
        (SELECT COUNT(*)::int FROM users WHERE role != 'admin') AS total_users,
        (SELECT COUNT(DISTINCT LOWER(TRIM(name)))::int FROM channels WHERE active = true) AS total_channels,
        (SELECT COUNT(*)::int FROM channels WHERE active = true) AS channel_entries,
        (SELECT COUNT(DISTINCT LOWER(TRIM(name)))::int
         FROM channels WHERE active = true AND COALESCE(is_live, true) = true) AS live_channels,
        (SELECT COUNT(*)::int FROM subscriptions WHERE status = 'active' AND expires_at > NOW()) AS active_subscriptions
    `),
    db.query(`
      SELECT COUNT(*)::int AS c FROM users
      WHERE role != 'admin' AND created_at >= CURRENT_DATE
    `),
    db.query(`
      SELECT COUNT(DISTINCT user_id)::int AS c FROM channel_views
      WHERE viewed_at > NOW() - INTERVAL '30 minutes'
    `),
    db.query(`
      SELECT COALESCE(SUM(p.price), 0)::bigint AS total
      FROM subscriptions s
      LEFT JOIN packages p ON p.slug = LOWER(s.plan)
      WHERE s.status = 'active' AND s.expires_at > NOW()
    `),
    db.query(`
      SELECT COUNT(*)::int AS c FROM devices
      WHERE last_seen_at > NOW() - INTERVAL '24 hours'
    `),
    db.query(`
      SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS bucket,
             COUNT(*)::int AS value
      FROM users
      WHERE role != 'admin'
        AND created_at >= date_trunc('month', NOW()) - INTERVAL '11 months'
      GROUP BY 1 ORDER BY 1
    `),
    db.query(`
      SELECT to_char(viewed_at::date, 'YYYY-MM-DD') AS bucket,
             COUNT(*)::int AS value
      FROM channel_views
      WHERE viewed_at >= CURRENT_DATE - INTERVAL '11 days'
      GROUP BY viewed_at::date ORDER BY viewed_at::date
    `),
    db.query(`
      SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS bucket,
             COUNT(*)::int AS value
      FROM channels
      WHERE created_at >= date_trunc('month', NOW()) - INTERVAL '11 months'
      GROUP BY 1 ORDER BY 1
    `),
    db.query(`
      SELECT to_char(date_trunc('month', s.started_at), 'YYYY-MM') AS bucket,
             COALESCE(SUM(p.price), 0)::int AS value
      FROM subscriptions s
      LEFT JOIN packages p ON p.slug = LOWER(s.plan)
      WHERE s.started_at >= date_trunc('month', NOW()) - INTERVAL '11 months'
      GROUP BY 1 ORDER BY 1
    `),
    db.query(`
      SELECT DISTINCT ON (cv.user_id)
             u.name AS user_name,
             u.email AS user_email,
             c.name AS channel_name,
             cv.viewed_at
      FROM channel_views cv
      JOIN users u ON u.id = cv.user_id
      JOIN channels c ON c.id = cv.channel_id
      WHERE cv.viewed_at > NOW() - INTERVAL '30 minutes'
        AND u.role != 'admin'
      ORDER BY cv.user_id, cv.viewed_at DESC
      LIMIT 20
    `),
  ]);

  const t = totals.rows[0];
  const revMrr = Number(revenueMonth.rows[0]?.total || 0);
  const revenueSeries = fillSeries(revenueByMonth.rows);
  const revThisMonth = revenueSeries[revenueSeries.length - 1] ?? 0;
  const revPrevMonth = revenueSeries[revenueSeries.length - 2] ?? 0;
  let revenueChangePercent = null;
  if (revPrevMonth > 0) {
    revenueChangePercent = Math.round(((revThisMonth - revPrevMonth) / revPrevMonth) * 100);
  } else if (revThisMonth > 0) {
    revenueChangePercent = 100;
  }

  const uptimeSec = process.uptime();
  const activeStreamsCount = activeStreams.rows[0]?.c ?? 0;
  const totalUsers = t.total_users ?? 0;

  return {
    stats: {
      totalUsers,
      newUsersToday: newUsersToday.rows[0]?.c ?? 0,
      activeStreams: activeStreamsCount,
      totalChannels: t.total_channels ?? 0,
      channelEntries: t.channel_entries ?? 0,
      liveChannels: t.live_channels ?? 0,
      activeSubscriptions: t.active_subscriptions ?? 0,
      revenueMonth: revMrr,
      revenueChangePercent,
      activeDevices: activeDevices.rows[0]?.c ?? 0,
      uptime: formatUptime(uptimeSec),
      uptimePercent: 99.9,
    },
    charts: {
      users: fillSeries(usersByMonth.rows),
      streams: fillSeries(viewsByDay.rows),
      channels: fillSeries(channelsByMonth.rows),
      revenue: revenueSeries,
    },
    revenue: {
      yearLabel: new Date().getFullYear().toString(),
      monthTotal: revMrr,
    },
    watchingNow: watchingNow.rows.map((row) => ({
      user: row.user_name,
      email: row.user_email,
      channel: row.channel_name,
      since: timeAgoId(row.viewed_at),
    })),
  };
}
