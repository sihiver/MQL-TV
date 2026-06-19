import { useCallback, useEffect, useState } from "react";
import { fetchUsers } from "../api/users";
import { fetchPackages } from "../api/packages";
import {
  createSubscription,
  deleteSubscription,
  fetchSubscriptions,
  renewSubscription,
  updateSubscription,
} from "../api/subscriptions";
import { fmtRp } from "../utils/format";
import ActionBtn from "../components/ActionBtn";
import Badge from "../components/Badge";
import Modal from "../components/Modal";

const STATUSES = ["active", "expired"];

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

function defaultExpires() {
  const d = new Date();
  d.setFullYear(d.getFullYear() + 1);
  return d.toISOString().slice(0, 10);
}

/** Tanggal berakhir = akhir hari (UTC) agar masih aktif sepanjang hari tersebut. */
function toExpiresIso(dateStr) {
  if (!dateStr) return null;
  // Parse as GMT+7, then convert to ISO UTC string
  const d = new Date(`${dateStr}T23:59:59.999+07:00`);
  return d.toISOString();
}

function isDateExpired(dateStr) {
  if (!dateStr) return false;
  return new Date(toExpiresIso(dateStr)) <= new Date();
}

const EMPTY_FORM = {
  userId: "",
  plan: "premium",
  status: "active",
  startedAt: new Date().toISOString().slice(0, 10),
  expiresAt: defaultExpires(),
  maxDevices: 3,
};

