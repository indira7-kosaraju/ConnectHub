import type { InfiniteData, QueryClient } from '@tanstack/react-query';
import type { CursorPage } from './api/types';
import type { PostDto } from './api/schema';
import { qk } from './query-keys';

type Lists = InfiniteData<CursorPage<PostDto>>;

/** Apply an update to a post in every cached list and in its detail query. */
export function patchPost(qc: QueryClient, id: string, update: (p: PostDto) => PostDto) {
  qc.setQueriesData<Lists>({ queryKey: qk.posts }, (data) =>
    data && {
      ...data,
      pages: data.pages.map((page) => ({ ...page, items: page.items.map((p) => (p.id === id ? update(p) : p)) })),
    },
  );
  qc.setQueryData<PostDto>(qk.post(id), (p) => (p ? update(p) : p));
}

/** Remove a post from every cached list. */
export function removePost(qc: QueryClient, id: string, from: readonly unknown[] = qk.posts) {
  qc.setQueriesData<Lists>({ queryKey: from }, (data) =>
    data && { ...data, pages: data.pages.map((page) => ({ ...page, items: page.items.filter((p) => p.id !== id) })) },
  );
  qc.removeQueries({ queryKey: qk.post(id) });
}

/** Snapshot of all post caches, for rolling back optimistic updates. */
export function snapshotPosts(qc: QueryClient) {
  return [...qc.getQueriesData({ queryKey: qk.posts }), ...qc.getQueriesData({ queryKey: ['post'] })];
}

export function restore(qc: QueryClient, snapshot: ReturnType<typeof snapshotPosts>) {
  snapshot.forEach(([key, data]) => qc.setQueryData(key, data));
}

/** De-duplicate items across infinite pages (ranked feeds can shift between fetches). */
export function flattenUnique<T extends { id: string }>(data: InfiniteData<CursorPage<T>> | undefined): T[] {
  const seen = new Set<string>();
  const out: T[] = [];
  data?.pages.forEach((page) =>
    page.items.forEach((item) => {
      if (!seen.has(item.id)) {
        seen.add(item.id);
        out.push(item);
      }
    }),
  );
  return out;
}
