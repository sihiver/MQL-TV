import { useState } from "react";
import { useAuth } from "./context/AuthContext.jsx";
import { NAV_ITEMS, PAGES } from "./constants/pages";
import { useApiHealth } from "./hooks/useApiHealth";
import BackendSettingsPage from "./pages/BackendSettingsPage";
import ChannelsPage from "./pages/ChannelsPage";
import DashboardPage from "./pages/DashboardPage";
import EpgMappingPage from "./pages/EpgMappingPage";
import PackagesPage from "./pages/PackagesPage";
import SubscriptionsPage from "./pages/SubscriptionsPage";
import UsersPage from "./pages/UsersPage";

export default function AdminApp() {
  const { user, logout } = useAuth();
  const [page, setPage] = useState(PAGES.DASHBOARD);
  const [notifications, setNotif] = useState(3);
  const apiOnline = useApiHealth();

  const handleLogout = async () => {
    await logout();
  };

  return (
    <div className="admin-app">
      <header className="admin-header">
        <div style={{ display: "flex", alignItems: "center", gap: 16, flex: 1, minWidth: 0 }}>
          <div style={{ flexShrink: 0 }}>
            <span style={{ fontSize: 14, fontWeight: 900, letterSpacing: 2 }}>NUSA</span>
            <span style={{ color: "#FF6B35", fontSize: 14, fontWeight: 900 }}>VISION</span>
            <span style={{ fontSize: 10, color: "#555", marginLeft: 8, letterSpacing: 2, fontFamily: "monospace" }}>
              ADMIN
            </span>
          </div>
          <nav style={{ display: "flex", gap: 2, marginLeft: 16, flexWrap: "wrap" }}>
            {NAV_ITEMS.map((n) => (
              <button
                key={n.id}
                type="button"
                onClick={() => setPage(n.id)}
                className={`nav-tab ${page === n.id ? "nav-tab--active" : ""}`}
              >
                <span>{n.icon}</span>
                <span>{n.label}</span>
              </button>
            ))}
          </nav>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 16, flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <div
              style={{
                width: 6,
                height: 6,
                borderRadius: 3,
                background: apiOnline === false ? "#FC8181" : "#68D391",
              }}
            />
            <span
              style={{
                fontSize: 11,
                color: apiOnline === false ? "#FC8181" : "#68D391",
                letterSpacing: 1,
              }}
            >
              {apiOnline === false ? "API OFFLINE" : "API ONLINE"}
            </span>
          </div>
          <button
            type="button"
            onClick={() => setNotif(0)}
            style={{ background: "none", border: "none", cursor: "pointer", position: "relative", fontSize: 18, color: "#888" }}
          >
            🔔
            {notifications > 0 && <span className="notif-badge">{notifications}</span>}
          </button>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <div style={{ textAlign: "right", lineHeight: 1.3 }}>
              <div style={{ fontSize: 12, fontWeight: 700 }}>{user?.name}</div>
              <div style={{ fontSize: 10, color: "#666" }}>{user?.email}</div>
            </div>
            <div className="avatar" title={user?.name}>
              {user?.name?.[0]?.toUpperCase() || "A"}
            </div>
            <button
              type="button"
              onClick={handleLogout}
              title="Keluar"
              style={{
                background: "rgba(255,255,255,0.06)",
                border: "1px solid rgba(255,255,255,0.1)",
                color: "#aaa",
                borderRadius: 8,
                padding: "6px 12px",
                fontSize: 11,
                cursor: "pointer",
              }}
            >
              Keluar
            </button>
          </div>
        </div>
      </header>

      <main className="admin-main">
        {page === PAGES.DASHBOARD && <DashboardPage apiOnline={apiOnline} />}
        {page === PAGES.CHANNELS && <ChannelsPage />}
        {page === PAGES.EPG_MAPPING && <EpgMappingPage />}
        {page === PAGES.USERS && <UsersPage />}
        {page === PAGES.PACKAGES && <PackagesPage />}
        {page === PAGES.SUBSCRIPTIONS && <SubscriptionsPage />}
        {page === PAGES.SETTINGS && <BackendSettingsPage />}
      </main>
    </div>
  );
}
