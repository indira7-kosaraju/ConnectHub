import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { momentsApi, type NewMoment } from '@/lib/api/moments';
import { ApiRequestError } from '@/lib/api/client';
import type { MomentGroup } from '@/lib/api/schema';
import { qk } from '@/lib/query-keys';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from '@/components/ui/toaster';

export function useMomentStrip() {
  const signedIn = useAuthStore((s) => !!s.user);
  return useQuery({
    queryKey: qk.momentStrip,
    queryFn: momentsApi.strip,
    enabled: signedIn,
    refetchInterval: 5 * 60_000, // moments expire continuously
  });
}

export function useRewind(enabled: boolean) {
  return useInfiniteQuery({
    queryKey: qk.rewind,
    queryFn: ({ pageParam }) => momentsApi.rewind(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled,
  });
}

export function useCreateMoment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (m: NewMoment) => momentsApi.create(m),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: qk.momentStrip });
      toast.success('Your moment is live for 24 hours');
    },
    onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Could not share your moment'),
  });
}

/** Marks a moment seen and updates the strip in place (no refetch, so rings settle smoothly). */
export function useMarkMomentSeen() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => momentsApi.view(id),
    onMutate: (id) => {
      qc.setQueryData<MomentGroup[]>(qk.momentStrip, (groups) =>
        groups?.map((g) => {
          if (!g.moments.some((m) => m.id === id)) return g;
          const moments = g.moments.map((m) => (m.id === id ? { ...m, seen: true } : m));
          return { ...g, moments, allSeen: moments.every((m) => m.seen) };
        }),
      );
    },
  });
}

export function useDeleteMoment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => momentsApi.remove(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['moments'] });
      toast('Moment deleted');
    },
    onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Could not delete'),
  });
}
