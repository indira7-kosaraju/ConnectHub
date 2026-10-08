import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Ban, CalendarDays, Flag, Link2, Lock, MapPin, MessageCircle, MoreHorizontal, Pencil } from 'lucide-react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ReportDialog } from '@/components/feature/posts/report-dialog';
import { useBlockMutations, useReportUser } from '@/hooks/use-settings';
import { useAuthStore } from '@/stores/auth-store';
import { useStartConversation } from '@/hooks/use-messages';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { EmptyState } from '@/components/ui/empty-state';
import { CirclesIllustration, QuietIllustration } from '@/components/ui/illustrations';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { CircleCover } from '@/components/feature/circles/circle-cover';
import { CircleCard, CircleCardSkeleton } from '@/components/feature/circles/circle-card';
import { EditProfileDialog } from '@/components/feature/profile/edit-profile-dialog';
import { useProfile, useProfileCircles } from '@/hooks/use-profile';
import { useUserPosts } from '@/hooks/use-posts';
import { PostList } from '@/components/feature/posts/post-list';
import { RewindGrid } from '@/components/feature/moments/rewind-grid';
import { ApiRequestError } from '@/lib/api/client';
import { NotFoundPanel } from './not-found-page';

function ProfileSkeleton() {
  return (
    <Card className="overflow-hidden" aria-busy>
      <Skeleton className="h-36 w-full rounded-none sm:h-44" />
      <div className="px-5 pb-5">
        <Skeleton className="-mt-12 size-24 rounded-full border-4 border-surface" />
        <Skeleton className="mt-3 h-6 w-48" />
        <Skeleton className="mt-2 h-3.5 w-24" />
        <Skeleton className="mt-4 h-3.5 w-3/4" />
      </div>
    </Card>
  );
}

function hostOf(url: string): string {
  try {
    return new URL(url).host.replace(/^www\./, '');
  } catch {
    return url;
  }
}

