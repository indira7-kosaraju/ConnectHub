import { Link } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import type { CircleSummary } from '@/lib/api/schema';
import { CircleCover } from './circle-cover';
import { CircleMeta } from './circle-meta';
import { JoinButton } from './join-button';

export function CircleCard({ circle, hideJoin }: { circle: CircleSummary; hideJoin?: boolean }) {
  return (
    <Card interactive className="group relative flex flex-col overflow-hidden">
      <div className="h-24 overflow-hidden">
        <CircleCover seed={circle.slug} src={circle.coverUrl} />
      </div>
      <div className="flex flex-1 flex-col p-4">
        <h3 className="text-lg font-semibold leading-snug">
          {/* Stretched link: the whole card is clickable, the join button stays independent. */}
          <Link to={`/c/${circle.slug}`} className="after:absolute after:inset-0 after:content-[''] focus-visible:outline-none group-has-[a:focus-visible]:ring-2">
            {circle.name}
          </Link>
        </h3>
        <CircleMeta circle={circle} className="mt-1" />
        {circle.description && <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-muted">{circle.description}</p>}
        {!hideJoin && (
          <div className="relative z-10 mt-auto flex pt-4">
            <JoinButton slug={circle.slug} name={circle.name} privateCircle={circle.privateCircle} full={circle.full} status={circle.viewerStatus} />
          </div>
        )}
      </div>
    </Card>
  );
}

export function CircleCardSkeleton() {
  return (
    <Card className="overflow-hidden" aria-hidden>
      <Skeleton className="h-24 w-full rounded-none" />
      <div className="space-y-2 p-4">
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-3 w-1/2" />
        <Skeleton className="h-3 w-full" />
        <Skeleton className="mt-3 h-8 w-16 rounded-full" />
      </div>
    </Card>
  );
}
