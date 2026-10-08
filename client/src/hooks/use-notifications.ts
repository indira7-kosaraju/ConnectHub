import { useInfiniteQuery, useMutation, useQuery, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { notificationsApi } from '@/lib/api/messages';
import type { CursorPage } from '@/lib/api/types';
import type { NotificationDto } from '@/lib/api/schema';
import { qk } from '@/lib/query-keys';
import { useAuthStore } from '@/stores/auth-store';

type Pages = InfiniteData<CursorPage<NotificationDto>>;

export function useNotifications() {
  return useInfiniteQuery({
    queryKey: qk.notifications,
    queryFn: ({ pageParam }) => notificationsApi.list(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  });
}

export function useUnreadNotifications() {
  const signedIn = useAuthStore((s) => !!s.user);
  return useQuery({ queryKey: qk.unreadNotifications, queryFn: notificationsApi.unreadCount, enabled: signedIn, select: (d) => d.count });
}

export function useNotificationMutations() {
  const qc = useQueryClient();
  const setRead = (predicate: (n: NotificationDto) => boolean) =>
    qc.setQueryData<Pages>(qk.notifications, (data) =>
      data && { ...data, pages: data.pages.map((p) => ({ ...p, items: p.items.map((n) => (predicate(n) ? { ...n, read: true } : n)) })) },
    );
  const refreshCount = () => void qc.invalidateQueries({ queryKey: qk.unreadNotifications });

  return {
    markRead: useMutation({
      mutationFn: (id: string) => notificationsApi.read(id),
      onMutate: (id) => setRead((n) => n.id === id),
      onSettled: refreshCount,
    }),
    markAllRead: useMutation({
      mutationFn: () => notificationsApi.readAll(),
      onMutate: () => {
        setRead(() => true);
        qc.setQueryData(qk.unreadNotifications, { count: 0 });
      },
      onSettled: refreshCount,
    }),
  };
}
