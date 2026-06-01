import { Router } from "express";
import { syncEpgFromUrl, getLastEpgSync, getEpgSourceUrl } from "../../services/epgSync.js";

const router = Router();

// GET /api/admin/epg/status
router.get("/status", async (_req, res, next) => {
  try {
    const last = await getLastEpgSync();
    res.json({
      sourceUrl: getEpgSourceUrl(),
      lastSync: last,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/admin/epg/sync
router.post("/sync", async (req, res, next) => {
  try {
    const url = req.body.url || getEpgSourceUrl();
    const result = await syncEpgFromUrl(url);
    res.json(result);
  } catch (err) {
    if (err.code === "ECONNABORTED" || err.code === "ENOTFOUND") {
      return res.status(502).json({ error: `Gagal mengunduh EPG: ${err.message}` });
    }
    if (err.message?.includes("XMLTV")) {
      return res.status(400).json({ error: err.message });
    }
    next(err);
  }
});

export default router;
