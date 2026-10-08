import type { ComponentType, ReactNode, SVGProps } from 'react';
import { cn } from '@/lib/utils';

interface EmptyStateProps {
  illustration: ComponentType<SVGProps<SVGSVGElement>>;
  title: string;
  description: ReactNode;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({ illustration: Illustration, title, description, action, className }: EmptyStateProps) {
  return (
    <div className={cn('flex flex-col items-center px-6 py-12 text-center', className)}>
      <Illustration className="mb-6 h-32 w-auto" />
      <h2 className="text-2xl font-semibold">{title}</h2>
      <p className="mt-2 max-w-sm text-[15px] leading-relaxed text-muted">{description}</p>
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}
