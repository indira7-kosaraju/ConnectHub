import { create } from 'zustand';
import type { MeResponse } from '@/lib/api/schema';

export type SessionUser = MeResponse;

interface AuthState {
  /** Access token lives in memory only; the refresh token is an httpOnly cookie. */
  accessToken: string | null;
  user: SessionUser | null;
  /** True once the initial silent refresh has completed (success or not). */
  bootstrapped: boolean;
  setAccessToken: (token: string | null) => void;
  setSession: (token: string, user: SessionUser) => void;
  setUser: (user: SessionUser) => void;
  setBootstrapped: () => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>()((set) => ({
  accessToken: null,
  user: null,
  bootstrapped: false,
  setAccessToken: (accessToken) => set({ accessToken }),
  setSession: (accessToken, user) => set({ accessToken, user }),
  setUser: (user) => set({ user }),
  setBootstrapped: () => set({ bootstrapped: true }),
  clear: () => set({ accessToken: null, user: null }),
}));
