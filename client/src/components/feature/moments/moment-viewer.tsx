import { useCallback, useEffect, useRef, useState } from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { Eye, Pause, Play, Trash2, X } from 'lucide-react';
import { Avatar } from '@/components/ui/avatar';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useDeleteMoment, useMarkMomentSeen } from '@/hooks/use-moments';
import { shortTime } from '@/lib/time';
import { useMomentViewer } from '@/stores/moment-viewer-store';
import { cn } from '@/lib/utils';
import { MomentContent } from './moment-content';

const DURATION_MS = { text: 5000, image: 6000 } as const;
const SWIPE_PX = 60;

/**
 * Full-screen moments player: progress bars, tap left/right to go back/forward, swipe to change
 * person, hold to pause, swipe down or Esc to close. Arrow keys and Space work too.
 */
export function MomentViewer() {
  const { groups, startGroup, startMoment, archive, close } = useMomentViewer();
  const open = !!groups;
  return (
    <DialogPrimitive.Root open={open} onOpenChange={(o) => !o && close()}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-[60] bg-[#0c0b0a]" />
        <DialogPrimitive.Content className="fixed inset-0 z-[60] flex items-center justify-center outline-none" aria-describedby={undefined}>
          {groups && <Player key={`${startGroup}-${startMoment}`} startGroup={startGroup} startMoment={startMoment} archive={archive} />}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}

function Player({ startGroup, startMoment, archive }: { startGroup: number; startMoment: number; archive: boolean }) {
  const groups = useMomentViewer((s) => s.groups)!;
  const close = useMomentViewer((s) => s.close);
  const markSeen = useMarkMomentSeen();
  const remove = useDeleteMoment();
  const reduceMotion = useReducedMotion();

  const [gi, setGi] = useState(startGroup);
  const [mi, setMi] = useState(startMoment);
  const [direction, setDirection] = useState(1);
  const [progress, setProgress] = useState(0);
  const [paused, setPaused] = useState(false);
  const [held, setHeld] = useState(false);
  const [ready, setReady] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const seenSent = useRef(new Set<string>());
  const pointer = useRef<{ x: number; y: number; t: number } | null>(null);

  const group = groups[gi]!;
  const moment = group.moments[mi]!;
  const name = group.user.displayName ?? group.user.username;
  const running = ready && !paused && !held && !confirmDelete;

  const next = useCallback(() => {
    setProgress(0);
    setReady(false);
    if (mi < group.moments.length - 1) {
      setMi(mi + 1);
    } else if (gi < groups.length - 1) {
      setDirection(1);
      setGi(gi + 1);
      setMi(0);
    } else {
      close();
    }
  }, [mi, gi, group.moments.length, groups.length, close]);

  const prev = useCallback(() => {
    setProgress(0);
    setReady(false);
    if (mi > 0) {
      setMi(mi - 1);
    } else if (gi > 0) {
      setDirection(-1);
      setGi(gi - 1);
      setMi(groups[gi - 1]!.moments.length - 1);
    } else {
      setReady(true); // restart the first moment
    }
  }, [mi, gi, groups]);

  const jumpGroup = useCallback(
    (delta: 1 | -1) => {
      const target = gi + delta;
      if (target < 0) return;
      if (target >= groups.length) return close();
      setDirection(delta);
      setProgress(0);
      setReady(false);
      setGi(target);
      setMi(0);
    },
    [gi, groups.length, close],
  );

  // Text moments are ready immediately; photos once loaded.
  useEffect(() => {
    if (moment.kind === 'text') setReady(true);
  }, [moment]);

  // Mark seen (others' live moments only).
  useEffect(() => {
    if (archive || group.mine || moment.seen || seenSent.current.has(moment.id)) return;
    seenSent.current.add(moment.id);
    markSeen.mutate(moment.id);
  }, [archive, group.mine, moment, markSeen]);

  // Progress clock.
  useEffect(() => {
    if (!running) return;
    const duration = DURATION_MS[moment.kind];
    let last = performance.now();
    let raf = 0;
    const tick = (now: number) => {
      const dt = now - last;
      last = now;
      setProgress((p) => {
        const np = p + dt / duration;
        if (np >= 1) {
          queueMicrotask(next);
          return 1;
        }
        return np;
      });
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [running, moment, next]);

  // Pause while the tab is hidden.
  useEffect(() => {
    const onVis = () => setHeld(document.hidden);
    document.addEventListener('visibilitychange', onVis);
    return () => document.removeEventListener('visibilitychange', onVis);
  }, []);

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowRight') next();
    else if (e.key === 'ArrowLeft') prev();
    else if (e.key === ' ') {
      e.preventDefault();
      setPaused((p) => !p);
    }
  };

  const onPointerDown = (e: React.PointerEvent) => {
    pointer.current = { x: e.clientX, y: e.clientY, t: performance.now() };
    setHeld(true);
  };
  const onPointerUp = (e: React.PointerEvent) => {
    setHeld(false);
    const start = pointer.current;
    pointer.current = null;
    if (!start) return;
    const dx = e.clientX - start.x;
    const dy = e.clientY - start.y;
    if (dy > 100 && Math.abs(dy) > Math.abs(dx)) return close();
    if (Math.abs(dx) > SWIPE_PX && Math.abs(dx) > Math.abs(dy)) return jumpGroup(dx < 0 ? 1 : -1);
    if (performance.now() - start.t > 400) return; // long press = pause only
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    if (e.clientX - rect.left < rect.width / 3) prev();
    else next();
  };

  return (
    <div className="relative h-dvh w-full max-w-[min(100vw,calc(100dvh*9/16))] overflow-hidden sm:h-[min(92dvh,860px)] sm:rounded-2xl" onKeyDown={onKeyDown}>
      <DialogPrimitive.Title className="sr-only">{`${name}'s moments`}</DialogPrimitive.Title>
      <AnimatePresence initial={false} custom={direction} mode="popLayout">
        <motion.div
          key={`${gi}-${mi}`}
          custom={direction}
          initial={reduceMotion ? { opacity: 0 } : { opacity: 0, x: direction * 40 }}
          animate={{ opacity: 1, x: 0 }}
          exit={reduceMotion ? { opacity: 0 } : { opacity: 0, x: direction * -40 }}
          transition={{ duration: 0.2 }}
          className="absolute inset-0"
        >
          <MomentContent moment={moment} onLoad={() => setReady(true)} />
        </motion.div>
      </AnimatePresence>

      {/* Tap / swipe surface */}
      <div
        className="absolute inset-0 touch-none select-none"
        onPointerDown={onPointerDown}
        onPointerUp={onPointerUp}
        onPointerCancel={() => setHeld(false)}
        aria-hidden
      />

      {/* Top chrome */}
      <div className="pointer-events-none absolute inset-x-0 top-0 bg-gradient-to-b from-[#0c0b0a]/70 to-transparent p-3 pt-[max(0.75rem,env(safe-area-inset-top))]">
        <div className="flex gap-1" role="progressbar" aria-label="Moment progress" aria-valuemin={1} aria-valuemax={group.moments.length} aria-valuenow={mi + 1}>
          {group.moments.map((m, i) => (
            <span key={m.id} className="h-[3px] flex-1 overflow-hidden rounded-full bg-white/35">
              <span
                className="block h-full origin-left rounded-full bg-white"
                style={{ transform: `scaleX(${i < mi ? 1 : i === mi ? progress : 0})` }}
              />
            </span>
          ))}
        </div>
        <div className="pointer-events-auto mt-3 flex items-center gap-2.5 text-white">
          <Avatar size="sm" src={group.user.avatarUrl} name={name} />
          <span className="text-sm font-semibold">{group.mine ? 'You' : name}</span>
          <time className="text-sm text-white/70" dateTime={moment.createdAt}>
            {shortTime(moment.createdAt)}
          </time>
          <span className="ml-auto flex gap-1">
            <button type="button" onClick={() => setPaused((p) => !p)} className="grid size-9 place-items-center rounded-full hover:bg-white/15" aria-label={paused ? 'Play' : 'Pause'}>
              {paused ? <Play className="size-4" /> : <Pause className="size-4" />}
            </button>
            <DialogPrimitive.Close className="grid size-9 place-items-center rounded-full hover:bg-white/15" aria-label="Close">
              <X className="size-5" />
            </DialogPrimitive.Close>
          </span>
        </div>
      </div>

      {/* Author footer: views (only you see this) + delete */}
      {group.mine && (
        <div className="absolute inset-x-0 bottom-0 flex items-center justify-between bg-gradient-to-t from-[#0c0b0a]/70 to-transparent p-4 pb-[max(1rem,env(safe-area-inset-bottom))] text-white">
          <span className="inline-flex items-center gap-2 text-sm" aria-live="polite">
            <Eye className="size-4" aria-hidden /> {moment.viewCount ?? 0} {moment.viewCount === 1 ? 'view' : 'views'}
            <span className="text-white/60">· only you can see this</span>
          </span>
          <button type="button" onClick={() => setConfirmDelete(true)} className="grid size-9 place-items-center rounded-full hover:bg-white/15" aria-label="Delete this moment">
            <Trash2 className="size-4" />
          </button>
        </div>
      )}

      {/* Keyboard-accessible prev/next (visually hidden until focused) */}
      <button type="button" onClick={prev} className={cn('sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-1/2 focus:rounded-full focus:bg-white focus:px-3 focus:py-1 focus:text-sm focus:text-[#1c1917]')}>
        Previous
      </button>
      <button type="button" onClick={next} className="sr-only focus:not-sr-only focus:absolute focus:right-3 focus:top-1/2 focus:rounded-full focus:bg-white focus:px-3 focus:py-1 focus:text-sm focus:text-[#1c1917]">
        Next
      </button>

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Delete this moment?"
        description="It disappears for everyone and won’t appear in your Rewind."
        confirmLabel="Delete"
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(moment.id, { onSuccess: () => close() })}
      />
    </div>
  );
}
