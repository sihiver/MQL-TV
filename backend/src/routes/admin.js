import { Router } from "express";
import { authenticate, isAdmin } from "../middleware/auth.js";

const router = Router();
router.use(authenticate, isAdmin);

router.get("/stats", (_req, res) => {
  res.json({ message: "Admin stats — coming soon" });
});

export default router;
