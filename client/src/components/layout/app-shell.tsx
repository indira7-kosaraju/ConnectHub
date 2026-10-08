import { lazy, Suspense } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { MotionConfig } from 'framer-motion';
import { TooltipProvider } from '@/components/ui/tooltip';
import { useAuthStore } from '@/stores/auth-store';
import { useMomentViewer } from '@/stores/moment-viewer-store';
import { useUiStore } from '@/stores/ui-store';
import { SideNav } from './side-nav';
import { RightRail } from './right-rail';
import { BottomTabBar } from './bottom-tab-bar';
import { MobileHeader } from './mobile-header';
import { PageTransition } from './page-transition';

// Heavy, on-demand pieces load after the shell is visible.
const ComposerDialog = lazy(() => import('@/components/feature/composer-dialog').then((m) => ({ default: m.ComposerDialog })));
const MomentComposer = lazy(() => import('@/components/feature/moments/moment-composer').then((m) => ({ default: m.MomentComposer })));
const MomentViewer = lazy(() => import('@/components/feature/moments/moment-viewer').then((m) => ({ default: m.MomentViewer })));
const RealtimeBridge = lazy(() => import('@/components/feature/realtime-bridge').then((m) => ({ default: m.RealtimeBridge })));

/**
 * Three columns on desktop (nav / content / right rail), icon rail on tablet,
 * single column with header + bottom tab bar on mobile.
 */
export function AppShell() {
  const location = useLocation();
  const composerOpen = useUiStore((s) => s.composerOpen);
  const momentComposerOpen = useUiStore((s) => s.momentComposerOpen);
  const viewerOpen = useMomentViewer((s) => !!s.groups);
  const signedIn = useAuthStore((s) => !!s.user);

  return (
    // Honour prefers-reduced-motion for every Framer Motion animation in the app.
    <MotionConfig reducedMotion="user">
      <TooltipProvider delayDuration={400}>
        <div className="min-h-dvh">
          <a
            href="#main"
            className="sr-only z-50 rounded-full bg-ink px-4 py-2 text-bg focus:not-sr-only focus:fixed focus:left-4 focus:top-4"
          >
            Skip to content
          </a>
          <MobileHeader />
          <div className="mx-auto flex max-w-[1240px] md:px-4">
            <div className="hidden w-[76px] shrink-0 md:block lg:w-[248px]">
              <SideNav />
            </div>
            <main id="main" className="min-w-0 flex-1 px-4 pb-28 pt-4 md:border-x md:border-line md:px-6 md:pb-10 md:pt-6">
              <div className="mx-auto max-w-[620px]">
                <PageTransition key={location.pathname}>
                  <Outlet />
                </PageTransition>
              </div>
            </main>
            <div className="hidden w-[320px] shrink-0 xl:block">
              <RightRail />
            </div>
          </div>
          <BottomTabBar />
          <Suspense fallback={null}>
            {composerOpen && <ComposerDialog />}
            {momentComposerOpen && <MomentComposer />}
            {viewerOpen && <MomentViewer />}
            {signedIn && <RealtimeBridge />}
          </Suspense>
        </div>
      </TooltipProvider>
    </MotionConfig>
  );
}
