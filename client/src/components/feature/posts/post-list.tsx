import { useEffect, useRef, type ReactNode } from 'react';
import type { InfiniteData, UseInfiniteQueryResult } from '@tanstack/react-query';
import { AnimatePresence, motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { FeedSkeleton } from '@/components/feature/post-card-skeleton';
import type { CursorPage } from '@/lib/api/types';
import type { PostDto } from '@/lib/api/schema';
import { flattenUnique } from '@/lib/post-cache';
import { PostCard } from './post-card';

interface PostListProps {
  query: UseInfiniteQueryResult<InfiniteData<CursorPage<PostDto>>>;
  empty: ReactNode;
  hideCircle?: boolean;
  /** Post ids to leave out (e.g. the pinned post shown separately). */
  exclude?: string | null;
}

/** Infinite list: loads the next page when the sentinel scrolls into view; items slide in. */
export function PostList({ query, empty, hideCircle, exclude }: PostListProps) {
  const sentinel = useRef<HTMLDivElement>(null);
  const { hasNextPage, isFetchingNextPage, fetchNextPage } = query;

  useEffect(() => {
    const el = sentinel.current;
    if (!el || !hasNextPage) return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && !isFetchingNextPage) void fetchNextPage();
      },
      { rootMargin: '600px 0px' },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  if (query.isPending) return <FeedSkeleton count={3} />;
  if (query.isError) {
    return (
      <div className="rounded-[var(--radius-card)] border border-line bg-surface p-6 text-center">
        <p className="text-muted">We couldn’t load these posts.</p>
        <Button variant="outline" className="mt-3" onClick={() => void query.refetch()}>
          Try again
        </Button>
      </div>
    );
  }

  const posts = flattenUnique(query.data).filter((p) => p.id !== exclude);
  if (posts.length === 0) return <>{empty}</>;

  return (
    <div className="space-y-4">
      <AnimatePresence initial={false}>
        {posts.map((post, i) => (
          <motion.div
            key={post.id}
            layout="position"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, height: 0, marginTop: 0 }}
            transition={{ duration: 0.25, delay: Math.min(i % 10, 5) * 0.03 }}
          >
            <PostCard post={post} hideCircle={hideCircle} />
          </motion.div>
        ))}
      </AnimatePresence>
      <div ref={sentinel} aria-hidden />
      {isFetchingNextPage && (
        <div className="flex justify-center py-4">
          <Spinner label="Loading more posts" />
        </div>
      )}
      {!hasNextPage && posts.length > 4 && <p className="py-6 text-center text-sm text-muted">You’re all caught up ✦</p>}
    </div>
  );
}
