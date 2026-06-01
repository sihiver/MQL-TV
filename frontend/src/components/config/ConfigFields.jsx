export function CfgCard({ title, color, children }) {
  return (
    <div
      style={{
        background: "rgba(255,255,255,0.03)",
        border: "1px solid rgba(255,255,255,0.07)",
        borderRadius: 16,
        padding: 22,
        position: "relative",
        overflow: "hidden",
      }}
    >
      <div
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          right: 0,
          height: 2,
          background: `linear-gradient(90deg, ${color}, transparent)`,
        }}
      />
      <div style={{ fontSize: 14, fontWeight: 800, marginBottom: 18 }}>{title}</div>
      <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>{children}</div>
    </div>
  );
}

export function CfgField({ label, value, onChange, mono, type = "text" }) {
  return (
    <div>
      <div style={{ fontSize: 10, color: "#888", letterSpacing: 1, marginBottom: 5 }}>
        {label.toUpperCase()}
      </div>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        style={{
          width: "100%",
          background: "rgba(255,255,255,0.06)",
          border: "1px solid rgba(255,255,255,0.1)",
          borderRadius: 8,
          color: "#fff",
          fontSize: mono ? 11 : 13,
          padding: "9px 12px",
          outline: "none",
          boxSizing: "border-box",
          fontFamily: mono ? "monospace" : "inherit",
        }}
      />
    </div>
  );
}

export function CfgSelect({ label, value, onChange, opts }) {
  return (
    <div>
      <div style={{ fontSize: 10, color: "#888", letterSpacing: 1, marginBottom: 5 }}>
        {label.toUpperCase()}
      </div>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        style={{
          width: "100%",
          background: "rgba(255,255,255,0.06)",
          border: "1px solid rgba(255,255,255,0.1)",
          borderRadius: 8,
          color: "#fff",
          fontSize: 13,
          padding: "9px 12px",
          outline: "none",
        }}
      >
        {opts.map((o) => (
          <option key={o} value={o}>
            {o}
          </option>
        ))}
      </select>
    </div>
  );
}

export function CfgToggle({ label, value, onChange, danger }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
      <span style={{ fontSize: 13, color: danger && value ? "#FC8181" : "#ccc" }}>{label}</span>
      <button
        type="button"
        onClick={() => onChange(!value)}
        style={{
          width: 44,
          height: 24,
          borderRadius: 12,
          background: value ? (danger ? "#FC8181" : "#FF6B35") : "rgba(255,255,255,0.15)",
          position: "relative",
          cursor: "pointer",
          border: "none",
          padding: 0,
        }}
      >
        <div
          style={{
            position: "absolute",
            top: 3,
            left: value ? 23 : 3,
            width: 18,
            height: 18,
            borderRadius: 9,
            background: "#fff",
            transition: "left 0.2s",
          }}
        />
      </button>
    </div>
  );
}
