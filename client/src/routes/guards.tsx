import { Navigate, Outlet, useLocation, useSearchParams } from 'react-router-dom';
import { Splash } from '@/components/ui/splash';
import { useSessionBootstrap } from '@/hooks/use-session-bootstrap';
import { useAuthStore } from '@/stores/auth-store';
import { safeRedirect } from '@/lib/utils';

/** Waits for the silent refresh so guards never flash the wrong screen. */
export function SessionGate() {
  const ready = useSessionBootstrap();
  return ready ? <Outlet /> : <Splash />;
}

/** Signed-in users only; remembers where they were headed. */
export function RequireAuth() {
  const user = useAuthStore((s) => s.user);
  const location = useLocation();
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?next=${next}`} replace />;
  }
  return <Outlet />;
}

/** Signed-in users who finished onboarding. */
export function RequireOnboarded() {
  const user = useAuthStore((s) => s.user);
  if (user && !user.onboarded) return <Navigate to="/onboarding" replace />;
  return <Outlet />;
}

/** Onboarding is only for users who haven't finished it. */
export function RequireNotOnboarded() {
  const user = useAuthStore((s) => s.user);
  if (user?.onboarded) return <Navigate to="/" replace />;
  return <Outlet />;
}

/** Login / register: bounce signed-in users onward. */
export function GuestOnly() {
  const user = useAuthStore((s) => s.user);
  const [params] = useSearchParams();
  if (user) {
    return <Navigate to={user.onboarded ? safeRedirect(params.get('next')) : '/onboarding'} replace />;
  }
  return <Outlet />;
}
