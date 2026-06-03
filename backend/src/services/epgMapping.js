import axios from "axios";
import { db } from "../config/database.js";
import { redis } from "../config/redis.js";
import { getEpgSourceUrl } from "./epgConfig.js";
import { normalizeChannelName, parseXmltv } from "./xmltvParser.js";

const CACHE_KEY = "epg:xmltv:channels";
const CACHE_TTL_SEC = 3600;

export function buildXmltvIndexes(channelList) {
  const byId = new Map();
  const byName = new Map();

  for (const ch of channelList) {
    const names = ch.names || [];
    byId.set(ch.id, { id: ch.id, names });
    for (const name of names) {
      const key = normalizeChannelName(name);
      if (key && !byName.has(key)) byName.set(key, ch.id);
    }
  }

  return { byId, byName };
}

function flattenXmltvChannels(xmltvChannels) {
  return xmltvChannels.map((ch) => ({
    id: ch.id,
    displayName: ch.names?.[0] || ch.id,
    names: ch.names || [],
  }));
}

export async function loadXmltvChannelList(url = getEpgSourceUrl(), { refresh = false } = {}) {
  if (!refresh) {
    const cached = await redis.get(CACHE_KEY);
    if (cached) {
      const parsed = JSON.parse(cached);
      if (parsed.sourceUrl === url && parsed.channels?.length) {
        return parsed;
      }
    }
  }

  const { data: xml } = await axios.get(url, {
    timeout: 120_000,
    responseType: "text",
    headers: { "User-Agent": "NusaVision-EPG-Sync/1.0" },
  });

  const { channels: xmltvChannels } = await parseXmltv(xml);
  const payload = {
    sourceUrl: url,
    fetchedAt: new Date().toISOString(),
    channels: flattenXmltvChannels(xmltvChannels),
  };

  await redis.setex(CACHE_KEY, CACHE_TTL_SEC, JSON.stringify(payload));
  return payload;
}

function suggestEpgId(channelName, byName) {
  const key = normalizeChannelName(channelName);
  return key ? byName.get(key) || null : null;
}

function resolveEpgStatus(epgId, byId) {
  if (!epgId) return "unmapped";
  return byId.has(String(epgId)) ? "mapped" : "invalid";
}

export async function getMappingStats() {
  const { rows } = await db.query(
    `SELECT
       COUNT(*)::int AS total,
       COUNT(*) FILTER (WHERE epg_id IS NOT NULL AND TRIM(epg_id) <> '')::int AS with_epg
     FROM channels`,
  );
  return rows[0];
}

export async function getChannelMappingList({
  search = "",
  filter = "all",
  page = 1,
  limit = 50,
  url,
} = {}) {
  const xmltv = await loadXmltvChannelList(url || getEpgSourceUrl());
  const { byId, byName } = buildXmltvIndexes(xmltv.channels);

  const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 50));
  const offset = (Math.max(1, parseInt(page, 10)) - 1) * lim;

  const params = [];
  const conditions = ["1=1"];

  if (search) {
    params.push(`%${search}%`);
    conditions.push(`(c.name ILIKE $${params.length} OR c.epg_id ILIKE $${params.length})`);
  }

  if (filter === "mapped") {
    conditions.push("c.epg_id IS NOT NULL AND TRIM(c.epg_id) <> ''");
  } else if (filter === "unmapped") {
    conditions.push("(c.epg_id IS NULL OR TRIM(c.epg_id) = '')");
  }

  const where = conditions.join(" AND ");

  const { rows: allRows } = await db.query(
    `SELECT c.id, c.name, c.category, c.epg_id
     FROM channels c
     WHERE ${where}
     ORDER BY c.name ASC`,
    params,
  );

  const enriched = allRows.map((row) => {
    const epgId = row.epg_id ? String(row.epg_id) : null;
    let status = resolveEpgStatus(epgId, byId);
    const suggestedEpgId = suggestEpgId(row.name, byName);
    const suggestedDisplayName = suggestedEpgId
      ? xmltv.channels.find((x) => x.id === suggestedEpgId)?.displayName || suggestedEpgId
      : null;

    return {
      id: row.id,
      name: row.name,
      category: row.category,
      epgId,
      epgDisplayName: epgId ? byId.get(epgId)?.names?.[0] || epgId : null,
      status,
      suggestedEpgId,
      suggestedDisplayName,
    };
  });

  const filtered =
    filter === "invalid"
      ? enriched.filter((r) => r.status === "invalid")
      : filter === "mapped"
        ? enriched.filter((r) => r.status === "mapped")
        : filter === "unmapped"
          ? enriched.filter((r) => r.status === "unmapped")
          : enriched;

  const stats = {
    total: enriched.length,
    mapped: enriched.filter((r) => r.status === "mapped").length,
    unmapped: enriched.filter((r) => r.status === "unmapped").length,
    invalid: enriched.filter((r) => r.status === "invalid").length,
  };

  const slice = filtered.slice(offset, offset + lim);

  return {
    data: slice,
    total: filtered.length,
    page: parseInt(page, 10) || 1,
    limit: lim,
    stats,
    sourceUrl: xmltv.sourceUrl,
    xmltvChannelCount: xmltv.channels.length,
    cacheFetchedAt: xmltv.fetchedAt,
  };
}

