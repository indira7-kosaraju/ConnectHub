import { Fragment, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowLeft, Check, CheckCheck, Clock, Lock, SendHorizontal } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Spinner } from '@/components/ui/spinner';
import { RichText } from '@/components/feature/posts/rich-text';
import { markConversationRead, useConversation, useMessages, useSendMessage, type ChatMessage } from '@/hooks/use-messages';
import type { PostDto } from '@/lib/api/schema';
import { flattenUnique } from '@/lib/post-cache';
import { realtime } from '@/lib/realtime';
import { useAuthStore } from '@/stores/auth-store';
import { useRealtimeStore } from '@/stores/realtime-store';
import { cn } from '@/lib/utils';

const TYPING_IDLE_MS = 3000;

function dayLabel(iso: string): string {
  const d = new Date(iso);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  if (d.toDateString() === today.toDateString()) return 'Today';
  if (d.toDateString() === yesterday.toDateString()) return 'Yesterday';
  return d.toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' });
}

function SharedPostCard({ post, mine }: { post: PostDto; mine: boolean }) {
  return (
    <Link
      to={`/p/${post.id}`}
      className={cn('mt-1 block w-64 max-w-full overflow-hidden rounded-xl border text-left', mine ? 'border-bg/20 bg-bg/10' : 'border-line bg-surface')}
    >
      {post.images[0] && <img src={post.images[0].url} alt="" className="h-28 w-full object-cover" loading="lazy" />}
      <span className="block p-3">
        <span className={cn('block text-xs', mine ? 'text-bg/70' : 'text-muted')}>
          {post.author.displayName ?? post.author.username} · {post.circle.name}
        </span>
        {post.content && <span className="mt-1 line-clamp-3 block text-sm">{post.content}</span>}
      </span>
    </Link>
  );
}

function Bubble({ m, mine, showReceipt }: { m: ChatMessage; mine: boolean; showReceipt: boolean }) {
  return (
    <motion.li
      layout="position"
      initial={{ opacity: 0, y: 8, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.18 }}
      className={cn('flex flex-col', mine ? 'items-end' : 'items-start')}
    >
      <div
        className={cn(
          'max-w-[80%] rounded-[20px] px-3.5 py-2 text-[15px] leading-relaxed',
          mine ? 'rounded-br-md bg-ink text-bg' : 'rounded-bl-md bg-surface-2 text-ink',
          m.pending && 'opacity-70',
        )}
      >
        {m.content && <RichText text={m.content} className="whitespace-pre-line break-words [&_a]:text-inherit [&_a]:underline" />}
        {m.sharedPost && <SharedPostCard post={m.sharedPost} mine={mine} />}
        {m.sharedPostUnavailable && (
          <span className={cn('mt-1 flex items-center gap-1.5 text-sm italic', mine ? 'text-bg/70' : 'text-muted')}>
            <Lock className="size-3.5" aria-hidden /> Shared a post you can’t see
          </span>
        )}
      </div>
      {mine && showReceipt && (
        <span className="mt-1 flex items-center gap-1 pr-1 text-[11px] text-muted" aria-live="polite">
          {m.pending ? (
            <>
              <Clock className="size-3" aria-hidden /> Sending
            </>
          ) : m.readAt ? (
            <>
              <CheckCheck className="size-3.5 text-secondary" aria-hidden /> Seen
            </>
          ) : (
            <>
              <Check className="size-3.5" aria-hidden /> Sent
            </>
          )}
        </span>
      )}
    </motion.li>
  );
}

function TypingDots() {
  return (
    <motion.li initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} className="flex" aria-label="Typing">
      <span className="flex gap-1 rounded-[20px] rounded-bl-md bg-surface-2 px-4 py-3">
        {[0, 1, 2].map((i) => (
          <motion.span
            key={i}
            className="size-1.5 rounded-full bg-muted"
            animate={{ y: [0, -3, 0] }}
            transition={{ duration: 0.9, repeat: Infinity, delay: i * 0.15 }}
          />
        ))}
      </span>
    </motion.li>
  );
}

