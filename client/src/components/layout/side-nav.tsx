import { Link, NavLink } from 'react-router-dom';
import { ChevronsUpDown, Feather, LogOut, Settings, UserRound } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useLogout } from '@/hooks/use-auth';
import { useMyCircles } from '@/hooks/use-circles';
import { useUnreadMessages } from '@/hooks/use-messages';
import { useUnreadNotifications } from '@/hooks/use-notifications';
import { Badge } from '@/components/ui/badge';
import { CircleCover } from '@/components/feature/circles/circle-cover';
import { Logo, LogoMark } from '@/components/ui/logo';
import { Button } from '@/components/ui/button';
import { Avatar } from '@/components/ui/avatar';
import { Tooltip } from '@/components/ui/tooltip';
import { useAuthStore } from '@/stores/auth-store';
import { useUiStore } from '@/stores/ui-store';
import { cn } from '@/lib/utils';
import { PRIMARY_NAV } from './nav-items';

/** Desktop navigation: icon rail at md, full labels at lg+. */
export function SideNav() {
  const user = useAuthStore((s) => s.user);
  const openComposer = useUiStore((s) => s.openComposer);
  const logout = useLogout();
  const myCircles = useMyCircles();
  const unread = { messages: useUnreadMessages().data ?? 0, notifications: useUnreadNotifications().data ?? 0 };

  return (
    <nav aria-label="Primary" className="sticky top-0 flex h-dvh flex-col gap-1 py-6 pr-2">
      <Link to="/" className="mb-6 px-3" aria-label="ConnectHub home">
        <Logo className="hidden lg:inline-flex" />
        <LogoMark className="lg:hidden" />
      </Link>

      {PRIMARY_NAV.map(({ to, label, icon: Icon, badge }) => {
        const count = badge ? unread[badge] : 0;
        return (
        <Tooltip key={to} content={label} side="right">
          <NavLink
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              cn(
                'group flex items-center gap-4 rounded-full px-3 py-2.5 text-[15px] transition-colors hover:bg-surface-2',
                isActive ? 'font-semibold text-ink' : 'text-muted hover:text-ink',
              )
            }
          >
            {({ isActive }) => (
              <>
                <span className="relative">
                  <Icon className={cn('size-[22px]', isActive && 'text-accent-ink')} strokeWidth={isActive ? 2.25 : 1.75} aria-hidden />
                  {count > 0 && (
                    <Badge variant="count" className="absolute -right-2.5 -top-2 lg:hidden" aria-hidden>
                      {count > 99 ? '99+' : count}
                    </Badge>
                  )}
                </span>
                <span className="hidden lg:inline">{label}</span>
                {count > 0 && (
                  <Badge variant="count" className="ml-auto hidden lg:inline-flex">
                    {count > 99 ? '99+' : count}
                    <span className="sr-only"> unread</span>
                  </Badge>
                )}
              </>
            )}
          </NavLink>
        </Tooltip>
        );
      })}

      <Button variant="accent" size="lg" className="mt-4 hidden lg:inline-flex" onClick={() => openComposer()}>
        <Feather aria-hidden /> New post
      </Button>
      <Tooltip content="New post" side="right">
        <Button variant="accent" size="icon" className="mt-4 self-center lg:hidden" onClick={() => openComposer()} aria-label="New post">
          <Feather aria-hidden />
        </Button>
      </Tooltip>

      {!!myCircles.data?.length && (
        <section aria-labelledby="your-circles" className="mt-6 hidden min-h-0 flex-col lg:flex">
          <h2 id="your-circles" className="px-3 pb-2 font-sans text-xs font-medium uppercase tracking-[0.12em] text-muted">
            Your circles
          </h2>
          <ul className="min-h-0 space-y-0.5 overflow-y-auto scrollbar-none">
            {myCircles.data.slice(0, 8).map((c) => (
              <li key={c.id}>
                <NavLink
                  to={`/c/${c.slug}`}
                  className={({ isActive }) =>
                    cn('flex items-center gap-3 rounded-full px-3 py-1.5 text-sm transition-colors hover:bg-surface-2', isActive ? 'font-semibold' : 'text-muted hover:text-ink')
                  }
                >
                  <span className="size-6 shrink-0 overflow-hidden rounded-lg" aria-hidden>
                    <CircleCover seed={c.slug} src={c.coverUrl} />
                  </span>
                  <span className="truncate">{c.name}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </section>
      )}

      {user ? (
        <DropdownMenu>
          <DropdownMenuTrigger
            className="mt-auto flex items-center gap-3 rounded-full p-2 text-left transition-colors hover:bg-surface-2"
            aria-label="Account menu"
          >
            <Avatar size="sm" src={user.avatarUrl} name={user.displayName ?? user.username} />
            <span className="hidden min-w-0 flex-1 lg:block">
              <span className="block truncate text-sm font-medium">{user.displayName ?? user.username}</span>
              {user.username && <span className="block truncate text-xs text-muted">@{user.username}</span>}
            </span>
            <ChevronsUpDown className="hidden size-4 text-muted lg:block" aria-hidden />
          </DropdownMenuTrigger>
          <DropdownMenuContent side="top" align="start" className="w-56">
            {user.username && (
              <DropdownMenuItem asChild>
                <Link to={`/u/${user.username}`}>
                  <UserRound /> Your profile
                </Link>
              </DropdownMenuItem>
            )}
            <DropdownMenuItem asChild>
              <Link to="/settings">
                <Settings /> Settings
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => logout.mutate()}>
              <LogOut /> Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ) : (
        <Link to="/login" className="mt-auto flex items-center gap-3 rounded-full p-2 transition-colors hover:bg-surface-2">
          <span className="grid size-8 place-items-center rounded-full bg-surface-2">
            <UserRound className="size-4" aria-hidden />
          </span>
          <span className="hidden text-sm font-medium lg:block">Sign in</span>
        </Link>
      )}
    </nav>
  );
}
