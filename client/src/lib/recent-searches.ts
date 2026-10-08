const KEY = 'connecthub-recent-searches';
const MAX = 8;

/** Recent searches live only in this browser (localStorage); failures are silently ignored. */
export function readRecentSearches(): string[] {
  try {
    const raw = localStorage.getItem(KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((x): x is string => typeof x === 'string').slice(0, MAX) : [];
  } catch {
    return [];
  }
}

export function rememberSearch(q: string): string[] {
  const term = q.trim();
  if (term.length < 2) return readRecentSearches();
  const next = [term, ...readRecentSearches().filter((x) => x.toLowerCase() !== term.toLowerCase())].slice(0, MAX);
  try {
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* storage unavailable */
  }
  return next;
}

export function forgetSearch(q: string): string[] {
  const next = readRecentSearches().filter((x) => x !== q);
  try {
    localStorage.setItem(KEY, JSON.stringify(next));
  } catch {
    /* storage unavailable */
  }
  return next;
}

export function clearRecentSearches(): string[] {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* storage unavailable */
  }
  return [];
}
