import { Router } from "express";
import { adminPanelAuth } from "../middleware/adminPanel.js";
import usersRouter from "./admin/users.js";

const router = Router();

router.use(adminPanelAuth);

router.get("/stats", (_req, res) => {
  res.json({ message: "Admin stats — coming soon" });
});

router.use("/users", usersRouter);

export default router;
