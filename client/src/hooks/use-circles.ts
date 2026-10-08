import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { circlesApi, myCirclesApi, type BrowseParams } from '@/lib/api/circles';
import { ApiRequestError } from '@/lib/api/client';
import type { CircleDetail, CreateCircleRequest, MemberStatus, UpdateCircleRequest } from '@/lib/api/schema';
import { qk } from '@/lib/query-keys';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from '@/components/ui/toaster';

const errorToast = (fallback: string) => (err: unknown) =>
  toast.error(err instanceof ApiRequestError ? err.message : fallback);

export function useCircle(slug: string) {
  return useQuery({ queryKey: qk.circle(slug), queryFn: () => circlesApi.get(slug) });
}

export function useTrendingCircles(limit = 8) {
  return useQuery({ queryKey: [...qk.trending, limit], queryFn: () => circlesApi.trending(limit) });
}

export function useBrowseCircles(params: Omit<BrowseParams, 'page'>, enabled = true) {
  return useInfiniteQuery({
    queryKey: qk.browse(params),
    queryFn: ({ pageParam }) => circlesApi.browse({ ...params, page: pageParam, size: 12 }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
    enabled,
  });
}

export function useMyCircles() {
  const signedIn = useAuthStore((s) => !!s.user);
  return useQuery({ queryKey: qk.myCircles, queryFn: myCirclesApi.list, enabled: signedIn });
}

export function useCircleSuggestions(limit = 5) {
  const signedIn = useAuthStore((s) => !!s.user);
  return useQuery({ queryKey: qk.suggestions(limit), queryFn: () => myCirclesApi.suggestions(limit), enabled: signedIn });
}

export function useCircleMembers(slug: string, status: MemberStatus, enabled = true) {
  return useInfiniteQuery({
    queryKey: qk.members(slug, status),
    queryFn: ({ pageParam }) => circlesApi.members(slug, status, pageParam),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
    enabled,
  });
}

/** Refresh every view that depends on membership. */
function useInvalidateMembership() {
  const qc = useQueryClient();
  return (slug: string) => {
    void qc.invalidateQueries({ queryKey: qk.circle(slug) });
    void qc.invalidateQueries({ queryKey: qk.myCircles });
    void qc.invalidateQueries({ queryKey: qk.allSuggestions });
    void qc.invalidateQueries({ queryKey: qk.circles });
    void qc.invalidateQueries({ queryKey: ['profile'] });
  };
}

export function useJoinCircle() {
  const invalidate = useInvalidateMembership();
  return useMutation({
    mutationFn: (slug: string) => circlesApi.join(slug),
    onSuccess: (res, slug) => {
      toast.success(res.membership.status === 'pending' ? 'Request sent — an admin will review it' : 'You’re in! Welcome to the circle');
      invalidate(slug);
    },
    onError: errorToast('Could not join this circle'),
  });
}

export function useLeaveCircle() {
  const invalidate = useInvalidateMembership();
  return useMutation({
    mutationFn: (args: { slug: string; pending: boolean }) => circlesApi.leave(args.slug),
    onSuccess: (_, { slug, pending }) => {
      toast(pending ? 'Request withdrawn' : 'You left the circle');
      invalidate(slug);
    },
    onError: errorToast('Could not leave this circle'),
  });
}

export function useCreateCircle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateCircleRequest) => circlesApi.create(body),
    onSuccess: (circle) => {
      qc.setQueryData(qk.circle(circle.slug), circle);
      void qc.invalidateQueries({ queryKey: qk.myCircles });
      void qc.invalidateQueries({ queryKey: qk.circles });
    },
  });
}

export function useUpdateCircle(slug: string) {
  const qc = useQueryClient();
  const onSuccess = (circle: CircleDetail) => {
    qc.setQueryData(qk.circle(slug), circle);
    void qc.invalidateQueries({ queryKey: qk.myCircles });
    void qc.invalidateQueries({ queryKey: qk.circles });
  };
  return {
    update: useMutation({ mutationFn: (body: UpdateCircleRequest) => circlesApi.update(slug, body), onSuccess }),
    cover: useMutation({
      mutationFn: (file: Blob) => circlesApi.uploadCover(slug, file),
      onSuccess,
      onError: errorToast('Cover upload failed'),
    }),
  };
}

export function useModerateMember(slug: string) {
  const qc = useQueryClient();
  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ['circle', slug] });
  };
  return {
    approve: useMutation({
      mutationFn: (userId: string) => circlesApi.approve(slug, userId),
      onSuccess: () => {
        toast.success('Request approved');
        refresh();
      },
      onError: errorToast('Could not approve'),
    }),
    remove: useMutation({
      mutationFn: (args: { userId: string; pending: boolean }) => circlesApi.remove(slug, args.userId),
      onSuccess: (_, { pending }) => {
        toast(pending ? 'Request declined' : 'Member removed');
        refresh();
      },
      onError: errorToast('Could not remove'),
    }),
  };
}
