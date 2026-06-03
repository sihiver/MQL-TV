import { Router } from "express";
import {
  getPublicSettingsPayload,
  getServerSettings,
  updateServerSettings,
} from "../../services/serverSettings.js";

const router = Router();

router.get("/", async (_req, res, next) => {
  try {
    const settings = await getServerSettings();
    res.json(getPublicSettingsPayload(settings));
  } catch (err) {
    next(err);
  }
});

router.put("/", async (req, res, next) => {
  try {
    const updated = await updateServerSettings(req.body);
    res.json(getPublicSettingsPayload(updated));
  } catch (err) {
    next(err);
  }
});

export default router;
