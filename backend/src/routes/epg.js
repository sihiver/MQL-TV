import { Router } from "express";
import { db } from "../config/database.js";
import { authenticate } from "../middleware/auth.js";
import {
  resolvePackageAccess,
  channelAllowedForPackage,
} from "../services/packageAccess.js";
import { getLastEpgSync } from "../services/epgSync.js";

const router = Router();
router.use(authenticate);

function mapProgram(row) {
  return {
    id: row.id,
    title: row.title,
    description: row.description || "",
    start: row.start_time,
    end: row.end_time,
    category: row.category,
  };
}

async function assertChannelAccess(userId, channelId) {
  const access = await resolvePackageAccess(userId);
  if (!access.hasPackage) {
    const err = new Error("Subscription tidak aktif");
    err.status = 403;
    throw err;
  }

  if (!access.includesAll) {
    const allowed = await channelAllowedForPackage(
      access.packageId,
      channelId,
      false,
    );
    if (!allowed) {
      const err = new Error("Channel tidak termasuk paket Anda");
      err.status = 403;
      throw err;
    }
  }

  const ch = await db.query(
    "SELECT id, name, epg_id FROM channels WHERE id = $1 AND active = true",
    [channelId],
  );
  if (!ch.rows.length) {
    const err = new Error("Channel tidak ditemukan");
    err.status = 404;
    throw err;
  }
  return ch.rows[0];
}

// GET /api/epg/status — info sync terakhir
router.get("/status", async (_req, res, next) => {
  try {
    const last = await getLastEpgSync();
    res.json({ lastSync: last });
  } catch (err) {
    next(err);
  }
});

async function getChannelEpg(req, res, next) {
  try {
    const channel = await assertChannelAccess(req.user.id, req.params.channelId);

    const result = await db.query(
      `SELECT id, title, description, start_time, end_time, category
       FROM epg_programs
       WHERE channel_id = $1
         AND end_time >= NOW() - INTERVAL '6 hours'
         AND start_time <= NOW() + INTERVAL '7 days'
       ORDER BY start_time
       LIMIT 100`,
      [channel.id],
    );

    const now = new Date();
    const data = result.rows.map((row) => {
      const p = mapProgram(row);
      return {
        ...p,
        isLive: now >= new Date(p.start) && now < new Date(p.end),
      };
    });

    res.json({
      channel: { id: channel.id, name: channel.name, epgId: channel.epg_id },
      data,
      total: data.length,
    });
  } catch (err) {
    if (err.status) return res.status(err.status).json({ error: err.message });
    next(err);
  }
}

router.get("/channel/:channelId", getChannelEpg);
router.get("/:channelId", getChannelEpg);

export default router;
