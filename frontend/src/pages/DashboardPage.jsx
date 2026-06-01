import { LIVE_ACTIVITY, MOCK } from "../data/mock";
import { fmt } from "../utils/format";
import Badge from "../components/Badge";
import BarChart from "../components/BarChart";
import Sparkline from "../components/Sparkline";

export default function DashboardPage({ apiOnline }) {
  const s = MOCK.stats;

  const statCards = [
    {
      label: "Total User",
      value: fmt(s.totalUsers),
      sub: `+${s.newUsersToday} hari ini`,
      icon: "👥",
      color: "#FF6B35",
      chart: [900, 950, 980, 1020, 1060, 1100, 1140, 1160, 1180, 1195, 1200, 1204],
    },
    {
      label: "Stream Aktif",
      value: fmt(s.activeStreams),
      sub: "dari 1.200 user",
      icon: "📡",
      color: "#63B3ED",
      chart: MOCK.streamChart,
    },
    {
      label: "Total Channel",
      value: fmt(s.totalChannels),
      sub: "1.190 live",
      icon: "📺",
      color: "#68D391",
      chart: [1200, 1210, 1215, 1220, 1225, 1230, 1235, 1237, 1240, 1244, 1246, 1247],
    },
    {
      label: "Revenue Bulan Ini",
      value: `Rp ${(s.revenue / 1_000_000).toFixed(1)}M`,
      sub: "+12% vs bulan lalu",
      icon: "💰",
      color: "#F6AD55",
      chart: MOCK.revenueChart,
    },
  ];

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
          ⚠ Backend API offline — menampilkan data mock
        </div>
      )}

      <div className="admin-grid-4" style={{ marginBottom: 28 }}>
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

      <div className="admin-grid-2" style={{ marginBottom: 20 }}>
        <div style={{ background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 18, padding: 22 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
            <div>
              <div style={{ fontSize: 13, fontWeight: 800 }}>Revenue 2024</div>
              <div style={{ fontSize: 11, color: "#888", marginTop: 2 }}>Total: Rp 2.4M</div>
            </div>
            <Badge label="+18% YoY" type="active" />
          </div>
          <BarChart data={MOCK.revenueChart} color="#FF6B35" />
        </div>

        <div style={{ background: "rgba(255,255,255,0.03)", border: "1px solid rgba(255,255,255,0.07)", borderRadius: 18, padding: 22 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
            <div style={{ fontSize: 13, fontWeight: 800 }}>Aktivitas Live</div>
            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <div style={{ width: 6, height: 6, borderRadius: 3, background: "#FC8181" }} />
              <span style={{ fontSize: 10, color: "#FC8181", letterSpacing: 1 }}>LIVE</span>
            </div>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {LIVE_ACTIVITY.map((a, i) => (
              <div
                key={i}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "8px 0",
                  borderBottom: "1px solid rgba(255,255,255,0.04)",
                }}
              >
                <div style={{ width: 7, height: 7, borderRadius: "50%", background: a.color, flexShrink: 0 }} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <span style={{ fontSize: 12, fontWeight: 700 }}>{a.user}</span>
                  <span style={{ fontSize: 12, color: "#888" }}> → {a.channel}</span>
                </div>
                <div style={{ fontSize: 11, color: a.color, whiteSpace: "nowrap" }}>{a.action}</div>
                <div style={{ fontSize: 10, color: "#555", whiteSpace: "nowrap" }}>{a.time}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="admin-grid-4">
        {[
          { label: "Uptime Server", value: s.uptime, bar: 99.8, color: "#68D391" },
          { label: "Bandwidth Hari Ini", value: s.bandwidth, bar: 68, color: "#63B3ED" },
          { label: "Perangkat Aktif", value: String(s.activeDevices), bar: 74, color: "#FF6B35" },
          { label: "Error Rate", value: "0.02%", bar: 2, color: "#FC8181" },
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
    </div>
  );
}
