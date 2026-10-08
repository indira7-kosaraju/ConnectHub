import { useMemo, type ReactNode } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { VIBES, VIBE_META, type VibeCounts } from '@/types/vibe';
import { cn } from '@/lib/utils';

/**
 * Builds a conic gradient whose arcs are proportional to each vibe's share. Colour stops are
 * placed at the centre of each arc so neighbouring vibes blend into each other smoothly.
 */
export function vibeGradient(counts: VibeCounts): string | null {
  const total = VIBES.reduce((sum, v) => sum + counts[v], 0);
  if (total === 0) return null;
  const present = VIBES.filter((v) => counts[v] > 0);
  if (present.length === 1) return VIBE_META[present[0]!].color;

  let cursor = 0;
  const stops = present.map((v) => {
    const sweep = (counts[v] / total) * 360;
    const mid = cursor + sweep / 2;
    cursor += sweep;
    return `${VIBE_META[v].color} ${mid.toFixed(1)}deg`;
  });
  // Close the loop: the colour at 0deg/360deg interpolates between the last and first vibe.
  const first = VIBE_META[present[0]!].color;
  return `conic-gradient(from -90deg, ${first} 0deg, ${stops.join(', ')}, ${first} 360deg)`;
}

interface VibeRingProps {
  counts: VibeCounts;
  /** Ring thickness in px. */
  thickness?: number;
  className?: string;
  children?: ReactNode;
}

/** A soft colour ring around its children, coloured by the post's reaction mix — never a number. */
export function VibeRing({ counts, thickness = 2.5, className, children }: VibeRingProps) {
  const gradient = useMemo(() => vibeGradient(counts), [counts]);
  const mask = `radial-gradient(farthest-side, transparent calc(100% - ${thickness}px), #000 calc(100% - ${thickness - 0.5}px))`;

  return (
    <span className={cn('relative inline-grid place-items-center rounded-full', className)}>
      <span aria-hidden className="absolute inset-0 rounded-full border border-dashed border-line-strong" hidden={!!gradient} />
      <AnimatePresence initial={false}>
        {gradient && (
          <motion.span
            key={gradient}
            aria-hidden
            className="absolute inset-0 rounded-full"
            // Dynamic per-post value: the only way to express a data-driven gradient.
            style={{ background: gradient, WebkitMask: mask, mask }}
            initial={{ opacity: 0, rotate: -40, scale: 0.9 }}
            animate={{ opacity: 1, rotate: 0, scale: 1 }}
            exit={{ opacity: 0 }}
            transition={{ type: 'spring', stiffness: 220, damping: 24 }}
          />
        )}
      </AnimatePresence>
      {children}
    </span>
  );
}
