import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Logo } from '@/components/ui/logo';
import { CirclesIllustration } from '@/components/ui/illustrations';

/** Split editorial layout for auth and onboarding screens. */
export function AuthLayout({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <div className="grid min-h-dvh lg:grid-cols-[1.1fr_1fr]">
      <aside className="relative hidden flex-col justify-between overflow-hidden bg-secondary p-12 text-[#f5f1ea] lg:flex">
        <Link to="/" aria-label="ConnectHub home">
          <Logo inverse />
        </Link>
        <div>
          <CirclesIllustration className="mb-10 h-40 w-auto" />
          <p className="max-w-md font-serif text-4xl leading-tight">
            Find your people through what you love — not who has the most followers.
          </p>
        </div>
        <p className="text-sm opacity-80">Circles · Vibes · Moments</p>
      </aside>
      <main className="flex flex-col justify-center px-6 py-12 sm:px-12">
        <div className="mx-auto w-full max-w-sm">
          <Link to="/" className="mb-10 inline-flex lg:hidden" aria-label="ConnectHub home">
            <Logo />
          </Link>
          <h1 className="text-3xl font-semibold">{title}</h1>
          {subtitle && <p className="mt-2 text-muted">{subtitle}</p>}
          <div className="mt-8">{children}</div>
        </div>
      </main>
    </div>
  );
}
