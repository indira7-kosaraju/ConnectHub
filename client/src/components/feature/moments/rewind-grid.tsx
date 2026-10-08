import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { MomentsIllustration } from '@/components/ui/illustrations';
import { Skeleton } from '@/components/ui/skeleton';
import { useRewind } from '@/hooks/use-moments';
import { flattenUnique } from '@/lib/post-cache';
import type { MomentGroup } from '@/lib/api/schema';
import { useAuthStore } from '@/stores/auth-store';
import { useMomentViewer } from '@/stores/moment-viewer-store';
import { MomentContent } from './moment-content';

/** Your expired moments as a private, chronological grid. */
export function RewindGrid() {
  const rewind = useRewind(true);
  const me = useAuthStore((s) => s.user);
  const openViewer = useMomentViewer((s) => s.open);
  const moments = flattenUnique(rewind.data);

  if (rewind.isPending) {
    return (
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-4" aria-busy>
        {Array.from({ length: 8 }, (_, i) => (
          <Skeleton key={i} className="aspect-[9/16] rounded-xl" />
        ))}
      </div>
    );
  }
  if (moments.length === 0 || !me) {
    return (
      <EmptyState
        illustration={MomentsIllustration}
        title="Your Rewind is empty"
        description="Moments disappear from everyone else after 24 hours, then land here — visible only to you."
      />
    );
  }

  const group: MomentGroup = {
    user: { id: me.id, username: me.username ?? '', displayName: me.displayName, avatarUrl: me.avatarUrl },
    mine: true,
    allSeen: true,
    moments,
  };

  return (
    <div>
      <p className="mb-3 text-sm text-muted">Only you can see your Rewind.</p>
      <ul className="grid grid-cols-3 gap-2 sm:grid-cols-4">
        {moments.map((m, i) => (
          <li key={m.id}>
            <button
              type="button"
              onClick={() => openViewer([group], 0, { startMoment: i, archive: true })}
              className="group relative block aspect-[9/16] w-full overflow-hidden rounded-xl transition-transform hover:-translate-y-0.5"
              aria-label={`Moment from ${new Date(m.createdAt).toLocaleDateString()}`}
            >
              <MomentContent moment={m} size="tile" />
              <span className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-[#0c0b0a]/70 to-transparent px-2 pb-1.5 pt-6 text-left text-[11px] font-medium text-white">
                {new Date(m.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
              </span>
            </button>
          </li>
        ))}
      </ul>
      {rewind.hasNextPage && (
        <Button variant="outline" className="mt-4 w-full" onClick={() => void rewind.fetchNextPage()} loading={rewind.isFetchingNextPage}>
          Show older
        </Button>
      )}
    </div>
  );
}