export async function searchXmltvSources({ search = "", limit = 40, url } = {}) {
  const xmltv = await loadXmltvChannelList(url || getEpgSourceUrl());
  const q = search.trim().toLowerCase();
  const lim = Math.min(100, Math.max(1, parseInt(limit, 10) || 40));

  let list = xmltv.channels;
  if (q) {
    list = list.filter(
      (ch) =>
        ch.id.toLowerCase().includes(q) ||
        ch.displayName.toLowerCase().includes(q) ||
        ch.names.some((n) => n.toLowerCase().includes(q)),
    );
  }

  return {
    data: list.slice(0, lim),
    total: list.length,
    sourceUrl: xmltv.sourceUrl,
    cacheFetchedAt: xmltv.fetchedAt,
  };
}

export async function setChannelEpgId(channelId, epgId) {
  const normalized =
    epgId === null || epgId === undefined || epgId === ""
      ? null
      : String(epgId).trim().slice(0, 100);

  const result = await db.query(
    `UPDATE channels SET epg_id = $1 WHERE id = $2
     RETURNING id, name, epg_id`,
    [normalized, channelId],
  );

  if (!result.rows.length) {
    const err = new Error("Channel tidak ditemukan");
    err.status = 404;
    throw err;
  }

  return result.rows[0];
}

/**
 * Cocokkan nama channel → epg_id XMLTV dan simpan ke DB.
 */
export async function autoMapChannels({ onlyEmpty = true, url } = {}) {
  const xmltv = await loadXmltvChannelList(url || getEpgSourceUrl(), { refresh: true });
  const { byId, byName } = buildXmltvIndexes(xmltv.channels);

  const { rows: dbChannels } = await db.query(
    "SELECT id, name, epg_id FROM channels ORDER BY id",
  );

  let updated = 0;
  const preview = [];

  for (const ch of dbChannels) {
    const current = ch.epg_id ? String(ch.epg_id) : null;
    const currentValid = current && byId.has(current);

    if (onlyEmpty && currentValid) continue;

    const suggested = suggestEpgId(ch.name, byName);
    if (!suggested) continue;

    const needsUpdate = current !== suggested;
    if (!needsUpdate) continue;

    await db.query("UPDATE channels SET epg_id = $1 WHERE id = $2", [suggested, ch.id]);
    updated += 1;
    preview.push({
      channelId: ch.id,
      name: ch.name,
      from: current,
      to: suggested,
    });
  }

  return {
    updated,
    preview: preview.slice(0, 50),
    xmltvChannelCount: xmltv.channels.length,
  };
}

export async function matchChannelsForSync(xmltvChannels) {
  const { byId, byName } = buildXmltvIndexes(xmltvChannels);
  const { rows: dbChannels } = await db.query(
    "SELECT id, name, epg_id FROM channels WHERE active = true",
  );

  const mapping = new Map();
  let updatedEpgIds = 0;

  for (const ch of dbChannels) {
    let xmltvId = ch.epg_id ? String(ch.epg_id) : null;

    if (xmltvId && !byId.has(xmltvId)) {
      xmltvId = null;
    }

    if (!xmltvId) {
      const key = normalizeChannelName(ch.name);
      xmltvId = byName.get(key) || null;
      if (xmltvId && ch.epg_id !== xmltvId) {
        await db.query("UPDATE channels SET epg_id = $1 WHERE id = $2", [xmltvId, ch.id]);
        updatedEpgIds += 1;
      }
    }

    if (xmltvId) {
      if (!mapping.has(xmltvId)) mapping.set(xmltvId, []);
      mapping.get(xmltvId).push(ch.id);
    }
  }

  let matchedChannels = 0;
  for (const ids of mapping.values()) matchedChannels += ids.length;

  return { mapping, matched: matchedChannels, updatedEpgIds };
}
