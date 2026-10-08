import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { CalendarDays, Lock, Pencil } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { QuietIllustration } from '@/components/ui/illustrations';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { CircleCover } from '@/components/feature/circles/circle-cover';
import { CircleMeta } from '@/components/feature/circles/circle-meta';
import { JoinButton } from '@/components/feature/circles/join-button';
import { EditCircleDialog } from '@/components/feature/circles/edit-circle-dialog';
import { MemberList } from '@/components/feature/circles/member-list';
import { useCircle } from '@/hooks/use-circles';
import { useCirclePosts, usePost } from '@/hooks/use-posts';
import { PostList } from '@/components/feature/posts/post-list';
import { PostCard } from '@/components/feature/posts/post-card';
import { PostCardSkeleton } from '@/components/feature/post-card-skeleton';
import { ApiRequestError } from '@/lib/api/client';
import { useUiStore } from '@/stores/ui-store';
import { NotFoundPanel } from './not-found-page';

function CircleSkeleton() {
  return (
    <Card className="overflow-hidden" aria-busy>
      <Skeleton className="h-40 w-full rounded-none sm:h-48" />
      <div className="space-y-3 p-5">
        <Skeleton className="h-7 w-2/3" />
        <Skeleton className="h-3.5 w-1/3" />
        <Skeleton className="h-3.5 w-full" />
      </div>
    </Card>
  );
}

export function CirclePage() {
  const { slug = '' } = useParams<{ slug: string }>();
  const circle = useCircle(slug);
  const canView = !!circle.data?.canViewContent;
  const posts = useCirclePosts(slug, canView);
  const pinned = usePost(canView ? circle.data?.pinnedPostId : null);
  const openComposer = useUiStore((s) => s.openComposer);
  const [editing, setEditing] = useState(false);

  if (circle.isPending) return <CircleSkeleton />;
  if (circle.error instanceof ApiRequestError && circle.error.status === 404) {
    return <NotFoundPanel title="This circle doesn’t exist" description="It may have been renamed or closed." />;
  }
  if (!circle.data) throw circle.error;

  const c = circle.data;
  const isAdmin = c.viewer?.role === 'admin' && c.viewer.status === 'active';
  const isMember = c.viewer?.status === 'active';
  const created = new Date(c.createdAt).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden">
        <div className="h-40 sm:h-48">
          <CircleCover seed={c.slug} src={c.coverUrl} alt={`${c.name} cover`} priority />
        </div>
        <div className="p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <h1 className="text-[28px] font-semibold leading-tight">{c.name}</h1>
              <CircleMeta circle={c} className="mt-1.5" />
            </div>
            <div className="flex items-center gap-2">
              {isAdmin && (
                <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
                  <Pencil aria-hidden /> Edit
                </Button>
              )}
              <JoinButton
                slug={c.slug}
                name={c.name}
                privateCircle={c.privateCircle}
                full={c.full}
                status={c.viewer?.status}
                isAdmin={isAdmin}
                size="md"
              />
            </div>
          </div>
          {c.description && <p className="mt-4 max-w-prose text-[15px] leading-relaxed">{c.description}</p>}
        </div>
      </Card>

      {!c.canViewContent ? (
        <Card className="p-8 text-center">
          <Lock className="mx-auto size-8 text-muted" aria-hidden />
          <h2 className="mt-3 text-xl font-semibold">This circle is private</h2>
          <p className="mx-auto mt-1 max-w-sm text-muted">
            {c.viewer?.status === 'pending'
              ? 'Your request is with the admins. You’ll be notified when they respond.'
              : 'Request to join to see posts and members.'}
          </p>
        </Card>
      ) : (
        <Tabs defaultValue="feed">
          <TabsList>
            <TabsTrigger value="feed">Feed</TabsTrigger>
            <TabsTrigger value="members">Members</TabsTrigger>
            <TabsTrigger value="about">About</TabsTrigger>
            {isAdmin && (
              <TabsTrigger value="requests" className="inline-flex items-center gap-1.5">
                Requests {c.pendingCount > 0 && <Badge variant="count">{c.pendingCount}</Badge>}
              </TabsTrigger>
            )}
          </TabsList>
          <TabsContent value="feed" className="space-y-4">
            {isMember && (
              <button
                type="button"
                onClick={() => openComposer(c.id)}
                className="flex w-full items-center gap-3 rounded-[var(--radius-card)] border border-line bg-surface p-3 text-left text-muted transition-colors hover:bg-surface-2"
              >
                <Pencil className="size-4" aria-hidden /> Share something with {c.name}…
              </button>
            )}
            {pinned.data ? <PostCard post={pinned.data} hideCircle /> : c.pinnedPostId && pinned.isPending && <PostCardSkeleton />}
            <PostList
              query={posts}
              hideCircle
              exclude={c.pinnedPostId}
              empty={
                pinned.data ? null : (
                  <EmptyState
                    illustration={QuietIllustration}
                    title="Nothing shared yet"
                    description={isMember ? 'Start the conversation — your post will be the first thing people see.' : 'Join to share with this circle.'}
                    action={isMember ? <Button variant="accent" onClick={() => openComposer(c.id)}>Share something</Button> : undefined}
                  />
                )
              }
            />
          </TabsContent>
          <TabsContent value="members">
            <MemberList slug={c.slug} status="active" isAdmin={isAdmin} />
          </TabsContent>
          <TabsContent value="about" className="space-y-6">
            <section>
              <h2 className="text-lg font-semibold">About</h2>
              <p className="mt-1 text-[15px] leading-relaxed text-muted">{c.description || 'No description yet.'}</p>
              <p className="mt-3 inline-flex items-center gap-2 text-sm text-muted">
                <CalendarDays className="size-4" aria-hidden /> Started {created}
              </p>
            </section>
            <section>
              <h2 className="text-lg font-semibold">Admins</h2>
              <ul className="mt-2 flex flex-wrap gap-3">
                {c.admins.map((a) => (
                  <li key={a.id}>
                    <Link to={`/u/${a.username}`} className="flex items-center gap-2 rounded-full bg-surface-2 py-1 pl-1 pr-3 text-sm hover:bg-line">
                      <Avatar size="sm" src={a.avatarUrl} name={a.displayName ?? a.username} />
                      {a.displayName ?? a.username}
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          </TabsContent>
          {isAdmin && (
            <TabsContent value="requests">
              <MemberList slug={c.slug} status="pending" isAdmin />
            </TabsContent>
          )}
        </Tabs>
      )}

      {isAdmin && <EditCircleDialog circle={c} open={editing} onOpenChange={setEditing} />}
    </div>
  );
}
