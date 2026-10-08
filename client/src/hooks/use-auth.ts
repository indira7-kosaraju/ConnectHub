import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/lib/api/auth';
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/lib/api/schema';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from '@/components/ui/toaster';

function useStoreSession() {
  const setSession = useAuthStore((s) => s.setSession);
  return (res: AuthResponse) => setSession(res.accessToken, res.user);
}

export function useLogin() {
  const store = useStoreSession();
  return useMutation({ mutationFn: (body: LoginRequest) => authApi.login(body), onSuccess: store });
}

export function useRegister() {
  const store = useStoreSession();
  return useMutation({ mutationFn: (body: RegisterRequest) => authApi.register(body), onSuccess: store });
}

export function useLogout() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const clear = useAuthStore((s) => s.clear);
  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => {
      // Clear locally even if the network call failed — the user asked to leave.
      clear();
      queryClient.clear();
      navigate('/login', { replace: true });
      toast('Signed out. See you soon.');
    },
  });
}

export function useAuthProviders() {
  return useQuery({ queryKey: ['auth', 'providers'], queryFn: authApi.providers, staleTime: Infinity });
}
