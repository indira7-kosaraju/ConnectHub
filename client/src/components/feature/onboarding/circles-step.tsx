import { useMutation } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { CirclesIllustration } from '@/components/ui/illustrations';
import { toast } from '@/components/ui/toaster';
import { CircleRow, CircleRowSkeleton } from '@/components/feature/circles/circle-row';
import { useCircleSuggestions, useMyCircles } from '@/hooks/use-circles';
import { meApi } from '@/lib/api/auth';
import { ApiRequestError } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth-store';
import { StepFrame } from './step-frame';

export function CirclesStep({ onBack, onDone }: { onBack: () => void; onDone: () => void }) {
  const setUser = useAuthStore((s) => s.setUser);
  const suggestions = useCircleSuggestions(5);
  const mine = useMyCircles();
  const joined = mine.data?.length ?? 0;

  const finish = useMutation({
    mutationFn: meApi.completeOnboarding,
    onSuccess: (me) => {
      setUser(me);
      onDone();
    },
    onError: (err) => toast.error(err instanceof ApiRequestError ? err.message : 'Could not finish onboarding'),
  });

  return (
    <StepFrame
      eyebrow="Step 4 · Your circles"
      title="Circles picked for you"
      description="Based on what you’re into. Join a few — your home feed is built from the circles you’re in."
      footer={
        <>
          <Button variant="ghost" onClick={onBack}>
            Back
          </Button>
          <div className="flex items-center gap-4">
            <span className="hidden text-sm text-muted sm:inline" aria-live="polite">
              {joined === 0 ? 'Join at least one to fill your feed' : `${joined} joined`}
            </span>
            <Button size="lg" variant={joined ? 'accent' : 'outline'} loading={finish.isPending} onClick={() => finish.mutate()}>
              {joined ? 'Finish' : 'Skip for now'}
            </Button>
          </div>
        </>
      }
    >
      {suggestions.isPending ? (
        <div className="space-y-5">
          {Array.from({ length: 5 }, (_, i) => (
            <CircleRowSkeleton key={i} />
          ))}
        </div>
      ) : suggestions.data?.length ? (
        <ul className="divide-y divide-line rounded-[var(--radius-card)] border border-line bg-surface px-4">
          {suggestions.data.map((c) => (
            <li key={c.id} className="py-4">
              <CircleRow circle={c} />
              {c.description && <p className="mt-2 pl-14 text-sm leading-relaxed text-muted">{c.description}</p>}
            </li>
          ))}
        </ul>
      ) : (
        <EmptyState
          illustration={CirclesIllustration}
          title="Nothing to suggest yet"
          description="You can explore every circle from Discover once you’re in."
        />
      )}
    </StepFrame>
  );
}
