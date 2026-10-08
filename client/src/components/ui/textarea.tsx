import { forwardRef, type TextareaHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';
import { inputClasses } from './input';

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => (
    <textarea ref={ref} className={cn(inputClasses, 'min-h-24 resize-none py-3 leading-relaxed', className)} {...props} />
  ),
);
Textarea.displayName = 'Textarea';
