import { isRouteErrorResponse, useRouteError } from 'react-router-dom';
import { RotateCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { CrackedIllustration } from '@/components/ui/illustrations';
import { NotFoundPage } from '@/pages/not-found-page';
import { ServerErrorPage } from '@/pages/server-error-page';

/** A new deploy replaced the chunk this tab expected: reload once to pick up the new build. */
function isStaleChunk(error: unknown): boolean {
  return error instanceof Error && /dynamically imported module|Importing a module script failed|error loading dynamically/i.test(error.message);
}

function reloadOnce(): boolean {
  try {
    if (sessionStorage.getItem('connecthub-chunk-reload')) return false;
    sessionStorage.setItem('connecthub-chunk-reload', '1');
  } catch {
    return false;
  }
  window.location.reload();
  return true;
}

/** Full-page errorElement: 404s get the lost page, everything else the 500 page. */
export function RouteErrorBoundary() {
  const error = useRouteError();
  if (isStaleChunk(error) && reloadOnce()) return null;
  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundPage />;
  }
  if (import.meta.env.DEV) {
    console.error(error);
  }
  return <ServerErrorPage />;
}

/** In-shell errorElement: keeps the navigation and lets the user retry just this page. */
export function ShellErrorBoundary() {
  const error = useRouteError();
  if (isStaleChunk(error) && reloadOnce()) return null;
  if (import.meta.env.DEV) {
    console.error(error);
  }
  return (
    <EmptyState
      illustration={CrackedIllustration}
      title="This page hit a snag"
      description="Something went wrong while loading it. The rest of ConnectHub is fine."
      action={
        <Button variant="outline" onClick={() => window.location.reload()}>
          <RotateCw aria-hidden /> Try again
        </Button>
      }
    />
  );
}
