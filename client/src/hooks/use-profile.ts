import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { profileApi } from '@/lib/api/circles';
import type { MeResponse, UpdateProfileRequest } from '@/lib/api/schema';
import { qk } from '@/lib/query-keys';
import { useAuthStore } from '@/stores/auth-store';

export function useProfile(username: string) {
  return useQuery({ queryKey: qk.profile(username.toLowerCase()), queryFn: () => profileApi.get(username) });
}

export function useProfileCircles(username: string, enabled: boolean) {
  return useQuery({
    queryKey: qk.profileCircles(username.toLowerCase()),
    queryFn: () => profileApi.circles(username),
    enabled,
  });
}

/** Profile edits update the session user and refetch the public profile. */
export function useProfileMutations() {
  const qc = useQueryClient();
  const setUser = useAuthStore((s) => s.setUser);
  const onSuccess = (me: MeResponse) => {
    setUser(me);
    if (me.username) void qc.invalidateQueries({ queryKey: qk.profile(me.username) });
  };
  return {
    update: useMutation({ mutationFn: (body: UpdateProfileRequest) => profileApi.update(body), onSuccess }),
    avatar: useMutation({ mutationFn: (file: Blob) => profileApi.uploadAvatar(file), onSuccess }),
    cover: useMutation({ mutationFn: (file: Blob) => profileApi.uploadCover(file), onSuccess }),
    removeCover: useMutation({ mutationFn: () => profileApi.removeCover(), onSuccess }),
  };
}
