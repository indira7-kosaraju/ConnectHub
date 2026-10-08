import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { PageHeader } from '@/components/ui/page-header';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { CompassIllustration } from '@/components/ui/illustrations';
import { CircleCard, CircleCardSkeleton } from '@/components/feature/circles/circle-card';
import { CreateCircleDialog } from '@/components/feature/circles/create-circle-dialog';
import { useBrowseCircles, useTrendingCircles } from '@/hooks/use-circles';
import { useDebouncedValue } from '@/hooks/use-debounced-value';
import { interestsApi } from '@/lib/api/auth';
import { qk } from '@/lib/query-keys';
import { cn } from '@/lib/utils';

function Grid({ children, busy }: { children: React.ReactNode; busy?: boolean }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2" aria-busy={busy || undefined}>
      {children}
    </div>
  );
}

const skeletons = (n: number) => Array.from({ length: n }, (_, i) => <CircleCardSkeleton key={i} />);

export function DiscoverPage() {
  const [params, setParams] = useSearchParams();
  const category = params.get('category') ?? '';
  const [query, setQuery] = useState(params.get('q') ?? '');
  const q = useDebouncedValue(query.trim(), 350);

  // Keep the URL in sync so searches are shareable and survive back/forward.
  useEffect(() => {
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (q) next.set('q', q);
        else next.delete('q');
        return next;
      },
      { replace: true },
    );
  }, [q, setParams]);

  const setCategory = (slug: string) =>
    setParams((prev) => {
      const next = new URLSearchParams(prev);
      if (slug && slug !== category) next.set('category', slug);
      else next.delete('category');
      return next;
    });

  const filtering = !!q || !!category;
  const interests = useQuery({ queryKey: qk.interests, queryFn: interestsApi.list, staleTime: Infinity });
  const trending = useTrendingCircles(8);
  const results = useBrowseCircles({ q: q || undefined, category: category || undefined }, filtering);
  const items = results.data?.pages.flatMap((p) => p.items) ?? [];

  return (
    <div className="space-y-6">
      <PageHeader title="Discover" description="Small circles, big conversations. Find yours." actions={<CreateCircleDialog />} />

      <div className="relative">
        <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted" aria-hidden />
        <Input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search circles"
          aria-label="Search circles"
          className="pl-10"
        />
      </div>

      <div className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 scrollbar-none md:mx-0 md:flex-wrap md:px-0" role="group" aria-label="Categories">
        {interests.data?.map((i) => (
          <button
            key={i.id}
            type="button"
            aria-pressed={category === i.slug}
            onClick={() => setCategory(i.slug)}
            className={cn(
              'inline-flex h-9 shrink-0 items-center gap-1.5 rounded-full border px-3.5 text-sm transition-colors',
              category === i.slug ? 'border-ink bg-ink text-bg' : 'border-line-strong bg-surface hover:bg-surface-2',
            )}
          >
            <span aria-hidden>{i.emoji}</span>
            {i.name}
          </button>
        ))}
      </div>

      {filtering ? (
        <section aria-label="Results">
          {results.isPending ? (
            <Grid busy>{skeletons(4)}</Grid>
          ) : items.length === 0 ? (
            <EmptyState
              illustration={CompassIllustration}
              title="No circles match"
              description="Try a different word or category — or start the circle you wish existed."
              action={<CreateCircleDialog />}
            />
          ) : (
            <>
              <Grid>
                {items.map((c) => (
                  <CircleCard key={c.id} circle={c} />
                ))}
              </Grid>
              {results.hasNextPage && (
                <Button variant="outline" className="mt-6 w-full" onClick={() => void results.fetchNextPage()} loading={results.isFetchingNextPage}>
                  Show more circles
                </Button>
              )}
            </>
          )}
        </section>
      ) : (
        <section aria-labelledby="trending-heading">
          <h2 id="trending-heading" className="mb-4 text-xl font-semibold">
            Trending this week
          </h2>
          {trending.isPending ? (
            <Grid busy>{skeletons(4)}</Grid>
          ) : trending.data?.length ? (
            <Grid>
              {trending.data.map((c) => (
                <CircleCard key={c.id} circle={c} />
              ))}
            </Grid>
          ) : (
            <EmptyState
              illustration={CompassIllustration}
              title="You’ve joined them all"
              description="Nothing new is trending that you’re not already part of. Start a circle of your own?"
              action={<CreateCircleDialog />}
            />
          )}
        </section>
      )}
    </div>
  );
}
