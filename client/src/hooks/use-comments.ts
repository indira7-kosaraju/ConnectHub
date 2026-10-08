import { useInfiniteQuery, useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { postsApi } from '@/lib/api/posts';
import { ApiRequestError } from '@/lib/api/client';
import type { CursorPage } from '@/lib/api/types';
import type { CommentDto, ReactionType } from '@/lib/api/schema';
import { patchPost } from '@/lib/post-cache';
import { qk } from '@/lib/query-keys';
import { toast } from '@/components/ui/toaster';

type Pages = InfiniteData<CursorPage<CommentDto>>;

export function useComments(postId: string) {
  return useInfiniteQuery({
    queryKey: qk.comments(postId),
    queryFn: ({ pageParam }) => postsApi.comments(postId, { cursor: pageParam, limit: 20 }),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  });
}

function mapComments(data: Pages | undefined, fn: (c: CommentDto) => CommentDto | null): Pages | undefined {
  if (!data) return data;
  const apply = (list: CommentDto[]): CommentDto[] =>
    list.flatMap((c) => {
      const next = fn(c);
      return next ? [{ ...next, replies: apply(next.replies) }] : [];
    });
  return { ...data, pages: data.pages.map((p) => ({ ...p, items: apply(p.items) })) };
}

export function useCommentMutations(postId: string) {
  const qc = useQueryClient();
  const key = qk.comments(postId);
  const err = (fallback: string) => (e: unknown) => toast.error(e instanceof ApiRequestError ? e.message : fallback);

  const create = useMutation({
    mutationFn: ({ content, parentId }: { content: string; parentId?: string }) => postsApi.comment(postId, content, parentId),
    onSuccess: (comment) => {
      qc.setQueryData<Pages>(key, (data) => {
        if (!data) return data;
        if (comment.parentId) return mapComments(data, (c) => (c.id === comment.parentId ? { ...c, replies: [...c.replies, comment] } : c));
        // New top-level comments go at the end of the last loaded page (oldest-first order).
        const pages = [...data.pages];
        const last = pages[pages.length - 1]!;
        pages[pages.length - 1] = { ...last, items: [...last.items, comment] };
        return { ...data, pages };
      });
      patchPost(qc, postId, (p) => ({ ...p, commentCount: p.commentCount + 1 }));
    },
    onError: err('Could not post your comment'),
  });

  const remove = useMutation({
    mutationFn: (comment: CommentDto) => postsApi.deleteComment(comment.id),
    onSuccess: (_, comment) => {
      qc.setQueryData<Pages>(key, (data) => mapComments(data, (c) => (c.id === comment.id ? null : c)));
      patchPost(qc, postId, (p) => ({ ...p, commentCount: Math.max(0, p.commentCount - 1 - comment.replies.length) }));
      toast('Comment deleted');
    },
    onError: err('Could not delete the comment'),
  });

  const react = useMutation({
    mutationFn: ({ id, type }: { id: string; type: ReactionType | null }) =>
      type ? postsApi.reactComment(id, type) : postsApi.unreactComment(id),
    onMutate: ({ id, type }) => {
      qc.setQueryData<Pages>(key, (data) => mapComments(data, (c) => (c.id === id ? { ...c, myReaction: type } : c)));
    },
    onSuccess: (res, { id }) => {
      qc.setQueryData<Pages>(key, (data) =>
        mapComments(data, (c) => (c.id === id ? { ...c, vibe: res.vibe, myReaction: res.myReaction } : c)),
      );
    },
    onError: (e) => {
      void qc.invalidateQueries({ queryKey: key });
      err('Could not react')(e);
    },
  });

  return { create, remove, react };
}
