import { createContext, useCallback, useContext, useEffect, useState } from "react";
import {
  clearSession,
  fetchMe,
  getStoredUser,
  getToken,
  loginAdmin,
  logout as apiLogout,
  setSession,
} from "../api/auth.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser);
  const [loading, setLoading] = useState(!!getToken());

  const restoreSession = useCallback(async () => {
    const token = getToken();
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const me = await fetchMe();
      if (me.role !== "admin") {
        clearSession();
        setUser(null);
      } else {
        setSession(token, me);
        setUser(me);
      }
    } catch {
      clearSession();
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    restoreSession();
    const onLogout = () => setUser(null);
    window.addEventListener("auth:logout", onLogout);
    return () => window.removeEventListener("auth:logout", onLogout);
  }, [restoreSession]);

  const login = async (email, password) => {
    const data = await loginAdmin(email, password);
    setUser(data.user);
    return data.user;
  };

  const logout = async () => {
    await apiLogout();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth harus di dalam AuthProvider");
  return ctx;
}
