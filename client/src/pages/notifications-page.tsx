import { useNavigate } from 'react-router-dom';
import { AtSign, CheckCheck, Heart, MessageCircle, Reply, UserCheck, UserPlus } from 'lucide-react';
import { PageHeader } from '@/components/ui/page-header';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { BellIllustration } from '@/components/ui/illustrations';
import { Skeleton } from '@/components/ui/skeleton';
import { useNotificationMutations, useNotifications } from '@/hooks/use-notifications';
import { useStartConversation } from '@/hooks/use-messages';
import type { NotificationDto, NotificationType } from '@/lib/api/schema';
import { notificationHref, notificationText } from '@/lib/notification-text';
import { flattenUnique } from '@/lib/post-cache';
import { shortTime, fullTime } from '@/lib/time';
import { VIBE_META } from '@/types/vibe';
import { cn } from '@/lib/utils';

const ICONS: Record<NotificationType, typeof Heart> = {
  reaction: Heart,
  comment: MessageCircle,
  reply: Reply,
  circle_request: UserPlus,
  circle_approved: UserCheck,
  message: AtSign,
};

function NotificationRow({ n, onOpen }: { n: NotificationDto; onOpen: (n: NotificationDto) => void }) {
  const Icon = ICONS[n.type];
  const name = n.actor?.displayName ?? n.actor?.username ?? 'Someone';
  const vibe = n.type === 'reaction' && n.reaction ? VIBE_META[n.reaction] : null;
  return (
    <li>
      <button
        type="button"
        onClick={() => onOpen(n)}
        className={cn('flex w-full items-start gap-3 rounded-2xl p-3 text-left transition-colors hover:bg-surface-2', !n.read && 'bg-accent-soft/50')}
      >
        <span className="relative shrink-0">
          <Avatar src={n.actor?.avatarUrl} name={name} />
          <span
            className="absolute -bottom-1 -right-1 grid size-5 place-items-center rounded-full border-2 border-bg bg-surface text-[11px]"
            style={vibe ? { color: vibe.color } : undefined}
            aria-hidden
          >
            {vibe ? vibe.glyph : <Icon className="size-3 text-secondary" />}
          </span>
        </span>
        <span className="min-w-0 flex-1">
          <span className="block text-[15px] leading-snug">
            <span className="font-semibold">{name}</span> {notificationText(n)}
          </span>
          {(n.commentSnippet ?? n.postSnippet) && (
            <span className="mt-0.5 line-clamp-2 block text-sm text-muted">“{n.commentSnippet ?? n.postSnippet}”</span>
          )}
          <time className="mt-1 block text-xs text-muted" dateTime={n.createdAt} title={fullTime(n.createdAt)}>
            {shortTime(n.createdAt)}
          </time>
        </span>
        {!n.read && <span className="mt-2 size-2.5 shrink-0 rounded-full bg-accent" aria-label="Unread" />}
      </button>
    </li>
  );
}

export function NotificationsPage() {
  const navigate = useNavigate();
  const query = useNotifications();
  const { markRead, markAllRead } = useNotificationMutations();
  const start = useStartConversation();
  const items = flattenUnique(query.data);
  const anyUnread = items.some((n) => !n.read);

  const open = (n: NotificationDto) => {
    if (!n.read) markRead.mutate(n.id);
    if (n.type === 'message' && n.actor) {
      start.mutate(n.actor.id, { onSuccess: (c) => navigate(`/messages/${c.id}`) });
      return;
    }
    const href = notificationHref(n);
    if (href) navigate(href);
  };

  return (
    <>
      <PageHeader
        title="Notifications"
        actions={
          anyUnread && (
            <Button variant="outline" size="sm" onClick={() => markAllRead.mutate()}>
              <CheckCheck aria-hidden /> Mark all read
            </Button>
          )
        }
      />
      {query.isPending ? (
        <ul className="space-y-1" aria-busy>
          {Array.from({ length: 6 }, (_, i) => (
            <li key={i} className="flex gap-3 p-3">
              <Skeleton className="size-10 rounded-full" />
              <div className="flex-1 space-y-1.5">
                <Skeleton className="h-3.5 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            </li>
          ))}
        </ul>
      ) : items.length === 0 ? (
        <EmptyState illustration={BellIllustration} title="All caught up" description="Vibes, comments and circle invites will land here as they happen." />
      ) : (
        <>
          <ul className="space-y-1">
            {items.map((n) => (
              <NotificationRow key={n.id} n={n} onOpen={open} />
            ))}
          </ul>
          {query.hasNextPage && (
            <Button variant="outline" className="mt-4 w-full" onClick={() => void query.fetchNextPage()} loading={query.isFetchingNextPage}>
              Show older
            </Button>
          )}
        </>
      )}
    </>
  );
}
