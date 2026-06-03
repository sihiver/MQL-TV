/** Angka bulat — pakai pemisah ribuan; singkat hanya ≥10K */
export const fmt = (n) => {
  const num = Number(n) || 0;
  if (num >= 1_000_000) return `${(num / 1_000_000).toFixed(1)}M`;
  if (num >= 10_000) return `${(num / 1000).toFixed(1)}K`;
  return new Intl.NumberFormat("id-ID").format(num);
};

export const fmtRp = (n) => `Rp ${(n / 1000).toFixed(0)}K`;
