export const fmt = (n) =>
  n >= 1_000_000 ? `${(n / 1_000_000).toFixed(1)}M` : n >= 1000 ? `${(n / 1000).toFixed(0)}K` : n;

export const fmtRp = (n) => `Rp ${(n / 1000).toFixed(0)}K`;
