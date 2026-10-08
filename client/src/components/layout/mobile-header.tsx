import { Link } from 'react-router-dom';
import { Bell, Search } from 'lucide-react';
import { Logo } from '@/components/ui/logo';
import { Button } from '@/components/ui/button';
import { useUnreadNotifications } from '@/hooks/use-notifications';

export function MobileHeader() {
  const unread = useUnreadNotifications().data ?? 0;
  return (
    <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-line bg-bg/85 px-4 backdrop-blur-lg md:hidden">
      <Link to="/" aria-label="ConnectHub home">
        <Logo />
      </Link>
      <div className="flex items-center gap-1">
        <Button asChild variant="ghost" size="icon" aria-label="Search">
          <Link to="/search">
            <Search />
          </Link>
        </Button>
        <Button asChild variant="ghost" size="icon" aria-label={unread ? `Notifications, ${unread} unread` : 'Notifications'}>
          <Link to="/notifications" className="relative">
            <Bell />
            {unread > 0 && <span className="absolute right-2 top-2 size-2.5 rounded-full border-2 border-bg bg-accent" aria-hidden />}
          </Link>
        </Button>
      </div>
    </header>
  );
}
