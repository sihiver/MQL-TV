const MONTHS = ["Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des"];

export default function BarChart({ data, color }) {
  const max = Math.max(...data);
  return (
    <div style={{ display: "flex", alignItems: "flex-end", gap: 6, height: 80 }}>
      {data.map((v, i) => (
        <div
          key={i}
          style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 4 }}
        >
          <div
            style={{
              width: "100%",
              height: (v / max) * 68,
              borderRadius: "4px 4px 0 0",
              background: i === data.length - 1 ? color : `${color}66`,
              transition: "height 0.5s",
            }}
          />
          <div style={{ fontSize: 8, color: "#555", letterSpacing: 0.5 }}>{MONTHS[i]}</div>
        </div>
      ))}
    </div>
  );
}
