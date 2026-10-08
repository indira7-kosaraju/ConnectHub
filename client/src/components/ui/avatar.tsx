import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import * as AvatarPrimitive from '@radix-ui/react-avatar';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn, initials } from '@/lib/utils';
import { cdn } from '@/lib/images-cdn';

const avatarVariants = cva('relative flex shrink-0 overflow-hidden rounded-full bg-surface-2', {
  variants: {
    size: { xs: 'size-6 text-[10px]', sm: 'size-8 text-xs', md: 'size-10 text-sm', lg: 'size-14 text-base', xl: 'size-24 text-2xl' },
  },
  defaultVariants: { size: 'md' },
});

interface UserAvatarProps
  extends ComponentPropsWithoutRef<typeof AvatarPrimitive.Root>,
    VariantProps<typeof avatarVariants> {
  src?: string | null;
  name?: string | null;
}

export const Avatar = forwardRef<ElementRef<typeof AvatarPrimitive.Root>, UserAvatarProps>(
  ({ className, size, src, name, ...props }, ref) => (
    <AvatarPrimitive.Root ref={ref} className={cn(avatarVariants({ size }), className)} {...props}>
      {src && <AvatarPrimitive.Image src={cdn(src, 192)} alt={name ?? ''} className="size-full object-cover" loading="lazy" />}
      <AvatarPrimitive.Fallback
        className="flex size-full items-center justify-center bg-secondary-soft font-medium text-secondary"
        delayMs={src ? 300 : 0}
        aria-label={name ?? undefined}
      >
        {initials(name)}
      </AvatarPrimitive.Fallback>
    </AvatarPrimitive.Root>
  ),
);
Avatar.displayName = 'Avatar';
