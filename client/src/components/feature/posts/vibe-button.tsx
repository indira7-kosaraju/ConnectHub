import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { VibeRing } from '@/components/ui/vibe-ring';
import type { ReactionType, VibeMix } from '@/lib/api/schema';
import { VIBES, VIBE_META } from '@/types/vibe';
import { useAuthStore } from '@/stores/auth-store';
import { cn } from '@/lib/utils';

interface VibeButtonProps {
  vibe: VibeMix;
  mine: ReactionType | null;
  onChange: (type: ReactionType | null) => void;
  size?: 'sm' | 'md';
  disabled?: boolean;
}

/**
 * The reaction control: a button wrapped in the post's vibe ring. Tapping opens a picker that fans
 * out the five vibes; choosing your current vibe again removes it.
 */
export function VibeButton({ vibe, mine, onChange, size = 'md', disabled }: VibeButtonProps) {
  const [open, setOpen] = useState(false);
  const signedIn = useAuthStore((s) => !!s.user);
  const navigate = useNavigate();
  const location = useLocation();
  const meta = mine ? VIBE_META[mine] : null;
  const dims = size === 'md' ? 'size-10' : 'size-8';
  const inner = size === 'md' ? 'size-[30px] text-[15px]' : 'size-6 text-[13px]';

  const choose = (type: ReactionType) => {
    setOpen(false);
    onChange(type === mine ? null : type);
  };

  const trigger = (
    <button
      type="button"
      disabled={disabled}
      aria-label={meta ? `Your vibe: ${meta.label}. Change or remove` : 'Add a vibe'}
      aria-haspopup="menu"
      onClick={signedIn ? undefined : () => navigate(`/login?next=${encodeURIComponent(location.pathname)}`)}
      className={cn('group relative grid shrink-0 place-items-center rounded-full transition-transform active:scale-95', dims)}
    >
      <VibeRing counts={vibe} thickness={size === 'md' ? 3 : 2.5} className={dims}>
        <motion.span
          key={mine ?? 'none'}
          initial={{ scale: 0.6, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: 'spring', stiffness: 500, damping: 20 }}
          className={cn('grid place-items-center rounded-full bg-surface-2 leading-none transition-colors group-hover:bg-line', inner)}
          style={meta ? { color: meta.color } : undefined}
          aria-hidden
        >
          {meta ? meta.glyph : <span className="text-muted">✦</span>}
        </motion.span>
      </VibeRing>
    </button>
  );

  if (!signedIn) return trigger;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>{trigger}</PopoverTrigger>
      <PopoverContent side="top" align="start" role="menu" aria-label="Choose a vibe" className="flex gap-1">
        {VIBES.map((v, i) => {
          const m = VIBE_META[v];
          return (
            <motion.button
              key={v}
              type="button"
              role="menuitemradio"
              aria-checked={mine === v}
              aria-label={m.label}
              title={m.label}
              onClick={() => choose(v)}
              initial={{ opacity: 0, y: 10, scale: 0.5, rotate: -20 }}
              animate={{ opacity: 1, y: 0, scale: 1, rotate: 0 }}
              transition={{ delay: i * 0.035, type: 'spring', stiffness: 520, damping: 22 }}
              whileHover={{ y: -3, scale: 1.12 }}
              whileTap={{ scale: 0.9 }}
              className={cn(
                'grid size-10 place-items-center rounded-full text-lg outline-none transition-colors hover:bg-surface-2 focus-visible:ring-2 focus-visible:ring-ring',
                mine === v && 'bg-surface-2 ring-2 ring-line-strong',
              )}
              style={{ color: m.color }}
            >
              {m.glyph}
            </motion.button>
          );
        })}
      </PopoverContent>
    </Popover>
  );
}
