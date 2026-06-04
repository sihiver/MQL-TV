import { useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import { API_BASE } from "../api/client.js";

export default function LoginPage() {
  const { login } = useAuth();
  const [email, setEmail] = useState("admin@nusavision.id");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showPass, setShowPass] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email.trim(), password);
    } catch (err) {
      setError(err.message || "Login gagal");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <span className="login-brand-nusa">MQL</span>
          <span className="login-brand-vision">TV</span>
          <div className="login-brand-sub">ADMIN PANEL</div>
        </div>

        <p className="login-desc">Masuk dengan akun administrator untuk mengelola platform.</p>

        <form onSubmit={handleSubmit} className="login-form">
          {error && <div className="login-error">{error}</div>}

          <label className="login-label">
            EMAIL
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@nusavision.id"
              autoComplete="email"
              required
              className="login-input"
            />
          </label>

          <label className="login-label">
            PASSWORD
            <div className="login-pass-wrap">
              <input
                type={showPass ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                autoComplete="current-password"
                required
                className="login-input"
              />
              <button
                type="button"
                className="login-pass-toggle"
                onClick={() => setShowPass((v) => !v)}
                tabIndex={-1}
              >
                {showPass ? "🙈" : "👁"}
              </button>
            </div>
          </label>

          <button type="submit" disabled={loading} className="login-submit">
            {loading ? "Memuat…" : "Masuk"}
          </button>
        </form>
      </div>
    </div>
  );
}
