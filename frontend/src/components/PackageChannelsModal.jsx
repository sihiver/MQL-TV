import { useCallback, useEffect, useState } from "react";
import { fetchChannelCategories } from "../api/channels";
import {
  addChannelsToPackage,
  addPackageChannelsByCategory,
  clearPackageChannels,
  fetchPackageChannels,
  fetchPackageChannelsAvailable,
  removeChannelFromPackage,
  updatePackage,
} from "../api/packages";
import ActionBtn from "./ActionBtn";
import Modal from "./Modal";

const inputStyle = {
  width: "100%",
  background: "rgba(255,255,255,0.07)",
  border: "1px solid rgba(255,255,255,0.12)",
  borderRadius: 10,
  color: "#fff",
  fontSize: 13,
  padding: "9px 12px",
  outline: "none",
  boxSizing: "border-box",
};

export default function PackageChannelsModal({ pkg, onClose, onUpdated }) {
  const [includesAll, setIncludesAll] = useState(pkg.includesAllChannels);
  const [inPackage, setInPackage] = useState([]);
  const [inTotal, setInTotal] = useState(0);
  const [available, setAvailable] = useState([]);
  const [categories, setCategories] = useState([]);
  const [searchIn, setSearchIn] = useState("");
  const [searchAdd, setSearchAdd] = useState("");
  const [filterCat, setFilterCat] = useState("");
  const [selected, setSelected] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [availablePage, setAvailablePage] = useState(1);
  const [availableTotal, setAvailableTotal] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [msg, setMsg] = useState(null);

  const AVAIL_LIMIT = 100;

  const loadInPackage = useCallback(async () => {
    const res = await fetchPackageChannels(pkg.id, { search: searchIn, limit: 200 });
    setInPackage(res.data);
    setInTotal(res.total);
  }, [pkg.id, searchIn]);

  const loadAvailable = useCallback(async (page = 1, append = false) => {
    if (includesAll) return;
    const res = await fetchPackageChannelsAvailable(pkg.id, {
      search: searchAdd,
      category: filterCat || undefined,
      page,
      limit: AVAIL_LIMIT,
    });
    setAvailable((prev) => append ? [...prev, ...res.data] : res.data);
    setAvailableTotal(res.total);
    setAvailablePage(page);
  }, [pkg.id, searchAdd, filterCat, includesAll]);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      loadInPackage(),
      loadAvailable(1),
      fetchChannelCategories().then((r) => setCategories(r.data.map((c) => c.category ?? c))),
    ])
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [loadInPackage, loadAvailable]);

  useEffect(() => {
    const t = setTimeout(() => {
      loadInPackage().catch((e) => setError(e.message));
    }, 300);
    return () => clearTimeout(t);
  }, [loadInPackage]);

  useEffect(() => {
    const t = setTimeout(() => {
      setAvailablePage(1);
      loadAvailable(1).catch(() => {});
    }, 300);
    return () => clearTimeout(t);
  }, [loadAvailable]);

  const toggleIncludesAll = async (checked) => {
    setBusy(true);
    setError(null);
    try {
      await updatePackage(pkg.id, { includesAllChannels: checked });
      setIncludesAll(checked);
      setMsg(checked ? "Mode: semua channel aktif" : "Mode: channel pilihan");
      onUpdated?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const addSelected = async () => {
    if (!selected.size) return;
    setBusy(true);
    setError(null);
    try {
      const r = await addChannelsToPackage(pkg.id, [...selected]);
      setMsg(`${r.added} channel ditambahkan`);
      setSelected(new Set());
      await loadInPackage();
      await loadAvailable(1);
      onUpdated?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const addByCategory = async () => {
    if (!filterCat) return;
    setBusy(true);
    setError(null);
    try {
      const r = await addPackageChannelsByCategory(pkg.id, [filterCat]);
      setMsg(`${r.added} channel dari kategori "${filterCat}" ditambahkan`);
      await loadInPackage();
      await loadAvailable(1);
      onUpdated?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const removeOne = async (channelId) => {
    setBusy(true);
    try {
      await removeChannelFromPackage(pkg.id, channelId);
      await loadInPackage();
      await loadAvailable();
      onUpdated?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const handleClearAll = async () => {
    if (!window.confirm(`Kosongkan semua channel di paket ${pkg.name}?`)) return;
    setBusy(true);
    try {
      const r = await clearPackageChannels(pkg.id);
      setMsg(`${r.removed} channel dihapus dari paket`);
      await loadInPackage();
      await loadAvailable(1);
      onUpdated?.();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const toggleSelect = (id) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  return (
    <Modal title={`Channel — ${pkg.name}`} onClose={onClose}>
      <p style={{ fontSize: 12, color: "#888", marginBottom: 12, lineHeight: 1.5 }}>
        Tentukan channel yang bisa ditonton pelanggan dengan paket <strong>{pkg.slug}</strong>.
      </p>

      <label
        style={{
          display: "flex",
          alignItems: "center",
          gap: 10,
          marginBottom: 16,
          fontSize: 13,
          cursor: busy ? "wait" : "pointer",
        }}
      >
        <input
          type="checkbox"
          checked={includesAll}
          disabled={busy}
          onChange={(e) => toggleIncludesAll(e.target.checked)}
        />
        Semua channel (tanpa pilih manual)
      </label>

      {error && (
        <div style={{ color: "#FC8181", fontSize: 12, marginBottom: 10 }}>{error}</div>
      )}
      {msg && (
        <div style={{ color: "#68D391", fontSize: 12, marginBottom: 10 }}>{msg}</div>
      )}

      {includesAll ? (
        <div style={{ padding: 20, textAlign: "center", color: "#888", fontSize: 13 }}>
          Pelanggan paket ini melihat seluruh channel aktif di sistem.
        </div>
      ) : (
        <>
          <div style={{ marginBottom: 20 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
              <span style={{ fontSize: 11, color: "#FF6B35", fontWeight: 700 }}>
                DI PAKET ({inTotal})
              </span>
              {inTotal > 0 && (
                <button
                  type="button"
                  onClick={handleClearAll}
                  disabled={busy}
                  style={{
                    background: "none",
                    border: "none",
                    color: "#FC8181",
                    fontSize: 11,
                    cursor: "pointer",
                  }}
                >
                  Kosongkan semua
                </button>
              )}
            </div>
            <input
              style={inputStyle}
              placeholder="Cari di paket…"
              value={searchIn}
              onChange={(e) => setSearchIn(e.target.value)}
            />
            <div
              style={{
                maxHeight: 160,
                overflowY: "auto",
                marginTop: 8,
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: 10,
              }}
            >
              {loading && <div style={{ padding: 12, fontSize: 12, color: "#666" }}>Memuat…</div>}
              {!loading && inPackage.length === 0 && (
                <div style={{ padding: 12, fontSize: 12, color: "#666" }}>Belum ada channel</div>
              )}
              {inPackage.map((ch) => (
                <div
                  key={ch.id}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "8px 12px",
                    borderBottom: "1px solid rgba(255,255,255,0.04)",
                    fontSize: 12,
                  }}
                >
                  <span>
                    {ch.name}{" "}
                    <span style={{ color: "#666" }}>({ch.category})</span>
                  </span>
                  <ActionBtn label="×" color="#FC8181" onClick={() => removeOne(ch.id)} />
                </div>
              ))}
            </div>
          </div>

          <div>
            <div style={{ fontSize: 11, color: "#63B3ED", fontWeight: 700, marginBottom: 8 }}>
              TAMBAH CHANNEL
            </div>
            <div style={{ display: "flex", gap: 8, marginBottom: 8, flexWrap: "wrap" }}>
              <input
                style={{ ...inputStyle, flex: 1, minWidth: 140 }}
                placeholder="Cari channel…"
                value={searchAdd}
                onChange={(e) => setSearchAdd(e.target.value)}
              />
              <select
                value={filterCat}
                onChange={(e) => setFilterCat(e.target.value)}
                style={{ ...inputStyle, width: 140 }}
              >
                <option value="">Kategori…</option>
                {categories.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>
            <div style={{ display: "flex", gap: 8, marginBottom: 10 }}>
              <button
                type="button"
                disabled={busy || !selected.size}
                onClick={addSelected}
                style={{
                  flex: 1,
                  background: "#FF6B35",
                  border: "none",
                  color: "#fff",
                  borderRadius: 8,
                  padding: "8px",
                  fontSize: 12,
                  fontWeight: 700,
                  cursor: "pointer",
                  opacity: selected.size ? 1 : 0.5,
                }}
              >
                + Tambah terpilih ({selected.size})
              </button>
              <button
                type="button"
                disabled={busy || !filterCat}
                onClick={addByCategory}
                style={{
                  flex: 1,
                  background: "rgba(99,179,237,0.2)",
                  border: "1px solid rgba(99,179,237,0.4)",
                  color: "#63B3ED",
                  borderRadius: 8,
                  padding: "8px",
                  fontSize: 12,
                  fontWeight: 700,
                  cursor: "pointer",
                  opacity: filterCat ? 1 : 0.5,
                }}
              >
                + Semua kategori
              </button>
            </div>
            <div
              style={{
                maxHeight: 200,
                overflowY: "auto",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: 10,
              }}
            >
              {available.map((ch) => (
                <label
                  key={ch.id}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    padding: "8px 12px",
                    borderBottom: "1px solid rgba(255,255,255,0.04)",
                    fontSize: 12,
                    cursor: "pointer",
                  }}
                >
                  <input
                    type="checkbox"
                    checked={selected.has(ch.id)}
                    onChange={() => toggleSelect(ch.id)}
                  />
                  <span>
                    {ch.name} <span style={{ color: "#666" }}>({ch.category})</span>
                  </span>
                </label>
              ))}
              {!available.length && (
                <div style={{ padding: 12, color: "#666", fontSize: 12 }}>Tidak ada channel tersedia</div>
              )}
              {available.length > 0 && available.length < availableTotal && (
                <button
                  type="button"
                  onClick={() => loadAvailable(availablePage + 1, true)}
                  style={{
                    width: "100%",
                    background: "rgba(255,255,255,0.05)",
                    border: "none",
                    color: "#aaa",
                    padding: "10px",
                    fontSize: 12,
                    cursor: "pointer",
                  }}
                >
                  Muat lebih… ({available.length}/{availableTotal})
                </button>
              )}
            </div>
          </div>
        </>
      )}
    </Modal>
  );
}
