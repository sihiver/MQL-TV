import { useState } from "react";
import { MOCK } from "../data/mock";
import { fmt } from "../utils/format";
import ActionBtn from "../components/ActionBtn";
import Badge from "../components/Badge";
import Modal from "../components/Modal";

const EMPTY_FORM = { name: "", url: "", category: "Nasional", logo: "📺" };

export default function ChannelsPage() {
  const [channels, setChannels] = useState(MOCK.channels);
  const [search, setSearch] = useState("");
  const [filterCat, setFilterCat] = useState("Semua");
  const [showAdd, setShowAdd] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const cats = ["Semua", ...new Set(channels.map((c) => c.category))];
  const filtered = channels
    .filter((c) => filterCat === "Semua" || c.category === filterCat)
    .filter((c) => c.name.toLowerCase().includes(search.toLowerCase()));

  const toggleStatus = (id) => {
    setChannels((p) =>
      p.map((c) => (c.id === id ? { ...c, status: c.status === "live" ? "offline" : "live" } : c)),
    );
  };

  const deleteChannel = (id) => setChannels((p) => p.filter((c) => c.id !== id));

  const saveChannel = () => {
    if (editing) {
      setChannels((p) => p.map((c) => (c.id === editing.id ? { ...c, ...form } : c)));
    } else {
      setChannels((p) => [
        ...p,
        { ...form, id: Date.now(), status: "offline", viewers: 0, bitrate: "—", uptime: "—" },
      ]);
    }
    setShowAdd(false);
    setEditing(null);
    setForm(EMPTY_FORM);
  };

  const openAdd = () => {
    setShowAdd(true);
    setEditing(null);
    setForm(EMPTY_FORM);
  };

  const openEdit = (ch) => {
    setEditing(ch);
    setForm({
      name: ch.name,
      url: `https://stream.example.com/${ch.name.toLowerCase()}`,
      category: ch.category,
      logo: ch.logo,
    });
    setShowAdd(true);
  };

  return (
    <div className="admin-page">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 22 }}>
        <div>
          <div style={{ fontSize: 20, fontWeight: 900 }}>Manajemen Channel</div>
          <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>
            {channels.filter((c) => c.status === "live").length} live · {channels.length} total
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
            <span style={{ color: "#888", fontSize: 13 }}>🔍</span>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Cari channel…"
              style={{ background: "none", border: "none", color: "#fff", fontSize: 13, outline: "none", width: 160 }}
            />
          </div>
          <button
            type="button"
            onClick={openAdd}
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

      <div style={{ display: "flex", gap: 8, marginBottom: 18 }}>
        {cats.map((cat) => (
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
              {["Channel", "Kategori", "Status", "Penonton", "Bitrate", "Uptime", "Aksi"].map((h) => (
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
            {filtered.map((ch, i) => (
              <tr
                key={ch.id}
                style={{
                  borderBottom: "1px solid rgba(255,255,255,0.04)",
                  background: i % 2 === 0 ? "transparent" : "rgba(255,255,255,0.01)",
                }}
              >
                <td style={{ padding: "13px 16px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <span style={{ fontSize: 20 }}>{ch.logo}</span>
                    <span style={{ fontSize: 13, fontWeight: 700 }}>{ch.name}</span>
                  </div>
                </td>
                <td style={{ padding: "13px 16px" }}>
                  <Badge label={ch.category} type="basic" />
                </td>
                <td style={{ padding: "13px 16px" }}>
                  <Badge label={ch.status === "live" ? "LIVE" : "Offline"} type={ch.status} />
                </td>
                <td style={{ padding: "13px 16px", fontSize: 13, color: "#ccc" }}>
                  {ch.status === "live" ? fmt(ch.viewers) : "—"}
                </td>
                <td style={{ padding: "13px 16px", fontSize: 12, color: "#888", fontFamily: "monospace" }}>{ch.bitrate}</td>
                <td style={{ padding: "13px 16px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                    <div style={{ height: 4, width: 48, background: "rgba(255,255,255,0.1)", borderRadius: 2 }}>
                      <div
                        style={{
                          width: `${parseFloat(ch.uptime) || 0}%`,
                          height: "100%",
                          background: parseFloat(ch.uptime) > 98 ? "#68D391" : "#F6AD55",
                          borderRadius: 2,
                        }}
                      />
                    </div>
                    <span style={{ fontSize: 11, color: "#888" }}>{ch.uptime}</span>
                  </div>
                </td>
                <td style={{ padding: "13px 16px" }}>
                  <div style={{ display: "flex", gap: 6 }}>
                    <ActionBtn onClick={() => openEdit(ch)} label="✏" color="#63B3ED" />
                    <ActionBtn onClick={() => toggleStatus(ch.id)} label={ch.status === "live" ? "⏹" : "▶"} color="#F6AD55" />
                    <ActionBtn onClick={() => deleteChannel(ch.id)} label="🗑" color="#FC8181" />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showAdd && (
        <Modal title={editing ? "Edit Channel" : "Tambah Channel Baru"} onClose={() => { setShowAdd(false); setEditing(null); }}>
          {[
            { label: "Nama Channel", key: "name", type: "text", placeholder: "Contoh: RCTI HD" },
            { label: "Stream URL", key: "url", type: "text", placeholder: "https://stream.example.com/live.m3u8" },
            { label: "Kategori", key: "category", type: "select", opts: ["Nasional", "Sport", "News", "Movie", "Documentary", "Kids"] },
            { label: "Logo (Emoji)", key: "logo", type: "text", placeholder: "📺" },
          ].map((f) => (
            <div key={f.key} style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 11, color: "#888", letterSpacing: 1, marginBottom: 6 }}>{f.label.toUpperCase()}</div>
              {f.type === "select" ? (
                <select
                  value={form[f.key]}
                  onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.value }))}
                  style={{
                    width: "100%",
                    background: "rgba(255,255,255,0.07)",
                    border: "1px solid rgba(255,255,255,0.12)",
                    borderRadius: 10,
                    color: "#fff",
                    fontSize: 13,
                    padding: "10px 14px",
                    outline: "none",
                  }}
                >
                  {f.opts.map((o) => (
                    <option key={o} value={o}>
                      {o}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  value={form[f.key]}
                  onChange={(e) => setForm((p) => ({ ...p, [f.key]: e.target.value }))}
                  placeholder={f.placeholder}
                  style={{
                    width: "100%",
                    background: "rgba(255,255,255,0.07)",
                    border: "1px solid rgba(255,255,255,0.12)",
                    borderRadius: 10,
                    color: "#fff",
                    fontSize: 13,
                    padding: "10px 14px",
                    outline: "none",
                    boxSizing: "border-box",
                  }}
                />
              )}
            </div>
          ))}
          <div style={{ display: "flex", gap: 10, marginTop: 20 }}>
            <button
              type="button"
              onClick={saveChannel}
              style={{
                flex: 1,
                background: "#FF6B35",
                border: "none",
                color: "#fff",
                borderRadius: 10,
                padding: 12,
                fontSize: 13,
                fontWeight: 700,
                cursor: "pointer",
              }}
            >
              {editing ? "Simpan Perubahan" : "Tambah Channel"}
            </button>
            <button
              type="button"
              onClick={() => { setShowAdd(false); setEditing(null); }}
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
    </div>
  );
}
