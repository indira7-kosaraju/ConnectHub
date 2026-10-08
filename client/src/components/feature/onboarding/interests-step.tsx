import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from '@/components/ui/toaster';
import { interestsApi, meApi } from '@/lib/api/auth';
import { ApiRequestError } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth-store';
import { qk } from '@/lib/query-keys';
import { cn } from '@/lib/utils';
import { StepFrame } from './step-frame';

const MIN = 3;
const SKELETON_WIDTHS = ['w-24', 'w-32', 'w-28', 'w-36', 'w-24', 'w-30', 'w-28', 'w-32', 'w-24', 'w-36', 'w-28', 'w-24', 'w-32', 'w-28'];
const MAX = 10;

export function InterestsStep({ onBack, onNext }: { onBack: () => void; onNext: () => void }) {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const [selected, setSelected] = useState<Set<string>>(() => new Set(user?.interests.map((i) => i.id)));
  const interests = useQuery({ queryKey: qk.interests, queryFn: interestsApi.list, staleTime: Infinity });

  const save = useMutation({
    mutationFn: () => meApi.setInterests([...selected]),
    onSuccess: (me) => {
      setUser(me);
      onNext();
    },
    onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Could not save your interests'),
  });

  const toggle = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else if (next.size < MAX) next.add(id);
      else toast(`You can pick up to ${MAX}`);
      return next;
    });

  const remaining = Math.max(0, MIN - selected.size);

  return (
    <StepFrame
      eyebrow="Step 3 · Your world"
      title="What are you into?"
      description="Pick at least three. We’ll use them to suggest circles you’ll actually enjoy."
      footer={
        <>
          <Button variant="ghost" onClick={onBack}>
            Back
          </Button>
          <div className="flex items-center gap-4">
            <span className="hidden text-sm text-muted sm:inline" aria-live="polite">
              {remaining > 0 ? `${remaining} more to go` : `${selected.size} selected`}
            </span>
            <Button size="lg" disabled={remaining > 0} loading={save.isPending} onClick={() => save.mutate()}>
              Continue
            </Button>
          </div>
        </>
      }
    >
      {interests.isPending ? (
        <div className="flex flex-wrap gap-2.5" aria-hidden>
          {SKELETON_WIDTHS.map((w, i) => (
            <Skeleton key={i} className={cn('h-11 rounded-full', w)} />
          ))}
        </div>
      ) : (
        <div className="flex flex-wrap gap-2.5" role="group" aria-label="Interests">
          {interests.data?.map((interest, i) => {
            const on = selected.has(interest.id);
            return (
              <motion.button
                key={interest.id}
                type="button"
                aria-pressed={on}
                onClick={() => toggle(interest.id)}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.015 }}
                whileTap={{ scale: 0.96 }}
                className={cn(
                  'inline-flex h-11 items-center gap-2 rounded-full border px-4 text-[15px] transition-colors',
                  on
                    ? 'border-accent bg-accent-soft font-medium text-ink'
                    : 'border-line-strong bg-surface text-ink hover:border-ink/30 hover:bg-surface-2',
                )}
              >
                <span aria-hidden>{interest.emoji}</span>
                {interest.name}
                {on && <Check className="size-4 text-accent-ink" aria-hidden />}
              </motion.button>
            );
          })}
        </div>
      )}
    </StepFrame>
  );
}
