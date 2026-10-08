import { NavLink } from 'react-router-dom';
import { Compass, Home, MessageCircle, Plus, UserRound } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useUiStore } from '@/stores/ui-store';
import { cn } from '@/lib/utils';
import { useUnreadMessages } from '@/hooks/use-messages';

/** Mobile tab bar: Home, Discover, +, Messages, Profile. */
export function BottomTabBar() {
  const username = useAuthStore((s) => s.user?.username);
  const openComposer = useUiStore((s) => s.openComposer);
  const unreadMessages = useUnreadMessages().data ?? 0;

  const tab = (to: string, label: string, Icon: typeof Home, end = false, count = 0) => (
    <NavLink
      to={to}
      end={end}
      aria-label={label}
      className={({ isActive }) =>
        cn('flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] transition-colors', isActive ? 'text-ink' : 'text-muted')
      }
    >
      {({ isActive }) => (
        <>
          <span className="relative">
            <Icon className={cn('size-6', isActive && 'text-accent-ink')} strokeWidth={isActive ? 2.25 : 1.75} aria-hidden />
            {count > 0 && <span className="absolute -right-1 -top-0.5 size-2.5 rounded-full border-2 border-surface bg-accent" aria-label={`${count} unread`} />}
          </span>
          <span>{label}</span>
        </>
      )}
    </NavLink>
  );

  return (
    <nav
      aria-label="Primary"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-surface/90 pb-safe backdrop-blur-lg md:hidden"
    >
      <div className="mx-auto flex max-w-md items-center px-2">
        {tab('/', 'Home', Home, true)}
        {tab('/discover', 'Discover', Compass)}
        <div className="flex flex-1 justify-center">
          <button
            type="button"
            onClick={() => openComposer()}
            aria-label="New post"
            className="grid size-12 -translate-y-1 place-items-center rounded-full bg-accent text-[#1c1917] shadow-lift transition-transform active:scale-95"
          >
            <Plus className="size-6" strokeWidth={2.25} aria-hidden />
          </button>
        </div>
        {tab('/messages', 'Messages', MessageCircle, false, unreadMessages)}
        {tab(username ? `/u/${username}` : '/login', 'Profile', UserRound)}
      </div>
    </nav>
  );
}
