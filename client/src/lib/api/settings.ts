import { api } from './client';
import type { AuthResponse, MeResponse, ReportReason, SearchResults, UserSummary } from './schema';

const enc = encodeURIComponent;

export type SearchScope = 'all' | 'people' | 'circles' | 'posts';

export const searchApi = {
  search: (q: string, type: SearchScope, signal?: AbortSignal) =>
    api.get<SearchResults>('/search', { params: { q, type, limit: type === 'all' ? 6 : 20 }, signal }),
};

export const settingsApi = {
  update: (body: { privateProfile?: boolean; notificationPrefs?: Record<string, boolean> }) => api.patch<MeResponse>('/me/settings', body),
  changeEmail: (email: string, currentPassword: string) => api.put<MeResponse>('/me/email', { email, currentPassword }),
  changePassword: (newPassword: string, currentPassword?: string) => api.put<AuthResponse>('/me/password', { currentPassword, newPassword }),
  deleteAccount: (body: { password?: string; confirm?: string }) => api.delete<null>('/me', { data: body }),
  blocks: () => api.get<UserSummary[]>('/me/blocks'),
  block: (username: string) => api.post<null>(`/users/${enc(username)}/block`),
  unblock: (username: string) => api.delete<null>(`/users/${enc(username)}/block`),
  reportUser: (username: string, reason: ReportReason, details?: string) =>
    api.post<null>(`/users/${enc(username)}/report`, { reason, details }),
  reportComment: (id: string, reason: ReportReason, details?: string) => api.post<null>(`/comments/${id}/report`, { reason, details }),
};
