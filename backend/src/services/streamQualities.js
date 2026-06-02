import axios from "axios";
import { parseStringPromise } from "xml2js";
import { parseIptvStreamUrl } from "../utils/iptvStreamUrl.js";
import { generateStreamToken } from "./streamToken.js";

function requestHeaders(userAgent, referer) {
  const headers = {};
  if (userAgent) headers["User-Agent"] = userAgent;
  if (referer) headers.Referer = referer;
  return headers;
}

function labelFromHeight(height) {
  if (!height) return "Kualitas";
  if (height >= 1080) return "1080p";
  if (height >= 720) return "720p";
  if (height >= 480) return "480p";
  if (height >= 360) return "360p";
  return `${height}p`;
}

/** Satu entri per resolusi; jika ada beberapa variant, ambil bandwidth tertinggi. */
function dedupeQualitiesByHeight(qualities) {
  const byKey = new Map();

  for (const q of qualities) {
    const key =
      q.height != null ? `h${q.height}` : `bw${q.bandwidth ?? byKey.size}`;
    const existing = byKey.get(key);
    if (!existing || (q.bandwidth || 0) > (existing.bandwidth || 0)) {
      byKey.set(key, q);
    }
  }

  return [...byKey.values()].sort((a, b) => (b.height || 0) - (a.height || 0));
}

function resolveUrl(uri, baseUrl) {
  try {
    return new URL(uri.trim(), baseUrl).href;
  } catch {
    return null;
  }
}

function parseHlsQualities(text, masterUrl) {
  const lines = text.split("\n").map((l) => l.trim());
  const qualities = [];
  const seen = new Set();

  for (let i = 0; i < lines.length; i++) {
    if (!lines[i].startsWith("#EXT-X-STREAM-INF")) continue;
    const line = lines[i];
    const resMatch = line.match(/RESOLUTION=(\d+)x(\d+)/i);
    const height = resMatch ? parseInt(resMatch[2], 10) : null;
    const bwMatch = line.match(/BANDWIDTH=(\d+)/i);
    const bandwidth = bwMatch ? parseInt(bwMatch[1], 10) : null;

    let uri = "";
    for (let j = i + 1; j < lines.length; j++) {
      if (!lines[j] || lines[j].startsWith("#")) continue;
      uri = lines[j];
      break;
    }
    if (!uri) continue;

    const abs = resolveUrl(uri, masterUrl);
    if (!abs || seen.has(abs)) continue;
    seen.add(abs);

    const id = height ? `h${height}` : `bw${bandwidth || qualities.length}`;
    qualities.push({
      id,
      label: labelFromHeight(height),
      height,
      bandwidth,
      url: abs,
    });
  }

  return qualities.sort((a, b) => (b.height || 0) - (a.height || 0));
}

async function parseDashQualities(xml, manifestUrl) {
  const parsed = await parseStringPromise(xml, {
    explicitArray: false,
    mergeAttrs: true,
  });

  const mpd = parsed.MPD;
  if (!mpd) return [];

  const periods = []
    .concat(mpd.Period || [])
    .filter(Boolean);
  const qualities = [];
  const seen = new Set();

  for (const period of periods) {
    const sets = []
      .concat(period.AdaptationSet || [])
      .filter(Boolean);

    for (const set of sets) {
      const mime = (set.mimeType || set.contentType || "").toLowerCase();
      if (mime && !mime.includes("video")) continue;

      const reps = []
        .concat(set.Representation || [])
        .filter(Boolean);

      for (const rep of reps) {
        const height = rep.height ? parseInt(rep.height, 10) : null;
        const bandwidth = rep.bandwidth ? parseInt(rep.bandwidth, 10) : null;

        let path = "";
        if (rep.SegmentTemplate?.media) {
          const st = rep.SegmentTemplate;
          const media = st.media;
          const init = st.initialization || "";
          path = init.replace("$RepresentationID$", rep.id || "") || media;
        }
        if (rep.BaseURL) {
          path = typeof rep.BaseURL === "string" ? rep.BaseURL : rep.BaseURL;
        }

        const base =
          (typeof set.BaseURL === "string" ? set.BaseURL : null) ||
          manifestUrl;

        const abs = path.startsWith("http")
          ? path
          : resolveUrl(path || "", manifestUrl);

        const key = `${height || 0}-${bandwidth || 0}`;
        if (seen.has(key)) continue;
        seen.add(key);

        qualities.push({
          id: height ? `h${height}` : `bw${bandwidth || qualities.length}`,
          label: labelFromHeight(height),
          height,
          bandwidth,
          url: abs || manifestUrl,
        });
      }
    }
  }

  return qualities.sort((a, b) => (b.height || 0) - (a.height || 0));
}

/**
 * Ambil daftar resolusi dari manifest HLS/DASH di CDN.
 */
export async function fetchStreamQualities(manifestUrl, userAgent, referer, userId) {
  const { url: cleanUrl } = parseIptvStreamUrl(manifestUrl);
  const headers = requestHeaders(userAgent, referer);

  let variants = [];

  try {
    const { data, headers: resHeaders } = await axios.get(cleanUrl, {
      headers,
      timeout: 15_000,
      responseType: "text",
      validateStatus: (s) => s >= 200 && s < 400,
    });

    const contentType = String(resHeaders["content-type"] || "");
    const body = typeof data === "string" ? data : String(data);

    if (
      cleanUrl.includes(".m3u8") ||
      contentType.includes("mpegurl") ||
      body.includes("#EXTM3U")
    ) {
      if (body.includes("#EXT-X-STREAM-INF")) {
        variants = parseHlsQualities(body, cleanUrl);
      }
    } else if (cleanUrl.includes(".mpd") || contentType.includes("dash+xml")) {
      variants = await parseDashQualities(body, cleanUrl);
    }
  } catch (err) {
    console.warn("[streamQualities]", cleanUrl, err.message);
  }

  variants = dedupeQualitiesByHeight(variants);

  const withToken = variants.map((v) => {
    const { streamUrl } = generateStreamToken(v.url, userId);
    return { ...v, url: streamUrl };
  });

  const data = [
    { id: "auto", label: "Otomatis", height: null, bandwidth: null, url: null },
    ...withToken,
  ];

  return { data, masterUrl: generateStreamToken(cleanUrl, userId).streamUrl };
}
