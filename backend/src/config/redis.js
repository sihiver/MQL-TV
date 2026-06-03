import Redis from "ioredis";

const REDIS_URL = process.env.REDIS_URL || "redis://127.0.0.1:6379";

/** In-memory fallback bila Redis tidak tersedia (dev). */
class MemoryRedis {
  constructor() {
    this.store = new Map();
    this.warned = false;
  }

  _warn() {
    if (!this.warned) {
      console.warn("⚠️  Redis tidak tersedia — memakai cache in-memory (dev only)");
      this.warned = true;
    }
  }

  async get(key) {
    this._warn();
    const entry = this.store.get(key);
    if (!entry) return null;
    if (entry.expiresAt && Date.now() > entry.expiresAt) {
      this.store.delete(key);
      return null;
    }
    return entry.value;
  }

  async setex(key, seconds, value) {
    this._warn();
    this.store.set(key, { value, expiresAt: Date.now() + seconds * 1000 });
    return "OK";
  }

  async del(...keys) {
    this._warn();
    keys.forEach((k) => this.store.delete(k));
    return keys.length;
  }

  async keys(pattern) {
    this._warn();
    const prefix = pattern.replace("*", "");
    return [...this.store.keys()].filter((k) => k.startsWith(prefix));
  }

  async incr(key) {
    this._warn();
    const entry = this.store.get(key);
    const next = entry ? parseInt(entry.value, 10) + 1 : 1;
    this.store.set(key, { value: String(next), expiresAt: entry?.expiresAt });
    return next;
  }

  async expire(key, seconds) {
    this._warn();
    const entry = this.store.get(key);
    if (entry) {
      entry.expiresAt = Date.now() + seconds * 1000;
      this.store.set(key, entry);
    }
    return 1;
  }
}

function createRedis() {
  const client = new Redis(REDIS_URL, {
    maxRetriesPerRequest: 1,
    lazyConnect: true,
    enableOfflineQueue: false,
  });

  client.on("error", () => {
    if (!client._usingMemory) {
      client._usingMemory = true;
      console.warn(`⚠️  Redis (${REDIS_URL}) gagal — fallback in-memory`);
    }
  });

  return client;
}

const realRedis = createRedis();
let memoryRedis = null;

function getMemory() {
  if (!memoryRedis) memoryRedis = new MemoryRedis();
  return memoryRedis;
}

async function useRedis(fn) {
  try {
    if (!realRedis._usingMemory && realRedis.status !== "ready") {
      await realRedis.connect();
    }
    if (!realRedis._usingMemory) return await fn(realRedis);
  } catch {
    realRedis._usingMemory = true;
  }
  return fn(getMemory());
}

export const redis = {
  get: (key) => useRedis((r) => r.get(key)),
  setex: (key, seconds, value) => useRedis((r) => r.setex(key, seconds, value)),
  del: (...keys) => useRedis((r) => r.del(...keys)),
  keys: (pattern) => useRedis((r) => r.keys(pattern)),
  incr: (key) => useRedis((r) => r.incr(key)),
  expire: (key, seconds) => useRedis((r) => r.expire(key, seconds)),
};
