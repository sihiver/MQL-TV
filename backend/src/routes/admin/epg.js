import { Router } from "express";
import { syncEpgFromUrl, getLastEpgSync } from "../../services/epgSync.js";
import { getEpgSourceUrl } from "../../services/epgConfig.js";
import {
  autoMapChannels,
  getChannelMappingList,
  getMappingStats,
  loadXmltvChannelList,
  searchXmltvSources,
  setChannelEpgId,
} from "../../services/epgMapping.js";

const router = Router();

// GET /api/admin/epg/status
router.get("/status", async (_req, res, next) => {
  try {
    const last = await getLastEpgSync();
    const stats = await getMappingStats();
    res.json({
      sourceUrl: getEpgSourceUrl(),
      lastSync: last,
      channelStats: stats,
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/admin/epg/mapping
router.get("/mapping", async (req, res, next) => {
  try {
    const result = await getChannelMappingList({
      search: req.query.search || "",
      filter: req.query.filter || "all",
      page: req.query.page,
      limit: req.query.limit,
      url: req.query.url,
    });
    res.json(result);
  } catch (err) {
    if (err.code === "ECONNABORTED" || err.code === "ENOTFOUND") {
      return res.status(502).json({ error: `Gagal memuat sumber EPG: ${err.message}` });
    }
    next(err);
  }
});

// GET /api/admin/epg/xmltv-sources?search=
router.get("/xmltv-sources", async (req, res, next) => {
  try {
    const refresh = req.query.refresh === "1";
    if (refresh) {
      await loadXmltvChannelList(req.query.url || getEpgSourceUrl(), { refresh: true });
    }
    const result = await searchXmltvSources({
      search: req.query.search || "",
      limit: req.query.limit,
      url: req.query.url,
    });
    res.json(result);
  } catch (err) {
    if (err.code === "ECONNABORTED" || err.code === "ENOTFOUND") {
      return res.status(502).json({ error: `Gagal memuat sumber EPG: ${err.message}` });
    }
    next(err);
  }
});

// PATCH /api/admin/epg/mapping/:channelId
router.patch("/mapping/:channelId", async (req, res, next) => {
  try {
    const row = await setChannelEpgId(req.params.channelId, req.body.epgId);
    res.json({
      id: row.id,
      name: row.name,
      epgId: row.epg_id,
    });
  } catch (err) {
    if (err.status === 404) return res.status(404).json({ error: err.message });
    next(err);
  }
});

// POST /api/admin/epg/auto-map
router.post("/auto-map", async (req, res, next) => {
  try {
    const onlyEmpty = req.body.onlyEmpty !== false;
    const result = await autoMapChannels({
      onlyEmpty,
      url: req.body.url,
    });
    res.json(result);
  } catch (err) {
    if (err.code === "ECONNABORTED" || err.code === "ENOTFOUND") {
      return res.status(502).json({ error: `Gagal memuat sumber EPG: ${err.message}` });
    }
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
