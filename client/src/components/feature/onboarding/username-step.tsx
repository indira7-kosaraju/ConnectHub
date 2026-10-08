import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Check, Loader2, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { inputClasses } from '@/components/ui/input';
import { meApi } from '@/lib/api/auth';
import { ApiRequestError } from '@/lib/api/client';
import { useDebouncedValue } from '@/hooks/use-debounced-value';
import { useAuthStore } from '@/stores/auth-store';
import { cn } from '@/lib/utils';
import { StepFrame } from './step-frame';

const VALID = /^[a-z0-9_]{3,30}$/;

function suggestFrom(name: string | null | undefined): string {
  return (name ?? '')
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 30);
}

const REASONS: Record<string, string> = {
  TAKEN: 'Someone already has that one.',
  RESERVED: 'That name is reserved.',
  INVALID: 'Use 3–30 lowercase letters, numbers or underscores.',
};

export function UsernameStep({ onNext }: { onNext: () => void }) {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const [value, setValue] = useState(() => user?.username ?? suggestFrom(user?.displayName));
  const debounced = useDebouncedValue(value, 350);
  const formatOk = VALID.test(debounced);

  const check = useQuery({
    queryKey: ['username-availability', debounced],
    queryFn: ({ signal }) => meApi.usernameAvailability(debounced, signal),
    enabled: formatOk,
    staleTime: 10_000,
  });

  const save = useMutation({
    mutationFn: () => meApi.setUsername(value),
    onSuccess: (me) => {
      setUser(me);
      onNext();
    },
  });

  const status = useMemo(() => {
    if (!value) return { tone: 'muted', text: 'This is how people will find you.' } as const;
    if (!VALID.test(value)) return { tone: 'error', text: REASONS.INVALID } as const;
    if (value !== debounced || check.isFetching) return { tone: 'muted', text: 'Checking…' } as const;
    if (check.data?.available) return { tone: 'ok', text: `@${value} is yours.` } as const;
    if (check.data) return { tone: 'error', text: REASONS[check.data.reason ?? 'TAKEN'] ?? REASONS.TAKEN } as const;
    return { tone: 'muted', text: '' } as const;
  }, [value, debounced, check.isFetching, check.data]);

  const canContinue = value === debounced && check.data?.available === true && !save.isPending;
  const serverError = save.error instanceof ApiRequestError ? save.error.message : null;

  return (
    <StepFrame
      eyebrow="Step 1 · Your name"
      title="Pick a username"
      description="Lowercase letters, numbers and underscores. You can change it later in settings."
      footer={
        <>
          <span />
          <Button size="lg" onClick={() => save.mutate()} disabled={!canContinue} loading={save.isPending}>
            Continue
          </Button>
        </>
      }
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (canContinue) save.mutate();
        }}
        className="space-y-2"
      >
        <Label htmlFor="username">Username</Label>
        <div className="relative">
          <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 font-serif text-xl text-muted">@</span>
          <input
            id="username"
            autoFocus
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            maxLength={30}
            value={value}
            onChange={(e) => setValue(e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, ''))}
            aria-invalid={status.tone === 'error' || undefined}
            aria-describedby="username-status"
            className={cn(inputClasses, 'h-14 pl-10 pr-12 font-serif text-xl')}
          />
          <span className="absolute right-4 top-1/2 -translate-y-1/2" aria-hidden>
            {status.tone === 'ok' && <Check className="size-5 text-secondary" />}
            {status.tone === 'error' && <X className="size-5 text-danger" />}
            {status.text === 'Checking…' && <Loader2 className="size-5 animate-spin text-muted" />}
          </span>
        </div>
        <p
          id="username-status"
          aria-live="polite"
          className={cn('min-h-5 text-sm', status.tone === 'ok' ? 'text-secondary' : status.tone === 'error' ? 'text-danger' : 'text-muted')}
        >
          {serverError ?? status.text}
        </p>
      </form>
    </StepFrame>
  );
}
