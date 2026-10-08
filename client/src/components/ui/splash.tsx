import { LogoMark } from './logo';

/** Shown while the session is being restored on first load. */
export function Splash() {
  return (
    <div className="grid min-h-dvh place-items-center bg-bg" role="status" aria-label="Loading ConnectHub">
      <LogoMark className="size-12 animate-pulse motion-reduce:animate-none" />
    </div>
  );
}
