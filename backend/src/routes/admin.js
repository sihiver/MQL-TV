import { Router } from "express";
import { adminPanelAuth } from "../middleware/adminPanel.js";
import usersRouter from "./admin/users.js";
import subscriptionsRouter from "./admin/subscriptions.js";
import packagesRouter from "./admin/packages.js";
import channelsRouter from "./admin/channels.js";

const router = Router();

router.use(adminPanelAuth);

router.get("/stats", (_req, res) => {
  res.json({ message: "Admin stats — coming soon" });
});

router.use("/users", usersRouter);
router.use("/subscriptions", subscriptionsRouter);
router.use("/packages", packagesRouter);
router.use("/channels", channelsRouter);

export default router;