export default function SubscriptionsPage() {
  const [subs, setSubs] = useState([]);
  const [packages, setPackages] = useState([]);
  const [activeRevenue, setActiveRevenue] = useState(0);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState("Semua");
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [confirmDelete, setConfirmDelete] = useState(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchSubscriptions({ filter, search });
      setSubs(res.data);
      setActiveRevenue(res.activeRevenue ?? 0);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [filter, search]);

  useEffect(() => {
    const t = setTimeout(load, 300);
    return () => clearTimeout(t);
  }, [load]);

  useEffect(() => {
    fetchUsers({ filter: "Semua" })
      .then((r) => setUsers(r.data))
      .catch(() => {});
    fetchPackages({ active: true })
      .then((r) => setPackages(r.data))
      .catch(() => {});
  }, []);

  const openCreate = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setShowForm(true);
  };

  const openEdit = (s) => {
    setEditing(s);
    setForm({
      userId: String(s.userId),
      plan: s.planKey || s.plan.toLowerCase(),
      status: s.status,
      startedAt: s.start?.slice(0, 10) || new Date().toISOString().slice(0, 10),
      expiresAt: s.end?.slice(0, 10) || defaultExpires(),
      maxDevices: s.maxDevices ?? 1,
    });
    setShowForm(true);
  };

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      const body = {
        userId: parseInt(form.userId, 10),
        plan: form.plan,
        status: isDateExpired(form.expiresAt) ? "expired" : form.status,
        startedAt: new Date(form.startedAt).toISOString(),
        expiresAt: toExpiresIso(form.expiresAt),
        maxDevices: parseInt(form.maxDevices, 10) || 1,
      };

      if (editing) {
        await updateSubscription(editing.id, body);
      } else {
        await createSubscription(body);
      }
      setShowForm(false);
      setEditing(null);
      load();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleRenew = async (s) => {
    try {
      await renewSubscription(s.id, 1);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteSubscription(id);
      setConfirmDelete(null);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const formPrice = packages.find((p) => p.slug === form.plan)?.price ?? 0;
  const planFilters = ["Semua", "Active", "Expired", ...packages.map((p) => p.name)];
  const activeCount = subs.filter((s) => s.status === "active").length;

  return (
    <div className="admin-page">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 22, flexWrap: "wrap", gap: 12 }}>
        <div>
          <div style={{ fontSize: 20, fontWeight: 900 }}>Subscriptions</div>
          <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>
            {loading ? "Memuat…" : `Aktif: ${activeCount} · Revenue: ${fmtRp(activeRevenue)}/bulan`}
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
              placeholder="Cari user…"
              style={{ background: "none", border: "none", color: "#fff", fontSize: 13, outline: "none", width: 140 }}
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
            + Tambah Subscription
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

      <div className="admin-grid-4" style={{ marginBottom: 24 }}>
        {[
          { label: "Aktif", value: subs.filter((s) => s.status === "active").length, color: "#68D391" },
          { label: "Expired", value: subs.filter((s) => s.status === "expired").length, color: "#F6AD55" },
          { label: "Premium", value: subs.filter((s) => s.planKey === "premium" || s.plan === "Premium").length, color: "#FF6B35" },
          { label: "Basic", value: subs.filter((s) => s.planKey === "basic" || s.plan === "Basic").length, color: "#63B3ED" },
        ].map((c) => (
          <div
            key={c.label}
            style={{
              background: "rgba(255,255,255,0.03)",
              border: `1px solid ${c.color}33`,
              borderRadius: 14,
              padding: "18px 20px",
            }}
          >
            <div style={{ fontSize: 11, color: c.color, letterSpacing: 1, marginBottom: 6 }}>{c.label.toUpperCase()}</div>
            <div style={{ fontSize: 28, fontWeight: 900 }}>{c.value}</div>
          </div>
        ))}
      </div>

      <div style={{ display: "flex", gap: 8, marginBottom: 16, flexWrap: "wrap" }}>
        {planFilters.map((f) => (
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
          overflow: "auto",
        }}
      >
        <table style={{ width: "100%", borderCollapse: "collapse", minWidth: 900 }}>
          <thead>
            <tr style={{ borderBottom: "1px solid rgba(255,255,255,0.07)" }}>
              {["User", "Paket", "Harga", "Mulai", "Berakhir", "Status", "Perangkat", "Aksi"].map((h) => (
                <th
                  key={h}
                  style={{ padding: "12px 16px", textAlign: "left", fontSize: 10, color: "#888", letterSpacing: 1, fontWeight: 700 }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {!loading && subs.length === 0 && (
              <tr>
                <td colSpan={8} style={{ padding: 32, textAlign: "center", color: "#888" }}>
                  Belum ada data. Jalankan <code style={{ color: "#FF6B35" }}>npm run db:seed-subscriptions</code>
                </td>
              </tr>
            )}
            {subs.map((s, i) => (
              <tr
                key={s.id}
                style={{
                  borderBottom: "1px solid rgba(255,255,255,0.04)",
                  background: i % 2 === 0 ? "transparent" : "rgba(255,255,255,0.01)",
                }}
              >
                <td style={{ padding: "13px 16px" }}>
                  <div style={{ fontSize: 13, fontWeight: 700 }}>{s.user}</div>
                  <div style={{ fontSize: 11, color: "#888" }}>{s.userEmail}</div>
                </td>
                <td style={{ padding: "13px 16px" }}>
                  <Badge label={s.plan} type={(s.planKey || s.plan).toLowerCase()} />
                </td>
                <td style={{ padding: "13px 16px", fontSize: 13, color: "#FF6B35", fontWeight: 700 }}>{fmtRp(s.price)}</td>
                <td style={{ padding: "13px 16px", fontSize: 12, color: "#888" }}>{s.start}</td>
                <td style={{ padding: "13px 16px", fontSize: 12, color: "#888" }}>{s.end}</td>
                <td style={{ padding: "13px 16px" }}>
                  <Badge label={s.status === "active" ? "Aktif" : "Expired"} type={s.status === "active" ? "active" : "expired"} />
                </td>
                <td style={{ padding: "13px 16px", fontSize: 13, color: "#ccc" }}>{s.maxDevices}</td>
                <td style={{ padding: "13px 16px" }}>
                  <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                    <ActionBtn onClick={() => handleRenew(s)} label="↻" color="#68D391" />
                    <ActionBtn onClick={() => openEdit(s)} label="✏" color="#63B3ED" />
                    <ActionBtn onClick={() => setConfirmDelete(s)} label="🗑" color="#FC8181" />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showForm && (
        <Modal title={editing ? "Edit Subscription" : "Tambah Subscription"} onClose={() => { setShowForm(false); setEditing(null); }}>
          {!editing && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>USER</div>
              <select
                value={form.userId}
                onChange={(e) => setForm((p) => ({ ...p, userId: e.target.value }))}
                style={inputStyle}
                required
              >
                <option value="">Pilih user…</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name} ({u.email})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 16 }}>
            <div>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>PAKET</div>
              <select value={form.plan} onChange={(e) => setForm((p) => ({ ...p, plan: e.target.value }))} style={inputStyle}>
                {packages.map((p) => (
                  <option key={p.slug} value={p.slug}>
                    {p.name} — {fmtRp(p.price)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>STATUS</div>
              <select value={form.status} onChange={(e) => setForm((p) => ({ ...p, status: e.target.value }))} style={inputStyle}>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s === "active" ? "Aktif" : "Expired"}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 16 }}>
            <div>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>MULAI</div>
              <input type="date" value={form.startedAt} onChange={(e) => setForm((p) => ({ ...p, startedAt: e.target.value }))} style={inputStyle} />
            </div>
            <div>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>BERAKHIR</div>
              <input
                type="date"
                value={form.expiresAt}
                onChange={(e) => {
                  const expiresAt = e.target.value;
                  setForm((p) => ({
                    ...p,
                    expiresAt,
                    status: isDateExpired(expiresAt) ? "expired" : "active",
                  }));
                }}
                style={inputStyle}
              />
            </div>
          </div>

          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>MAKS PERANGKAT</div>
            <input
              type="number"
              min={1}
              max={10}
              value={form.maxDevices}
              onChange={(e) => setForm((p) => ({ ...p, maxDevices: e.target.value }))}
              style={inputStyle}
            />
          </div>

          <div style={{ fontSize: 12, color: "#888", marginBottom: 16 }}>
            Harga paket: <span style={{ color: "#FF6B35", fontWeight: 700 }}>{fmtRp(formPrice)}</span>/bulan
          </div>

          <div style={{ display: "flex", gap: 10 }}>
            <button
              type="button"
              disabled={saving}
              onClick={save}
              style={{
                flex: 1,
                background: "#FF6B35",
                border: "none",
                color: "#fff",
                borderRadius: 10,
                padding: 12,
                fontWeight: 700,
                cursor: saving ? "wait" : "pointer",
              }}
            >
              {saving ? "Menyimpan…" : editing ? "Simpan" : "Tambah"}
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
                cursor: "pointer",
              }}
            >
              Batal
            </button>
          </div>
        </Modal>
      )}

      {confirmDelete && (
        <Modal title="Hapus Subscription?" onClose={() => setConfirmDelete(null)}>
          <p style={{ fontSize: 13, color: "#aaa", marginBottom: 20 }}>
            Hapus subscription <strong>{confirmDelete.user}</strong> ({confirmDelete.plan})?
          </p>
          <div style={{ display: "flex", gap: 10 }}>
            <button
              type="button"
              onClick={() => handleDelete(confirmDelete.id)}
              style={{ flex: 1, background: "#FC8181", border: "none", borderRadius: 10, padding: 12, fontWeight: 800, cursor: "pointer" }}
            >
              Ya, Hapus
            </button>
            <button type="button" onClick={() => setConfirmDelete(null)} style={{ flex: 1, background: "rgba(255,255,255,0.07)", border: "1px solid rgba(255,255,255,0.1)", color: "#ccc", borderRadius: 10, padding: 12, cursor: "pointer" }}>
              Batal
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
