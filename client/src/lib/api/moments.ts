import { api } from './client';
import type { CursorPage } from './types';
import type { MomentBackground, MomentDto, MomentGroup } from './schema';

export type NewMoment =
  | { kind: 'text'; text: string; background: MomentBackground }
  | { kind: 'image'; image: Blob; caption?: string };

export const momentsApi = {
  strip: () => api.get<MomentGroup[]>('/moments/strip'),
  create: (m: NewMoment) => {
    const form = new FormData();
    const meta = m.kind === 'text' ? { kind: 'text', text: m.text, background: m.background } : { kind: 'image', text: m.caption || undefined };
    form.append('moment', new Blob([JSON.stringify(meta)], { type: 'application/json' }));
    if (m.kind === 'image') form.append('image', m.image, 'moment.jpg');
    return api.post<MomentDto>('/moments', form);
  },
  view: (id: string) => api.post<null>(`/moments/${id}/view`),
  remove: (id: string) => api.delete<null>(`/moments/${id}`),
  rewind: (cursor: string | null) => api.get<CursorPage<MomentDto>>('/me/rewind', { params: { cursor: cursor ?? undefined, limit: 24 } }),
};
