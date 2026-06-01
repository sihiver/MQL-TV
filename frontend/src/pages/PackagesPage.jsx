import { useCallback, useEffect, useState } from "react";
import {
  createPackage,
  deletePackage,
  fetchPackages,
  updatePackage,
} from "../api/packages";
import { fmtRp } from "../utils/format";
import ActionBtn from "../components/ActionBtn";
import Badge from "../components/Badge";
import Modal from "../components/Modal";
import PackageChannelsModal from "../components/PackageChannelsModal";

const EMPTY_FORM = {
  name: "",
  slug: "",
  price: 0,
  maxDevices: 1,
  description: "",
  features: "",
  active: true,
  sortOrder: 0,
  includesAllChannels: false,
};

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

function slugFromName(name) {
  return name
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "-")
    .replace(/[^a-z0-9-]/g, "");
}

export default function PackagesPage() {
  const [packages, setPackages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [confirmDelete, setConfirmDelete] = useState(null);
  const [saving, setSaving] = useState(false);
  const [slugTouched, setSlugTouched] = useState(false);
  const [managingChannels, setManagingChannels] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchPackages({ search });
      setPackages(res.data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    const t = setTimeout(load, 300);
    return () => clearTimeout(t);
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setSlugTouched(false);
    setShowForm(true);
  };

  const openEdit = (p) => {
    setEditing(p);
    setForm({
      name: p.name,
      slug: p.slug,
      price: p.price,
      maxDevices: p.maxDevices,
      description: p.description || "",
      features: p.features || "",
      active: p.active,
      sortOrder: p.sortOrder ?? 0,
      includesAllChannels: p.includesAllChannels ?? false,
    });
    setSlugTouched(true);
    setShowForm(true);
  };

  const onNameChange = (name) => {
    setForm((f) => ({
      ...f,
      name,
      slug: !slugTouched && !editing ? slugFromName(name) : f.slug,
    }));
  };

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      const body = {
        name: form.name,
        slug: form.slug,
        price: parseInt(form.price, 10) || 0,
        maxDevices: parseInt(form.maxDevices, 10) || 1,
        description: form.description,
        features: form.features,
        active: form.active,
        sortOrder: parseInt(form.sortOrder, 10) || 0,
        includesAllChannels: form.includesAllChannels,
      };

      if (editing) {
        await updatePackage(editing.id, body);
      } else {
        await createPackage(body);
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

  const handleDelete = async (id) => {
    try {
      await deletePackage(id);
      setConfirmDelete(null);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const activeCount = packages.filter((p) => p.active).length;

  return (
    <div className="admin-page">
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          marginBottom: 22,
          flexWrap: "wrap",
          gap: 12,
        }}
      >
        <div>
          <div style={{ fontSize: 20, fontWeight: 900 }}>Paket Langganan</div>
          <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>
            {loading ? "Memuat…" : `${packages.length} paket · ${activeCount} aktif`}
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
              placeholder="Cari paket…"
              style={{
                background: "none",
                border: "none",
                color: "#fff",
                fontSize: 13,
                outline: "none",
                width: 140,
              }}
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
            + Tambah Paket
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

      <div className="admin-grid-3" style={{ marginBottom: 24 }}>
        {packages.map((p) => (
          <div
            key={p.id}
            style={{
              background: "rgba(255,255,255,0.03)",
              border: `1px solid ${p.active ? "rgba(255,107,53,0.35)" : "rgba(255,255,255,0.08)"}`,
              borderRadius: 14,
              padding: "20px 22px",
              opacity: p.active ? 1 : 0.65,
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 12 }}>
              <div>
                <div style={{ fontSize: 18, fontWeight: 900 }}>{p.name}</div>
                <div style={{ fontSize: 11, color: "#666", fontFamily: "monospace", marginTop: 4 }}>{p.slug}</div>
              </div>
              <Badge label={p.active ? "Aktif" : "Nonaktif"} type={p.active ? "active" : "offline"} />
            </div>
            <div style={{ fontSize: 26, fontWeight: 900, color: "#FF6B35", marginBottom: 8 }}>
              {fmtRp(p.price)}
              <span style={{ fontSize: 11, color: "#888", fontWeight: 500 }}>/bulan</span>
            </div>
            <div style={{ fontSize: 12, color: "#aaa", marginBottom: 8 }}>
              Maks. {p.maxDevices} perangkat · {p.subscriptionCount} subscription
              {p.includesAllChannels ? (
                <span style={{ color: "#68D391" }}> · semua channel</span>
              ) : (
                <span> · {p.channelCount ?? 0} channel</span>
              )}
            </div>
            {p.description && (
              <div style={{ fontSize: 12, color: "#888", marginBottom: 10 }}>{p.description}</div>
            )}
            {p.features && (
              <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 14 }}>
                {p.features.split(",").map((f) => (
                  <span
                    key={f}
                    style={{
                      fontSize: 10,
                      padding: "3px 8px",
                      borderRadius: 6,
                      background: "rgba(255,255,255,0.06)",
                      color: "#ccc",
                    }}
                  >
                    {f.trim()}
                  </span>
                ))}
              </div>
            )}
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <ActionBtn label="Channel" color="#68D391" onClick={() => setManagingChannels(p)} />
              <ActionBtn label="Edit" color="#63B3ED" onClick={() => openEdit(p)} />
              <ActionBtn label="Hapus" color="#FC8181" onClick={() => setConfirmDelete(p)} />
            </div>
          </div>
        ))}
      </div>

      {!loading && packages.length === 0 && (
        <div style={{ textAlign: "center", color: "#666", padding: 40, fontSize: 13 }}>
          Belum ada paket. Jalankan seed atau tambah paket baru.
        </div>
      )}

      <div
        style={{
          background: "rgba(255,255,255,0.02)",
          border: "1px solid rgba(255,255,255,0.07)",
          borderRadius: 16,
          overflow: "hidden",
        }}
      >
        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
          <thead>
            <tr style={{ borderBottom: "1px solid rgba(255,255,255,0.08)" }}>
              {["Nama", "Slug", "Harga", "Channel", "Subs", "Status", ""].map((h) => (
                <th
                  key={h}
                  style={{
                    textAlign: "left",
                    padding: "12px 16px",
                    color: "#666",
                    fontWeight: 600,
                    letterSpacing: 0.5,
                  }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {packages.map((p) => (
              <tr key={p.id} style={{ borderBottom: "1px solid rgba(255,255,255,0.04)" }}>
                <td style={{ padding: "12px 16px", fontWeight: 700 }}>{p.name}</td>
                <td style={{ padding: "12px 16px", fontFamily: "monospace", color: "#888" }}>{p.slug}</td>
                <td style={{ padding: "12px 16px" }}>{fmtRp(p.price)}</td>
                <td style={{ padding: "12px 16px", fontSize: 11 }}>
                  {p.includesAllChannels ? "Semua" : p.channelCount ?? 0}
                </td>
                <td style={{ padding: "12px 16px" }}>{p.subscriptionCount}</td>
                <td style={{ padding: "12px 16px" }}>
                  <Badge label={p.active ? "Aktif" : "Nonaktif"} type={p.active ? "active" : "offline"} />
                </td>
                <td style={{ padding: "12px 16px" }}>
                  <div style={{ display: "flex", gap: 6 }}>
                    <ActionBtn label="Ch" color="#68D391" onClick={() => setManagingChannels(p)} />
                    <ActionBtn label="Edit" color="#63B3ED" onClick={() => openEdit(p)} />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showForm && (
      <Modal
        title={editing ? "Edit Paket" : "Tambah Paket"}
        onClose={() => setShowForm(false)}
      >
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <label style={{ fontSize: 11, color: "#888" }}>
            Nama
            <input
              style={{ ...inputStyle, marginTop: 6 }}
              value={form.name}
              onChange={(e) => onNameChange(e.target.value)}
            />
          </label>
          <label style={{ fontSize: 11, color: "#888" }}>
            Slug (untuk subscription)
            <input
              style={{ ...inputStyle, marginTop: 6, fontFamily: "monospace" }}
              value={form.slug}
              onChange={(e) => {
                setSlugTouched(true);
                setForm((f) => ({ ...f, slug: e.target.value.toLowerCase() }));
              }}
              disabled={Boolean(editing)}
            />
          </label>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
            <label style={{ fontSize: 11, color: "#888" }}>
              Harga (Rp/bulan)
              <input
                type="number"
                style={{ ...inputStyle, marginTop: 6 }}
                value={form.price}
                onChange={(e) => setForm((f) => ({ ...f, price: e.target.value }))}
              />
            </label>
            <label style={{ fontSize: 11, color: "#888" }}>
              Maks. perangkat
              <input
                type="number"
                min={1}
                style={{ ...inputStyle, marginTop: 6 }}
                value={form.maxDevices}
                onChange={(e) => setForm((f) => ({ ...f, maxDevices: e.target.value }))}
              />
            </label>
          </div>
          <label style={{ fontSize: 11, color: "#888" }}>
            Deskripsi
            <textarea
              rows={2}
              style={{ ...inputStyle, marginTop: 6, resize: "vertical" }}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </label>
          <label style={{ fontSize: 11, color: "#888" }}>
            Fitur (pisahkan koma)
            <input
              style={{ ...inputStyle, marginTop: 6 }}
              value={form.features}
              onChange={(e) => setForm((f) => ({ ...f, features: e.target.value }))}
              placeholder="HD,EPG,Catch-up"
            />
          </label>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
            <label style={{ fontSize: 11, color: "#888" }}>
              Urutan tampil
              <input
                type="number"
                style={{ ...inputStyle, marginTop: 6 }}
                value={form.sortOrder}
                onChange={(e) => setForm((f) => ({ ...f, sortOrder: e.target.value }))}
              />
            </label>
            <label style={{ fontSize: 11, color: "#888", display: "flex", alignItems: "center", gap: 8, marginTop: 20 }}>
              <input
                type="checkbox"
                checked={form.active}
                onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
              />
              Paket aktif
            </label>
          </div>
          <label style={{ fontSize: 11, color: "#888", display: "flex", alignItems: "center", gap: 8 }}>
            <input
              type="checkbox"
              checked={form.includesAllChannels}
              onChange={(e) => setForm((f) => ({ ...f, includesAllChannels: e.target.checked }))}
            />
            Akses semua channel (tanpa kurasi manual)
          </label>
          <button
            type="button"
            disabled={saving || !form.name || !form.slug}
            onClick={save}
            style={{
              background: "#FF6B35",
              border: "none",
              color: "#fff",
              borderRadius: 10,
              padding: "12px",
              fontWeight: 700,
              cursor: saving ? "wait" : "pointer",
              opacity: saving || !form.name || !form.slug ? 0.6 : 1,
            }}
          >
            {saving ? "Menyimpan…" : editing ? "Simpan Perubahan" : "Buat Paket"}
          </button>
        </div>
      </Modal>
      )}

      {managingChannels && (
        <PackageChannelsModal
          pkg={managingChannels}
          onClose={() => setManagingChannels(null)}
          onUpdated={load}
        />
      )}

      {confirmDelete && (
      <Modal title="Hapus Paket?" onClose={() => setConfirmDelete(null)}>
        <p style={{ fontSize: 13, color: "#aaa", marginBottom: 16 }}>
          Hapus paket <strong style={{ color: "#fff" }}>{confirmDelete?.name}</strong>? Tidak bisa dihapus jika masih
          dipakai subscription.
        </p>
        <div style={{ display: "flex", gap: 10 }}>
          <button
            type="button"
            onClick={() => handleDelete(confirmDelete.id)}
            style={{
              flex: 1,
              background: "rgba(252,129,129,0.2)",
              border: "1px solid rgba(252,129,129,0.4)",
              color: "#FC8181",
              borderRadius: 10,
              padding: "10px",
              fontWeight: 700,
              cursor: "pointer",
            }}
          >
            Ya, Hapus
          </button>
          <button
            type="button"
            onClick={() => setConfirmDelete(null)}
            style={{
              flex: 1,
              background: "rgba(255,255,255,0.08)",
              border: "none",
              color: "#fff",
              borderRadius: 10,
              padding: "10px",
              cursor: "pointer",
            }}
          >
            Batal
          </button>
        </div>
      </Modal>
      )}
    </div>
  );
}
