import { useEffect, useRef, useState } from 'react';

const THRESHOLD = 72;

/**
 * Touch pull-to-refresh at the top of the page. Returns the current pull distance (for the
 * indicator) and whether a refresh is running.
 */
export function usePullToRefresh(onRefresh: () => Promise<unknown>) {
  const [pull, setPull] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const start = useRef<number | null>(null);
  const callback = useRef(onRefresh);
  callback.current = onRefresh;

  useEffect(() => {
    const onStart = (e: TouchEvent) => {
      start.current = window.scrollY <= 0 ? (e.touches[0]?.clientY ?? null) : null;
    };
    const onMove = (e: TouchEvent) => {
      if (start.current === null) return;
      const dy = (e.touches[0]?.clientY ?? 0) - start.current;
      setPull(dy > 0 ? Math.min(dy * 0.5, THRESHOLD * 1.5) : 0);
    };
    const onEnd = () => {
      if (start.current === null) return;
      start.current = null;
      setPull((current) => {
        if (current >= THRESHOLD) {
          setRefreshing(true);
          void callback.current().finally(() => setRefreshing(false));
        }
        return 0;
      });
    };
    window.addEventListener('touchstart', onStart, { passive: true });
    window.addEventListener('touchmove', onMove, { passive: true });
    window.addEventListener('touchend', onEnd);
    return () => {
      window.removeEventListener('touchstart', onStart);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onEnd);
    };
  }, []);

  return { pull, refreshing, ready: pull >= THRESHOLD };
}
