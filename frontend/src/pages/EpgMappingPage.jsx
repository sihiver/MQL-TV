import { useCallback, useEffect, useState } from "react";
import {
  autoMapEpg,
  fetchEpgMapping,
  refreshEpgXmltvSources,
  searchEpgXmltvSources,
  syncEpg,
  updateChannelEpgMapping,
} from "../api/epg";
import { fmt } from "../utils/format";
import Badge from "../components/Badge";
import Modal from "../components/Modal";

const PAGE_SIZE = 50;

const FILTERS = [
  { id: "all", label: "Semua" },
  { id: "mapped", label: "Terpetakan" },
  { id: "unmapped", label: "Belum" },
  { id: "invalid", label: "Invalid" },
];

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

function statusBadge(status) {
  if (status === "mapped") return { label: "Terpetakan", type: "active" };
  if (status === "invalid") return { label: "Invalid", type: "offline" };
  return { label: "Belum", type: "basic" };
}

export default function EpgMappingPage() {
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [stats, setStats] = useState(null);
  const [meta, setMeta] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("unmapped");
  const [mapping, setMapping] = useState(null);
  const [epgSearch, setEpgSearch] = useState("");
  const [epgOptions, setEpgOptions] = useState([]);
  const [selectedEpgId, setSelectedEpgId] = useState("");
  const [saving, setSaving] = useState(false);
  const [autoMapping, setAutoMapping] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [msg, setMsg] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetchEpgMapping({
        search,
        filter,
        page,
        limit: PAGE_SIZE,
      });
      setRows(res.data);
      setTotal(res.total);
      setStats(res.stats);
      setMeta({
        sourceUrl: res.sourceUrl,
        xmltvChannelCount: res.xmltvChannelCount,
        cacheFetchedAt: res.cacheFetchedAt,
      });
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [search, filter, page]);

  useEffect(() => {
    const t = setTimeout(load, 300);
    return () => clearTimeout(t);
  }, [load]);

  useEffect(() => {
    setPage(1);
  }, [search, filter]);

  useEffect(() => {
    if (!mapping) return;
    const t = setTimeout(async () => {
      try {
        const res = await searchEpgXmltvSources(epgSearch, 50);
        setEpgOptions(res.data);
      } catch {
        setEpgOptions([]);
      }
    }, 250);
    return () => clearTimeout(t);
  }, [mapping, epgSearch]);

  const openMapping = (row) => {
    setMapping(row);
    setSelectedEpgId(row.epgId || row.suggestedEpgId || "");
    setEpgSearch(row.epgDisplayName || row.suggestedDisplayName || row.name || "");
    setMsg(null);
  };

  const saveMapping = async () => {
    if (!mapping) return;
    setSaving(true);
    setError(null);
    try {
      await updateChannelEpgMapping(mapping.id, selectedEpgId || null);
      setMapping(null);
      setMsg({ ok: true, text: `EPG channel "${mapping.name}" disimpan.` });
      load();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleAutoMap = async () => {
    if (!window.confirm("Auto-map akan mengisi epg_id kosong/invalid berdasarkan nama channel. Lanjutkan?")) {
      return;
    }
    setAutoMapping(true);
    setError(null);
    setMsg(null);
    try {
      const res = await autoMapEpg(true);
      setMsg({
        ok: true,
        text: `Auto-map selesai: ${res.updated} channel diperbarui.`,
      });
      load();
    } catch (e) {
      setError(e.message);
    } finally {
      setAutoMapping(false);
    }
  };

  const handleRefreshSources = async () => {
    setError(null);
    try {
      await refreshEpgXmltvSources();
      setMsg({ ok: true, text: "Daftar sumber XMLTV diperbarui dari epg.pw." });
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    setError(null);
    setMsg(null);
    try {
      const res = await syncEpg();
      setMsg({
        ok: true,
        text: `Sync EPG selesai: ${res.channelsMatched} channel · ${res.programmesImported} program.`,
      });
      load();
    } catch (e) {
      setError(e.message);
    } finally {
      setSyncing(false);
    }
  };

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

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
          <div style={{ fontSize: 20, fontWeight: 900 }}>Mapping EPG</div>
          <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>
            {loading
              ? "Memuat…"
              : stats
                ? `${fmt(stats.mapped)} terpetakan · ${fmt(stats.unmapped)} belum · ${fmt(stats.invalid)} invalid`
                : "Petakan channel ke sumber XMLTV (epg.pw)"}
          </div>
          {meta?.sourceUrl && (
            <div style={{ fontSize: 11, color: "#666", marginTop: 4, fontFamily: "monospace" }}>
              Sumber: {meta.sourceUrl} · {fmt(meta.xmltvChannelCount)} channel XMLTV
            </div>
          )}
        </div>
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <button
            type="button"
            onClick={handleRefreshSources}
            style={{
              background: "rgba(255,255,255,0.08)",
              border: "1px solid rgba(255,255,255,0.12)",
              color: "#ccc",
              borderRadius: 10,
              padding: "9px 16px",
              fontSize: 12,
              fontWeight: 700,
              cursor: "pointer",
            }}
          >
            ↻ Muat ulang XMLTV
          </button>
          <button
            type="button"
            disabled={autoMapping}
            onClick={handleAutoMap}
            style={{
              background: "rgba(99,179,237,0.2)",
              border: "1px solid rgba(99,179,237,0.4)",
              color: "#63B3ED",
              borderRadius: 10,
              padding: "9px 16px",
              fontSize: 12,
              fontWeight: 700,
              cursor: autoMapping ? "wait" : "pointer",
            }}
          >
            {autoMapping ? "Auto-map…" : "⚡ Auto-map nama"}
          </button>
          <button
            type="button"
            disabled={syncing}
            onClick={handleSync}
            style={{
              background: "rgba(246,173,85,0.2)",
              border: "1px solid rgba(246,173,85,0.4)",
              color: "#F6AD55",
              borderRadius: 10,
              padding: "9px 16px",
              fontSize: 12,
              fontWeight: 700,
              cursor: syncing ? "wait" : "pointer",
            }}
          >
            {syncing ? "Sync EPG…" : "🔄 Sync jadwal"}
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

      {msg && (
        <div
          style={{
            marginBottom: 16,
            padding: "10px 14px",
            background: msg.ok ? "rgba(104,211,145,0.1)" : "rgba(252,129,129,0.1)",
            border: `1px solid ${msg.ok ? "rgba(104,211,145,0.3)" : "rgba(252,129,129,0.3)"}`,
            borderRadius: 10,
            fontSize: 12,
            color: msg.ok ? "#68D391" : "#FC8181",
          }}
        >
          {msg.text}
        </div>
      )}

      <div style={{ display: "flex", gap: 8, marginBottom: 16, flexWrap: "wrap" }}>
        {FILTERS.map((f) => (
          <button
            key={f.id}
            type="button"
            onClick={() => setFilter(f.id)}
            style={{
              background: filter === f.id ? "#F6AD55" : "rgba(255,255,255,0.07)",
              color: "#fff",
              border: "none",
              borderRadius: 20,
              padding: "7px 16px",
              fontSize: 11,
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            {f.label}
          </button>
        ))}
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Cari channel atau epg_id…"
          style={{ ...inputStyle, maxWidth: 260, marginLeft: "auto" }}
        />
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
              {["Channel", "Kategori", "EPG ID", "Nama EPG", "Status", "Saran", "Aksi"].map((h) => (
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
            {rows.map((row, i) => {
              const badge = statusBadge(row.status);
              return (
                <tr
                  key={row.id}
                  style={{
                    borderBottom: "1px solid rgba(255,255,255,0.04)",
                    background: i % 2 === 0 ? "transparent" : "rgba(255,255,255,0.01)",
                  }}
                >
                  <td style={{ padding: "13px 16px", fontSize: 13, fontWeight: 700 }}>{row.name}</td>
                  <td style={{ padding: "13px 16px" }}>
                    <Badge label={row.category || "—"} type="basic" />
                  </td>
                  <td
                    style={{
                      padding: "13px 16px",
                      fontSize: 11,
                      fontFamily: "monospace",
                      color: row.epgId ? "#63B3ED" : "#666",
                    }}
                  >
                    {row.epgId || "—"}
                  </td>
                  <td style={{ padding: "13px 16px", fontSize: 12, color: "#aaa" }}>
                    {row.epgDisplayName || "—"}
                  </td>
                  <td style={{ padding: "13px 16px" }}>
                    <Badge label={badge.label} type={badge.type} />
                  </td>
                  <td style={{ padding: "13px 16px", fontSize: 11, color: "#888" }}>
                    {row.suggestedEpgId ? (
                      <span title={row.suggestedDisplayName || ""}>
                        {row.suggestedEpgId}
                        {row.suggestedDisplayName ? ` (${row.suggestedDisplayName})` : ""}
                      </span>
                    ) : (
                      "—"
                    )}
                  </td>
                  <td style={{ padding: "13px 16px" }}>
                    <button
                      type="button"
                      onClick={() => openMapping(row)}
                      style={{
                        background: "rgba(99,179,237,0.15)",
                        border: "1px solid rgba(99,179,237,0.35)",
                        color: "#63B3ED",
                        borderRadius: 8,
                        padding: "6px 12px",
                        fontSize: 11,
                        fontWeight: 700,
                        cursor: "pointer",
                      }}
                    >
                      Map
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>

        {!loading && rows.length === 0 && (
          <div style={{ padding: 40, textAlign: "center", color: "#666", fontSize: 13 }}>
            Tidak ada channel untuk filter ini.
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

      {mapping && (
        <Modal title={`Map EPG — ${mapping.name}`} onClose={() => setMapping(null)}>
          <p style={{ fontSize: 12, color: "#aaa", marginBottom: 16, lineHeight: 1.6 }}>
            Pilih channel XMLTV dari epg.pw. Kosongkan untuk menghapus mapping.
          </p>

          {mapping.suggestedEpgId && mapping.suggestedEpgId !== selectedEpgId && (
            <button
              type="button"
              onClick={() => {
                setSelectedEpgId(mapping.suggestedEpgId);
                setEpgSearch(mapping.suggestedDisplayName || mapping.suggestedEpgId);
              }}
              style={{
                marginBottom: 12,
                background: "rgba(104,211,145,0.15)",
                border: "1px solid rgba(104,211,145,0.35)",
                color: "#68D391",
                borderRadius: 8,
                padding: "8px 12px",
                fontSize: 11,
                fontWeight: 700,
                cursor: "pointer",
              }}
            >
              Gunakan saran: {mapping.suggestedEpgId}
              {mapping.suggestedDisplayName ? ` (${mapping.suggestedDisplayName})` : ""}
            </button>
          )}

          <div style={{ marginBottom: 12 }}>
            <div style={{ fontSize: 11, color: "#888", marginBottom: 6 }}>CARI CHANNEL XMLTV</div>
            <input
              value={epgSearch}
              onChange={(e) => setEpgSearch(e.target.value)}
              placeholder="Ketik nama channel EPG…"
              style={inputStyle}
            />
          </div>

          <div
            style={{
              maxHeight: 220,
              overflowY: "auto",
              border: "1px solid rgba(255,255,255,0.1)",
              borderRadius: 10,
              marginBottom: 16,
            }}
          >
            {epgOptions.length === 0 ? (
              <div style={{ padding: 16, fontSize: 12, color: "#666" }}>Tidak ada hasil.</div>
            ) : (
              epgOptions.map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => {
                    setSelectedEpgId(opt.id);
                    setEpgSearch(opt.displayName);
                  }}
                  style={{
                    display: "block",
                    width: "100%",
                    textAlign: "left",
                    padding: "10px 14px",
                    background:
                      selectedEpgId === opt.id ? "rgba(99,179,237,0.2)" : "transparent",
                    border: "none",
                    borderBottom: "1px solid rgba(255,255,255,0.05)",
                    color: selectedEpgId === opt.id ? "#63B3ED" : "#ddd",
                    cursor: "pointer",
                  }}
                >
                  <div style={{ fontSize: 13, fontWeight: 700 }}>{opt.displayName}</div>
                  <div style={{ fontSize: 10, color: "#888", fontFamily: "monospace" }}>{opt.id}</div>
                </button>
              ))
            )}
          </div>

          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 11, color: "#888", marginBottom: 6 }}>EPG ID TERPILIH</div>
            <input
              value={selectedEpgId}
              onChange={(e) => setSelectedEpgId(e.target.value)}
              placeholder="rcti.id"
              style={{ ...inputStyle, fontFamily: "monospace" }}
            />
          </div>

          <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
            <button
              type="button"
              onClick={() => setMapping(null)}
              style={{
                background: "rgba(255,255,255,0.08)",
                border: "none",
                color: "#aaa",
                borderRadius: 10,
                padding: "10px 18px",
                fontSize: 13,
                cursor: "pointer",
              }}
            >
              Batal
            </button>
            <button
              type="button"
              disabled={saving}
              onClick={saveMapping}
              style={{
                background: "#FF6B35",
                border: "none",
                color: "#fff",
                borderRadius: 10,
                padding: "10px 18px",
                fontSize: 13,
                fontWeight: 700,
                cursor: saving ? "wait" : "pointer",
              }}
            >
              {saving ? "Menyimpan…" : "Simpan mapping"}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
