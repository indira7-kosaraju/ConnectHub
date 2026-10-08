import { useInfiniteQuery, useMutation, useQuery, useQueryClient, type InfiniteData, type QueryClient } from '@tanstack/react-query';
import { messagesApi } from '@/lib/api/messages';
import { ApiRequestError } from '@/lib/api/client';
import type { CursorPage } from '@/lib/api/types';
import type { ConversationDto, MessageDto, PostDto } from '@/lib/api/schema';
import { qk } from '@/lib/query-keys';
import { realtime } from '@/lib/realtime';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from '@/components/ui/toaster';

/** A message as shown in the UI: server messages plus optimistic ones still in flight. */
export type ChatMessage = MessageDto & { pending?: boolean };
type MessagePages = InfiniteData<CursorPage<ChatMessage>>;
type ConversationPages = InfiniteData<CursorPage<ConversationDto>>;

import { conversationsQuery } from '@/lib/queries';

export function useConversations() {
  const signedIn = useAuthStore((s) => !!s.user);
  return useInfiniteQuery({ ...conversationsQuery, enabled: signedIn });
}

export function useConversation(id: string) {
  return useQuery({ queryKey: qk.conversation(id), queryFn: () => messagesApi.conversation(id) });
}

export function useMessages(id: string) {
  return useInfiniteQuery({
    queryKey: qk.messages(id),
    queryFn: ({ pageParam }) => messagesApi.messages(id, pageParam) as Promise<CursorPage<ChatMessage>>,
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    staleTime: Infinity, // kept fresh by the socket
  });
}

export function useUnreadMessages() {
  const signedIn = useAuthStore((s) => !!s.user);
  return useQuery({ queryKey: qk.unreadMessages, queryFn: messagesApi.unreadCount, enabled: signedIn, select: (d) => d.count });
}

export function useContacts(q: string, enabled = true) {
  return useQuery({ queryKey: qk.contacts(q), queryFn: () => messagesApi.contacts(q), enabled });
}

export function useStartConversation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => messagesApi.start(userId),
    onSuccess: (c) => qc.setQueryData(qk.conversation(c.id), c),
    onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Could not open the conversation'),
  });
}

// ------------------------------------------------------------------ cache helpers (shared with the realtime bridge)

/** Insert or reconcile a message in a thread's cache (pages are newest-first). */
export function upsertMessage(qc: QueryClient, m: ChatMessage) {
  qc.setQueryData<MessagePages>(qk.messages(m.conversationId), (data) => {
    if (!data) return data;
    const all = data.pages.flatMap((p) => p.items);
    const exists = all.some((x) => x.id === m.id || (m.clientId && x.clientId === m.clientId));
    if (exists) {
      return {
        ...data,
        pages: data.pages.map((p) => ({
          ...p,
          items: p.items.map((x) => (x.id === m.id || (m.clientId && x.clientId === m.clientId) ? { ...m, pending: false } : x)),
        })),
      };
    }
    const [first, ...rest] = data.pages;
    return { ...data, pages: [{ ...first!, items: [m, ...first!.items] }, ...rest] };
  });
}

export function removeMessage(qc: QueryClient, conversationId: string, clientId: string) {
  qc.setQueryData<MessagePages>(qk.messages(conversationId), (data) =>
    data && { ...data, pages: data.pages.map((p) => ({ ...p, items: p.items.filter((x) => x.clientId !== clientId) })) },
  );
}

/** Move a conversation to the top with its new last message; unknown conversations trigger a refetch. */
export function bumpConversation(qc: QueryClient, m: MessageDto, incrementUnread: boolean) {
  let found = false;
  qc.setQueryData<ConversationPages>(qk.conversations, (data) => {
    if (!data) return data;
    let target: ConversationDto | undefined;
    const pages = data.pages.map((p) => ({
      ...p,
      items: p.items.filter((c) => {
        if (c.id !== m.conversationId) return true;
        target = c;
        return false;
      }),
    }));
    if (!target) return data;
    found = true;
    const updated: ConversationDto = {
      ...target,
      lastMessage: m,
      lastMessageAt: m.createdAt,
      unreadCount: incrementUnread ? target.unreadCount + 1 : target.unreadCount,
    };
    pages[0] = { ...pages[0]!, items: [updated, ...pages[0]!.items] };
    return { ...data, pages };
  });
  if (!found) void qc.invalidateQueries({ queryKey: qk.conversations });
}

export function useSendMessage(conversationId: string) {
  const qc = useQueryClient();
  const me = useAuthStore((s) => s.user);
  return useMutation({
    mutationFn: async ({ content, sharedPost }: { content: string; sharedPost?: PostDto }) => {
      const clientId = crypto.randomUUID();
      const optimistic: ChatMessage = {
        id: `tmp-${clientId}`,
        conversationId,
        senderId: me!.id,
        sender: { id: me!.id, username: me!.username ?? '', displayName: me!.displayName, avatarUrl: me!.avatarUrl },
        content: content || null,
        sharedPost: sharedPost ?? null,
        sharedPostUnavailable: false,
        createdAt: new Date().toISOString(),
        readAt: null,
        clientId,
        pending: true,
      };
      upsertMessage(qc, optimistic);
      const frame = { conversationId, content: content || undefined, sharedPostId: sharedPost?.id, clientId };
      // Realtime path first; the server echoes the saved message on /user/queue/messages.
      if (realtime.publish('/app/chat.send', frame)) return optimistic;
      try {
        const saved = await messagesApi.send(conversationId, frame);
        upsertMessage(qc, saved);
        bumpConversation(qc, saved, false);
        return saved;
      } catch (err) {
        removeMessage(qc, conversationId, clientId);
        throw err;
      }
    },
    onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Message not sent'),
  });
}

/** Mark a thread read: locally clear its unread count, tell the server (socket or REST). */
export function markConversationRead(qc: QueryClient, conversationId: string) {
  qc.setQueryData<ConversationPages>(qk.conversations, (data) =>
    data && {
      ...data,
      pages: data.pages.map((p) => ({ ...p, items: p.items.map((c) => (c.id === conversationId ? { ...c, unreadCount: 0 } : c)) })),
    },
  );
  if (!realtime.publish('/app/chat.read', { conversationId })) {
    void messagesApi.read(conversationId).catch(() => undefined);
  }
  window.setTimeout(() => void qc.invalidateQueries({ queryKey: qk.unreadMessages }), 400);
}
