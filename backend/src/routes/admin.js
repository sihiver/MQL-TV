import { Router } from "express";
import { adminPanelAuth } from "../middleware/adminPanel.js";
import { getDashboardStats } from "../services/dashboardStats.js";
import usersRouter from "./admin/users.js";
import subscriptionsRouter from "./admin/subscriptions.js";
import packagesRouter from "./admin/packages.js";
import channelsRouter from "./admin/channels.js";
import packageChannelsRouter from "./admin/packageChannels.js";
import epgAdminRouter from "./admin/epg.js";
import settingsRouter from "./admin/settings.js";

const router = Router();

router.use(adminPanelAuth);

router.get("/stats", async (_req, res, next) => {
  try {
    const data = await getDashboardStats();
    res.json(data);
  } catch (err) {
    next(err);
  }
});

router.use("/users", usersRouter);
router.use("/subscriptions", subscriptionsRouter);
router.use("/packages", packagesRouter);
router.use("/packages/:packageId/channels", packageChannelsRouter);
router.use("/epg", epgAdminRouter);
router.use("/settings", settingsRouter);
router.use("/channels", channelsRouter);

export default router;
