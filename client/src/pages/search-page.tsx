import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Clock, Search, X } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { CompassIllustration } from '@/components/ui/illustrations';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { CircleRow } from '@/components/feature/circles/circle-row';
import { PostCard } from '@/components/feature/posts/post-card';
import { useDebouncedValue } from '@/hooks/use-debounced-value';
import { searchApi, type SearchScope } from '@/lib/api/settings';
import { clearRecentSearches, forgetSearch, readRecentSearches, rememberSearch } from '@/lib/recent-searches';

const SCOPES: { value: SearchScope; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'people', label: 'People' },
  { value: 'circles', label: 'Circles' },
  { value: 'posts', label: 'Posts' },
];

function Section({ title, children, more }: { title: string; children: React.ReactNode; more?: () => void }) {
  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="font-sans text-sm font-medium uppercase tracking-[0.12em] text-muted">{title}</h2>
        {more && (
          <Button variant="link" size="sm" onClick={more}>
            See all
          </Button>
        )}
      </div>
      {children}
    </section>
  );
}

export function SearchPage() {
  const [params, setParams] = useSearchParams();
  const [text, setText] = useState(params.get('q') ?? '');
  const scope = (params.get('type') as SearchScope | null) ?? 'all';
  const q = useDebouncedValue(text.trim(), 300);
  const [recent, setRecent] = useState(readRecentSearches);
  const input = useRef<HTMLInputElement>(null);

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

  const results = useQuery({
    queryKey: ['search', q, scope],
    queryFn: ({ signal }) => searchApi.search(q, scope, signal),
    enabled: q.length >= 1,
    staleTime: 30_000,
  });

  // Remember a search once it actually produced results.
  useEffect(() => {
    const d = results.data;
    if (d && q && d.people.length + d.circles.length + d.posts.length > 0) setRecent(rememberSearch(q));
  }, [results.data, q]);

  const setScope = (s: SearchScope) =>
    setParams((prev) => {
      const next = new URLSearchParams(prev);
      if (s === 'all') next.delete('type');
      else next.set('type', s);
      return next;
    });

  const d = results.data;
  const empty = d && d.people.length + d.circles.length + d.posts.length === 0;

  return (
    <div className="space-y-6">
      <h1 className="sr-only">Search</h1>
      <div className="relative">
        <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-muted" aria-hidden />
        <Input
          ref={input}
          autoFocus
          type="search"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Search people, circles and posts"
          aria-label="Search"
          className="h-14 rounded-2xl pl-12 text-base"
        />
      </div>

      <Tabs value={scope} onValueChange={(v) => setScope(v as SearchScope)}>
        <TabsList>
          {SCOPES.map((s) => (
            <TabsTrigger key={s.value} value={s.value}>
              {s.label}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {!q ? (
        recent.length > 0 ? (
          <Section title="Recent">
            <ul className="space-y-1">
              {recent.map((r) => (
                <li key={r} className="flex items-center gap-2">
                  <button type="button" onClick={() => setText(r)} className="flex flex-1 items-center gap-3 rounded-xl p-2 text-left hover:bg-surface-2">
                    <Clock className="size-4 text-muted" aria-hidden /> {r}
                  </button>
                  <Button variant="ghost" size="icon-sm" aria-label={`Remove ${r} from recent searches`} onClick={() => setRecent(forgetSearch(r))}>
                    <X />
                  </Button>
                </li>
              ))}
            </ul>
            <Button variant="link" size="sm" onClick={() => setRecent(clearRecentSearches())}>
              Clear recent searches
            </Button>
          </Section>
        ) : (
          <p className="py-10 text-center text-muted">Find people from your circles, circles to join, and posts you remember.</p>
        )
      ) : results.isPending ? (
        <div className="space-y-4" aria-busy>
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="flex items-center gap-3">
              <Skeleton className="size-10 rounded-full" />
              <Skeleton className="h-4 w-48" />
            </div>
          ))}
        </div>
      ) : empty ? (
        <EmptyState illustration={CompassIllustration} title={`Nothing for “${q}”`} description="Try fewer words, or the start of a name." />
      ) : (
        d && (
          <div className="space-y-8" aria-live="polite">
            {d.people.length > 0 && (
              <Section title="People" more={scope === 'all' && d.people.length >= 6 ? () => setScope('people') : undefined}>
                <ul className="space-y-1">
                  {d.people.map((u) => (
                    <li key={u.id}>
                      <Link to={`/u/${u.username}`} className="flex items-center gap-3 rounded-xl p-2 hover:bg-surface-2">
                        <Avatar src={u.avatarUrl} name={u.displayName ?? u.username} />
                        <span>
                          <span className="block font-medium">{u.displayName ?? u.username}</span>
                          <span className="block text-sm text-muted">@{u.username}</span>
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </Section>
            )}
            {d.circles.length > 0 && (
              <Section title="Circles" more={scope === 'all' && d.circles.length >= 6 ? () => setScope('circles') : undefined}>
                <ul className="space-y-4">
                  {d.circles.map((c) => (
                    <li key={c.id}>
                      <CircleRow circle={c} />
                    </li>
                  ))}
                </ul>
              </Section>
            )}
            {d.posts.length > 0 && (
              <Section title="Posts" more={scope === 'all' && d.posts.length >= 6 ? () => setScope('posts') : undefined}>
                <div className="space-y-4">
                  {d.posts.map((p) => (
                    <PostCard key={p.id} post={p} />
                  ))}
                </div>
              </Section>
            )}
          </div>
        )
      )}
    </div>
  );
}
