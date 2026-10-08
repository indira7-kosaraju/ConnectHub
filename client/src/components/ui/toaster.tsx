import { Toaster as Sonner } from 'sonner';

export function Toaster({ theme }: { theme: 'light' | 'dark' }) {
  return (
    <Sonner
      theme={theme}
      position="bottom-center"
      offset={88}
      mobileOffset={88}
      toastOptions={{
        classNames: {
          toast: '!rounded-2xl !border !border-line !bg-surface !text-ink !shadow-lift !font-sans',
          description: '!text-muted',
          actionButton: '!bg-ink !text-bg !rounded-full',
        },
      }}
    />
  );
}

export { toast } from 'sonner';
