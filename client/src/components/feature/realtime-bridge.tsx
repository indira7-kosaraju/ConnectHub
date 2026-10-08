import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { toast } from '@/components/ui/toaster';
import type { CursorPage } from '@/lib/api/types';
import type { ConversationDto, NotificationDto, ReadReceipt, TypingEvent } from '@/lib/api/schema';
import { notificationText } from '@/lib/notification-text';
import { qk } from '@/lib/query-keys';
import { QUEUES, realtime } from '@/lib/realtime';
import { bumpConversation, markConversationRead, removeMessage, upsertMessage, type ChatMessage } from '@/hooks/use-messages';
import { useAuthStore } from '@/stores/auth-store';
import { useRealtimeStore } from '@/stores/realtime-store';

/**
 * Connects the signed-in user's socket and routes live events into the query cache:
 * messages, read receipts, typing indicators, notifications and send errors.
 */
export function RealtimeBridge() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const userId = useAuthStore((s) => s.user?.id);

  useEffect(() => {
    if (!userId) return;
    realtime.start();

    const offMessages = realtime.on(QUEUES.messages, (body) => {
      const m = body as ChatMessage;
      const mine = m.senderId === userId;
      upsertMessage(qc, m);
      const { activeConversationId, setTyping } = useRealtimeStore.getState();
      const viewing = activeConversationId === m.conversationId && document.visibilityState === 'visible';
      bumpConversation(qc, m, !mine && !viewing);
      if (mine) return;
      setTyping(m.conversationId, false);
      if (viewing) {
        markConversationRead(qc, m.conversationId);
      } else {
        void qc.invalidateQueries({ queryKey: qk.unreadMessages });
        toast(m.sender.displayName ?? m.sender.username, {
          description: m.content ?? (m.sharedPost || m.sharedPostUnavailable ? 'Shared a post' : ''),
          action: { label: 'Open', onClick: () => navigate(`/messages/${m.conversationId}`) },
        });
      }
    });

    const offRead = realtime.on(QUEUES.read, (body) => {
      const r = body as ReadReceipt;
      qc.setQueryData<InfiniteData<CursorPage<ChatMessage>>>(qk.messages(r.conversationId), (data) =>
        data && {
          ...data,
          pages: data.pages.map((p) => ({
            ...p,
            items: p.items.map((x) => (x.senderId === userId && !x.readAt ? { ...x, readAt: r.readAt } : x)),
          })),
        },
      );
      qc.setQueryData<InfiniteData<CursorPage<ConversationDto>>>(qk.conversations, (data) =>
        data && {
          ...data,
          pages: data.pages.map((p) => ({ ...p, items: p.items.map((c) => (c.id === r.conversationId ? { ...c, otherLastReadAt: r.readAt } : c)) })),
        },
      );
    });

    const offTyping = realtime.on(QUEUES.typing, (body) => {
      const t = body as TypingEvent;
      useRealtimeStore.getState().setTyping(t.conversationId, t.typing);
    });

    const offNotifications = realtime.on(QUEUES.notifications, (body) => {
      const n = body as NotificationDto;
      qc.setQueryData<InfiniteData<CursorPage<NotificationDto>>>(qk.notifications, (data) => {
        if (!data) return data;
        const pages = data.pages.map((p) => ({ ...p, items: p.items.filter((x) => x.id !== n.id) }));
        pages[0] = { ...pages[0]!, items: [n, ...pages[0]!.items] };
        return { ...data, pages };
      });
      void qc.invalidateQueries({ queryKey: qk.unreadNotifications });
      // Messages already get their own toast.
      if (n.type !== 'message' && window.location.pathname !== '/notifications') {
        toast(`${n.actor?.displayName ?? n.actor?.username ?? 'Someone'} ${notificationText(n)}`);
      }
    });

    const offErrors = realtime.on(QUEUES.errors, (body) => {
      const e = body as { error?: { message?: string; fields?: { clientId?: string } } };
      const clientId = e.error?.fields?.clientId;
      if (clientId) {
        const cid = useRealtimeStore.getState().activeConversationId;
        if (cid) removeMessage(qc, cid, clientId);
      }
      toast.error(e.error?.message ?? 'Something went wrong');
    });

    return () => {
      offMessages();
      offRead();
      offTyping();
      offNotifications();
      offErrors();
      void realtime.stop();
    };
  }, [userId, qc, navigate]);

  return null;
}
