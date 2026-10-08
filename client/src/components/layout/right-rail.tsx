import { Link } from 'react-router-dom';
import { Search } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { inputClasses } from '@/components/ui/input';
import { CircleRow, CircleRowSkeleton } from '@/components/feature/circles/circle-row';
import { useCircleSuggestions } from '@/hooks/use-circles';
import { useMomentStrip } from '@/hooks/use-moments';
import { useMomentViewer } from '@/stores/moment-viewer-store';
import { useUiStore } from '@/stores/ui-store';
import { MomentAvatar } from '@/components/feature/moments/moment-avatar';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/stores/auth-store';
import { cn } from '@/lib/utils';

function SuggestedCircles() {
  const suggestions = useCircleSuggestions(4);
  if (suggestions.isSuccess && suggestions.data.length === 0) return null;
  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle className="text-base">Circles for you</CardTitle>
        <Link to="/discover" className="text-sm text-accent-ink hover:underline">
          See all
        </Link>
      </CardHeader>
      <CardContent className="space-y-4">
        {suggestions.isPending
          ? Array.from({ length: 3 }, (_, i) => <CircleRowSkeleton key={i} />)
          : suggestions.data?.map((c) => <CircleRow key={c.id} circle={c} />)}
      </CardContent>
    </Card>
  );
}

function TodaysMoments() {
  const strip = useMomentStrip();
  const openViewer = useMomentViewer((s) => s.open);
  const openComposer = useUiStore((s) => s.setMomentComposerOpen);
  const others = (strip.data ?? []).map((g, i) => ({ g, i })).filter(({ g }) => !g.mine);
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Today’s moments</CardTitle>
      </CardHeader>
      <CardContent>
        {others.length === 0 ? (
          <p className="text-sm text-muted">Moments from your circles appear here for 24 hours.</p>
        ) : (
          <ul className="grid grid-cols-4 gap-3">
            {others.slice(0, 8).map(({ g, i }) => (
              <li key={g.user.id}>
                <button
                  type="button"
                  onClick={() => openViewer(strip.data!, i)}
                  className="flex w-full flex-col items-center gap-1 rounded-xl"
                  aria-label={`${g.user.displayName ?? g.user.username}'s moments${g.allSeen ? '' : ', new'}`}
                >
                  <MomentAvatar user={g.user} unseen={!g.allSeen} size="md" />
                  <span className="w-full truncate text-center text-[11px] text-muted">{g.user.displayName?.split(' ')[0] ?? g.user.username}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        <Button variant="outline" size="sm" className="mt-4 w-full" onClick={() => openComposer(true)}>
          Share a moment
        </Button>
      </CardContent>
    </Card>
  );
}

/** Desktop right rail: search, suggested circles and today's moments. */
export function RightRail() {
  const signedIn = useAuthStore((s) => !!s.user);
  return (
    <aside aria-label="Suggestions" className="sticky top-0 flex h-dvh flex-col gap-4 overflow-y-auto py-6 pl-2 scrollbar-none">
      <Link to="/search" className={cn(inputClasses, 'flex h-11 items-center gap-2 text-muted')}>
        <Search className="size-4" aria-hidden /> Search ConnectHub
      </Link>

      {signedIn && <SuggestedCircles />}

      {signedIn && <TodaysMoments />}

      <p className="px-1 text-xs leading-relaxed text-muted">
        No follower counts. No like numbers. Just circles, vibes and moments.
      </p>
    </aside>
  );
}
