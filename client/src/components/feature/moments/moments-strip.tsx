import { Plus } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { Skeleton } from '@/components/ui/skeleton';
import { useMomentStrip } from '@/hooks/use-moments';
import { useAuthStore } from '@/stores/auth-store';
import { useMomentViewer } from '@/stores/moment-viewer-store';
import { useUiStore } from '@/stores/ui-store';
import { MomentAvatar } from './moment-avatar';

/** Horizontal "Today" strip: your moment first, then people from your circles. */
export function MomentsStrip() {
  const strip = useMomentStrip();
  const me = useAuthStore((s) => s.user);
  const openViewer = useMomentViewer((s) => s.open);
  const openComposer = useUiStore((s) => s.setMomentComposerOpen);
  const groups = strip.data ?? [];
  const mineIndex = groups.findIndex((g) => g.mine);

  return (
    <section aria-labelledby="today-heading" className="mb-6">
      <h2 id="today-heading" className="mb-3 font-sans text-sm font-medium uppercase tracking-[0.12em] text-muted">
        Today
      </h2>
      <ul className="-mx-4 flex gap-4 overflow-x-auto px-4 pb-1 scrollbar-none md:mx-0 md:px-0">
        <li className="flex w-[72px] shrink-0 flex-col items-center gap-1.5">
          <div className="relative">
            {mineIndex >= 0 ? (
              <button type="button" onClick={() => openViewer(groups, mineIndex, { startMoment: 0 })} aria-label="View your moments" className="rounded-full">
                <MomentAvatar user={groups[mineIndex]!.user} unseen={false} />
              </button>
            ) : (
              <button
                type="button"
                onClick={() => openComposer(true)}
                aria-label="Share a moment"
                className="grid size-16 place-items-center rounded-full border-2 border-dashed border-line-strong p-1"
              >
                <Avatar src={me?.avatarUrl} name={me?.displayName ?? me?.username} className="size-full" />
              </button>
            )}
            <button
              type="button"
              onClick={() => openComposer(true)}
              aria-label="Add a moment"
              className="absolute -bottom-0.5 -right-0.5 grid size-6 place-items-center rounded-full border-2 border-bg bg-accent text-[#1c1917]"
            >
              <Plus className="size-3.5" strokeWidth={3} aria-hidden />
            </button>
          </div>
          <span className="w-full truncate text-center text-xs text-muted">You</span>
        </li>

        {strip.isPending
          ? Array.from({ length: 6 }, (_, i) => (
              <li key={i} className="flex w-[72px] shrink-0 flex-col items-center gap-1.5" aria-hidden>
                <Skeleton className="size-16 rounded-full" />
                <Skeleton className="h-2.5 w-12" />
              </li>
            ))
          : groups.map((g, i) =>
              g.mine ? null : (
                <li key={g.user.id} className="flex w-[72px] shrink-0 flex-col items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => openViewer(groups, i)}
                    className="rounded-full transition-transform active:scale-95"
                    aria-label={`${g.user.displayName ?? g.user.username}'s moments${g.allSeen ? '' : ', new'}`}
                  >
                    <MomentAvatar user={g.user} unseen={!g.allSeen} />
                  </button>
                  <span className={g.allSeen ? 'w-full truncate text-center text-xs text-muted' : 'w-full truncate text-center text-xs font-medium'}>
                    {g.user.displayName?.split(' ')[0] ?? g.user.username}
                  </span>
                </li>
              ),
            )}
      </ul>
    </section>
  );
}
