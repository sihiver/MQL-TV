export const PAGES = {
  DASHBOARD: "dashboard",
  CHANNELS: "channels",
  EPG_MAPPING: "epg-mapping",
  USERS: "users",
  SUBSCRIPTIONS: "subscriptions",
  PACKAGES: "packages",
  SETTINGS: "settings",
};

export const NAV_ITEMS = [
  { id: PAGES.DASHBOARD, icon: "⊞", label: "Dashboard" },
  { id: PAGES.CHANNELS, icon: "📺", label: "Channel" },
  { id: PAGES.EPG_MAPPING, icon: "📅", label: "EPG" },
  { id: PAGES.USERS, icon: "👥", label: "Users" },
  { id: PAGES.PACKAGES, icon: "📦", label: "Paket" },
  { id: PAGES.SUBSCRIPTIONS, icon: "💳", label: "Subscription" },
  { id: PAGES.SETTINGS, icon: "⚙️", label: "Konfigurasi" },
];
