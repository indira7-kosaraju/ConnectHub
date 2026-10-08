import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

/** Mirrors PostCard: author row, 3 text lines, optional media, action row. */
export function PostCardSkeleton({ withMedia = false }: { withMedia?: boolean }) {
  return (
    <Card className="p-5" aria-hidden>
      <div className="flex items-center gap-3">
        <Skeleton className="size-10 rounded-full" />
        <div className="space-y-1.5">
          <Skeleton className="h-3.5 w-32" />
          <Skeleton className="h-3 w-24" />
        </div>
      </div>
      <div className="mt-4 space-y-2">
        <Skeleton className="h-3.5 w-full" />
        <Skeleton className="h-3.5 w-[92%]" />
        <Skeleton className="h-3.5 w-[60%]" />
      </div>
      {withMedia && <Skeleton className="mt-4 aspect-[4/3] w-full rounded-xl" />}
      <div className="mt-5 flex items-center gap-4">
        <Skeleton className="size-9 rounded-full" />
        <Skeleton className="h-3.5 w-10" />
        <Skeleton className="ml-auto size-8 rounded-full" />
      </div>
    </Card>
  );
}

export function FeedSkeleton({ count = 3 }: { count?: number }) {
  return (
    <div className="space-y-4" role="status" aria-label="Loading posts">
      {Array.from({ length: count }, (_, i) => (
        <PostCardSkeleton key={i} withMedia={i % 2 === 1} />
      ))}
    </div>
  );
}
