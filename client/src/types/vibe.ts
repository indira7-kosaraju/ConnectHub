export const VIBES = ['spark', 'deep', 'funny', 'support', 'mindblown'] as const;
export type Vibe = (typeof VIBES)[number];

export type VibeCounts = Record<Vibe, number>;

export interface VibeMeta {
  label: string;
  glyph: string;
  /** CSS variable holding the vibe colour. */
  color: string;
}

export const VIBE_META: Record<Vibe, VibeMeta> = {
  spark: { label: 'Spark', glyph: '✦', color: 'var(--vibe-spark)' },
  deep: { label: 'Deep', glyph: '◉', color: 'var(--vibe-deep)' },
  funny: { label: 'Funny', glyph: '☺', color: 'var(--vibe-funny)' },
  support: { label: 'Support', glyph: '♥', color: 'var(--vibe-support)' },
  mindblown: { label: 'Mind-blown', glyph: '✶', color: 'var(--vibe-mindblown)' },
};

export const EMPTY_VIBES: VibeCounts = { spark: 0, deep: 0, funny: 0, support: 0, mindblown: 0 };
