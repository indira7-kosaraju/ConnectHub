import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Splash } from '@/components/ui/splash';
import { toast } from '@/components/ui/toaster';
import { POST_AUTH_REDIRECT_KEY } from '@/components/feature/auth/google-button';
import { refreshAccessToken } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth-store';
import { safeRedirect } from '@/lib/utils';

const ERRORS: Record<string, string> = {
  email_unverified: 'Your Google account needs a verified email address.',
  oauth_failed: 'Google sign-in didn’t complete. Please try again.',
};

function takeStoredNext(): string | null {
  try {
    const next = sessionStorage.getItem(POST_AUTH_REDIRECT_KEY);
    sessionStorage.removeItem(POST_AUTH_REDIRECT_KEY);
    return next;
  } catch {
    return null;
  }
}

/** Landing page after Google sign-in: the server has set the refresh cookie; exchange it for a session. */
export function AuthCallbackPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return; // StrictMode double-invokes effects in dev
    ran.current = true;

    const error = params.get('error');
    if (error) {
      toast.error(ERRORS[error] ?? ERRORS.oauth_failed);
      navigate('/login', { replace: true });
      return;
    }
    void refreshAccessToken().then((token) => {
      const user = useAuthStore.getState().user;
      if (!token || !user) {
        toast.error(ERRORS.oauth_failed);
        navigate('/login', { replace: true });
        return;
      }
      navigate(user.onboarded ? safeRedirect(takeStoredNext()) : '/onboarding', { replace: true });
    });
  }, [navigate, params]);

  return <Splash />;
}
