import { postsApi } from './api/posts';
import { messagesApi } from './api/messages';
import type { CursorPage } from './api/types';
import type { ConversationDto, PostDto } from './api/schema';
import { qk } from './query-keys';

/**
 * Query definitions shared by hooks and route loaders. Deliberately dependency-light (no UI,
 * realtime or animation imports) so the router can use them without bloating the entry bundle.
 */
export const feedQuery = {
  queryKey: qk.feed,
  queryFn: ({ pageParam }: { pageParam: string | null }) => postsApi.feed({ cursor: pageParam, limit: 10 }),
  initialPageParam: null as string | null,
  getNextPageParam: (last: CursorPage<PostDto>) => last.nextCursor,
};

export const circlePostsQuery = (slug: string) => ({
  queryKey: qk.circlePosts(slug),
  queryFn: ({ pageParam }: { pageParam: string | null }) => postsApi.circle(slug, { cursor: pageParam, limit: 10 }),
  initialPageParam: null as string | null,
  getNextPageParam: (last: CursorPage<PostDto>) => last.nextCursor,
});

export const conversationsQuery = {
  queryKey: qk.conversations,
  queryFn: ({ pageParam }: { pageParam: string | null }) => messagesApi.conversations(pageParam),
  initialPageParam: null as string | null,
  getNextPageParam: (last: CursorPage<ConversationDto>) => last.nextCursor,
};
