import { api } from './client';
import type { CursorPage } from './types';
import type { ConversationDto, MessageDto, NotificationDto, ReadReceipt, UserSummary } from './schema';

export interface SendMessage {
  content?: string;
  sharedPostId?: string;
  clientId?: string;
}

export const messagesApi = {
  conversations: (cursor: string | null) =>
    api.get<CursorPage<ConversationDto>>('/conversations', { params: { cursor: cursor ?? undefined, limit: 30 } }),
  start: (userId: string) => api.post<ConversationDto>('/conversations', { userId }),
  conversation: (id: string) => api.get<ConversationDto>(`/conversations/${id}`),
  messages: (id: string, cursor: string | null) =>
    api.get<CursorPage<MessageDto>>(`/conversations/${id}/messages`, { params: { cursor: cursor ?? undefined, limit: 30 } }),
  send: (id: string, body: SendMessage) => api.post<MessageDto>(`/conversations/${id}/messages`, body),
  read: (id: string) => api.post<ReadReceipt>(`/conversations/${id}/read`),
  unreadCount: () => api.get<{ count: number }>('/conversations/unread-count'),
  contacts: (q: string) => api.get<UserSummary[]>('/me/contacts', { params: { q } }),
};

export const notificationsApi = {
  list: (cursor: string | null) =>
    api.get<CursorPage<NotificationDto>>('/notifications', { params: { cursor: cursor ?? undefined, limit: 20 } }),
  unreadCount: () => api.get<{ count: number }>('/notifications/unread-count'),
  read: (id: string) => api.post<null>(`/notifications/${id}/read`),
  readAll: () => api.post<{ updated: number }>('/notifications/read-all'),
};
