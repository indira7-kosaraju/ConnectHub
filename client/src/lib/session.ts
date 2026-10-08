import { refreshAccessToken } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth-store';

/**
 * The silent session restore starts as soon as this module is imported (from main.tsx), in
 * parallel with route-chunk downloads, instead of waiting for React to mount.
 */
export const sessionReady: Promise<void> = refreshAccessToken()
  .then(() => undefined)
  .finally(() => useAuthStore.getState().setBootstrapped());

export const isSignedIn = () => !!useAuthStore.getState().user;
