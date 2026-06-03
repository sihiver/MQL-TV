import { useEffect, useState } from "react";
import { checkHealth } from "../api/client";
import { fetchEpgStatus, syncEpg } from "../api/epg";
import { fetchServerSettings, saveServerSettings } from "../api/settings";
import { CfgCard, CfgField, CfgSelect, CfgToggle } from "../components/config/ConfigFields";

const DEFAULT_CFG = {
  dbUrl: "—",
  redisUrl: "—",
  jwtExpiry: "1d",
  streamExpiry: "6h",
  maxDevices: 3,
  rateLimit: 100,
  epgSync: "6h",
  m3uRefresh: "12h",
  debugMode: false,
  maintenanceMode: false,
  allowRegistration: true,
  requireEmailVerify: false,
};

export default function BackendSettingsPage() {
  const [cfg, setCfg] = useState(DEFAULT_CFG);
  const set = (k, v) => setCfg((p) => ({ ...p, [k]: v }));
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dbTest, setDbTest] = useState(null);
  const [epgInfo, setEpgInfo] = useState(null);
  const [epgSyncing, setEpgSyncing] = useState(false);
  const [epgMsg, setEpgMsg] = useState(null);

  useEffect(() => {
    Promise.all([
      fetchServerSettings().catch(() => null),
      fetchEpgStatus().catch(() => null),
    ]).then(([settings, epg]) => {
      if (settings) {
        setCfg((p) => ({
          ...p,
          ...settings,
          maxDevices: Number(settings.maxDevices) || 3,
          rateLimit: Number(settings.rateLimit) || 100,
        }));
      }
      if (epg) setEpgInfo(epg);
      setLoading(false);
    });
  }, []);

  const save = async () => {
    setSaveError(null);
    try {
      const payload = {
        maxDevices: Number(cfg.maxDevices) || 3,
        rateLimit: Number(cfg.rateLimit) || 100,
        jwtExpiry: cfg.jwtExpiry,
        streamExpiry: cfg.streamExpiry,
        epgSync: cfg.epgSync,
        m3uRefresh: cfg.m3uRefresh,
        debugMode: cfg.debugMode,
        maintenanceMode: cfg.maintenanceMode,
        allowRegistration: cfg.allowRegistration,
        requireEmailVerify: cfg.requireEmailVerify,
      };
      const updated = await saveServerSettings(payload);
      setCfg((p) => ({ ...p, ...updated }));
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (e) {
      setSaveError(e.message || "Gagal menyimpan");
    }
  };

  const testDb = async () => {
    setDbTest("loading");
    try {
      const data = await checkHealth({ force: true });
      setDbTest(`OK — ${data.database} @ ${new Date(data.time).toLocaleTimeString()}`);
    } catch {
      setDbTest("Gagal — pastikan backend berjalan di port 3000");
    }
  };

  return (
    <div className="admin-page">
      {loading && (
        <div style={{ fontSize: 12, color: "#888", marginBottom: 12 }}>Memuat konfigurasi…</div>
      )}
      {saveError && (
        <div
          style={{
            marginBottom: 12,
            padding: "10px 14px",
            background: "rgba(252,129,129,0.1)",
            border: "1px solid rgba(252,129,129,0.3)",
            borderRadius: 10,
            fontSize: 12,
            color: "#FC8181",
          }}
        >
          {saveError}
        </div>
      )}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 28 }}>
        <div>
          <div style={{ fontSize: 20, fontWeight: 900 }}>Konfigurasi Backend</div>
          <div style={{ fontSize: 12, color: "#888", marginTop: 2 }}>Pengaturan server, database, dan sistem</div>
        </div>
        <button
          type="button"
          onClick={save}
          style={{
            background: saved ? "#68D391" : "#FF6B35",
            border: "none",
            color: saved ? "#0d3a1a" : "#fff",
            borderRadius: 10,
            padding: "10px 22px",
            fontSize: 13,
            fontWeight: 700,
            cursor: "pointer",
          }}
        >
          {saved ? "✓ Tersimpan" : "Simpan Perubahan"}
        </button>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20 }}>
        <CfgCard title="🗄️ Database" color="#63B3ED">
          <CfgField label="PostgreSQL URL" value={cfg.dbUrl} onChange={(v) => set("dbUrl", v)} mono />
          <CfgField label="Redis URL" value={cfg.redisUrl} onChange={(v) => set("redisUrl", v)} mono />
          <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
            <button
              type="button"
              onClick={testDb}
              style={{
                flex: 1,
                background: "rgba(99,179,237,0.1)",
                border: "1px solid rgba(99,179,237,0.25)",
                color: "#63B3ED",
                borderRadius: 8,
                padding: 8,
                fontSize: 11,
                cursor: "pointer",
              }}
            >
              🔗 Test Koneksi API
            </button>
          </div>
          {dbTest && (
            <div style={{ fontSize: 11, color: dbTest.startsWith("OK") ? "#68D391" : "#FC8181", marginTop: 4 }}>{dbTest}</div>
          )}
        </CfgCard>

        <CfgCard title="🔐 Autentikasi" color="#FF6B35">
          <CfgSelect label="JWT Expiry" value={cfg.jwtExpiry} onChange={(v) => set("jwtExpiry", v)} opts={["1h", "6h", "1d", "7d", "30d"]} />
          <CfgSelect label="Stream Token Expiry" value={cfg.streamExpiry} onChange={(v) => set("streamExpiry", v)} opts={["1h", "3h", "6h", "12h", "24h"]} />
          <CfgToggle label="Wajib Verifikasi Email" value={cfg.requireEmailVerify} onChange={(v) => set("requireEmailVerify", v)} />
          <CfgToggle label="Izinkan Registrasi Baru" value={cfg.allowRegistration} onChange={(v) => set("allowRegistration", v)} />
        </CfgCard>

        <CfgCard title="⚡ Batas & Rate Limit" color="#68D391">
          <CfgField label="Maks Perangkat per User (default)" value={cfg.maxDevices} onChange={(v) => set("maxDevices", v)} type="number" />
          <CfgField label="Rate Limit (req/menit per IP)" value={cfg.rateLimit} onChange={(v) => set("rateLimit", v)} type="number" />
          <div style={{ fontSize: 10, color: "#666", marginTop: 8, lineHeight: 1.5 }}>
            Aktif di server · Batas perangkat mengikuti paket langganan jika lebih ketat · Admin panel tidak kena rate limit
          </div>
        </CfgCard>

        <CfgCard title="📺 EPG (epg.pw)" color="#F6AD55">
          <CfgField
            label="URL XMLTV"
            value={epgInfo?.sourceUrl || "https://epg.pw/xmltv/epg_ID.xml"}
            onChange={() => {}}
            mono
          />
          {epgInfo?.lastSync && (
            <div style={{ fontSize: 11, color: "#888", marginBottom: 10, lineHeight: 1.6 }}>
              Terakhir sync:{" "}
              {new Date(epgInfo.lastSync.synced_at).toLocaleString("id-ID")}
              <br />
              Channel cocok: {epgInfo.lastSync.channels_matched} · Program:{" "}
              {epgInfo.lastSync.programmes_imported}
            </div>
          )}
          {epgMsg && (
            <div
              style={{
                fontSize: 11,
                color: epgMsg.ok ? "#68D391" : "#FC8181",
                marginBottom: 10,
                lineHeight: 1.5,
              }}
            >
              {epgMsg.text}
            </div>
          )}
          <button
            type="button"
            disabled={epgSyncing}
            onClick={async () => {
              setEpgSyncing(true);
              setEpgMsg(null);
              try {
                const r = await syncEpg();
                setEpgMsg({
                  ok: true,
                  text: `Berhasil: ${r.programmesImported} program, ${r.channelsMatched} channel cocok (${Math.round(r.durationMs / 1000)}s)`,
                });
                const st = await fetchEpgStatus();
                setEpgInfo(st);
              } catch (e) {
                setEpgMsg({ ok: false, text: e.message });
              } finally {
                setEpgSyncing(false);
              }
            }}
            style={{
              width: "100%",
              background: "rgba(246,173,85,0.15)",
              border: "1px solid rgba(246,173,85,0.35)",
              color: "#F6AD55",
              borderRadius: 8,
              padding: "10px 14px",
              fontSize: 12,
              fontWeight: 700,
              cursor: epgSyncing ? "wait" : "pointer",
            }}
          >
            {epgSyncing ? "Menyinkronkan EPG…" : "🔄 Sync EPG Indonesia sekarang"}
          </button>
          <div style={{ fontSize: 10, color: "#666", marginTop: 8 }}>
            Sumber: epg.pw · Jadwal 1 hari lalu s/d 8 hari ke depan
          </div>
        </CfgCard>

        <CfgCard title="🔄 Sinkronisasi" color="#9AE6B4">
          <CfgSelect label="Interval Sync EPG (cron)" value={cfg.epgSync} onChange={(v) => set("epgSync", v)} opts={["1h", "3h", "6h", "12h", "24h"]} />
          <CfgSelect label="Refresh M3U Playlist" value={cfg.m3uRefresh} onChange={(v) => set("m3uRefresh", v)} opts={["1h", "6h", "12h", "24h"]} />
        </CfgCard>

        <CfgCard title="🛠️ Mode Sistem" color="#FC8181">
          <CfgToggle label="Mode Debug (verbose logs)" value={cfg.debugMode} onChange={(v) => set("debugMode", v)} />
          <CfgToggle label="Mode Maintenance" value={cfg.maintenanceMode} onChange={(v) => set("maintenanceMode", v)} danger />
          {cfg.maintenanceMode && (
            <div
              style={{
                padding: "10px 12px",
                background: "rgba(252,129,129,0.1)",
                border: "1px solid rgba(252,129,129,0.25)",
                borderRadius: 8,
                fontSize: 11,
                color: "#FC8181",
              }}
            >
              ⚠ Mode maintenance aktif — user tidak bisa login
            </div>
          )}
        </CfgCard>

        <CfgCard title="ℹ️ Info Server" color="#805AD5">
          {[
            { label: "Node.js", value: "v18.20.5" },
            { label: "Express", value: "4.18.2" },
            { label: "PostgreSQL", value: "16.x" },
            { label: "Redis", value: "7.2.3" },
            { label: "API URL", value: import.meta.env.VITE_API_URL || "http://localhost:3000" },
            { label: "Admin URL", value: import.meta.env.VITE_PUBLIC_URL || "http://localhost:5173" },
          ].map((item) => (
            <div
              key={item.label}
              style={{
                display: "flex",
                justifyContent: "space-between",
                padding: "8px 0",
                borderBottom: "1px solid rgba(255,255,255,0.05)",
              }}
            >
              <span style={{ fontSize: 12, color: "#888" }}>{item.label}</span>
              <span style={{ fontSize: 12, fontWeight: 700, fontFamily: "monospace", color: "#B794F4" }}>{item.value}</span>
            </div>
          ))}
        </CfgCard>
      </div>
    </div>
  );
}
