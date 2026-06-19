import { Router } from "express";
import multer from "multer";
import fs from "fs";
import path from "path";
import { db } from "../../config/database.js";

const router = Router();

// Konfigurasi Multer untuk menyimpan file APK
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const dir = "public/uploads/apks";
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    cb(null, dir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    cb(null, "MQLTV-" + uniqueSuffix + path.extname(file.originalname));
  },
});

const upload = multer({
  storage,
  fileFilter: (req, file, cb) => {
    if (path.extname(file.originalname).toLowerCase() !== ".apk") {
      return cb(new Error("Hanya file .apk yang diperbolehkan!"));
    }
    cb(null, true);
  },
});

// GET /api/admin/app-updates (List all)
router.get("/", async (req, res, next) => {
  try {
    const { rows } = await db.query("SELECT * FROM app_updates ORDER BY version_code DESC");
    const mapped = rows.map((r) => ({
      id: r.id,
      versionCode: r.version_code,
      versionName: r.version_name,
      apkUrl: r.apk_url,
      releaseNotes: r.release_notes,
      isForceUpdate: r.is_force_update,
      createdAt: r.created_at,
      appId: r.app_id,
    }));
    res.json(mapped);
  } catch (err) {
    next(err);
  }
});

// POST /api/admin/app-updates (Create & Upload)
router.post("/", upload.single("apkFile"), async (req, res, next) => {
  try {
    const { versionCode, versionName, releaseNotes, isForceUpdate, appId } = req.body;
    
    if (!versionCode || !versionName) {
      return res.status(400).json({ error: "versionCode dan versionName wajib diisi." });
    }

    if (!req.file) {
      return res.status(400).json({ error: "File APK wajib diunggah." });
    }

    // Simpan path relatif ke database agar klien bisa menyesuaikan base URL-nya sendiri
    const apkUrl = `/public/uploads/apks/${req.file.filename}`;

    const { rows } = await db.query(
      `INSERT INTO app_updates (version_code, version_name, apk_url, release_notes, is_force_update, app_id)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [
        parseInt(versionCode),
        versionName,
        apkUrl,
        releaseNotes || "",
        isForceUpdate === "true" || isForceUpdate === true,
        appId || "com.mqltv"
      ]
    );

    res.status(201).json(rows[0]);
  } catch (err) {
    next(err);
  }
});

// DELETE /api/admin/app-updates/:id
router.delete("/:id", async (req, res, next) => {
  try {
    const { id } = req.params;
    const { rows } = await db.query("DELETE FROM app_updates WHERE id = $1 RETURNING apk_url", [id]);
    
    if (rows.length === 0) {
      return res.status(404).json({ error: "Update tidak ditemukan" });
    }

    // Ekstrak nama file dari apk_url
    const apkUrl = rows[0].apk_url;
    const parts = apkUrl.split("/");
    const filename = parts[parts.length - 1];
    const filepath = path.join(process.cwd(), "public/uploads/apks", filename);
    
    if (fs.existsSync(filepath)) {
      fs.unlinkSync(filepath);
    }

    res.json({ message: "Update berhasil dihapus" });
  } catch (err) {
    next(err);
  }
});

export default router;
