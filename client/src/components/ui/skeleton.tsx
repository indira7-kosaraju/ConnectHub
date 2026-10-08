import type { HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

/** Warm shimmer placeholder. Compose these to mirror the final layout exactly. */
export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden
      className={cn(
        'relative overflow-hidden rounded-md bg-surface-2',
        'after:absolute after:inset-0 after:-translate-x-full after:animate-[shimmer_1.6s_infinite] after:bg-gradient-to-r after:from-transparent after:via-white/40 after:to-transparent dark:after:via-white/5',
        className,
      )}
      {...props}
    />
  );
}
