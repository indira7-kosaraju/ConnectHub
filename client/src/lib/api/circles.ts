import { api } from './client';
import type { PageResponse } from './types';
import type {
  CircleDetail,
  CircleSummary,
  CreateCircleRequest,
  JoinResult,
  MeResponse,
  MemberDto,
  MemberStatus,
  ProfileResponse,
  UpdateCircleRequest,
  UpdateProfileRequest,
} from './schema';

function imageForm(file: Blob, name: string): FormData {
  const form = new FormData();
  form.append('file', file, name);
  return form;
}

export interface BrowseParams {
  category?: string;
  q?: string;
  page?: number;
  size?: number;
}

export const circlesApi = {
  browse: (params: BrowseParams) => api.get<PageResponse<CircleSummary>>('/circles', { params }),
  trending: (limit = 8) => api.get<CircleSummary[]>('/circles/discover/trending', { params: { limit } }),
  get: (slug: string) => api.get<CircleDetail>(`/circles/${encodeURIComponent(slug)}`),
  create: (body: CreateCircleRequest) => api.post<CircleDetail>('/circles', body),
  update: (slug: string, body: UpdateCircleRequest) => api.patch<CircleDetail>(`/circles/${encodeURIComponent(slug)}`, body),
  uploadCover: (slug: string, file: Blob) =>
    api.post<CircleDetail>(`/circles/${encodeURIComponent(slug)}/cover`, imageForm(file, 'cover.jpg')),
  join: (slug: string) => api.post<JoinResult>(`/circles/${encodeURIComponent(slug)}/join`),
  leave: (slug: string) => api.delete<null>(`/circles/${encodeURIComponent(slug)}/membership`),
  members: (slug: string, status: MemberStatus, page = 0) =>
    api.get<PageResponse<MemberDto>>(`/circles/${encodeURIComponent(slug)}/members`, { params: { status, page } }),
  approve: (slug: string, userId: string) =>
    api.post<MemberDto>(`/circles/${encodeURIComponent(slug)}/members/${userId}/approve`),
  remove: (slug: string, userId: string) => api.delete<null>(`/circles/${encodeURIComponent(slug)}/members/${userId}`),
};

export const myCirclesApi = {
  list: () => api.get<CircleSummary[]>('/me/circles'),
  suggestions: (limit = 5) => api.get<CircleSummary[]>('/me/circle-suggestions', { params: { limit } }),
};

export const profileApi = {
  get: (username: string) => api.get<ProfileResponse>(`/users/${encodeURIComponent(username)}`),
  circles: (username: string) => api.get<CircleSummary[]>(`/users/${encodeURIComponent(username)}/circles`),
  update: (body: UpdateProfileRequest) => api.patch<MeResponse>('/me/profile', body),
  uploadCover: (file: Blob) => api.post<MeResponse>('/me/cover', imageForm(file, 'cover.jpg')),
  removeCover: () => api.delete<MeResponse>('/me/cover'),
  uploadAvatar: (file: Blob) => api.post<MeResponse>('/me/avatar', imageForm(file, 'avatar.jpg')),
};
