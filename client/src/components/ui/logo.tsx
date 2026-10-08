import { cn } from '@/lib/utils';

interface LogoProps {
  className?: string;
  /** For dark/coloured backgrounds: the centre dot follows the text colour. */
  inverse?: boolean;
}

export function LogoMark({ className, inverse = false }: LogoProps) {
  return (
    <svg viewBox="0 0 32 32" className={cn('size-8', className)} aria-hidden>
      <circle cx="16" cy="16" r="10.5" fill="none" stroke="var(--accent)" strokeWidth="3" />
      <circle cx="16" cy="16" r="4" fill={inverse ? 'currentColor' : 'var(--secondary)'} />
    </svg>
  );
}

export function Logo({ className, inverse = false }: LogoProps) {
  return (
    <span className={cn('inline-flex items-center gap-2', className)}>
      <LogoMark inverse={inverse} />
      <span className="font-serif text-[22px] font-semibold tracking-tight">ConnectHub</span>
    </span>
  );
}
