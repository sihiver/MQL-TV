import "dotenv/config";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import morgan from "morgan";

import { testConnection } from "./config/database.js";
import authRoutes from "./routes/auth.js";
import channelRoutes from "./routes/channels.js";
import playlistRoutes from "./routes/playlist.js";
import epgRoutes from "./routes/epg.js";
import favoriteRoutes from "./routes/favorites.js";
import subscriptionRoutes from "./routes/subscription.js";
import deviceRoutes from "./routes/devices.js";
import adminRoutes from "./routes/admin.js";

const app = express();
const PORT = process.env.PORT || 3000;

app.use(helmet());
app.use(cors({ origin: process.env.ALLOWED_ORIGINS?.split(",") }));
app.use(express.json({ limit: "50mb" }));
app.use(morgan("combined"));

app.get("/health", async (_req, res) => {
  try {
    const dbInfo = await testConnection();
    res.json({ status: "ok", database: dbInfo.db, time: dbInfo.now });
  } catch (err) {
    res.status(503).json({ status: "error", database: err.message });
  }
});

app.use("/api/auth", authRoutes);
app.use("/api/channels", channelRoutes);
app.use("/api/playlist", playlistRoutes);
app.use("/api/epg", epgRoutes);
app.use("/api/favorites", favoriteRoutes);
app.use("/api/subscription", subscriptionRoutes);
app.use("/api/devices", deviceRoutes);
app.use("/api/admin", adminRoutes);

app.use((err, _req, res, _next) => {
  console.error(err.stack);
  res.status(err.status || 500).json({
    error: err.message || "Internal Server Error",
  });
});

let server;

async function start() {
  try {
    const dbInfo = await testConnection();
    console.log(`✅ PostgreSQL terhubung — db: ${dbInfo.db}`);
  } catch (err) {
    console.error("❌ PostgreSQL gagal:", err.message);
    process.exit(1);
  }

  if (server) {
    await new Promise((resolve) => server.close(resolve));
  }

  server = app.listen(PORT, () => {
    console.log(`🚀 Server running on http://localhost:${PORT}`);
  });

  server.on("error", (err) => {
    if (err.code === "EADDRINUSE") {
      console.error(`❌ Port ${PORT} sudah dipakai. Hentikan proses lain:`);
      console.error(`   kill -9 $(lsof -ti :${PORT})`);
      process.exit(1);
    }
    throw err;
  });
}

function shutdown() {
  if (server) {
    server.close(() => process.exit(0));
  } else {
    process.exit(0);
  }
}

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);

start();

export default app;
