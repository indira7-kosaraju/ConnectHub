import { cn } from '@/lib/utils';

/** Remaining-characters counter; turns accent near the limit. Pair with aria-describedby. */
export function CharCount({ id, value, max }: { id?: string; value: string | undefined; max: number }) {
  const remaining = max - (value?.length ?? 0);
  return (
    <span id={id} className={cn('text-xs tabular-nums', remaining < 20 ? 'text-accent-ink' : 'text-muted')} aria-live="polite">
      {remaining}
    </span>
  );
}
