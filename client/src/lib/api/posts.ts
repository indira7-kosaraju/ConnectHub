import { api } from './client';
import type { CursorPage } from './types';
import type { CircleDetail, CommentDto, PostDto, ReactionResult, ReactionType, ReportReason } from './schema';

const enc = encodeURIComponent;
type PageParams = { cursor?: string | null; limit?: number };
const params = ({ cursor, limit }: PageParams) => ({ params: { cursor: cursor ?? undefined, limit } });

export interface NewPost {
  circleId: string;
  content: string;
  images: File[];
}

export const postsApi = {
  feed: (p: PageParams) => api.get<CursorPage<PostDto>>('/feed', params(p)),
  circle: (slug: string, p: PageParams) => api.get<CursorPage<PostDto>>(`/circles/${enc(slug)}/posts`, params(p)),
  user: (username: string, p: PageParams) => api.get<CursorPage<PostDto>>(`/users/${enc(username)}/posts`, params(p)),
  saved: (p: PageParams) => api.get<CursorPage<PostDto>>('/me/saved', params(p)),
  get: (id: string) => api.get<PostDto>(`/posts/${id}`),

  create: ({ circleId, content, images }: NewPost) => {
    const form = new FormData();
    form.append('post', new Blob([JSON.stringify({ circleId, content: content || undefined })], { type: 'application/json' }));
    images.forEach((f) => form.append('images', f, f.name));
    return api.post<PostDto>('/posts', form);
  },
  update: (id: string, content: string) => api.patch<PostDto>(`/posts/${id}`, { content }),
  remove: (id: string) => api.delete<null>(`/posts/${id}`),
  react: (id: string, type: ReactionType) => api.put<ReactionResult>(`/posts/${id}/reaction`, { type }),
  unreact: (id: string) => api.delete<ReactionResult>(`/posts/${id}/reaction`),
  save: (id: string) => api.put<{ saved: boolean }>(`/posts/${id}/save`),
  unsave: (id: string) => api.delete<{ saved: boolean }>(`/posts/${id}/save`),
  report: (id: string, reason: ReportReason, details?: string) => api.post<null>(`/posts/${id}/report`, { reason, details }),
  pin: (slug: string, postId: string) => api.put<CircleDetail>(`/circles/${enc(slug)}/pin`, { postId }),
  unpin: (slug: string) => api.delete<CircleDetail>(`/circles/${enc(slug)}/pin`),

  comments: (postId: string, p: PageParams) => api.get<CursorPage<CommentDto>>(`/posts/${postId}/comments`, params(p)),
  comment: (postId: string, content: string, parentId?: string) =>
    api.post<CommentDto>(`/posts/${postId}/comments`, { content, parentId }),
  deleteComment: (id: string) => api.delete<null>(`/comments/${id}`),
  reactComment: (id: string, type: ReactionType) => api.put<ReactionResult>(`/comments/${id}/reaction`, { type }),
  unreactComment: (id: string) => api.delete<ReactionResult>(`/comments/${id}/reaction`),
};
