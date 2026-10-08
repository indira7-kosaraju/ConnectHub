import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { CompassIllustration } from '@/components/ui/illustrations';

/** In-shell "not found" for missing users/circles. */
export function NotFoundPanel({ title, description }: { title: string; description: string }) {
  return (
    <EmptyState
      illustration={CompassIllustration}
      title={title}
      description={description}
      action={
        <Button asChild variant="outline">
          <Link to="/discover">Explore circles</Link>
        </Button>
      }
    />
  );
}

export function NotFoundPage() {
  return (
    <main className="grid min-h-dvh place-items-center bg-bg px-4">
      <EmptyState
        illustration={CompassIllustration}
        title="This page wandered off"
        description="The link may be broken, or the page may have been removed. Let’s get you somewhere familiar."
        action={
          <Button asChild variant="primary" size="lg">
            <Link to="/">Take me home</Link>
          </Button>
        }
      />
    </main>
  );
}
