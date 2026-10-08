import { Button } from '@/components/ui/button';
import { EmptyState } from '@/components/ui/empty-state';
import { CrackedIllustration } from '@/components/ui/illustrations';

export function ServerErrorPage() {
  return (
    <main className="grid min-h-dvh place-items-center bg-bg px-4">
      <EmptyState
        illustration={CrackedIllustration}
        title="Something cracked on our side"
        description="We hit an unexpected error. It’s not you — try again in a moment."
        action={
          <Button variant="primary" size="lg" onClick={() => window.location.reload()}>
            Try again
          </Button>
        }
      />
    </main>
  );
}
