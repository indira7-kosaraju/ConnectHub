import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { env } from '@/lib/env';
import { useAuthStore } from '@/stores/auth-store';
import type { ApiError, ApiResponse } from './types';
import type { AuthResponse } from './schema';

/** Error thrown by the typed API helpers; carries the server's error code and field errors. */
export class ApiRequestError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fields: Record<string, string>;

  constructor(status: number, error: ApiError | null | undefined) {
    super(error?.message ?? 'Something went wrong');
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = error?.code ?? 'UNKNOWN';
    this.fields = error?.fields ?? {};
  }
}

export const http = axios.create({
  baseURL: `${env.apiUrl}/api/v1`,
  withCredentials: true, // refresh token is an httpOnly cookie
  timeout: 20_000,
});

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// --- Refresh on 401 -------------------------------------------------------
// A single in-flight refresh is shared by every request that 401s concurrently.
let refreshing: Promise<string | null> | null = null;

/** Exchanges the httpOnly refresh cookie for a new access token and stores the session. */
export function refreshAccessToken(): Promise<string | null> {
  refreshing ??= axios
    .post<ApiResponse<AuthResponse>>(`${env.apiUrl}/api/v1/auth/refresh`, null, { withCredentials: true })
    .then((res) => {
      const session = res.data.data;
      if (!session) throw new Error('Empty refresh response');
      useAuthStore.getState().setSession(session.accessToken, session.user);
      return session.accessToken;
    })
    .catch(() => {
      useAuthStore.getState().clear();
      return null;
    })
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

http.interceptors.response.use(
  (res) => res,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as RetriableConfig | undefined;
    const isAuthCall = original?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && original && !original._retried && !isAuthCall) {
      original._retried = true;
      const token = await refreshAccessToken();
      if (token) {
        original.headers.set('Authorization', `Bearer ${token}`);
        return http(original);
      }
    }
    return Promise.reject(new ApiRequestError(error.response?.status ?? 0, error.response?.data?.error ?? {
      code: error.code ?? 'NETWORK_ERROR',
      message: error.response ? 'Something went wrong' : 'Network error — check your connection',
    }));
  },
);

async function unwrap<T>(p: Promise<{ data: ApiResponse<T>; status: number }>): Promise<T> {
  const res = await p;
  if (!res.data.success) {
    throw new ApiRequestError(res.status, res.data.error);
  }
  return res.data.data as T;
}

/** Typed helpers that unwrap the `{ success, data, error }` envelope. */
export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) => unwrap<T>(http.get<ApiResponse<T>>(url, config)),
  post: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(http.post<ApiResponse<T>>(url, body, config)),
  put: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(http.put<ApiResponse<T>>(url, body, config)),
  patch: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
    unwrap<T>(http.patch<ApiResponse<T>>(url, body, config)),
  delete: <T>(url: string, config?: AxiosRequestConfig) => unwrap<T>(http.delete<ApiResponse<T>>(url, config)),
};
