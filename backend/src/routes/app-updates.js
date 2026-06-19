import { Router } from "express";
import { db } from "../config/database.js";

const router = Router();

// GET /api/app-updates/latest
router.get("/latest", async (req, res, next) => {
  try {
    const appId = req.query.appId || "com.mqltv";
    const { rows } = await db.query(
      "SELECT * FROM app_updates WHERE app_id = $1 ORDER BY version_code DESC LIMIT 1",
      [appId]
    );
    if (rows.length === 0) {
      return res.status(404).json({ message: "Belum ada pembaruan aplikasi." });
    }
    
    // Convert to camelCase
    const latest = rows[0];
    res.json({
      id: latest.id,
      versionCode: latest.version_code,
      versionName: latest.version_name,
      apkUrl: latest.apk_url,
      releaseNotes: latest.release_notes,
      isForceUpdate: latest.is_force_update,
      createdAt: latest.created_at,
    });
  } catch (err) {
    next(err);
  }
});

export default router;
