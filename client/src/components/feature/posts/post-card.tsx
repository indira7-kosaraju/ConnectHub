import { memo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bookmark, Flag, Link2, MessageCircle, MoreHorizontal, Pencil, Pin, PinOff, Send, Trash2 } from 'lucide-react';
import { SharePostDialog } from '@/components/feature/messages/share-post-dialog';
import { Avatar } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Textarea } from '@/components/ui/textarea';
import { CharCount } from '@/components/ui/char-count';
import { toast } from '@/components/ui/toaster';
import type { PostDto } from '@/lib/api/schema';
import { useDeletePost, useEditPost, usePinPost, useReactToPost, useReportPost, useSavePost } from '@/hooks/use-posts';
import { fullTime, shortTime } from '@/lib/time';
import { useAuthStore } from '@/stores/auth-store';
import { cn } from '@/lib/utils';
import { VibeButton } from './vibe-button';
import { PostImages } from './post-images';
import { RichText } from './rich-text';
import { ReportDialog } from './report-dialog';

interface PostCardProps {
  post: PostDto;
  /** Hide the "in <circle>" label (on circle pages). */
  hideCircle?: boolean;
  /** Detail view: no link-through, larger text. */
  detail?: boolean;
}

function PostCardImpl({ post, hideCircle, detail }: PostCardProps) {
  const navigate = useNavigate();
  const signedIn = useAuthStore((s) => !!s.user);
  const react = useReactToPost();
  const save = useSavePost();
  const edit = useEditPost();
  const remove = useDeletePost();
  const report = useReportPost();
  const pin = usePinPost();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(post.content ?? '');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [sharing, setSharing] = useState(false);

  const name = post.author.displayName ?? post.author.username;
  const canEdit = !!post.editableUntil && new Date(post.editableUntil).getTime() > Date.now();
  const href = `/p/${post.id}`;
  // Pinning is circle context: only flag it on the circle's own page.
  const showPinned = post.pinned && !!hideCircle && !detail;

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(`${window.location.origin}${href}`);
      toast('Link copied');
    } catch {
      toast.error('Couldn’t copy the link');
    }
  };

  return (
    <Card className={cn('p-4 sm:p-5', showPinned && 'border-accent/40')}>
      <article aria-labelledby={`post-${post.id}-author`}>
        <header className="flex items-start gap-3">
          <Link to={`/u/${post.author.username}`} className="shrink-0 rounded-full" tabIndex={-1} aria-hidden>
            <Avatar src={post.author.avatarUrl} name={name} />
          </Link>
          <div className="min-w-0 flex-1">
            <p className="flex flex-wrap items-baseline gap-x-1.5 text-[15px] leading-tight">
              <Link id={`post-${post.id}-author`} to={`/u/${post.author.username}`} className="inline-flex min-h-6 items-center font-semibold hover:underline">
                {name}
              </Link>
              <span className="text-sm text-muted">@{post.author.username}</span>
            </p>
            <p className="mt-0.5 flex min-w-0 items-center gap-x-1.5 whitespace-nowrap text-[13px] text-muted">
              {!hideCircle && (
                <>
                  <span>in</span>
                  <Link to={`/c/${post.circle.slug}`} className="inline-block min-h-6 min-w-0 truncate font-medium leading-6 text-secondary hover:underline">
                    {post.circle.name}
                  </Link>
                  <span aria-hidden>·</span>
                </>
              )}
              <Link to={href} className="inline-flex min-h-6 shrink-0 items-center hover:underline">
                <time dateTime={post.createdAt} title={fullTime(post.createdAt)}>
                  {shortTime(post.createdAt)}
                </time>
              </Link>
              {post.editedAt && <span className="shrink-0" title={`Edited ${fullTime(post.editedAt)}`}>· edited</span>}
            </p>
          </div>
          {showPinned && (
            <Badge variant="accent" className="shrink-0">
              <Pin className="size-3" aria-hidden /> Pinned
            </Badge>
          )}
          {signedIn && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon-sm" className="-mr-1.5 -mt-1 shrink-0 text-muted" aria-label="Post options">
                  <MoreHorizontal />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent>
                <DropdownMenuItem onSelect={() => void copyLink()}>
                  <Link2 /> Copy link
                </DropdownMenuItem>
                <DropdownMenuItem onSelect={() => setSharing(true)}>
                  <Send /> Send in a message
                </DropdownMenuItem>
                {canEdit && (
                  <DropdownMenuItem
                    onSelect={() => {
                      setDraft(post.content ?? '');
                      setEditing(true);
                    }}
                  >
                    <Pencil /> Edit
                  </DropdownMenuItem>
                )}
                {post.canPin && (
                  <DropdownMenuItem onSelect={() => pin.mutate({ slug: post.circle.slug, postId: post.id, pin: !post.pinned })}>
                    {post.pinned ? <PinOff /> : <Pin />} {post.pinned ? 'Unpin from circle' : 'Pin to circle'}
                  </DropdownMenuItem>
                )}
                {!post.canDelete && (
                  <DropdownMenuItem onSelect={() => setReporting(true)}>
                    <Flag /> Report
                  </DropdownMenuItem>
                )}
                {post.canDelete && (
                  <>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem destructive onSelect={() => setConfirmDelete(true)}>
                      <Trash2 /> Delete
                    </DropdownMenuItem>
                  </>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </header>

        {editing ? (
          <form
            className="mt-3 space-y-2"
            onSubmit={(e) => {
              e.preventDefault();
              edit.mutate({ id: post.id, content: draft }, { onSuccess: () => setEditing(false) });
            }}
          >
            <Textarea
              autoFocus
              aria-label="Edit post"
              maxLength={500}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              aria-describedby={`edit-${post.id}-count`}
            />
            <div className="flex items-center justify-end gap-2">
              <CharCount id={`edit-${post.id}-count`} value={draft} max={500} />
              <Button type="button" size="sm" variant="ghost" onClick={() => setEditing(false)}>
                Cancel
              </Button>
              <Button type="submit" size="sm" loading={edit.isPending}>
                Save
              </Button>
            </div>
          </form>
        ) : (
          post.content && (
            <RichText
              text={post.content}
              className={cn('mt-3 whitespace-pre-line break-words leading-relaxed', detail ? 'text-[17px]' : 'text-[15px]')}
            />
          )
        )}

        <PostImages images={post.images} alt={`Photo shared by ${name}`} />

        <footer className="mt-4 flex items-center gap-1">
          <VibeButton vibe={post.vibe} mine={post.myReaction} onChange={(type) => react.mutate({ id: post.id, type })} />
          <Button
            variant="ghost"
            size="sm"
            className="ml-1 text-muted"
            onClick={() => !detail && navigate(href)}
            aria-label={`${post.commentCount} comments`}
          >
            <MessageCircle aria-hidden />
            {post.commentCount > 0 && <span className="tabular-nums">{post.commentCount}</span>}
          </Button>
          {signedIn && (
            <Button
              variant="ghost"
              size="icon-sm"
              className={cn('ml-auto', post.saved ? 'text-accent-ink' : 'text-muted')}
              aria-pressed={post.saved}
              aria-label={post.saved ? 'Remove from saved' : 'Save post'}
              onClick={() => save.mutate({ id: post.id, save: !post.saved })}
            >
              <Bookmark className={cn(post.saved && 'fill-current')} />
            </Button>
          )}
        </footer>
      </article>

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Delete this post?"
        description="Its vibes and comments go with it. This can’t be undone."
        confirmLabel="Delete"
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          remove.mutate(post.id, {
            onSuccess: () => {
              setConfirmDelete(false);
              if (detail) navigate(-1);
            },
          })
        }
      />
      {sharing && <SharePostDialog post={post} open={sharing} onOpenChange={setSharing} />}
      <ReportDialog
        open={reporting}
        onOpenChange={setReporting}
        loading={report.isPending}
        onSubmit={(reason, details) =>
          report.mutate({ id: post.id, reason, details: details || undefined }, { onSuccess: () => setReporting(false) })
        }
      />
    </Card>
  );
}

export const PostCard = memo(PostCardImpl);
