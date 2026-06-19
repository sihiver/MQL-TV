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
import AppUpdatesPage from "./pages/AppUpdatesPage";

const VALID_PAGES = new Set(Object.values(PAGES));

function getInitialPage() {
  const saved = localStorage.getItem("admin_page");
  return saved && VALID_PAGES.has(saved) ? saved : PAGES.DASHBOARD;
}

export default function AdminApp() {
  const { user, logout } = useAuth();
  const [page, setPage] = useState(getInitialPage);
  const [notifications, setNotif] = useState(3);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const apiOnline = useApiHealth();

  const navigate = (id) => {
    localStorage.setItem("admin_page", id);
    setPage(id);
    setIsMobileMenuOpen(false); // Close menu on mobile
  };

  const handleLogout = async () => {
    await logout();
  };

  const NavTabs = ({ isMobile }) => (
    <nav style={{ display: "flex", gap: isMobile ? 8 : 2, flexDirection: isMobile ? "column" : "row", marginLeft: isMobile ? 0 : 16, flexWrap: "wrap" }}>
      {NAV_ITEMS.map((n) => (
        <button
          key={n.id}
          type="button"
          onClick={() => navigate(n.id)}
          className={`nav-tab ${page === n.id ? "nav-tab--active" : ""}`}
          style={isMobile ? { padding: "12px 16px", fontSize: 14 } : {}}
        >
          <span>{n.icon}</span>
          <span>{n.label}</span>
        </button>
      ))}
    </nav>
  );

  return (
    <div className="admin-app">
      <header className="admin-header">
        <div style={{ display: "flex", alignItems: "center", gap: 16, flex: 1, minWidth: 0 }}>
          <button className="hamburger-btn" onClick={() => setIsMobileMenuOpen(true)}>
            ☰
          </button>
          <div style={{ flexShrink: 0 }}>
            <span style={{ fontSize: 14, fontWeight: 900, letterSpacing: 2 }}>NUSA</span>
            <span style={{ color: "#FF6B35", fontSize: 14, fontWeight: 900 }}>VISION</span>
            <span className="mobile-hide" style={{ fontSize: 10, color: "#555", marginLeft: 8, letterSpacing: 2, fontFamily: "monospace" }}>
              ADMIN
            </span>
          </div>
          <div className="nav-desktop">
            <NavTabs isMobile={false} />
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 16, flexShrink: 0 }}>
          <div className="mobile-hide" style={{ display: "flex", alignItems: "center", gap: 6 }}>
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
            <div className="mobile-hide" style={{ textAlign: "right", lineHeight: 1.3 }}>
              <div style={{ fontSize: 12, fontWeight: 700 }}>{user?.name}</div>
              <div style={{ fontSize: 10, color: "#666" }}>{user?.email}</div>
            </div>
            <div className="avatar" title={user?.name}>
              {user?.name?.[0]?.toUpperCase() || "A"}
            </div>
            <button
              className="mobile-hide"
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

      {/* Mobile Nav Overlay */}
      {isMobileMenuOpen && (
        <div className="nav-mobile-overlay" onClick={() => setIsMobileMenuOpen(false)}>
          <div className="nav-mobile-menu" onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: "0 16px 16px 16px", borderBottom: "1px solid rgba(255,255,255,0.05)", marginBottom: 8 }}>
              <div style={{ fontSize: 14, fontWeight: 700, color: "#fff" }}>{user?.name}</div>
              <div style={{ fontSize: 12, color: "#888" }}>{user?.email}</div>
              <button
                type="button"
                onClick={handleLogout}
                style={{
                  marginTop: 12,
                  width: "100%",
                  background: "rgba(252,129,129,0.1)",
                  border: "1px solid rgba(252,129,129,0.2)",
                  color: "#fc8181",
                  borderRadius: 8,
                  padding: "8px 12px",
                  fontSize: 12,
                  cursor: "pointer",
                  fontWeight: 700
                }}
              >
                Keluar Aplikasi
              </button>
            </div>
            <NavTabs isMobile={true} />
          </div>
        </div>
      )}

      <main className="admin-main">
        {page === PAGES.DASHBOARD && <DashboardPage apiOnline={apiOnline} />}
        {page === PAGES.CHANNELS && <ChannelsPage />}
        {page === PAGES.EPG_MAPPING && <EpgMappingPage />}
        {page === PAGES.USERS && <UsersPage />}
        {page === PAGES.PACKAGES && <PackagesPage />}
        {page === PAGES.SUBSCRIPTIONS && <SubscriptionsPage />}
        {page === PAGES.APP_UPDATES && <AppUpdatesPage />}
        {page === PAGES.SETTINGS && <BackendSettingsPage />}
      </main>
    </div>
  );
}
