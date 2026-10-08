import type { SVGProps } from 'react';

/**
 * Abstract, warm line illustrations for empty and error states. They draw with design tokens
 * so they adapt to light and dark themes automatically.
 */
type IllustrationProps = SVGProps<SVGSVGElement>;

const base = { viewBox: '0 0 200 140', fill: 'none', 'aria-hidden': true } as const;

export function CirclesIllustration(props: IllustrationProps) {
  return (
    <svg {...base} {...props}>
      <ellipse cx="100" cy="122" rx="70" ry="8" fill="var(--surface-2)" />
      <circle cx="78" cy="66" r="38" fill="var(--accent-soft)" stroke="var(--accent)" strokeWidth="2" />
      <circle cx="122" cy="66" r="38" fill="var(--secondary-soft)" stroke="var(--secondary)" strokeWidth="2" opacity="0.9" />
      <circle cx="100" cy="66" r="9" fill="var(--vibe-spark)" />
      <circle cx="62" cy="54" r="5" fill="var(--accent)" />
      <circle cx="140" cy="80" r="5" fill="var(--secondary)" />
      <path d="M100 20v-8M128 28l5-6M72 28l-5-6" stroke="var(--vibe-spark)" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function QuietIllustration(props: IllustrationProps) {
  return (
    <svg {...base} {...props}>
      <path d="M20 104h160" stroke="var(--border-strong)" strokeWidth="2" strokeLinecap="round" />
      <path d="M58 104a42 42 0 0 1 84 0" fill="var(--accent-soft)" stroke="var(--accent)" strokeWidth="2" />
      <path d="M100 46v-14M70 56l-9-10M130 56l9-10M52 82H38M148 82h14" stroke="var(--vibe-spark)" strokeWidth="2" strokeLinecap="round" />
      <path d="M44 118h34M96 118h60" stroke="var(--border)" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function MessagesIllustration(props: IllustrationProps) {
  return (
    <svg {...base} {...props}>
      <ellipse cx="100" cy="124" rx="64" ry="7" fill="var(--surface-2)" />
      <rect x="34" y="24" width="88" height="56" rx="20" fill="var(--secondary-soft)" stroke="var(--secondary)" strokeWidth="2" />
      <path d="M54 80l-6 16 20-16" fill="var(--secondary-soft)" stroke="var(--secondary)" strokeWidth="2" strokeLinejoin="round" />
      <rect x="82" y="56" width="84" height="48" rx="18" fill="var(--accent-soft)" stroke="var(--accent)" strokeWidth="2" />
      <path d="M146 104l6 12-18-12" fill="var(--accent-soft)" stroke="var(--accent)" strokeWidth="2" strokeLinejoin="round" />
      <circle cx="108" cy="80" r="4" fill="var(--accent)" />
      <circle cx="124" cy="80" r="4" fill="var(--accent)" opacity="0.7" />
      <circle cx="140" cy="80" r="4" fill="var(--accent)" opacity="0.4" />
    </svg>
  );
}

export function MomentsIllustration(props: IllustrationProps) {
  return (
    <svg {...base} {...props}>
      <circle cx="100" cy="68" r="44" stroke="var(--border-strong)" strokeWidth="2" strokeDasharray="4 6" />
      <circle cx="100" cy="68" r="32" fill="var(--surface-2)" stroke="var(--vibe-deep)" strokeWidth="3" />
      <path d="M100 50v18l12 8" stroke="var(--vibe-deep)" strokeWidth="3" strokeLinecap="round" />
      <circle cx="148" cy="30" r="6" fill="var(--vibe-spark)" />
      <circle cx="50" cy="104" r="5" fill="var(--vibe-mindblown)" />
    </svg>
  );
}

export function BellIllustration(props: IllustrationProps) {
  return (
    <svg {...base} {...props}>
      <ellipse cx="100" cy="124" rx="56" ry="7" fill="var(--surface-2)" />
      <path d="M70 92V66a30 30 0 0 1 60 0v26l10 12H60l10-12z" fill="var(--secondary-soft)" stroke="var(--secondary)" strokeWidth="2" strokeLinejoin="round" />
      <path d="M90 108a10 10 0 0 0 20 0" stroke="var(--secondary)" strokeWidth="2" />
      <path d="M150 40c6 4 9 10 9 18M50 40c-6 4-9 10-9 18" stroke="var(--vibe-spark)" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function BookmarkIllustration(props: IllustrationProps) {
  return (
    <svg {...base} {...props}>
      <ellipse cx="100" cy="124" rx="56" ry="7" fill="var(--surface-2)" />
      <path d="M72 20h56a6 6 0 0 1 6 6v88l-34-22-34 22V26a6 6 0 0 1 6-6z" fill="var(--accent-soft)" stroke="var(--accent)" strokeWidth="2" strokeLinejoin="round" />
      <path d="M100 42l5 10 11 2-8 8 2 11-10-5-10 5 2-11-8-8 11-2z" fill="var(--vibe-spark)" />
    </svg>
  );
}

export function CompassIllustration(props: IllustrationProps) {
  return (
    <svg {...base} {...props}>
      <ellipse cx="100" cy="126" rx="60" ry="7" fill="var(--surface-2)" />
      <circle cx="100" cy="66" r="48" fill="var(--surface)" stroke="var(--border-strong)" strokeWidth="2" />
      <circle cx="100" cy="66" r="38" stroke="var(--border)" strokeWidth="2" strokeDasharray="2 6" />
      <path d="M116 46l-10 26-22 14 10-26z" fill="var(--accent)" />
      <path d="M94 60l22-14-10 26z" fill="var(--secondary)" />
      <circle cx="100" cy="66" r="4" fill="var(--surface)" stroke="var(--text)" strokeWidth="2" />
    </svg>
  );
}

export function CrackedIllustration(props: IllustrationProps) {
  return (
    <svg {...base} {...props}>
      <ellipse cx="100" cy="126" rx="60" ry="7" fill="var(--surface-2)" />
      <path d="M100 20a46 46 0 1 0 0 92" fill="var(--accent-soft)" stroke="var(--accent)" strokeWidth="2" />
      <path d="M108 20a46 46 0 1 1 0 92" fill="var(--secondary-soft)" stroke="var(--secondary)" strokeWidth="2" />
      <path d="M104 18l-8 22 12 14-10 18 10 18-6 22" stroke="var(--text)" strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}
