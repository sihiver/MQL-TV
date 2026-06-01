export default function Sparkline({ data, color, height = 40 }) {
  const max = Math.max(...data);
  const min = Math.min(...data);
  const pts = data
    .map((v, i) => {
      const x = (i / (data.length - 1)) * 200;
      const y = height - ((v - min) / (max - min + 1)) * (height - 6) - 3;
      return `${x},${y}`;
    })
    .join(" ");
  const area = `0,${height} ${pts} 200,${height}`;
  const gradId = `sg${color.replace("#", "")}`;

  return (
    <svg width="200" height={height} style={{ overflow: "visible" }}>
      <defs>
        <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.25" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      <polygon points={area} fill={`url(#${gradId})`} />
      <polyline
        points={pts}
        fill="none"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