export function ProfilePage() {
  const { username = '' } = useParams<{ username: string }>();
  const profile = useProfile(username);
  const p = profile.data;
  const circles = useProfileCircles(username, !!p && !p.restricted);
  const posts = useUserPosts(username, !!p && !p.restricted);
  const [editing, setEditing] = useState(false);
  const navigate = useNavigate();
  const startChat = useStartConversation();
  const signedIn = useAuthStore((s) => !!s.user);
  const { block } = useBlockMutations();
  const reportUser = useReportUser();
  const [confirmBlock, setConfirmBlock] = useState(false);
  const [reporting, setReporting] = useState(false);

  if (profile.isPending) return <ProfileSkeleton />;
  if (profile.error instanceof ApiRequestError && profile.error.status === 404) {
    return <NotFoundPanel title="No one goes by that name" description={`We couldn’t find @${username}. Check the spelling?`} />;
  }
  if (!p) throw profile.error;

  const name = p.displayName ?? p.username;
  const joined = new Date(p.joinedAt).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

  return (
    <div className="space-y-6">
      <Card className="overflow-hidden">
        <div className="h-36 sm:h-44">
          <CircleCover seed={p.username} src={p.coverUrl} alt="" priority />
        </div>
        <div className="px-5 pb-5">
          <div className="flex items-end justify-between gap-3">
            <Avatar src={p.avatarUrl} name={name} size="xl" className="-mt-12 border-4 border-surface" />
            <div className="flex items-center gap-2">
            {p.me && (
              <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
                <Pencil aria-hidden /> Edit profile
              </Button>
            )}
            {p.canMessage && (
              <Button
                variant="outline"
                size="sm"
                loading={startChat.isPending}
                onClick={() => startChat.mutate(p.id, { onSuccess: (c) => navigate(`/messages/${c.id}`) })}
              >
                <MessageCircle aria-hidden /> Message
              </Button>
            )}
            {signedIn && !p.me && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="icon-sm" aria-label={`More options for ${p.username}`}>
                    <MoreHorizontal />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent>
                  <DropdownMenuItem onSelect={() => setReporting(true)}>
                    <Flag /> Report @{p.username}
                  </DropdownMenuItem>
                  <DropdownMenuItem destructive onSelect={() => setConfirmBlock(true)}>
                    <Ban /> Block @{p.username}
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
            </div>
          </div>
          <h1 className="mt-3 text-[26px] font-semibold leading-tight">{name}</h1>
          <p className="text-muted">@{p.username}</p>
          {p.bio && <p className="mt-3 max-w-prose whitespace-pre-line text-[15px] leading-relaxed">{p.bio}</p>}
          <ul className="mt-3 flex flex-wrap gap-x-4 gap-y-1.5 text-sm text-muted">
            {p.location && (
              <li className="inline-flex items-center gap-1.5">
                <MapPin className="size-4" aria-hidden /> {p.location}
              </li>
            )}
            {p.website && (
              <li className="inline-flex items-center gap-1.5">
                <Link2 className="size-4" aria-hidden />
                <a href={p.website} target="_blank" rel="noopener noreferrer nofollow ugc" className="text-accent-ink hover:underline">
                  {hostOf(p.website)}
                </a>
              </li>
            )}
            <li className="inline-flex items-center gap-1.5">
              <CalendarDays className="size-4" aria-hidden /> Joined {joined}
            </li>
            {p.privateProfile && (
              <li className="inline-flex items-center gap-1.5">
                <Lock className="size-4" aria-hidden /> Private profile
              </li>
            )}
          </ul>
        </div>
      </Card>

      {p.restricted ? (
        <Card className="p-8 text-center">
          <Lock className="mx-auto size-8 text-muted" aria-hidden />
          <h2 className="mt-3 text-xl font-semibold">This profile is private</h2>
          <p className="mx-auto mt-1 max-w-sm text-muted">Only people who share a circle with {name} can see their posts and circles.</p>
        </Card>
      ) : (
        <Tabs defaultValue="posts">
          <TabsList>
            <TabsTrigger value="posts">Posts</TabsTrigger>
            {p.me && <TabsTrigger value="rewind">Rewind</TabsTrigger>}
            <TabsTrigger value="circles">Circles</TabsTrigger>
          </TabsList>
          <TabsContent value="posts">
            <PostList
              query={posts}
              empty={
                <EmptyState
                  illustration={QuietIllustration}
                  title={p.me ? 'You haven’t posted yet' : 'Nothing posted yet'}
                  description={p.me ? 'Share something with one of your circles — it’ll show up here.' : `When ${name} shares with a circle you can see, it’ll show up here.`}
                />
              }
            />
          </TabsContent>
          {p.me && (
            <TabsContent value="rewind">
              <RewindGrid />
            </TabsContent>
          )}
          <TabsContent value="circles">
            {circles.isPending ? (
              <div className="grid gap-4 sm:grid-cols-2">
                <CircleCardSkeleton />
                <CircleCardSkeleton />
              </div>
            ) : circles.data?.length ? (
              <div className="grid gap-4 sm:grid-cols-2">
                {circles.data.map((c) => (
                  <CircleCard key={c.id} circle={c} hideJoin={p.me} />
                ))}
              </div>
            ) : (
              <EmptyState
                illustration={CirclesIllustration}
                title="No circles yet"
                description={p.me ? 'Find a few circles that match your interests.' : `${name} hasn’t joined any circles you can see.`}
                action={
                  p.me ? (
                    <Button asChild variant="accent">
                      <Link to="/discover">Discover circles</Link>
                    </Button>
                  ) : undefined
                }
              />
            )}
          </TabsContent>
        </Tabs>
      )}

      {p.me && <EditProfileDialog open={editing} onOpenChange={setEditing} />}
      <ConfirmDialog
        open={confirmBlock}
        onOpenChange={setConfirmBlock}
        title={`Block ${name}?`}
        description="You won’t see each other’s posts, comments, moments or profile, and you won’t be able to message each other. They won’t be told. You can unblock from Settings."
        confirmLabel="Block"
        destructive
        loading={block.isPending}
        onConfirm={() => block.mutate(p.username, { onSuccess: () => navigate('/', { replace: true }) })}
      />
      <ReportDialog
        title={`Report @${p.username}`}
        open={reporting}
        onOpenChange={setReporting}
        loading={reportUser.isPending}
        onSubmit={(reason, details) =>
          reportUser.mutate({ username: p.username, reason, details: details || undefined }, { onSuccess: () => setReporting(false) })
        }
      />
    </div>
  );
}
