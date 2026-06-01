import { useEffect, useState } from "react";
import { checkHealth } from "../api/client";

const POLL_MS = 30_000;

export function useApiHealth() {
  const [apiOnline, setApiOnline] = useState(null);

  useEffect(() => {
    let cancelled = false;

    const ping = async () => {
      try {
        await checkHealth();
        if (!cancelled) setApiOnline(true);
      } catch {
        if (!cancelled) setApiOnline(false);
      }
    };

    ping();
    const id = setInterval(ping, POLL_MS);

    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, []);

  return apiOnline;
}
