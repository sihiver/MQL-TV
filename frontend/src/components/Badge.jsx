const STYLES = {
  active: { bg: "rgba(104,211,145,0.12)", text: "#68D391", border: "rgba(104,211,145,0.25)" },
  live: { bg: "rgba(252,129,129,0.12)", text: "#FC8181", border: "rgba(252,129,129,0.25)" },
  offline: { bg: "rgba(113,128,150,0.12)", text: "#718096", border: "rgba(113,128,150,0.25)" },
  banned: { bg: "rgba(252,129,129,0.12)", text: "#FC8181", border: "rgba(252,129,129,0.25)" },
  expired: { bg: "rgba(246,173,85,0.12)", text: "#F6AD55", border: "rgba(246,173,85,0.25)" },
  premium: { bg: "rgba(255,107,53,0.12)", text: "#FF6B35", border: "rgba(255,107,53,0.25)" },
  basic: { bg: "rgba(99,179,237,0.12)", text: "#63B3ED", border: "rgba(99,179,237,0.25)" },
  free: { bg: "rgba(160,174,192,0.1)", text: "#a0aec0", border: "rgba(160,174,192,0.2)" },
};

export default function Badge({ label, type }) {
  const s = STYLES[type] || STYLES.active;
  return (
    <span
      style={{
        background: s.bg,
        color: s.text,
        border: `1px solid ${s.border}`,
        borderRadius: 20,
        padding: "3px 10px",
        fontSize: 10,
        fontWeight: 700,
        letterSpacing: 0.5,
        whiteSpace: "nowrap",
      }}
    >
      {type === "live" ? "● " : ""}
      {label}
    </span>
  );
}
