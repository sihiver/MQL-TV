import { useCallback, useEffect, useState } from "react";
import { fetchDashboardStats } from "../api/dashboard";
import { fmt, fmtRp } from "../utils/format";
import Badge from "../components/Badge";
import BarChart from "../components/BarChart";
import Sparkline from "../components/Sparkline";

const EMPTY_STATS = {
  stats: {
    totalUsers: 0,
    newUsersToday: 0,
    activeStreams: 0,
    totalChannels: 0,
    channelEntries: 0,
    liveChannels: 0,
    revenueMonth: 0,
    mrr: 0,
    revenueChangePercent: null,
    activeDevices: 0,
    uptime: "—",
    uptimePercent: 0,
  },
  charts: { users: [], streams: [], channels: [], revenue: [] },
  revenue: { yearLabel: "", monthLabel: "", monthTotal: 0 },
  watchingNow: [],
};

function formatRevenue(n) {
  if (n >= 1_000_000) return `Rp ${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1000) return fmtRp(n);
  return `Rp ${n}`;
}

function revenueSubLabel(changePercent, mrr) {
  const parts = [];
  if (changePercent != null) {
    const sign = changePercent >= 0 ? "+" : "";
    parts.push(`${sign}${changePercent}% vs bulan lalu`);
  }
  if (mrr != null && mrr > 0) {
    parts.push(`MRR ${formatRevenue(mrr)}`);
  }
  return parts.length ? parts.join(" · ") : "Pembayaran tercatat bulan ini";
}

export default function DashboardPage({ apiOnline }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    try {
      const res = await fetchDashboardStats();
      setData(res);
    } catch (e) {
      if (!silent) {
        setError(e.message || "Gagal memuat dashboard");
        setData(null);
      }
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (apiOnline === false) return undefined;
    load(false);
    const id = setInterval(() => load(true), 30_000);
    return () => clearInterval(id);
  }, [apiOnline, load]);

  const d = data ?? EMPTY_STATS;
  const s = d.stats;
  const charts = d.charts;

  const statCards = [
    {
      label: "Total User",
      value: fmt(s.totalUsers),
      sub: `+${s.newUsersToday} hari ini`,
      icon: "👥",
      color: "#FF6B35",
      chart: charts.users?.length ? charts.users : [0],
    },
    {
      label: "Stream Aktif",
      value: fmt(s.activeStreams),
      sub: s.totalUsers > 0 ? `${Math.round((s.activeStreams / s.totalUsers) * 100)}% user (30 mnt)` : "30 menit terakhir",
      icon: "📡",
      color: "#63B3ED",
      chart: charts.streams?.length ? charts.streams : [0],
    },
    {
      label: "Total Channel",
      value: fmt(s.totalChannels),
      sub:
        s.channelEntries > s.totalChannels
          ? `${fmt(s.liveChannels)} live · ${fmt(s.channelEntries)} entri DB`
          : `${fmt(s.liveChannels)} live`,
      icon: "📺",
      color: "#68D391",
      chart: charts.channels?.length ? charts.channels : [0],
    },
    {
      label: "Revenue Bulan Ini",
      value: formatRevenue(s.revenueMonth),
      sub: revenueSubLabel(s.revenueChangePercent, s.mrr),
      icon: "💰",
      color: "#F6AD55",
      chart: charts.revenue?.length ? charts.revenue : [0],
    },
  ];

  const revenueChart = charts.revenue?.length ? charts.revenue : [0];
  const yoyBadge =
    s.revenueChangePercent != null
      ? `${s.revenueChangePercent >= 0 ? "+" : ""}${s.revenueChangePercent}%`
      : "Bulan ini";

  return (
    <div className="admin-page">
      {apiOnline === false && (
        <div
          style={{
            marginBottom: 16,
            padding: "10px 14px",
            background: "rgba(252,129,129,0.1)",
            border: "1px solid rgba(252,129,129,0.3)",
            borderRadius: 10,
            fontSize: 12,
            color: "#FC8181",
          }}
        >
          ⚠ Tidak dapat menjangkau backend — cek VITE_API_URL (frontend/.env), backend harus jalan, dan
          URL admin ini harus ada di ALLOWED_ORIGINS (backend/.env). Dev: npm run dev (proxy Vite).
        </div>
      )}

      {error && (
        <div
          style={{
            marginBottom: 16,
            padding: "10px 14px",
            background: "rgba(252,129,129,0.1)",
            border: "1px solid rgba(252,129,129,0.3)",
            borderRadius: 10,
            fontSize: 12,
            color: "#FC8181",
          }}
        >
          {error}
          <button
            type="button"
            onClick={load}
            style={{
              marginLeft: 12,
              background: "transparent",
              border: "1px solid rgba(252,129,129,0.5)",
              color: "#FC8181",
              borderRadius: 6,
              padding: "2px 8px",
              cursor: "pointer",
              fontSize: 11,
            }}
          >
            Coba lagi
          </button>
        </div>
      )}

      {loading && !data && (
        <div style={{ padding: 40, textAlign: "center", color: "#888", fontSize: 13 }}>
          Memuat statistik…
        </div>
      )}

      {(!loading || data) && (
        <>
          <div className="admin-grid-4" style={{ marginBottom: 28, opacity: loading ? 0.6 : 1 }}>
            {statCards.map((c) => (
              <div
                key={c.label}
                style={{
                  background: "rgba(255,255,255,0.03)",
                  border: "1px solid rgba(255,255,255,0.07)",
                  borderRadius: 18,
                  padding: "22px 22px 16px",
                  position: "relative",
                  overflow: "hidden",
                }}
              >
                <div
                  style={{
                    position: "absolute",
                    top: 0,
                    right: 0,
                    left: 0,
                    height: 2,
                    background: `linear-gradient(90deg, transparent, ${c.color}, transparent)`,
                  }}
                />
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 12 }}>
                  <div>
                    <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>{c.label.toUpperCase()}</div>
                    <div style={{ fontSize: 26, fontWeight: 900, letterSpacing: -0.5 }}>{c.value}</div>
                    <div style={{ fontSize: 11, color: "#666", marginTop: 4 }}>{c.sub}</div>
                  </div>
                  <span style={{ fontSize: 22 }}>{c.icon}</span>
                </div>
                <Sparkline data={c.chart} color={c.color} height={36} />
              </div>
            ))}
          </div>

          <div className="admin-grid-2" style={{ marginBottom: 20, opacity: loading ? 0.6 : 1 }}>
            <div style={{ background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 18, padding: 22 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
                <div>
                  <div style={{ fontSize: 13, fontWeight: 800 }}>Revenue {d.revenue?.yearLabel || new Date().getFullYear()}</div>
                  <div style={{ fontSize: 11, color: "#888", marginTop: 2 }}>
                    Bulan ini: {formatRevenue(d.revenue?.monthTotal ?? s.revenueMonth)}
                    {(d.revenue?.mrr ?? s.mrr) > 0 && (
                      <> · MRR {formatRevenue(d.revenue?.mrr ?? s.mrr)}</>
                    )}
                  </div>
                </div>
                <Badge
                  label={yoyBadge}
                  type={
                    s.revenueChangePercent == null
                      ? "active"
                      : s.revenueChangePercent >= 0
                        ? "active"
                        : "expired"
                  }
                />
              </div>
              <BarChart data={revenueChart} color="#FF6B35" />
            </div>

            <div style={{ background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 18, padding: 22 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
                <div>
                  <div style={{ fontSize: 13, fontWeight: 800 }}>Sedang Menonton</div>
                  <div style={{ fontSize: 11, color: "#888", marginTop: 2 }}>30 menit terakhir</div>
                </div>
                <Badge label={`${fmt(s.activeStreams)} online`} type="live" />
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {d.watchingNow?.length > 0 ? (
                  d.watchingNow.map((row, i) => (
                    <div
                      key={`${row.user}-${row.channel}-${i}`}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 12,
                        padding: "8px 0",
                        borderBottom: "1px solid rgba(255,255,255,0.04)",
                      }}
                    >
                      <div style={{ width: 7, height: 7, borderRadius: "50%", background: "#68D391", flexShrink: 0 }} />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 12, fontWeight: 700 }}>{row.user}</div>
                        {row.email && (
                          <div style={{ fontSize: 10, color: "#666", marginTop: 1 }}>{row.email}</div>
                        )}
                      </div>
                      <div style={{ fontSize: 12, color: "#63B3ED", fontWeight: 700, whiteSpace: "nowrap" }}>
                        {row.channel}
                      </div>
                      <div style={{ fontSize: 10, color: "#555", whiteSpace: "nowrap" }}>{row.since}</div>
                    </div>
                  ))
                ) : (
                  <div style={{ fontSize: 12, color: "#666", padding: "12px 0" }}>
                    Tidak ada user yang sedang menonton saat ini.
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="admin-grid-4" style={{ opacity: loading ? 0.6 : 1 }}>
            {[
              { label: "Uptime Server", value: s.uptime, bar: s.uptimePercent ?? 99.9, color: "#68D391" },
              {
                label: "Langganan Aktif",
                value: fmt(s.activeSubscriptions ?? 0),
                bar: s.totalUsers > 0 ? Math.min(100, Math.round(((s.activeSubscriptions ?? 0) / s.totalUsers) * 100)) : 0,
                color: "#63B3ED",
              },
              {
                label: "Perangkat Aktif",
                value: String(s.activeDevices),
                bar: s.totalUsers > 0 ? Math.min(100, Math.round((s.activeDevices / s.totalUsers) * 100)) : 0,
                color: "#FF6B35",
              },
              {
                label: "User Baru Hari Ini",
                value: String(s.newUsersToday),
                bar: s.totalUsers > 0 ? Math.min(100, Math.round((s.newUsersToday / s.totalUsers) * 100)) : 0,
                color: "#F6AD55",
              },
            ].map((m) => (
              <div
                key={m.label}
                style={{
                  background: "rgba(255,255,255,0.03)",
                  border: "1px solid rgba(255,255,255,0.07)",
                  borderRadius: 14,
                  padding: "18px",
                }}
              >
                <div style={{ fontSize: 11, color: "#888", marginBottom: 8 }}>{m.label}</div>
                <div style={{ fontSize: 20, fontWeight: 900, marginBottom: 12 }}>{m.value}</div>
                <div style={{ height: 4, background: "rgba(255,255,255,0.08)", borderRadius: 2 }}>
                  <div style={{ width: `${m.bar}%`, height: "100%", background: m.color, borderRadius: 2 }} />
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
