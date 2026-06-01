import { Router } from "express";
import { authenticate } from "../middleware/auth.js";

const router = Router();
router.use(authenticate);

// TODO: import M3U dari URL user
router.get("/", (_req, res) => {
  res.json({ data: [], message: "Playlist endpoint — coming soon" });
});

export default router;
