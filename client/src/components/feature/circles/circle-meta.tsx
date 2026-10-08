import { Lock, Users } from 'lucide-react';
import type { CircleSummary } from '@/lib/api/schema';
import { cn } from '@/lib/utils';

/** "📷 Photography · 12/150 · Private" */
export function CircleMeta({ circle, className }: { circle: Pick<CircleSummary, 'category' | 'memberCount' | 'maxMembers' | 'privateCircle'>; className?: string }) {
  return (
    <p className={cn('flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px] text-muted', className)}>
      <span>
        <span aria-hidden>{circle.category.emoji} </span>
        {circle.category.name}
      </span>
      <span aria-hidden>·</span>
      <span className="inline-flex items-center gap-1">
        <Users className="size-3.5" aria-hidden />
        <span className="tabular-nums">
          {circle.memberCount}/{circle.maxMembers}
        </span>
        <span className="sr-only">members</span>
      </span>
      {circle.privateCircle && (
        <>
          <span aria-hidden>·</span>
          <span className="inline-flex items-center gap-1">
            <Lock className="size-3.5" aria-hidden /> Private
          </span>
        </>
      )}
    </p>
  );
}
