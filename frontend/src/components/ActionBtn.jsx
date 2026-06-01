export default function ActionBtn({ label, onClick, color }) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        background: `${color}18`,
        border: `1px solid ${color}44`,
        color,
        borderRadius: 7,
        padding: "5px 10px",
        fontSize: 12,
        cursor: "pointer",
      }}
    >
      {label}
    </button>
  );
}
