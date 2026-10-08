import { Link } from 'react-router-dom';
import { Skeleton } from '@/components/ui/skeleton';
import type { CircleSummary } from '@/lib/api/schema';
import { CircleCover } from './circle-cover';
import { JoinButton } from './join-button';

/** Compact circle row for rails and onboarding. */
export function CircleRow({ circle, showJoin = true }: { circle: CircleSummary; showJoin?: boolean }) {
  return (
    <div className="flex items-center gap-3">
      <Link to={`/c/${circle.slug}`} className="size-11 shrink-0 overflow-hidden rounded-xl" tabIndex={-1} aria-hidden>
        <CircleCover seed={circle.slug} src={circle.coverUrl} />
      </Link>
      <div className="min-w-0 flex-1">
        <Link to={`/c/${circle.slug}`} className="block truncate text-sm font-medium hover:underline">
          {circle.name}
        </Link>
        <p className="truncate text-xs text-muted">
          {circle.category.emoji} {circle.category.name} · {circle.memberCount}/{circle.maxMembers}
        </p>
      </div>
      {showJoin && <JoinButton slug={circle.slug} name={circle.name} privateCircle={circle.privateCircle} full={circle.full} status={circle.viewerStatus} />}
    </div>
  );
}

export function CircleRowSkeleton() {
  return (
    <div className="flex items-center gap-3" aria-hidden>
      <Skeleton className="size-11 rounded-xl" />
      <div className="flex-1 space-y-1.5">
        <Skeleton className="h-3.5 w-28" />
        <Skeleton className="h-3 w-20" />
      </div>
      <Skeleton className="h-8 w-14 rounded-full" />
    </div>
  );
}
