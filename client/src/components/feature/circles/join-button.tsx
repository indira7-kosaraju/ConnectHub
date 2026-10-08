import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Check, Clock, Lock } from 'lucide-react';
import { Button, type ButtonProps } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useJoinCircle, useLeaveCircle } from '@/hooks/use-circles';
import type { MemberStatus } from '@/lib/api/schema';
import { useAuthStore } from '@/stores/auth-store';

interface JoinButtonProps {
  slug: string;
  name: string;
  privateCircle: boolean;
  full: boolean;
  /** Viewer's membership status; null/undefined when not a member. */
  status?: MemberStatus | null;
  isAdmin?: boolean;
  size?: ButtonProps['size'];
  className?: string;
}

/** Join / Request / Requested / Joined(→Leave) / Full — one control for every membership state. */
export function JoinButton({ slug, name, privateCircle, full, status, isAdmin = false, size = 'sm', className }: JoinButtonProps) {
  const signedIn = useAuthStore((s) => !!s.user);
  const location = useLocation();
  const join = useJoinCircle();
  const leave = useLeaveCircle();
  const [confirmLeave, setConfirmLeave] = useState(false);
  const busy = join.isPending || leave.isPending;
  const label = `${name} circle`;

  if (!signedIn) {
    return (
      <Button asChild size={size} variant="primary" className={className}>
        <Link to={`/login?next=${encodeURIComponent(location.pathname)}`}>Join</Link>
      </Button>
    );
  }

  if (status === 'active') {
    return (
      <>
        <Button
          size={size}
          variant="outline"
          className={className}
          onClick={() => setConfirmLeave(true)}
          aria-label={`Joined ${label}. Leave?`}
        >
          <Check aria-hidden /> Joined
        </Button>
        <ConfirmDialog
          open={confirmLeave}
          onOpenChange={setConfirmLeave}
          title={`Leave ${name}?`}
          description={
            isAdmin
              ? 'You’re an admin. If you’re the last one, the longest-standing member becomes admin.'
              : 'You can rejoin later if there’s room.'
          }
          confirmLabel="Leave circle"
          destructive
          loading={leave.isPending}
          onConfirm={() => leave.mutate({ slug, pending: false }, { onSettled: () => setConfirmLeave(false) })}
        />
      </>
    );
  }

  if (status === 'pending') {
    return (
      <Button
        size={size}
        variant="secondary"
        className={className}
        loading={busy}
        onClick={() => leave.mutate({ slug, pending: true })}
        aria-label={`Request pending for ${label}. Withdraw?`}
      >
        <Clock aria-hidden /> Requested
      </Button>
    );
  }

  if (full) {
    return (
      <Button size={size} variant="outline" className={className} disabled>
        Full
      </Button>
    );
  }

  return (
    <Button size={size} variant="primary" className={className} loading={busy} onClick={() => join.mutate(slug)} aria-label={`${privateCircle ? 'Request to join' : 'Join'} ${label}`}>
      {privateCircle && <Lock aria-hidden />}
      {privateCircle ? 'Request' : 'Join'}
    </Button>
  );
}
