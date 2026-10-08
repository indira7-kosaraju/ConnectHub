import { Bell, Bookmark, Compass, Home, MessageCircle, Settings, type LucideIcon } from 'lucide-react';

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  /** Key used to look up an unread badge count. */
  badge?: 'messages' | 'notifications';
}

export const PRIMARY_NAV: NavItem[] = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/discover', label: 'Discover', icon: Compass },
  { to: '/messages', label: 'Messages', icon: MessageCircle, badge: 'messages' },
  { to: '/notifications', label: 'Notifications', icon: Bell, badge: 'notifications' },
  { to: '/saved', label: 'Saved', icon: Bookmark },
  { to: '/settings', label: 'Settings', icon: Settings },
];
