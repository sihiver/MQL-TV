import { useState, useEffect } from "react";
import { getAppUpdates, createAppUpdate, deleteAppUpdate } from "../api/appUpdates.js";

export default function AppUpdatesPage() {
  const [updates, setUpdates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [form, setForm] = useState({
    appId: "com.mqltv",
    versionCode: "",
    versionName: "",
    releaseNotes: "",
    isForceUpdate: false,
  });
  const [apkFile, setApkFile] = useState(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    fetchUpdates();
  }, []);

  const fetchUpdates = async () => {
    try {
      setLoading(true);
      const data = await getAppUpdates();
      setUpdates(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!apkFile) {
      alert("Harap pilih file APK!");
      return;
    }

    try {
      setUploading(true);
      const formData = new FormData();
      formData.append("appId", form.appId);
      formData.append("versionCode", form.versionCode);
      formData.append("versionName", form.versionName);
      formData.append("releaseNotes", form.releaseNotes);
      formData.append("isForceUpdate", form.isForceUpdate);
      formData.append("apkFile", apkFile);

      await createAppUpdate(formData);
      
      // Reset form
      setForm({ appId: "com.mqltv", versionCode: "", versionName: "", releaseNotes: "", isForceUpdate: false });
      setApkFile(null);
      e.target.reset(); // reset file input
      
      alert("Update berhasil dipublikasikan!");
      fetchUpdates();
    } catch (err) {
      alert("Gagal upload: " + err.message);
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm("Yakin ingin menghapus update ini?")) return;
    try {
      await deleteAppUpdate(id);
      fetchUpdates();
    } catch (err) {
      alert("Gagal menghapus: " + err.message);
    }
  };

  if (loading) return <div className="admin-page" style={{ padding: "40px", color: "#888" }}>Memuat data...</div>;
  if (error) return <div className="admin-page" style={{ padding: "40px", color: "#fc8181" }}>Error: {error}</div>;

  return (
    <div className="admin-page ota-wrapper">
      <div className="ota-header">
        <h1 className="ota-title">App Updates (OTA)</h1>
      </div>

      <div className="ota-card">
        <h2 className="ota-card-title">🚀 Publikasi Versi Baru</h2>
        <form onSubmit={handleSubmit}>
          <div className="ota-form-grid">
            <div className="ota-form-group full-width" style={{ gridColumn: "1 / -1", marginBottom: "10px" }}>
              <label className="ota-label">Aplikasi Target</label>
              <select
                className="ota-input"
                style={{ cursor: "pointer", background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.1)", color: "#fff" }}
                value={form.appId}
                onChange={(e) => setForm({ ...form, appId: e.target.value })}
              >
                <option value="com.mqltv" style={{ background: "#1a1a2e" }}>MQLTV Versi 1 (Lama)</option>
                <option value="com.sihiver.mqltv" style={{ background: "#1a1a2e" }}>MQLTV (Intermediate)</option>
                <option value="com.sihiver.mqltv2" style={{ background: "#1a1a2e" }}>MQLTV Versi 2 (Baru)</option>
              </select>
            </div>

            <div className="ota-form-group">
              <label className="ota-label">Version Code (Internal)</label>
              <input
                type="number"
                required
                className="ota-input"
                placeholder="Misal: 2"
                value={form.versionCode}
                onChange={(e) => setForm({ ...form, versionCode: e.target.value })}
              />
            </div>
            
            <div className="ota-form-group">
              <label className="ota-label">Version Name (Tampilan)</label>
              <input
                type="text"
                required
                className="ota-input"
                placeholder="Misal: 1.0.1"
                value={form.versionName}
                onChange={(e) => setForm({ ...form, versionName: e.target.value })}
              />
            </div>

            <div className="ota-form-group full-width">
              <label className="ota-label">File APK</label>
              <label className="ota-file-upload">
                <input
                  type="file"
                  accept=".apk"
                  required
                  onChange={(e) => setApkFile(e.target.files[0])}
                />
                <div className="ota-file-text">
                  {apkFile ? `📄 ${apkFile.name}` : "📂 Klik di sini untuk memilih file APK"}
                </div>
              </label>
            </div>

            <div className="ota-form-group full-width">
              <label className="ota-label">Release Notes</label>
              <textarea
                className="ota-textarea"
                placeholder="Apa yang baru di rilis ini?"
                value={form.releaseNotes}
                onChange={(e) => setForm({ ...form, releaseNotes: e.target.value })}
              />
            </div>
          </div>

          <label className="ota-checkbox-wrap">
            <input
              type="checkbox"
              checked={form.isForceUpdate}
              onChange={(e) => setForm({ ...form, isForceUpdate: e.target.checked })}
            />
            <span className="ota-checkbox-label">⚠️ Force Update (Wajib Diinstal oleh Pengguna)</span>
          </label>

          <button
            type="submit"
            disabled={uploading}
            className="ota-btn-submit"
          >
            {uploading ? "⏳ Mengunggah..." : "✨ Publikasikan Update"}
          </button>
        </form>
      </div>

      <div className="ota-table-wrap">
        <table className="ota-table">
          <thead>
            <tr>
              <th>Aplikasi</th>
              <th>Ver Code</th>
              <th>Ver Name</th>
              <th>Force Update</th>
              <th>Release Notes</th>
              <th>Tgl Dibuat</th>
              <th style={{ textAlign: "right" }}>Aksi</th>
            </tr>
          </thead>
          <tbody>
            {updates.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: "center", color: "#666", padding: "40px" }}>Belum ada update.</td>
              </tr>
            ) : (
              updates.map((u) => (
                <tr key={u.id}>
                  <td>
                    {u.appId === "com.sihiver.mqltv2" ? (
                      <span className="ota-badge force" style={{ background: "rgba(168,85,247,0.2)", color: "#d8b4fe", borderColor: "rgba(168,85,247,0.4)" }}>MQLTV 2</span>
                    ) : u.appId === "com.sihiver.mqltv" ? (
                      <span className="ota-badge" style={{ background: "rgba(236,72,153,0.2)", color: "#f9a8d4", borderColor: "rgba(236,72,153,0.4)", display: "inline-block", padding: "4px 8px", borderRadius: "12px", fontSize: "12px", fontWeight: "600" }}>MQLTV Int</span>
                    ) : (
                      <span className="ota-badge normal">MQLTV 1</span>
                    )}
                  </td>
                  <td><strong>{u.versionCode}</strong></td>
                  <td style={{ color: "#63b3ed" }}>{u.versionName}</td>
                  <td>
                    <span className={`ota-badge ${u.isForceUpdate ? "force" : "normal"}`}>
                      {u.isForceUpdate ? "Ya (Wajib)" : "Tidak"}
                    </span>
                  </td>
                  <td style={{ color: "#a0aec0", fontSize: "13px" }}>{u.releaseNotes || "-"}</td>
                  <td style={{ color: "#888", fontSize: "12px" }}>
                    {new Date(u.createdAt).toLocaleDateString("id-ID", { day: 'numeric', month: 'short', year: 'numeric' })}
                  </td>
                  <td style={{ textAlign: "right" }}>
                    <button
                      onClick={() => handleDelete(u.id)}
                      className="ota-btn-delete"
                      title="Hapus Update"
                    >
                      Hapus
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
