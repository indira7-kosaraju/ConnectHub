import { forwardRef, type ComponentPropsWithoutRef, type ElementRef } from 'react';
import * as Menu from '@radix-ui/react-dropdown-menu';
import { cn } from '@/lib/utils';

export const DropdownMenu = Menu.Root;
export const DropdownMenuTrigger = Menu.Trigger;
export const DropdownMenuGroup = Menu.Group;

export const DropdownMenuContent = forwardRef<ElementRef<typeof Menu.Content>, ComponentPropsWithoutRef<typeof Menu.Content>>(
  ({ className, sideOffset = 6, align = 'end', ...props }, ref) => (
    <Menu.Portal>
      <Menu.Content
        ref={ref}
        sideOffset={sideOffset}
        align={align}
        className={cn('z-50 min-w-44 rounded-xl border border-line bg-surface p-1 shadow-lift', className)}
        {...props}
      />
    </Menu.Portal>
  ),
);
DropdownMenuContent.displayName = 'DropdownMenuContent';

interface ItemProps extends ComponentPropsWithoutRef<typeof Menu.Item> {
  destructive?: boolean;
}

export const DropdownMenuItem = forwardRef<ElementRef<typeof Menu.Item>, ItemProps>(
  ({ className, destructive, ...props }, ref) => (
    <Menu.Item
      ref={ref}
      className={cn(
        'flex cursor-pointer select-none items-center gap-2 rounded-lg px-2.5 py-2 text-sm outline-none transition-colors data-[disabled]:pointer-events-none data-[highlighted]:bg-surface-2 data-[disabled]:opacity-50 [&_svg]:size-4',
        destructive && 'text-danger',
        className,
      )}
      {...props}
    />
  ),
);
DropdownMenuItem.displayName = 'DropdownMenuItem';

export const DropdownMenuSeparator = forwardRef<
  ElementRef<typeof Menu.Separator>,
  ComponentPropsWithoutRef<typeof Menu.Separator>
>(({ className, ...props }, ref) => <Menu.Separator ref={ref} className={cn('my-1 h-px bg-line', className)} {...props} />);
DropdownMenuSeparator.displayName = 'DropdownMenuSeparator';

export const DropdownMenuLabel = forwardRef<ElementRef<typeof Menu.Label>, ComponentPropsWithoutRef<typeof Menu.Label>>(
  ({ className, ...props }, ref) => (
    <Menu.Label ref={ref} className={cn('px-2.5 py-1.5 text-xs font-medium text-muted', className)} {...props} />
  ),
);
DropdownMenuLabel.displayName = 'DropdownMenuLabel';
