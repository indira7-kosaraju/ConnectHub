import { forwardRef, type SelectHTMLAttributes } from 'react';
import { ChevronDown } from 'lucide-react';
import { inputClasses } from './input';
import { cn } from '@/lib/utils';

/** Native select (best mobile + a11y behaviour) styled to match inputs. */
export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, children, ...props }, ref) => (
    <div className="relative">
      <select ref={ref} className={cn(inputClasses, 'h-11 appearance-none pr-10', className)} {...props}>
        {children}
      </select>
      <ChevronDown className="pointer-events-none absolute right-3.5 top-1/2 size-4 -translate-y-1/2 text-muted" aria-hidden />
    </div>
  ),
);
Select.displayName = 'Select';
