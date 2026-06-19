import { useState, useEffect } from "react";
import { getAppUpdates, createAppUpdate, deleteAppUpdate } from "../api/appUpdates.js";

export default function AppUpdatesPage() {
  const [updates, setUpdates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [form, setForm] = useState({
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
      formData.append("versionCode", form.versionCode);
      formData.append("versionName", form.versionName);
      formData.append("releaseNotes", form.releaseNotes);
      formData.append("isForceUpdate", form.isForceUpdate);
      formData.append("apkFile", apkFile);

      await createAppUpdate(formData);
      
      // Reset form
      setForm({ versionCode: "", versionName: "", releaseNotes: "", isForceUpdate: false });
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
