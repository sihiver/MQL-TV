import { useCallback, useEffect, useRef, useState } from "react";
import {
  createChannel,
  deleteChannel,
  fetchAdminChannels,
  fetchChannelCategories,
  importChannelsFromJson,
  toggleChannel,
  updateChannel,
} from "../api/channels";
import { fmt } from "../utils/format";
import ActionBtn from "../components/ActionBtn";
import Badge from "../components/Badge";
import Modal from "../components/Modal";

const EMPTY_FORM = {
  name: "",
  streamUrl: "",
  category: "Nasional",
  logoUrl: "",
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

const PAGE_SIZE = 50;

export default function ChannelsPage() {
  const [channels, setChannels] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");
  const [filterCat, setFilterCat] = useState("Semua");
  const [showForm, setShowForm] = useState(false);
  const [showImport, setShowImport] = useState(false);
  const [importMode, setImportMode] = useState("replace");
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const fileRef = useRef(null);

  const loadCategories = useCallback(async () => {
    try {
      const res = await fetchChannelCategories();
      setCategories(res.data.map((c) => c.category));
    } catch {
      /* ignore */
    }
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchAdminChannels({
        search,
        category: filterCat,
        page,
        limit: PAGE_SIZE,
      });
      setChannels(res.data);
      setTotal(res.total);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [search, filterCat, page]);

  useEffect(() => {
    const t = setTimeout(load, 300);
    return () => clearTimeout(t);
  }, [load]);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  useEffect(() => {
    setPage(1);
  }, [search, filterCat]);

  const catFilters = ["Semua", ...new Set([...categories, ...channels.map((c) => c.category)])];

  const openCreate = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setShowForm(true);
  };

  const openEdit = (ch) => {
    setEditing(ch);
    setForm({
      name: ch.name,
      streamUrl: ch.streamUrl,
      category: ch.category,
      logoUrl: ch.logoUrl || "",
    });
    setShowForm(true);
  };

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      const body = {
        name: form.name,
        streamUrl: form.streamUrl,
        category: form.category,
        logoUrl: form.logoUrl || null,
      };
      if (editing) {
        await updateChannel(editing.id, body);
      } else {
        await createChannel(body);
      }
      setShowForm(false);
      setEditing(null);
      load();
      loadCategories();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (ch, field) => {
    try {
      await toggleChannel(ch.id, field);
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Hapus channel ini?")) return;
    try {
      await deleteChannel(id);
      load();
      loadCategories();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleImportFile = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setImporting(true);
    setError(null);
    setImportResult(null);

    try {
      const text = await file.text();
      const json = JSON.parse(text);
      const result = await importChannelsFromJson(json, importMode);
      setImportResult(result);
      setPage(1);
      load();
      loadCategories();
    } catch (err) {
      setError(err.message);
    } finally {
      setImporting(false);
      if (fileRef.current) fileRef.current.value = "";
    }
  };

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const liveCount = channels.filter((c) => c.isLive && c.active).length;

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
          <div style={{ fontSize: 20, fontWeight: 900 }}>Manajemen Channel</div>
          <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>
            {loading ? "Memuat…" : `${fmt(total)} total · halaman ${page}/${totalPages}`}
            {!loading && channels.length > 0 && ` · ${liveCount} live di halaman ini`}
          </div>
        </div>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
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
              placeholder="Cari channel…"
              style={{
                background: "none",
                border: "none",
                color: "#fff",
                fontSize: 13,
                outline: "none",
                width: 160,
              }}
            />
          </div>
          <button
            type="button"
            onClick={() => {
              setShowImport(true);
              setImportResult(null);
            }}
            style={{
              background: "rgba(99,179,237,0.2)",
              border: "1px solid rgba(99,179,237,0.4)",
              color: "#63B3ED",
              borderRadius: 10,
              padding: "9px 18px",
              fontSize: 13,
              fontWeight: 700,
              cursor: "pointer",
            }}
          >
            ⬆ Import JSON
          </button>
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
            + Tambah Channel
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

      <div style={{ display: "flex", gap: 8, marginBottom: 18, flexWrap: "wrap", maxHeight: 120, overflowY: "auto" }}>
        {catFilters.slice(0, 24).map((cat) => (
          <button
            key={cat}
            type="button"
            onClick={() => setFilterCat(cat)}
            style={{
              background: filterCat === cat ? "#FF6B35" : "rgba(255,255,255,0.07)",
              color: "#fff",
              border: "none",
              borderRadius: 20,
              padding: "7px 16px",
              fontSize: 11,
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            {cat}
          </button>
        ))}
        {catFilters.length > 24 && (
          <span style={{ fontSize: 11, color: "#666", alignSelf: "center" }}>
            +{catFilters.length - 24} kategori
          </span>
        )}
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
              {["Channel", "Kategori", "Status", "DRM", "Sumber", "Aksi"].map((h) => (
                <th
                  key={h}
                  style={{
                    padding: "12px 16px",
                    textAlign: "left",
                    fontSize: 10,
                    color: "#888",
                    letterSpacing: 1,
                    fontWeight: 700,
                  }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {channels.map((ch, i) => (
              <tr
                key={ch.id}
                style={{
                  borderBottom: "1px solid rgba(255,255,255,0.04)",
                  background: i % 2 === 0 ? "transparent" : "rgba(255,255,255,0.01)",
                  opacity: ch.active ? 1 : 0.5,
                }}
              >
                <td style={{ padding: "13px 16px", maxWidth: 280 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    {ch.logoUrl ? (
                      <img
                        src={ch.logoUrl}
                        alt=""
                        style={{ width: 28, height: 28, borderRadius: 6, objectFit: "cover" }}
                        onError={(e) => {
                          e.target.style.display = "none";
                        }}
                      />
                    ) : (
                      <span style={{ fontSize: 20 }}>📺</span>
                    )}
                    <span
                      style={{
                        fontSize: 13,
                        fontWeight: 700,
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                      title={ch.name}
                    >
                      {ch.name}
                    </span>
                  </div>
                </td>
                <td style={{ padding: "13px 16px" }}>
                  <Badge label={ch.category} type="basic" />
                </td>
                <td style={{ padding: "13px 16px" }}>
                  <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                    <Badge
                      label={ch.isLive ? "LIVE" : "Offline"}
                      type={ch.isLive ? "live" : "offline"}
                    />
                    <Badge label={ch.active ? "Aktif" : "Nonaktif"} type={ch.active ? "active" : "offline"} />
                  </div>
                </td>
                <td style={{ padding: "13px 16px", fontSize: 11, color: "#888" }}>
                  {ch.hasDrm ? ch.drmType || "DRM" : "—"}
                </td>
                <td style={{ padding: "13px 16px", fontSize: 11, color: "#666", fontFamily: "monospace" }}>
                  {ch.sourceCategory || "—"}
                </td>
                <td style={{ padding: "13px 16px" }}>
                  <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                    <ActionBtn onClick={() => openEdit(ch)} label="✏" color="#63B3ED" />
                    <ActionBtn
                      onClick={() => handleToggle(ch, "live")}
                      label={ch.isLive ? "⏹" : "▶"}
                      color="#F6AD55"
                    />
                    <ActionBtn onClick={() => handleDelete(ch.id)} label="🗑" color="#FC8181" />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {!loading && channels.length === 0 && (
          <div style={{ padding: 40, textAlign: "center", color: "#666", fontSize: 13 }}>
            Belum ada channel. Import file gvision_channels.json untuk memulai.
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <div style={{ display: "flex", justifyContent: "center", gap: 12, marginTop: 16 }}>
          <button
            type="button"
            disabled={page <= 1}
            onClick={() => setPage((p) => p - 1)}
            style={{
              background: "rgba(255,255,255,0.08)",
              border: "none",
              color: "#fff",
              borderRadius: 8,
              padding: "8px 16px",
              cursor: page <= 1 ? "not-allowed" : "pointer",
              opacity: page <= 1 ? 0.4 : 1,
            }}
          >
            ← Sebelumnya
          </button>
          <span style={{ fontSize: 12, color: "#888", alignSelf: "center" }}>
            {page} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages}
            onClick={() => setPage((p) => p + 1)}
            style={{
              background: "rgba(255,255,255,0.08)",
              border: "none",
              color: "#fff",
              borderRadius: 8,
              padding: "8px 16px",
              cursor: page >= totalPages ? "not-allowed" : "pointer",
              opacity: page >= totalPages ? 0.4 : 1,
            }}
          >
            Berikutnya →
          </button>
        </div>
      )}

      {showForm && (
        <Modal
          title={editing ? "Edit Channel" : "Tambah Channel"}
          onClose={() => {
            setShowForm(false);
            setEditing(null);
          }}
        >
          {[
            { label: "Nama", key: "name", type: "text" },
            { label: "Stream URL", key: "streamUrl", type: "text" },
            { label: "Kategori (group)", key: "category", type: "text" },
            { label: "Logo URL", key: "logoUrl", type: "text" },
          ].map((f) => (
            <label key={f.key} style={{ display: "block", marginBottom: 14, fontSize: 11, color: "#888" }}>
              {f.label}
              <input
                style={{ ...inputStyle, marginTop: 6 }}
                value={form[f.key]}
                onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.value }))}
              />
            </label>
          ))}
          <button
            type="button"
            disabled={saving || !form.name || !form.streamUrl}
            onClick={save}
            style={{
              width: "100%",
              background: "#FF6B35",
              border: "none",
              color: "#fff",
              borderRadius: 10,
              padding: 12,
              fontWeight: 700,
              cursor: "pointer",
              opacity: saving ? 0.6 : 1,
            }}
          >
            {saving ? "Menyimpan…" : "Simpan"}
          </button>
        </Modal>
      )}

      {showImport && (
        <Modal title="Import Channel dari JSON" onClose={() => setShowImport(false)}>
          <p style={{ fontSize: 12, color: "#aaa", marginBottom: 16, lineHeight: 1.6 }}>
            Format <strong>Gvision</strong>: file dengan <code>categories[].channels[]</code> (contoh:{" "}
            <code>gvision_channels.json</code>). ~2000 channel, proses bisa 10–30 detik.
          </p>

          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 11, color: "#888", marginBottom: 8 }}>MODE IMPORT</div>
            <label style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 6, fontSize: 13 }}>
              <input
                type="radio"
                checked={importMode === "replace"}
                onChange={() => setImportMode("replace")}
              />
              Ganti semua (hapus channel lama)
            </label>
            <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
              <input
                type="radio"
                checked={importMode === "append"}
                onChange={() => setImportMode("append")}
              />
              Tambahkan ke data yang ada
            </label>
          </div>

          <input
            ref={fileRef}
            type="file"
            accept=".json,application/json"
            disabled={importing}
            onChange={handleImportFile}
            style={{ fontSize: 12, color: "#ccc", width: "100%" }}
          />

          {importing && (
            <div style={{ marginTop: 16, fontSize: 12, color: "#F6AD55" }}>
              Mengimpor… mohon tunggu
            </div>
          )}

          {importResult && (
            <div
              style={{
                marginTop: 16,
                padding: 12,
                background: "rgba(104,211,145,0.1)",
                border: "1px solid rgba(104,211,145,0.3)",
                borderRadius: 10,
                fontSize: 12,
                color: "#68D391",
              }}
            >
              Berhasil: {importResult.imported} channel diimpor. Total di database:{" "}
              {importResult.totalInDb}
            </div>
          )}
        </Modal>
      )}
    </div>
  );
}
