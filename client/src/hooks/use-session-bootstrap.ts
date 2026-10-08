import { useAuthStore } from '@/stores/auth-store';

/** True once the silent session restore (started in lib/session.ts) has finished. */
export function useSessionBootstrap(): boolean {
  return useAuthStore((s) => s.bootstrapped);
}