export function ChatThread({ conversationId }: { conversationId: string }) {
  const qc = useQueryClient();
  const meId = useAuthStore((s) => s.user?.id);
  const conversation = useConversation(conversationId);
  const query = useMessages(conversationId);
  const send = useSendMessage(conversationId);
  const typingUntil = useRealtimeStore((s) => s.typing[conversationId] ?? 0);
  const status = useRealtimeStore((s) => s.status);
  const setActive = useRealtimeStore((s) => s.setActiveConversation);
  const [text, setText] = useState('');
  const [, forceTick] = useState(0);
  const scroller = useRef<HTMLDivElement>(null);
  const textarea = useRef<HTMLTextAreaElement>(null);
  const typingSent = useRef(0);
  const typingTimer = useRef<number>();

  // Oldest → newest for rendering (pages arrive newest-first).
  const messages = useMemo(() => [...flattenUnique(query.data)].reverse(), [query.data]);
  const lastMine = [...messages].reverse().find((m) => m.senderId === meId);
  const otherTyping = typingUntil > Date.now();

  // Track the open thread so incoming messages are marked read instead of toasted.
  useEffect(() => {
    setActive(conversationId);
    markConversationRead(qc, conversationId);
    const onVisible = () => document.visibilityState === 'visible' && markConversationRead(qc, conversationId);
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      setActive(null);
      document.removeEventListener('visibilitychange', onVisible);
    };
  }, [conversationId, qc, setActive]);

  // Typing indicators expire on their own.
  useEffect(() => {
    if (!otherTyping) return;
    const t = window.setTimeout(() => forceTick((x) => x + 1), typingUntil - Date.now() + 50);
    return () => window.clearTimeout(t);
  }, [otherTyping, typingUntil]);

  // Stick to the bottom when new messages arrive (if already near the bottom).
  const nearBottom = useRef(true);
  useLayoutEffect(() => {
    const el = scroller.current;
    if (el && nearBottom.current) el.scrollTop = el.scrollHeight;
  }, [messages.length, otherTyping]);

  const onScroll = () => {
    const el = scroller.current;
    if (!el) return;
    nearBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 120;
    if (el.scrollTop < 80 && query.hasNextPage && !query.isFetchingNextPage) {
      const before = el.scrollHeight;
      void query.fetchNextPage().then(() => {
        // Keep the viewport anchored when older messages are prepended.
        requestAnimationFrame(() => {
          if (scroller.current) scroller.current.scrollTop += scroller.current.scrollHeight - before;
        });
      });
    }
  };

  const emitTyping = (typing: boolean) => realtime.publish('/app/chat.typing', { conversationId, typing });

  const onChange = (value: string) => {
    setText(value);
    const now = Date.now();
    if (value && now - typingSent.current > TYPING_IDLE_MS) {
      typingSent.current = now;
      emitTyping(true);
    }
    window.clearTimeout(typingTimer.current);
    typingTimer.current = window.setTimeout(() => {
      typingSent.current = 0;
      emitTyping(false);
    }, TYPING_IDLE_MS);
  };

  const submit = () => {
    const content = text.trim();
    if (!content) return;
    nearBottom.current = true;
    send.mutate({ content });
    setText('');
    window.clearTimeout(typingTimer.current);
    typingSent.current = 0;
    emitTyping(false);
    textarea.current?.focus();
  };

  // Auto-grow the composer.
  useLayoutEffect(() => {
    const el = textarea.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 160)}px`;
  }, [text]);

  const other = conversation.data?.other;
  const name = other ? (other.displayName ?? other.username) : '';

  return (
    <div className="-mx-4 -mb-28 -mt-4 flex h-[calc(100dvh-3.5rem-4.5rem)] flex-col md:-mx-6 md:-mb-10 md:-mt-6 md:h-dvh">
      <header className="flex items-center gap-3 border-b border-line px-4 py-3 md:px-6">
        <Button asChild variant="ghost" size="icon-sm" aria-label="Back to conversations">
          <Link to="/messages">
            <ArrowLeft />
          </Link>
        </Button>
        {other ? (
          <Link to={`/u/${other.username}`} className="flex min-w-0 items-center gap-3">
            <Avatar src={other.avatarUrl} name={name} />
            <span className="min-w-0">
              <span className="block truncate font-semibold">{name}</span>
              <span className="block truncate text-xs text-muted">
                {otherTyping ? <span className="text-secondary">typing…</span> : `@${other.username}`}
              </span>
            </span>
          </Link>
        ) : (
          <Skeleton className="h-10 w-40 rounded-full" />
        )}
        {status !== 'connected' && status !== 'idle' && (
          <span className="ml-auto inline-flex items-center gap-1.5 text-xs text-muted" role="status">
            <Spinner className="[&_svg]:size-3.5" label="Reconnecting" /> Reconnecting…
          </span>
        )}
      </header>

      <div ref={scroller} onScroll={onScroll} className="flex-1 overflow-y-auto px-4 py-4 md:px-6" aria-live="polite" aria-label={`Conversation with ${name}`}>
        {query.isFetchingNextPage && (
          <div className="flex justify-center pb-3">
            <Spinner label="Loading older messages" />
          </div>
        )}
        {query.isPending ? (
          <div className="space-y-3" aria-busy>
            {['w-[48%]', 'w-[64%] ml-auto', 'w-[40%]', 'w-[56%] ml-auto'].map((cls) => (
              <Skeleton key={cls} className={cn('h-10 rounded-[20px]', cls)} />
            ))}
          </div>
        ) : messages.length === 0 ? (
          <p className="py-10 text-center text-sm text-muted">This is the start of your conversation with {name}. Say hello 👋</p>
        ) : (
          <ul className="space-y-1.5">
            {messages.map((m, i) => {
              const prev = messages[i - 1];
              const newDay = !prev || new Date(prev.createdAt).toDateString() !== new Date(m.createdAt).toDateString();
              const gap = prev && prev.senderId !== m.senderId;
              return (
                <Fragment key={m.clientId ?? m.id}>
                  {newDay && (
                    <li className="py-3 text-center text-xs font-medium uppercase tracking-[0.1em] text-muted" role="separator">
                      {dayLabel(m.createdAt)}
                    </li>
                  )}
                  {gap && !newDay && <li aria-hidden className="h-2" />}
                  <Bubble m={m} mine={m.senderId === meId} showReceipt={m.id === lastMine?.id} />
                </Fragment>
              );
            })}
            <AnimatePresence>{otherTyping && <TypingDots />}</AnimatePresence>
          </ul>
        )}
      </div>

      <form
        className="flex items-end gap-2 border-t border-line bg-bg px-4 py-3 md:px-6"
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <textarea
          ref={textarea}
          rows={1}
          value={text}
          maxLength={2000}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
              e.preventDefault();
              submit();
            }
          }}
          placeholder={`Message ${name}`}
          aria-label="Message"
          className="max-h-40 min-h-11 flex-1 resize-none rounded-[22px] border border-line-strong bg-surface px-4 py-2.5 text-[15px] leading-relaxed placeholder:text-muted focus-visible:border-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
        />
        <Button type="submit" size="icon" variant="accent" aria-label="Send" disabled={!text.trim()}>
          <SendHorizontal />
        </Button>
      </form>
    </div>
  );
}
