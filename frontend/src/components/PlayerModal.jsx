import { useEffect, useRef, useState } from "react";
import shaka from "shaka-player";
import { apiFetch } from "../api/http";

export default function PlayerModal({ channel, onClose }) {
  const videoRef = useRef(null);
  const playerRef = useRef(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    shaka.polyfill.installAll();
    if (!shaka.Player.isBrowserSupported()) {
      setError("Browser tidak mendukung Shaka Player");
      return;
    }

    const initPlayer = async () => {
      let playInfo = channel;
      try {
        playInfo = await apiFetch(`/api/admin/channels/${channel.id}/play-info`);
      } catch (err) {
        console.error("Gagal mengambil detail channel", err);
      }

      const player = new shaka.Player(videoRef.current);
      playerRef.current = player;

      player.addEventListener("error", (e) => {
        console.error("Error code", e.detail.code, "object", e.detail);
        if (e.detail.severity === 2 /* CRITICAL */) {
          setError(`Player error (${e.detail.code})`);
        }
      });

      // Configure DRM
      const drmConfig = {};
      const typeLow = (playInfo.drmType || "").toLowerCase();
      let isClearKey = typeLow.includes("clearkey");
      const isUrl = playInfo.drmKey && playInfo.drmKey.startsWith("http");

      if (!isClearKey && playInfo.drmKey && playInfo.drmKey.includes(":") && !isUrl) {
        // Fallback: Contains kid:key but labeled widevine
        isClearKey = true;
      }

      if (isClearKey && playInfo.drmKey && !isUrl) {
        const clearKeys = {};
        const keys = playInfo.drmKey.split(",");
        for (const k of keys) {
          const [kid, key] = k.split(":");
          if (kid && key) {
            clearKeys[kid.trim()] = key.trim();
          }
        }
        if (Object.keys(clearKeys).length > 0) {
          drmConfig.clearKeys = clearKeys;
        }
      } else if (playInfo.drmType && isUrl) {
        // Widevine or PlayReady with license server URL
        drmConfig.servers = {
          [playInfo.drmType]: playInfo.drmKey
        };
      }

      player.configure({
        drm: drmConfig,
      });

      try {
        await player.load(playInfo.streamUrl);
        console.log("The video has now been loaded!");
      } catch (e) {
        console.error("Error loading video", e);
        setError(`Gagal memuat video (${e.code || "Network Error"})`);
      }
    };

    initPlayer();

    return () => {
      if (playerRef.current) {
        playerRef.current.destroy();
      }
    };
  }, [channel]);

  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0,0,0,0.85)",
        zIndex: 9999,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
      onClick={onClose}
    >
      <div
        style={{
          width: "100%",
          maxWidth: 900,
          backgroundColor: "#111",
          borderRadius: 12,
          overflow: "hidden",
          position: "relative",
          boxShadow: "0 20px 40px rgba(0,0,0,0.5)",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          style={{
            padding: "12px 16px",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            borderBottom: "1px solid rgba(255,255,255,0.1)",
            background: "#1a1a1a",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            {channel.logoUrl && (
              <img
                src={channel.logoUrl}
                alt=""
                style={{ width: 24, height: 24, borderRadius: 4, objectFit: "cover" }}
                onError={(e) => (e.target.style.display = "none")}
              />
            )}
            <div style={{ fontWeight: 600, fontSize: 14 }}>{channel.name}</div>
            {channel.drmType && (
              <div
                style={{
                  fontSize: 10,
                  background: "#e53e3e",
                  color: "#fff",
                  padding: "2px 6px",
                  borderRadius: 4,
                  fontWeight: 700,
                  marginLeft: 8,
                }}
              >
                {channel.drmType.toUpperCase()}
              </div>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{
              background: "none",
              border: "none",
              color: "#aaa",
              fontSize: 20,
              cursor: "pointer",
              lineHeight: 1,
            }}
          >
            ×
          </button>
        </div>

        {error && (
          <div
            style={{
              position: "absolute",
              top: 20,
              left: "50%",
              transform: "translateX(-50%)",
              background: "rgba(0,0,0,0.85)",
              color: "#ff6b6b",
              padding: "12px 20px",
              borderRadius: 8,
              zIndex: 10,
              textAlign: "center",
              fontSize: 14,
              display: "flex",
              alignItems: "center",
              gap: 12,
              boxShadow: "0 4px 12px rgba(0,0,0,0.5)",
            }}
          >
            <span>{error}</span>
            <button
              onClick={() => setError(null)}
              style={{
                background: "rgba(255,255,255,0.1)",
                border: "none",
                color: "#fff",
                cursor: "pointer",
                fontSize: 16,
                padding: "4px 8px",
                borderRadius: 4,
                lineHeight: 1,
              }}
            >
              ×
            </button>
          </div>
        )}

        <video
          ref={videoRef}
          controls
          autoPlay
          style={{ width: "100%", maxHeight: "70vh", backgroundColor: "#000", display: "block" }}
        />
      </div>
    </div>
  );
}
