import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiTarget = env.VITE_API_URL || "http://localhost:3000";
  const port = Number(env.VITE_PORT) || 5173;
  const host = env.VITE_HOST || "0.0.0.0";
  const publicUrl = env.VITE_PUBLIC_URL || `http://localhost:${port}`;

  return {
    plugins: [react()],
    server: {
      host,
      port,
      strictPort: true,
      proxy: {
        "/api": {
          target: apiTarget,
          changeOrigin: true,
        },
        "/health": {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
    preview: {
      host,
      port,
    },
    define: {
      __DEV_PUBLIC_URL__: JSON.stringify(publicUrl),
    },
  };
});
