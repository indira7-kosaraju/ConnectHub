import { Link } from 'react-router-dom';
import { ArrowDown, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { CirclesIllustration, QuietIllustration } from '@/components/ui/illustrations';
import { PostList } from '@/components/feature/posts/post-list';
import { MomentsStrip } from '@/components/feature/moments/moments-strip';
import { useMyCircles } from '@/hooks/use-circles';
import { useFeed } from '@/hooks/use-posts';
import { usePullToRefresh } from '@/hooks/use-pull-to-refresh';
import { useUiStore } from '@/stores/ui-store';
import { cn } from '@/lib/utils';

export function HomePage() {
  const feed = useFeed();
  const myCircles = useMyCircles();
  const openComposer = useUiStore((s) => s.openComposer);
  const { pull, refreshing, ready } = usePullToRefresh(() => feed.refetch());
  const hasCircles = (myCircles.data?.length ?? 0) > 0;

  return (
    <div>
      {/* Pull-to-refresh indicator (touch devices) */}
      <div
        className="flex justify-center overflow-hidden transition-[height] duration-150 md:hidden"
        style={{ height: refreshing ? 40 : pull }}
        aria-live="polite"
      >
        <span className="flex items-center gap-2 self-end pb-2 text-sm text-muted">
          {refreshing ? (
            <>
              <Loader2 className="size-4 animate-spin" aria-hidden /> Refreshing…
            </>
          ) : (
            pull > 8 && (
              <>
                <ArrowDown className={cn('size-4 transition-transform', ready && 'rotate-180')} aria-hidden />
                {ready ? 'Release to refresh' : 'Pull to refresh'}
              </>
            )
          )}
        </span>
      </div>

      <h1 className="sr-only">Home</h1>
      <MomentsStrip />
      <PostList
        query={feed}
        empty={
          hasCircles ? (
            <EmptyState
              illustration={QuietIllustration}
              title="Your circles are quiet"
              description="Nobody has shared anything yet. Be the spark — post something to one of your circles."
              action={
                <Button variant="accent" size="lg" onClick={() => openComposer()}>
                  Share something
                </Button>
              }
            />
          ) : (
            <EmptyState
              illustration={CirclesIllustration}
              title="Find your circles"
              description="Your feed is built from the circles you join. Pick a few that match what you love."
              action={
                <Button asChild variant="accent" size="lg">
                  <Link to="/discover">Discover circles</Link>
                </Button>
              }
            />
          )
        }
      />
    </div>
  );
}
