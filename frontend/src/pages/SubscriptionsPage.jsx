import { useState } from "react";
import { MOCK } from "../data/mock";
import { fmtRp } from "../utils/format";
import Badge from "../components/Badge";

export default function SubscriptionsPage() {
  const [filter, setFilter] = useState("Semua");
  const subs = MOCK.subscriptions;
  const filtered = subs.filter(
    (s) =>
      filter === "Semua" ||
      s.status === filter.toLowerCase() ||
      s.plan.toLowerCase() === filter.toLowerCase(),
  );
  const totalRevenue = subs.filter((s) => s.status === "active").reduce((a, s) => a + s.price, 0);

  return (
    <div className="admin-page">
      <div style={{ marginBottom: 22 }}>
        <div style={{ fontSize: 20, fontWeight: 900 }}>Subscriptions</div>
        <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>
          Total aktif: {subs.filter((s) => s.status === "active").length} · Revenue: {fmtRp(totalRevenue)}/bulan
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 14, marginBottom: 24 }}>
        {[
          { label: "Aktif", value: subs.filter((s) => s.status === "active").length, color: "#68D391" },
          { label: "Expired", value: subs.filter((s) => s.status === "expired").length, color: "#F6AD55" },
          { label: "Premium", value: subs.filter((s) => s.plan === "Premium").length, color: "#FF6B35" },
          { label: "Basic", value: subs.filter((s) => s.plan === "Basic").length, color: "#63B3ED" },
        ].map((c) => (
          <div
            key={c.label}
            style={{
              background: "rgba(255,255,255,0.03)",
              border: `1px solid ${c.color}33`,
              borderRadius: 14,
              padding: "18px 20px",
            }}
          >
            <div style={{ fontSize: 11, color: c.color, letterSpacing: 1, marginBottom: 6 }}>{c.label.toUpperCase()}</div>
            <div style={{ fontSize: 28, fontWeight: 900 }}>{c.value}</div>
          </div>
        ))}
      </div>

      <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
        {["Semua", "Active", "Expired", "Premium", "Basic"].map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => setFilter(f)}
            style={{
              background: filter === f ? "#FF6B35" : "rgba(255,255,255,0.07)",
              color: "#fff",
              border: "none",
              borderRadius: 20,
              padding: "7px 16px",
              fontSize: 11,
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            {f}
          </button>
        ))}
      </div>

      <div
        style={{
          background: "rgba(255,255,255,0.02)",
          border: "1px solid rgba(255,255,255,0.07)",
          borderRadius: 16,
          overflow: "hidden",
        }}
      >
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr style={{ borderBottom: "1px solid rgba(255,255,255,0.07)" }}>
              {["User", "Paket", "Harga", "Mulai", "Berakhir", "Status", "Metode", "Aksi"].map((h) => (
                <th
                  key={h}
                  style={{ padding: "12px 16px", textAlign: "left", fontSize: 10, color: "#888", letterSpacing: 1, fontWeight: 700 }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map((s, i) => (
              <tr
                key={s.id}
                style={{
                  borderBottom: "1px solid rgba(255,255,255,0.04)",
                  background: i % 2 === 0 ? "transparent" : "rgba(255,255,255,0.01)",
                }}
              >
                <td style={{ padding: "13px 16px", fontSize: 13, fontWeight: 700 }}>{s.user}</td>
                <td style={{ padding: "13px 16px" }}>
                  <Badge label={s.plan} type={s.plan.toLowerCase()} />
                </td>
                <td style={{ padding: "13px 16px", fontSize: 13, color: "#FF6B35", fontWeight: 700 }}>{fmtRp(s.price)}</td>
                <td style={{ padding: "13px 16px", fontSize: 12, color: "#888" }}>{s.start}</td>
                <td style={{ padding: "13px 16px", fontSize: 12, color: "#888" }}>{s.end}</td>
                <td style={{ padding: "13px 16px" }}>
                  <Badge label={s.status === "active" ? "Aktif" : "Expired"} type={s.status === "active" ? "active" : "expired"} />
                </td>
                <td style={{ padding: "13px 16px", fontSize: 12, color: "#aaa" }}>{s.method}</td>
                <td style={{ padding: "13px 16px" }}>
                  <button
                    type="button"
                    style={{
                      background: "rgba(99,179,237,0.12)",
                      border: "1px solid rgba(99,179,237,0.25)",
                      color: "#63B3ED",
                      borderRadius: 8,
                      padding: "5px 12px",
                      fontSize: 11,
                      cursor: "pointer",
                    }}
                  >
                    Perpanjang
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
