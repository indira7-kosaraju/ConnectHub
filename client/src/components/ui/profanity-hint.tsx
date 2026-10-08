import { hasProfanity } from '@/lib/profanity';

/** Gentle heads-up shown under inputs when some words will be softened on save. */
export function ProfanityHint({ text, className }: { text: string; className?: string }) {
  if (!text || !hasProfanity(text)) return null;
  return (
    <p className={className ?? 'text-[13px] text-accent-ink'} role="status">
      Heads up: some words will be softened (✱✱✱) when you share this.
    </p>
  );
}
