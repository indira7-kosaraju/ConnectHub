import { Avatar } from '@/components/ui/avatar';
import type { UserSummary } from '@/lib/api/schema';
import { cn } from '@/lib/utils';

/** Avatar wrapped in a ring: vivid gradient for unseen moments, quiet line once seen. */
export function MomentAvatar({ user, unseen, size = 'lg', className }: { user: UserSummary; unseen: boolean; size?: 'md' | 'lg'; className?: string }) {
  const dims = size === 'lg' ? 'size-16' : 'size-12';
  return (
    <span className={cn('grid place-items-center rounded-full p-[3px]', unseen ? 'moment-ring-unseen' : 'bg-line-strong', dims, className)}>
      <span className="grid size-full place-items-center rounded-full bg-bg p-[2px]">
        <Avatar src={user.avatarUrl} name={user.displayName ?? user.username} className="size-full" />
      </span>
    </span>
  );
}
