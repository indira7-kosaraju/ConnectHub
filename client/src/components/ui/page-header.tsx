import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

export function PageHeader({ title, description, actions, className }: { title: string; description?: ReactNode; actions?: ReactNode; className?: string }) {
  return (
    <header className={cn('flex items-end justify-between gap-4 pb-5', className)}>
      <div>
        <h1 className="text-[28px] font-semibold leading-tight sm:text-[32px]">{title}</h1>
        {description && <p className="mt-1 text-[15px] text-muted">{description}</p>}
      </div>
      {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </header>
  );
}
