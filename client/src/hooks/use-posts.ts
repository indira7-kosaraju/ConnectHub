import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postsApi, type NewPost } from '@/lib/api/posts';
import { ApiRequestError } from '@/lib/api/client';
import type { CursorPage } from '@/lib/api/types';
import type { PostDto, ReactionType, ReportReason } from '@/lib/api/schema';
import { patchPost, removePost, restore, snapshotPosts } from '@/lib/post-cache';
import { qk } from '@/lib/query-keys';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from '@/components/ui/toaster';

const errorToast = (fallback: string) => (err: unknown) =>
  toast.error(err instanceof ApiRequestError ? err.message : fallback);

function usePostList(key: readonly unknown[], fetchPage: (cursor: string | null) => Promise<CursorPage<PostDto>>, enabled = true) {
  return useInfiniteQuery({
    queryKey: key,
    queryFn: ({ pageParam }) => fetchPage(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled,
  });
}

import { circlePostsQuery, feedQuery } from '@/lib/queries';

export const useFeed = () => useInfiniteQuery(feedQuery);
export const useCirclePosts = (slug: string, enabled = true) => useInfiniteQuery({ ...circlePostsQuery(slug), enabled });
export const useUserPosts = (username: string, enabled = true) =>
  usePostList(qk.userPosts(username.toLowerCase()), (cursor) => postsApi.user(username, { cursor, limit: 10 }), enabled);
export const useSavedPosts = () => usePostList(qk.saved, (cursor) => postsApi.saved({ cursor, limit: 10 }));

export function usePost(id: string | null | undefined) {
  return useQuery({ queryKey: qk.post(id ?? ''), queryFn: () => postsApi.get(id!), enabled: !!id });
}

export function useCreatePost() {
  const qc = useQueryClient();
  const username = useAuthStore((s) => s.user?.username);
  return useMutation({
    mutationFn: (p: NewPost) => postsApi.create(p),
    onSuccess: (post) => {
      void qc.invalidateQueries({ queryKey: qk.feed });
      void qc.invalidateQueries({ queryKey: qk.circlePosts(post.circle.slug) });
      if (username) void qc.invalidateQueries({ queryKey: qk.userPosts(username) });
    },
  });
}

/** Optimistically shows my reaction immediately; the server returns the new vibe blend. */
export function useReactToPost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, type }: { id: string; type: ReactionType | null }) => (type ? postsApi.react(id, type) : postsApi.unreact(id)),
    onMutate: async ({ id, type }) => {
      await qc.cancelQueries({ queryKey: qk.posts });
      const snapshot = snapshotPosts(qc);
      patchPost(qc, id, (p) => ({ ...p, myReaction: type }));
      return { snapshot };
    },
    onSuccess: (res, { id }) => patchPost(qc, id, (p) => ({ ...p, vibe: res.vibe, myReaction: res.myReaction })),
    onError: (err, _, ctx) => {
      if (ctx) restore(qc, ctx.snapshot);
      errorToast('Could not react')(err);
    },
  });
}

export function useSavePost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, save }: { id: string; save: boolean }) => (save ? postsApi.save(id) : postsApi.unsave(id)),
    onMutate: ({ id, save }) => {
      const snapshot = snapshotPosts(qc);
      patchPost(qc, id, (p) => ({ ...p, saved: save }));
      return { snapshot };
    },
    onSuccess: (_, { save }) => {
      toast(save ? 'Saved — find it under Saved' : 'Removed from Saved');
      void qc.invalidateQueries({ queryKey: qk.saved });
    },
    onError: (err, _, ctx) => {
      if (ctx) restore(qc, ctx.snapshot);
      errorToast('Could not update Saved')(err);
    },
  });
}

export function useEditPost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, content }: { id: string; content: string }) => postsApi.update(id, content),
    onSuccess: (post) => {
      patchPost(qc, post.id, () => post);
      toast.success('Post updated');
    },
    onError: errorToast('Could not save your edit'),
  });
}

export function useDeletePost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => postsApi.remove(id),
    onSuccess: (_, id) => {
      removePost(qc, id);
      toast('Post deleted');
    },
    onError: errorToast('Could not delete the post'),
  });
}

export function useReportPost() {
  return useMutation({
    mutationFn: ({ id, reason, details }: { id: string; reason: ReportReason; details?: string }) =>
      postsApi.report(id, reason, details),
    onSuccess: () => toast.success('Thanks — our team will take a look'),
    onError: errorToast('Could not send the report'),
  });
}

export function usePinPost() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ slug, postId, pin }: { slug: string; postId: string; pin: boolean }) =>
      pin ? postsApi.pin(slug, postId) : postsApi.unpin(slug),
    onSuccess: (circle) => {
      qc.setQueryData(qk.circle(circle.slug), circle);
      void qc.invalidateQueries({ queryKey: qk.posts });
      void qc.invalidateQueries({ queryKey: ['post'] });
      toast(circle.pinnedPostId ? 'Pinned to the top of the circle' : 'Unpinned');
    },
    onError: errorToast('Could not update the pin'),
  });
}
