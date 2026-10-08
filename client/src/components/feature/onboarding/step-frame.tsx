import type { ReactNode } from 'react';

interface StepFrameProps {
  eyebrow: string;
  title: string;
  description: string;
  children: ReactNode;
  footer: ReactNode;
}

export function StepFrame({ eyebrow, title, description, children, footer }: StepFrameProps) {
  return (
    <section className="flex flex-1 flex-col" aria-labelledby="step-title">
      <p className="text-sm font-medium uppercase tracking-[0.12em] text-accent-ink">{eyebrow}</p>
      <h1 id="step-title" className="mt-2 text-[32px] font-semibold leading-tight sm:text-4xl">
        {title}
      </h1>
      <p className="mt-2 max-w-md text-[15px] leading-relaxed text-muted">{description}</p>
      <div className="mt-8 flex-1">{children}</div>
      <div className="sticky bottom-0 -mx-6 mt-8 flex items-center justify-between gap-3 border-t border-line bg-bg/90 px-6 py-4 pb-safe backdrop-blur sm:static sm:mx-0 sm:border-none sm:bg-transparent sm:px-0 sm:backdrop-blur-none">
        {footer}
      </div>
    </section>
  );
}
