import { createBrowserRouter, type RouteObject } from 'react-router-dom';
import { RouteErrorBoundary, ShellErrorBoundary } from '@/components/layout/route-error-boundary';
import { GuestOnly, RequireAuth, RequireNotOnboarded, RequireOnboarded, SessionGate } from './guards';
import { queryClient } from '@/lib/query-client';
import { qk } from '@/lib/query-keys';
import { circlesApi } from '@/lib/api/circles';
import { isSignedIn, sessionReady } from '@/lib/session';
import { circlePostsQuery, conversationsQuery, feedQuery } from '@/lib/queries';

/**
 * Loaders only *start* data requests (never awaited), right after the session is known and in
 * parallel with the page chunk download — removing a full round trip from first render.
 */
const prefetch = (start: () => unknown) => () => {
  void sessionReady.then(() => {
    if (isSignedIn()) start();
  });
  return null;
};

/**
 * Every page is code-split: its chunk downloads the first time the route is visited, so the
 * sign-in screen doesn't pay for the feed, chat or moments code (and vice versa).
 */
const page = (load: () => Promise<{ Component: React.ComponentType }>): Pick<RouteObject, 'lazy'> => ({ lazy: load });

const authRoutes: RouteObject[] = [
  { path: '/login', ...page(() => import('@/pages/login-page').then((m) => ({ Component: m.LoginPage }))) },
  { path: '/register', ...page(() => import('@/pages/register-page').then((m) => ({ Component: m.RegisterPage }))) },
];

const memberRoutes: RouteObject[] = [
  {
    index: true,
    loader: prefetch(() => queryClient.prefetchInfiniteQuery(feedQuery)),
    ...page(() => import('@/pages/home-page').then((m) => ({ Component: m.HomePage }))),
  },
  { path: '/discover', ...page(() => import('@/pages/discover-page').then((m) => ({ Component: m.DiscoverPage }))) },
  {
    path: '/messages',
    loader: prefetch(() => queryClient.prefetchInfiniteQuery(conversationsQuery)),
    ...page(() => import('@/pages/messages-page').then((m) => ({ Component: m.MessagesPage }))),
  },
  { path: '/messages/:id', ...page(() => import('@/pages/messages-page').then((m) => ({ Component: m.ConversationPage }))) },
  { path: '/notifications', ...page(() => import('@/pages/notifications-page').then((m) => ({ Component: m.NotificationsPage }))) },
  { path: '/saved', ...page(() => import('@/pages/saved-page').then((m) => ({ Component: m.SavedPage }))) },
  { path: '/search', ...page(() => import('@/pages/search-page').then((m) => ({ Component: m.SearchPage }))) },
  { path: '/settings', ...page(() => import('@/pages/settings-page').then((m) => ({ Component: m.SettingsPage }))) },
  ...(import.meta.env.DEV
    ? [{ path: '/design', ...page(() => import('@/pages/design-page').then((m) => ({ Component: m.DesignPage }))) }]
    : []),
];

export const router = createBrowserRouter([
  {
    element: <SessionGate />,
    errorElement: <RouteErrorBoundary />,
    children: [
      { element: <GuestOnly />, children: authRoutes },
      { path: '/auth/callback', ...page(() => import('@/pages/auth-callback-page').then((m) => ({ Component: m.AuthCallbackPage }))) },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <RequireNotOnboarded />,
            children: [{ path: '/onboarding', ...page(() => import('@/pages/onboarding-page').then((m) => ({ Component: m.OnboardingPage }))) }],
          },
        ],
      },
      {
        ...page(() => import('@/components/layout/app-shell').then((m) => ({ Component: m.AppShell }))),
        children: [
          {
            // Errors inside a page render in the shell, so navigation stays usable.
            errorElement: <ShellErrorBoundary />,
            children: [
              // Public profiles, circles and posts are readable without an account.
              { path: '/u/:username', ...page(() => import('@/pages/profile-page').then((m) => ({ Component: m.ProfilePage }))) },
              {
                path: '/c/:slug',
                loader: ({ params }) => {
                  const slug = params.slug ?? '';
                  void sessionReady.then(() => {
                    void queryClient.prefetchQuery({ queryKey: qk.circle(slug), queryFn: () => circlesApi.get(slug) });
                    void queryClient.prefetchInfiniteQuery(circlePostsQuery(slug));
                  });
                  return null;
                },
                ...page(() => import('@/pages/circle-page').then((m) => ({ Component: m.CirclePage }))),
              },
              { path: '/p/:id', ...page(() => import('@/pages/post-page').then((m) => ({ Component: m.PostPage }))) },
              { element: <RequireAuth />, children: [{ element: <RequireOnboarded />, children: memberRoutes }] },
            ],
          },
        ],
      },
      { path: '*', ...page(() => import('@/pages/not-found-page').then((m) => ({ Component: m.NotFoundPage }))) },
    ],
  },
]);
