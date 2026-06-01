import { useCallback, useEffect, useState } from "react";
import {
  createUser,
  deleteUser,
  fetchUsers,
  setUserBanned,
  updateUser,
} from "../api/users";
import { fmtRp } from "../utils/format";
import ActionBtn from "../components/ActionBtn";
import Badge from "../components/Badge";
import Modal from "../components/Modal";

const PLANS = ["free", "basic", "premium"];
const ROLES = ["user", "admin"];
const EMPTY_FORM = { name: "", email: "", password: "", plan: "free", role: "user" };

const inputStyle = {
  width: "100%",
  background: "rgba(255,255,255,0.07)",
  border: "1px solid rgba(255,255,255,0.12)",
  borderRadius: 10,
  color: "#fff",
  fontSize: 13,
  padding: "10px 14px",
  outline: "none",
  boxSizing: "border-box",
};

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("Semua");
  const [selected, setSelected] = useState(null);
  const [confirmBan, setConfirmBan] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchUsers({ search, filter });
      setUsers(res.data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [search, filter]);

  useEffect(() => {
    const t = setTimeout(loadUsers, 300);
    return () => clearTimeout(t);
  }, [loadUsers]);

  const openCreate = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setShowForm(true);
  };

  const openEdit = (u) => {
    setEditing(u);
    setForm({
      name: u.name,
      email: u.email,
      password: "",
      plan: u.plan,
      role: u.role || "user",
    });
    setShowForm(true);
  };

  const saveUser = async () => {
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        const body = { name: form.name, email: form.email, plan: form.plan, role: form.role };
        if (form.password) body.password = form.password;
        const updated = await updateUser(editing.id, body);
        setUsers((p) => p.map((u) => (u.id === updated.id ? updated : u)));
        if (selected?.id === updated.id) setSelected(updated);
      } else {
        const created = await createUser(form);
        setUsers((p) => [created, ...p]);
      }
      setShowForm(false);
      setEditing(null);
      setForm(EMPTY_FORM);
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const toggleBan = async (user) => {
    try {
      const banned = user.status !== "banned";
      const updated = await setUserBanned(user.id, banned);
      setUsers((p) => p.map((u) => (u.id === updated.id ? updated : u)));
      if (selected?.id === updated.id) setSelected(updated);
      setConfirmBan(null);
    } catch (e) {
      setError(e.message);
    }
  };

  const removeUser = async (id) => {
    try {
      await deleteUser(id);
      setUsers((p) => p.filter((u) => u.id !== id));
      if (selected?.id === id) setSelected(null);
      setConfirmDelete(null);
    } catch (e) {
      setError(e.message);
    }
  };

  const activeCount = users.filter((u) => u.status === "active").length;

  return (
    <div className="admin-page" style={{ display: "flex", flexDirection: "row", padding: 0, overflow: "hidden" }}>
      <div style={{ flex: 1, padding: "28px 24px 28px 32px", overflowY: "auto", minWidth: 0 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
          <div>
            <div style={{ fontSize: 20, fontWeight: 900 }}>Manajemen User</div>
            <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>
              {loading ? "Memuat…" : `${users.length} terdaftar · ${activeCount} aktif`}
            </div>
          </div>
          <div style={{ display: "flex", gap: 10 }}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                background: "rgba(255,255,255,0.06)",
                borderRadius: 10,
                padding: "9px 14px",
                border: "1px solid rgba(255,255,255,0.1)",
              }}
            >
              <span style={{ color: "#888" }}>🔍</span>
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Cari nama / email…"
                style={{ background: "none", border: "none", color: "#fff", fontSize: 13, outline: "none", width: 200 }}
              />
            </div>
            <button
              type="button"
              onClick={openCreate}
              style={{
                background: "#FF6B35",
                border: "none",
                color: "#fff",
                borderRadius: 10,
                padding: "9px 18px",
                fontSize: 13,
                fontWeight: 700,
                cursor: "pointer",
              }}
            >
              + Tambah User
            </button>
          </div>
        </div>

        {error && (
          <div
            style={{
              marginBottom: 16,
              padding: "10px 14px",
              background: "rgba(252,129,129,0.1)",
              border: "1px solid rgba(252,129,129,0.3)",
              borderRadius: 10,
              fontSize: 12,
              color: "#FC8181",
            }}
          >
            {error}
          </div>
        )}

        <div style={{ display: "flex", gap: 8, marginBottom: 18 }}>
          {["Semua", "Premium", "Basic", "Free", "Banned"].map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setFilter(f)}
              style={{
                background: filter === f ? "#FF6B35" : "rgba(255,255,255,0.07)",
                color: "#fff",
                border: "none",
                borderRadius: 20,
                padding: "7px 16px",
                fontSize: 11,
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              {f}
            </button>
          ))}
        </div>

        <div
          style={{
            background: "rgba(255,255,255,0.02)",
            border: "1px solid rgba(255,255,255,0.07)",
            borderRadius: 16,
            overflow: "hidden",
          }}
        >
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ borderBottom: "1px solid rgba(255,255,255,0.07)" }}>
                {["User", "Plan", "Status", "Role", "Perangkat", "Bergabung", "Aksi"].map((h) => (
                  <th
                    key={h}
                    style={{ padding: "12px 14px", textAlign: "left", fontSize: 10, color: "#888", letterSpacing: 1, fontWeight: 700 }}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {!loading && users.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ padding: 32, textAlign: "center", color: "#888", fontSize: 13 }}>
                    Belum ada user. Klik &quot;+ Tambah User&quot; atau jalankan{" "}
                    <code style={{ color: "#FF6B35" }}>npm run db:seed-users</code> di backend.
                  </td>
                </tr>
              )}
              {users.map((u, i) => (
                <tr
                  key={u.id}
                  onClick={() => setSelected(u)}
                  style={{
                    borderBottom: "1px solid rgba(255,255,255,0.04)",
                    background:
                      selected?.id === u.id ? "rgba(255,107,53,0.06)" : i % 2 === 0 ? "transparent" : "rgba(255,255,255,0.01)",
                    cursor: "pointer",
                  }}
                >
                  <td style={{ padding: "12px 14px" }}>
                    <div style={{ fontSize: 13, fontWeight: 700 }}>{u.name}</div>
                    <div style={{ fontSize: 11, color: "#888" }}>{u.email}</div>
                  </td>
                  <td style={{ padding: "12px 14px" }}>
                    <Badge label={u.plan.charAt(0).toUpperCase() + u.plan.slice(1)} type={u.plan} />
                  </td>
                  <td style={{ padding: "12px 14px" }}>
                    <Badge label={u.status === "active" ? "Aktif" : "Dibanned"} type={u.status === "active" ? "active" : "banned"} />
                  </td>
                  <td style={{ padding: "12px 14px", fontSize: 12, color: "#aaa" }}>{u.role || "user"}</td>
                  <td style={{ padding: "12px 14px", fontSize: 13, color: "#ccc" }}>{u.devices} / 3</td>
                  <td style={{ padding: "12px 14px", fontSize: 11, color: "#888" }}>{u.joined}</td>
                  <td style={{ padding: "12px 14px" }} onClick={(e) => e.stopPropagation()}>
                    <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                      <ActionBtn onClick={() => openEdit(u)} label="✏" color="#63B3ED" />
                      <ActionBtn
                        onClick={() => setConfirmBan(u)}
                        label={u.status === "banned" ? "✓" : "🚫"}
                        color={u.status === "banned" ? "#68D391" : "#FC8181"}
                      />
                      <ActionBtn onClick={() => setConfirmDelete(u)} label="🗑" color="#FC8181" />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {selected && (
        <div
          style={{
            width: 280,
            flexShrink: 0,
            background: "rgba(6,6,14,0.97)",
            borderLeft: "1px solid rgba(255,255,255,0.06)",
            padding: "28px 22px",
            overflowY: "auto",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
            <div style={{ fontSize: 11, color: "#FF6B35", letterSpacing: 2 }}>DETAIL USER</div>
            <button type="button" onClick={() => setSelected(null)} style={{ background: "none", border: "none", color: "#888", fontSize: 16, cursor: "pointer" }}>
              ✕
            </button>
          </div>
          <div style={{ textAlign: "center", marginBottom: 20 }}>
            <div
              style={{
                width: 60,
                height: 60,
                borderRadius: 30,
                background: "linear-gradient(135deg,#FF6B35,#F7C59F)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: 26,
                margin: "0 auto 12px",
              }}
            >
              {selected.name[0]}
            </div>
            <div style={{ fontSize: 16, fontWeight: 800 }}>{selected.name}</div>
            <div style={{ fontSize: 12, color: "#888" }}>{selected.email}</div>
            <div style={{ marginTop: 8 }}>
              <Badge label={selected.plan.charAt(0).toUpperCase() + selected.plan.slice(1)} type={selected.plan} />
            </div>
          </div>
          {[
            { label: "Status", value: selected.status === "active" ? "✅ Aktif" : "🚫 Dibanned" },
            { label: "Role", value: selected.role || "user" },
            { label: "Bergabung", value: selected.joined },
            { label: "Perangkat", value: `${selected.devices} / 3` },
            { label: "Revenue", value: fmtRp(selected.revenue) },
          ].map((item) => (
            <div
              key={item.label}
              style={{
                display: "flex",
                justifyContent: "space-between",
                padding: "11px 0",
                borderBottom: "1px solid rgba(255,255,255,0.05)",
              }}
            >
              <span style={{ fontSize: 12, color: "#888" }}>{item.label}</span>
              <span style={{ fontSize: 12, fontWeight: 700 }}>{item.value}</span>
            </div>
          ))}
          <div style={{ marginTop: 20, display: "flex", flexDirection: "column", gap: 8 }}>
            <button type="button" onClick={() => openEdit(selected)} style={panelBtnStyle("#63B3ED")}>
              ✏ Edit User
            </button>
            <button type="button" onClick={() => setConfirmBan(selected)} style={panelBtnStyle(selected.status === "banned" ? "#68D391" : "#FC8181")}>
              {selected.status === "banned" ? "✓ Aktifkan Akun" : "🚫 Ban Akun"}
            </button>
            <button type="button" onClick={() => setConfirmDelete(selected)} style={panelBtnStyle("#FC8181")}>
              🗑 Hapus User
            </button>
          </div>
        </div>
      )}

      {showForm && (
        <Modal title={editing ? "Edit User" : "Tambah User Baru"} onClose={() => { setShowForm(false); setEditing(null); }}>
          {[
            { label: "Nama", key: "name", type: "text", placeholder: "Nama lengkap" },
            { label: "Email", key: "email", type: "email", placeholder: "user@email.com" },
            {
              label: editing ? "Password baru (kosongkan jika tidak diubah)" : "Password",
              key: "password",
              type: "password",
              placeholder: "••••••••",
            },
          ].map((f) => (
            <div key={f.key} style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>{f.label.toUpperCase()}</div>
              <input
                type={f.type}
                value={form[f.key]}
                onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.value }))}
                placeholder={f.placeholder}
                style={inputStyle}
              />
            </div>
          ))}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 16 }}>
            <div>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>PLAN</div>
              <select value={form.plan} onChange={(e) => setForm((p) => ({ ...p, plan: e.target.value }))} style={inputStyle}>
                {PLANS.map((p) => (
                  <option key={p} value={p}>
                    {p.charAt(0).toUpperCase() + p.slice(1)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>ROLE</div>
              <select value={form.role} onChange={(e) => setForm((p) => ({ ...p, role: e.target.value }))} style={inputStyle}>
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div style={{ display: "flex", gap: 10, marginTop: 8 }}>
            <button
              type="button"
              disabled={saving}
              onClick={saveUser}
              style={{
                flex: 1,
                background: "#FF6B35",
                border: "none",
                color: "#fff",
                borderRadius: 10,
                padding: 12,
                fontSize: 13,
                fontWeight: 700,
                cursor: saving ? "wait" : "pointer",
                opacity: saving ? 0.7 : 1,
              }}
            >
              {saving ? "Menyimpan…" : editing ? "Simpan Perubahan" : "Tambah User"}
            </button>
            <button
              type="button"
              onClick={() => { setShowForm(false); setEditing(null); }}
              style={{
                flex: 1,
                background: "rgba(255,255,255,0.07)",
                border: "1px solid rgba(255,255,255,0.1)",
                color: "#ccc",
                borderRadius: 10,
                padding: 12,
                fontSize: 13,
                cursor: "pointer",
              }}
            >
              Batal
            </button>
          </div>
        </Modal>
      )}

      {confirmBan && (
        <Modal title={confirmBan.status === "banned" ? "Aktifkan Akun?" : "Ban Akun?"} onClose={() => setConfirmBan(null)}>
          <p style={{ fontSize: 13, color: "#aaa", marginBottom: 20 }}>
            {confirmBan.status === "banned"
              ? `Aktifkan kembali akun ${confirmBan.name}?`
              : `Blokir akun ${confirmBan.name}? User tidak bisa login.`}
          </p>
          <div style={{ display: "flex", gap: 10 }}>
            <button
              type="button"
              onClick={() => toggleBan(confirmBan)}
              style={{
                flex: 1,
                background: confirmBan.status === "banned" ? "#68D391" : "#FC8181",
                border: "none",
                borderRadius: 10,
                padding: 12,
                fontWeight: 800,
                cursor: "pointer",
              }}
            >
              Ya
            </button>
            <button type="button" onClick={() => setConfirmBan(null)} style={{ flex: 1, ...panelBtnStyle("#888") }}>
              Batal
            </button>
          </div>
        </Modal>
      )}

      {confirmDelete && (
        <Modal title="Hapus User?" onClose={() => setConfirmDelete(null)}>
          <p style={{ fontSize: 13, color: "#aaa", marginBottom: 20 }}>
            Hapus permanen <strong>{confirmDelete.name}</strong> ({confirmDelete.email})? Tindakan ini tidak bisa dibatalkan.
          </p>
          <div style={{ display: "flex", gap: 10 }}>
            <button
              type="button"
              onClick={() => removeUser(confirmDelete.id)}
              style={{ flex: 1, background: "#FC8181", border: "none", borderRadius: 10, padding: 12, fontWeight: 800, cursor: "pointer" }}
            >
              Ya, Hapus
            </button>
            <button type="button" onClick={() => setConfirmDelete(null)} style={{ flex: 1, ...panelBtnStyle("#888") }}>
              Batal
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}

function panelBtnStyle(color) {
  return {
    background: `${color}18`,
    border: `1px solid ${color}44`,
    color,
    borderRadius: 10,
    padding: 10,
    fontSize: 12,
    fontWeight: 700,
    cursor: "pointer",
  };
}
