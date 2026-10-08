import { Link } from 'react-router-dom';
import { MoreHorizontal, UserMinus } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { EmptyState } from '@/components/ui/empty-state';
import { CirclesIllustration } from '@/components/ui/illustrations';
import { Skeleton } from '@/components/ui/skeleton';
import { useCircleMembers, useModerateMember } from '@/hooks/use-circles';
import type { MemberDto, MemberStatus } from '@/lib/api/schema';
import { useAuthStore } from '@/stores/auth-store';

function MemberRow({ member, actions }: { member: MemberDto; actions?: React.ReactNode }) {
  const name = member.user.displayName ?? member.user.username;
  return (
    <li className="flex items-center gap-3 py-3">
      <Link to={`/u/${member.user.username}`} className="flex min-w-0 flex-1 items-center gap-3 rounded-lg">
        <Avatar src={member.user.avatarUrl} name={name} />
        <span className="min-w-0">
          <span className="flex items-center gap-2">
            <span className="truncate font-medium">{name}</span>
            {member.role === 'admin' && member.status === 'active' && <Badge variant="secondary">Admin</Badge>}
          </span>
          <span className="block truncate text-sm text-muted">@{member.user.username}</span>
        </span>
      </Link>
      {actions}
    </li>
  );
}

export function MemberList({ slug, status, isAdmin }: { slug: string; status: MemberStatus; isAdmin: boolean }) {
  const meId = useAuthStore((s) => s.user?.id);
  const query = useCircleMembers(slug, status);
  const { approve, remove } = useModerateMember(slug);
  const members = query.data?.pages.flatMap((p) => p.items) ?? [];

  if (query.isPending) {
    return (
      <ul className="divide-y divide-line" aria-busy>
        {Array.from({ length: 5 }, (_, i) => (
          <li key={i} className="flex items-center gap-3 py-3">
            <Skeleton className="size-10 rounded-full" />
            <div className="space-y-1.5">
              <Skeleton className="h-3.5 w-32" />
              <Skeleton className="h-3 w-20" />
            </div>
          </li>
        ))}
      </ul>
    );
  }

  if (status === 'pending' && members.length === 0) {
    return <EmptyState illustration={CirclesIllustration} title="No requests right now" description="When someone asks to join, you’ll review them here." />;
  }

  return (
    <div>
      <ul className="divide-y divide-line">
        {members.map((m) => (
          <MemberRow
            key={m.user.id}
            member={m}
            actions={
              status === 'pending' ? (
                <div className="flex gap-2">
                  <Button size="sm" variant="ghost" onClick={() => remove.mutate({ userId: m.user.id, pending: true })} disabled={remove.isPending}>
                    Decline
                  </Button>
                  <Button size="sm" onClick={() => approve.mutate(m.user.id)} loading={approve.isPending && approve.variables === m.user.id}>
                    Approve
                  </Button>
                </div>
              ) : isAdmin && m.role !== 'admin' && m.user.id !== meId ? (
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon-sm" aria-label={`Manage ${m.user.username}`}>
                      <MoreHorizontal />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent>
                    <DropdownMenuItem destructive onSelect={() => remove.mutate({ userId: m.user.id, pending: false })}>
                      <UserMinus /> Remove from circle
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              ) : undefined
            }
          />
        ))}
      </ul>
      {query.hasNextPage && (
        <Button variant="outline" className="mt-4 w-full" onClick={() => void query.fetchNextPage()} loading={query.isFetchingNextPage}>
          Show more
        </Button>
      )}
    </div>
  );
}
