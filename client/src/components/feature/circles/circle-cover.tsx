import { useMemo } from 'react';
import { cn } from '@/lib/utils';
import { cdn, cdnSrcSet } from '@/lib/images-cdn';

const PALETTES: [string, string, string][] = [
  ['var(--vibe-spark)', 'var(--accent)', 'var(--secondary)'],
  ['var(--vibe-deep)', 'var(--vibe-mindblown)', 'var(--vibe-spark)'],
  ['var(--secondary)', 'var(--vibe-spark)', 'var(--vibe-funny)'],
  ['var(--accent)', 'var(--vibe-deep)', 'var(--secondary)'],
  ['var(--vibe-mindblown)', 'var(--secondary)', 'var(--accent)'],
  ['var(--vibe-funny)', 'var(--vibe-spark)', 'var(--vibe-deep)'],
];

/** FNV-1a — stable, cheap hash for picking a pattern per circle. */
function hash(seed: string): number {
  let h = 0x811c9dc5;
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i);
    h = Math.imul(h, 0x01000193);
  }
  return h >>> 0;
}

/**
 * A circle's cover: the uploaded image, or a deterministic overlapping-circles pattern derived
 * from the seed (slug) so every circle has a distinct, on-brand cover from day one.
 */
export function CircleCover({
  seed,
  src,
  className,
  alt = '',
  priority = false,
}: {
  seed: string;
  src?: string | null;
  className?: string;
  alt?: string;
  /** Above-the-fold hero covers load eagerly at high priority (they're often the LCP element). */
  priority?: boolean;
}) {
  const art = useMemo(() => {
    const h = hash(seed);
    const palette = PALETTES[h % PALETTES.length]!;
    const r = (n: number, mod: number) => ((h >>> n) % mod);
    return {
      palette,
      a: { cx: 40 + r(3, 80), cy: 20 + r(7, 50), r: 46 + r(11, 30) },
      b: { cx: 150 + r(13, 80), cy: 40 + r(17, 50), r: 38 + r(19, 34) },
      c: { cx: 250 + r(5, 60), cy: 10 + r(23, 60), r: 30 + r(2, 30) },
    };
  }, [seed]);

  if (src) {
    return (
      <img
        src={cdn(src, 1200)}
        srcSet={cdnSrcSet(src, [480, 800, 1200, 1600])}
        sizes="(min-width: 768px) 620px, 100vw"
        alt={alt}
        loading={priority ? 'eager' : 'lazy'}
        fetchPriority={priority ? 'high' : 'auto'}
        decoding={priority ? 'sync' : 'async'}
        className={cn('size-full object-cover', className)}
      />
    );
  }
  const [p1, p2, p3] = art.palette;
  return (
    <svg viewBox="0 0 320 107" preserveAspectRatio="xMidYMid slice" className={cn('size-full bg-surface-2', className)} aria-hidden>
      <circle {...art.a} fill={p1} opacity="0.55" />
      <circle {...art.b} fill={p2} opacity="0.45" />
      <circle {...art.c} fill={p3} opacity="0.5" />
      <circle cx={art.b.cx} cy={art.b.cy} r={art.b.r * 0.35} fill="none" stroke="var(--surface)" strokeWidth="2" opacity="0.7" />
    </svg>
  );
}
