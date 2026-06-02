const EPG_PW_JSON = "https://epg.pw/api/epg.json";

function parseStart(iso) {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

function formatTimeWib(date) {
  return date.toLocaleTimeString("id-ID", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    timeZone: "Asia/Jakarta",
  });
}

/**
 * Ambil program sedang tayang + berikutnya dari epg.pw JSON API.
 */
export async function fetchEpgPwLive(epgChannelId) {
  const url = `${EPG_PW_JSON}?channel_id=${encodeURIComponent(epgChannelId)}`;
  const res = await fetch(url, {
    headers: { "User-Agent": "NusaVision-EPG/1.0" },
    signal: AbortSignal.timeout(20_000),
  });
  if (!res.ok) {
    throw new Error(`epg.pw HTTP ${res.status}`);
  }
  const body = await res.json();
  const list = Array.isArray(body.epg_list) ? body.epg_list : [];
  if (!list.length) {
    return { channelName: body.name || null, current: null, next: null };
  }

  const slots = list
    .map((item) => {
      const start = parseStart(item.start_date);
      if (!start) return null;
      return { title: item.title || "—", desc: item.desc || "", start };
    })
    .filter(Boolean)
    .sort((a, b) => a.start - b.start);

  const now = Date.now();
  let current = null;
  let next = null;

  for (let i = 0; i < slots.length; i += 1) {
    const slot = slots[i];
    const end = slots[i + 1]?.start || new Date(slot.start.getTime() + 2 * 60 * 60 * 1000);
    if (slot.start.getTime() <= now && now < end.getTime()) {
      current = {
        title: slot.title,
        description: slot.desc,
        start: slot.start.toISOString(),
        end: end.toISOString(),
        startLabel: formatTimeWib(slot.start),
        endLabel: formatTimeWib(end),
        isLive: true,
      };
      const n = slots[i + 1];
      if (n) {
        next = {
          title: n.title,
          startLabel: formatTimeWib(n.start),
        };
      }
      break;
    }
    if (slot.start.getTime() > now && !next) {
      next = {
        title: slot.title,
        startLabel: formatTimeWib(slot.start),
      };
    }
  }

  if (!current && next) {
    current = {
      title: next.title,
      description: "",
      start: null,
      end: null,
      startLabel: next.startLabel,
      endLabel: "",
      isLive: false,
    };
    const idx = slots.findIndex((s) => s.title === next.title && formatTimeWib(s.start) === next.startLabel);
    const n = idx >= 0 ? slots[idx + 1] : null;
    if (n) {
      next = { title: n.title, startLabel: formatTimeWib(n.start) };
    } else {
      next = null;
    }
  }

  return {
    channelName: body.name || null,
    current,
    next,
  };
}
