import { Router } from "express";
import jwt from "jsonwebtoken";
import { db } from "../config/database.js";
import { touchChannelView } from "../services/channelViews.js";

const router = Router();

router.post("/", async (req, res, next) => {
  try {
    const { appKey, status, channelTitle } = req.body;

    if (!appKey) {
      return res.status(401).json({ error: "Missing appKey (token)" });
    }

    let decoded;
    try {
      decoded = jwt.verify(appKey, process.env.JWT_SECRET);
    } catch (err) {
      return res.status(401).json({ error: "Invalid token" });
    }

    const userId = decoded.id;

    if (status === "offline") {
      // User closed player, shift last view back to remove from 'watching now'
      await db.query(
        `UPDATE channel_views 
         SET viewed_at = NOW() - INTERVAL '3 minutes' - INTERVAL '1 second'
         WHERE id = (
           SELECT id FROM channel_views 
           WHERE user_id = $1 
           ORDER BY viewed_at DESC LIMIT 1
         )`,
        [userId]
      );
      return res.json({ success: true, message: "Marked offline" });
    }

    if ((status === "online" || status === "heartbeat") && channelTitle) {
      // Find channel by title
      const chRes = await db.query(
        "SELECT id FROM channels WHERE name = $1 LIMIT 1",
        [channelTitle]
      );
      
      if (chRes.rows.length > 0) {
        const channelId = chRes.rows[0].id;
        await touchChannelView(db, userId, channelId);
      }
      return res.json({ success: true, message: "Heartbeat accepted" });
    }

    res.json({ success: true, message: "Ignored" });
  } catch (err) {
    next(err);
  }
});

export default router;
