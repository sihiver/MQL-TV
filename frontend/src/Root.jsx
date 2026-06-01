import { useAuth } from "./context/AuthContext.jsx";
import AdminApp from "./App.jsx";
import LoginPage from "./pages/LoginPage.jsx";

export default function Root() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="login-page">
        <div style={{ color: "#888", fontSize: 14 }}>Memuat sesi…</div>
      </div>
    );
  }

  if (!user) {
    return <LoginPage />;
  }

  return <AdminApp />;
}
