import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { settingsApi } from '@/lib/api/settings';
import { ApiRequestError } from '@/lib/api/client';
import type { MeResponse, ReportReason } from '@/lib/api/schema';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from '@/components/ui/toaster';

const errorToast = (fallback: string) => (err: unknown) =>
  toast.error(err instanceof ApiRequestError ? err.message : fallback);

export function useSettingsMutations() {
  const setUser = useAuthStore((s) => s.setUser);
  const setSession = useAuthStore((s) => s.setSession);
  const onMe = (me: MeResponse) => setUser(me);
  return {
    // Optimistic: switches flip instantly (and rapid toggles act on the latest state); rolled back on error.
    update: useMutation({
      mutationFn: settingsApi.update,
      onMutate: (body) => {
        const previous = useAuthStore.getState().user;
        if (previous) {
          setUser({
            ...previous,
            privateProfile: body.privateProfile ?? previous.privateProfile,
            notificationPrefs: { ...previous.notificationPrefs, ...body.notificationPrefs },
          });
        }
        return { previous };
      },
      onError: (err, _, ctx) => {
        if (ctx?.previous) setUser(ctx.previous);
        errorToast('Could not save settings')(err);
      },
    }),
    email: useMutation({
      mutationFn: ({ email, password }: { email: string; password: string }) => settingsApi.changeEmail(email, password),
      onSuccess: (me) => {
        onMe(me);
        toast.success('Email updated');
      },
    }),
    password: useMutation({
      mutationFn: ({ next, current }: { next: string; current?: string }) => settingsApi.changePassword(next, current),
      onSuccess: (res) => {
        setSession(res.accessToken, res.user);
        toast.success('Password updated — other devices were signed out');
      },
    }),
  };
}

export function useDeleteAccount() {
  const qc = useQueryClient();
  const clear = useAuthStore((s) => s.clear);
  const navigate = useNavigate();
  return useMutation({
    mutationFn: settingsApi.deleteAccount,
    onSuccess: () => {
      clear();
      qc.clear();
      navigate('/register', { replace: true });
      toast('Your account has been deleted. Take care ✦');
    },
  });
}

export function useBlocks(enabled = true) {
  return useQuery({ queryKey: ['blocks'], queryFn: settingsApi.blocks, enabled });
}

export function useBlockMutations() {
  const qc = useQueryClient();
  const refreshEverything = () => {
    // Blocking changes what's visible almost everywhere.
    void qc.invalidateQueries();
  };
  return {
    block: useMutation({
      mutationFn: (username: string) => settingsApi.block(username),
      onSuccess: () => {
        toast('Blocked. You won’t see each other anymore.');
        refreshEverything();
      },
      onError: errorToast('Could not block'),
    }),
    unblock: useMutation({
      mutationFn: (username: string) => settingsApi.unblock(username),
      onSuccess: () => {
        toast('Unblocked');
        refreshEverything();
      },
      onError: errorToast('Could not unblock'),
    }),
  };
}

export function useReportUser() {
  return useMutation({
    mutationFn: ({ username, reason, details }: { username: string; reason: ReportReason; details?: string }) =>
      settingsApi.reportUser(username, reason, details),
    onSuccess: () => toast.success('Thanks — our team will take a look'),
    onError: errorToast('Could not send the report'),
  });
}

export function useReportComment() {
  return useMutation({
    mutationFn: ({ id, reason, details }: { id: string; reason: ReportReason; details?: string }) => settingsApi.reportComment(id, reason, details),
    onSuccess: () => toast.success('Thanks — our team will take a look'),
    onError: errorToast('Could not send the report'),
  });
}
