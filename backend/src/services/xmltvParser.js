import { parseStringPromise } from "xml2js";

export function asArray(value) {
  if (value == null) return [];
  return Array.isArray(value) ? value : [value];
}

export function xmlText(node) {
  if (node == null) return "";
  if (typeof node === "string") return node.trim();
  if (typeof node === "number") return String(node);
  if (Array.isArray(node)) return xmlText(node[0]);
  if (typeof node === "object") {
    return String(node._ ?? node["#text"] ?? "").trim();
  }
  return "";
}

/** XMLTV: 20260601000000 +0700 → Date UTC */
export function parseXmltvDateTime(raw) {
  if (!raw) return null;
  const s = String(raw).trim();
  const m = s.match(
    /^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})(?:\s*([+-])(\d{2})(\d{2}))?/,
  );
  if (!m) return null;

  const [, y, mo, d, h, mi, se, sign, tzh, tzm] = m;
  let utcMs = Date.UTC(+y, +mo - 1, +d, +h, +mi, +se);

  if (sign && tzh != null && tzm != null) {
    const offsetMs = (parseInt(tzh, 10) * 60 + parseInt(tzm, 10)) * 60 * 1000;
    utcMs -= sign === "+" ? offsetMs : -offsetMs;
  }

  const date = new Date(utcMs);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function normalizeChannelName(name) {
  return String(name || "")
    .toLowerCase()
    .replace(/\s*\(.*?\)\s*/g, " ")
    .replace(/\s+hd\b/g, "")
    .replace(/[^a-z0-9]+/g, "")
    .trim();
}

/**
 * @param {string} xml
 * @returns {{ channels: Array<{id:string, names:string[]}>, programmes: Array }}
 */
export async function parseXmltv(xml) {
  const doc = await parseStringPromise(xml, {
    explicitArray: false,
    trim: true,
    attrkey: "$",
  });

  const tv = doc?.tv;
  if (!tv) throw new Error("File bukan XMLTV valid (elemen <tv> tidak ditemukan)");

  const channels = asArray(tv.channel).map((ch) => {
    const id = ch?.$?.id;
    const names = asArray(ch?.["display-name"]).map(xmlText).filter(Boolean);
    return { id: String(id), names };
  });

  const programmes = asArray(tv.programme).map((p) => {
    const attrs = p?.$ || {};
    return {
      channel: String(attrs.channel || ""),
      start: attrs.start,
      stop: attrs.stop,
      title: xmlText(p?.title),
      description: xmlText(p?.desc),
      category: xmlText(p?.category),
    };
  });

  return { channels, programmes };
}
