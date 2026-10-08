import { Link, useParams } from 'react-router-dom';
import { Avatar } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { EmptyState } from '@/components/ui/empty-state';
import { MessagesIllustration } from '@/components/ui/illustrations';
import { Button } from '@/components/ui/button';
import { useConversations } from '@/hooks/use-messages';
import type { ConversationDto } from '@/lib/api/schema';
import { flattenUnique } from '@/lib/post-cache';
import { shortTime } from '@/lib/time';
import { useAuthStore } from '@/stores/auth-store';
import { useRealtimeStore } from '@/stores/realtime-store';
import { cn } from '@/lib/utils';

function preview(c: ConversationDto, meId: string | undefined): string {
  const m = c.lastMessage;
  if (!m) return 'Say hello 👋';
  const prefix = m.senderId === meId ? 'You: ' : '';
  if (m.content) return prefix + m.content;
  return prefix + 'Shared a post';
}

export function ConversationList() {
  const query = useConversations();
  const meId = useAuthStore((s) => s.user?.id);
  const typing = useRealtimeStore((s) => s.typing);
  const { id: activeId } = useParams<{ id: string }>();
  const conversations = flattenUnique(query.data);

  if (query.isPending) {
    return (
      <ul aria-busy className="space-y-1">
        {Array.from({ length: 5 }, (_, i) => (
          <li key={i} className="flex items-center gap-3 p-3">
            <Skeleton className="size-12 rounded-full" />
            <div className="flex-1 space-y-1.5">
              <Skeleton className="h-3.5 w-32" />
              <Skeleton className="h-3 w-48" />
            </div>
          </li>
        ))}
      </ul>
    );
  }
  if (conversations.length === 0) {
    return (
      <EmptyState
        illustration={MessagesIllustration}
        title="No conversations yet"
        description="Say hi to someone from one of your circles — or share a post that made you think of them."
      />
    );
  }

  return (
    <div>
      <ul className="space-y-1">
        {conversations.map((c) => {
          const name = c.other.displayName ?? c.other.username;
          const isTyping = (typing[c.id] ?? 0) > Date.now();
          const unread = c.unreadCount > 0;
          return (
            <li key={c.id}>
              <Link
                to={`/messages/${c.id}`}
                className={cn('flex items-center gap-3 rounded-2xl p-3 transition-colors hover:bg-surface-2', activeId === c.id && 'bg-surface-2')}
              >
                <Avatar size="lg" src={c.other.avatarUrl} name={name} className="size-12" />
                <span className="min-w-0 flex-1">
                  <span className="flex items-baseline gap-2">
                    <span className={cn('truncate', unread ? 'font-semibold' : 'font-medium')}>{name}</span>
                    {c.lastMessageAt && <time className="ml-auto shrink-0 text-xs text-muted">{shortTime(c.lastMessageAt)}</time>}
                  </span>
                  <span className={cn('mt-0.5 flex items-center gap-2 text-sm', unread ? 'text-ink' : 'text-muted')}>
                    <span className={cn('truncate', isTyping && 'italic text-secondary')}>{isTyping ? 'typing…' : preview(c, meId)}</span>
                    {unread && (
                      <Badge variant="count" className="ml-auto">
                        {c.unreadCount}
                        <span className="sr-only"> unread</span>
                      </Badge>
                    )}
                  </span>
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
      {query.hasNextPage && (
        <Button variant="outline" className="mt-3 w-full" onClick={() => void query.fetchNextPage()} loading={query.isFetchingNextPage}>
          Show older conversations
        </Button>
      )}
    </div>
  );
}
