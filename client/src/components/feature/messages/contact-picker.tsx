import { useState } from 'react';
import { Search } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { useContacts } from '@/hooks/use-messages';
import { useDebouncedValue } from '@/hooks/use-debounced-value';
import type { UserSummary } from '@/lib/api/schema';

/** Search people who share a circle with you. */
export function ContactPicker({ onPick, busyId }: { onPick: (u: UserSummary) => void; busyId?: string | null }) {
  const [q, setQ] = useState('');
  const debounced = useDebouncedValue(q.trim(), 250);
  const contacts = useContacts(debounced);

  return (
    <div className="space-y-3">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-muted" aria-hidden />
        <Input autoFocus type="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search people in your circles" aria-label="Search people" className="pl-10" />
      </div>
      <ul className="max-h-80 space-y-1 overflow-y-auto" aria-busy={contacts.isFetching || undefined}>
        {contacts.isPending
          ? Array.from({ length: 4 }, (_, i) => (
              <li key={i} className="flex items-center gap-3 p-2">
                <Skeleton className="size-10 rounded-full" />
                <Skeleton className="h-3.5 w-32" />
              </li>
            ))
          : contacts.data?.map((u) => (
              <li key={u.id}>
                <button
                  type="button"
                  disabled={busyId === u.id}
                  onClick={() => onPick(u)}
                  className="flex w-full items-center gap-3 rounded-xl p-2 text-left transition-colors hover:bg-surface-2 disabled:opacity-60"
                >
                  <Avatar src={u.avatarUrl} name={u.displayName ?? u.username} />
                  <span className="min-w-0">
                    <span className="block truncate font-medium">{u.displayName ?? u.username}</span>
                    <span className="block truncate text-sm text-muted">@{u.username}</span>
                  </span>
                </button>
              </li>
            ))}
        {contacts.isSuccess && contacts.data.length === 0 && (
          <li className="p-4 text-center text-sm text-muted">{debounced ? 'No one by that name in your circles.' : 'Join a circle to start conversations.'}</li>
        )}
      </ul>
    </div>
  );
}
