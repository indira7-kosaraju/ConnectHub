/** Empty string = same origin (Vite proxy in dev, Vercel rewrites in prod). */
const apiUrl = ((import.meta.env.VITE_API_URL as string | undefined) ?? '').replace(/\/$/, '');

export const env = {
  apiUrl,
  wsUrl: (import.meta.env.VITE_WS_URL as string | undefined) || `${apiUrl}/ws`,
} as const;
