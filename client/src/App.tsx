import { RouterProvider } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from '@/components/ui/toaster';
import { Splash } from '@/components/ui/splash';
import { useApplyTheme } from '@/hooks/use-theme';
import { queryClient } from '@/lib/query-client';
import { router } from '@/routes';

export function App() {
  const theme = useApplyTheme();
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} fallbackElement={<Splash />} />
      <Toaster theme={theme} />
    </QueryClientProvider>
  );
}
