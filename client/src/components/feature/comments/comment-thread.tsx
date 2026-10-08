import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Flag, Trash2 } from 'lucide-react';
import { ReportDialog } from '@/components/feature/posts/report-dialog';
import { useReportComment } from '@/hooks/use-settings';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { MessagesIllustration } from '@/components/ui/illustrations';
import { Skeleton } from '@/components/ui/skeleton';
import { VibeButton } from '@/components/feature/posts/vibe-button';
import { RichText } from '@/components/feature/posts/rich-text';
import { useCommentMutations, useComments } from '@/hooks/use-comments';
import type { CommentDto } from '@/lib/api/schema';
import { flattenUnique } from '@/lib/post-cache';
import { fullTime, shortTime } from '@/lib/time';
import { useAuthStore } from '@/stores/auth-store';
import { CommentForm } from './comment-form';

function CommentItem({
  comment,
  onReply,
  mutations,
}: {
  comment: CommentDto;
  onReply?: () => void;
  mutations: ReturnType<typeof useCommentMutations>;
}) {
  const name = comment.author.displayName ?? comment.author.username;
  const signedIn = useAuthStore((s) => !!s.user);
  const meId = useAuthStore((s) => s.user?.id);
  const report = useReportComment();
  const [reporting, setReporting] = useState(false);
  return (
    <div className="flex gap-3">
      <Link to={`/u/${comment.author.username}`} className="shrink-0" tabIndex={-1} aria-hidden>
        <Avatar size="sm" src={comment.author.avatarUrl} name={name} />
      </Link>
      <div className="min-w-0 flex-1">
        <div className="rounded-2xl bg-surface-2 px-3.5 py-2.5">
          <p className="text-sm">
            <Link to={`/u/${comment.author.username}`} className="font-semibold hover:underline">
              {name}
            </Link>{' '}
            <time className="text-xs text-muted" dateTime={comment.createdAt} title={fullTime(comment.createdAt)}>
              {shortTime(comment.createdAt)}
            </time>
          </p>
          <RichText text={comment.content} className="mt-0.5 whitespace-pre-line break-words text-[15px] leading-relaxed" />
        </div>
        <div className="mt-1 flex items-center gap-1 pl-1">
          <VibeButton size="sm" vibe={comment.vibe} mine={comment.myReaction} onChange={(type) => mutations.react.mutate({ id: comment.id, type })} />
          {onReply && (
            <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-muted" onClick={onReply}>
              Reply
            </Button>
          )}
          {signedIn && comment.author.id !== meId && (
            <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-muted" onClick={() => setReporting(true)} aria-label="Report comment">
              <Flag className="size-3.5" aria-hidden />
            </Button>
          )}
          {comment.canDelete && (
            <Button
              variant="ghost"
              size="sm"
              className="h-7 px-2 text-xs text-muted hover:text-danger"
              onClick={() => mutations.remove.mutate(comment)}
              aria-label="Delete comment"
            >
              <Trash2 className="size-3.5" aria-hidden />
            </Button>
          )}
        </div>
      </div>
      <ReportDialog
        title="Report this comment"
        open={reporting}
        onOpenChange={setReporting}
        loading={report.isPending}
        onSubmit={(reason, details) => report.mutate({ id: comment.id, reason, details: details || undefined }, { onSuccess: () => setReporting(false) })}
      />
    </div>
  );
}

export function CommentThread({ postId }: { postId: string }) {
  const signedIn = useAuthStore((s) => !!s.user);
  const query = useComments(postId);
  const mutations = useCommentMutations(postId);
  const [replyTo, setReplyTo] = useState<string | null>(null);
  const comments = flattenUnique(query.data);

  return (
    <section aria-labelledby="comments-heading" className="space-y-5">
      <h2 id="comments-heading" className="text-lg font-semibold">
        Comments
      </h2>
      {signedIn && <CommentForm onSubmit={(content) => mutations.create.mutateAsync({ content })} />}

      {query.isPending ? (
        <div className="space-y-4" aria-busy>
          {Array.from({ length: 3 }, (_, i) => (
            <div key={i} className="flex gap-3">
              <Skeleton className="size-8 rounded-full" />
              <Skeleton className="h-16 flex-1 rounded-2xl" />
            </div>
          ))}
        </div>
      ) : comments.length === 0 ? (
        <EmptyState illustration={MessagesIllustration} title="No comments yet" description="Start the conversation — be kind, be curious." className="py-6" />
      ) : (
        <ul className="space-y-5">
          {comments.map((c) => (
            <li key={c.id} className="space-y-3">
              <CommentItem comment={c} mutations={mutations} onReply={signedIn ? () => setReplyTo(c.id) : undefined} />
              {(c.replies.length > 0 || replyTo === c.id) && (
                <ul className="ml-11 space-y-3 border-l-2 border-line pl-4">
                  {c.replies.map((r) => (
                    <li key={r.id}>
                      <CommentItem comment={r} mutations={mutations} onReply={signedIn ? () => setReplyTo(c.id) : undefined} />
                    </li>
                  ))}
                  {replyTo === c.id && (
                    <li>
                      <CommentForm
                        compact
                        autoFocus
                        placeholder={`Reply to ${c.author.displayName ?? c.author.username}…`}
                        onCancel={() => setReplyTo(null)}
                        onSubmit={(content) =>
                          mutations.create.mutateAsync({ content, parentId: c.id }).then(() => setReplyTo(null))
                        }
                      />
                    </li>
                  )}
                </ul>
              )}
            </li>
          ))}
        </ul>
      )}
      {query.hasNextPage && (
        <Button variant="outline" className="w-full" onClick={() => void query.fetchNextPage()} loading={query.isFetchingNextPage}>
          Show more comments
        </Button>
      )}
    </section>
  );
}
