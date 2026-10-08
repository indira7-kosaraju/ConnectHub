import { useEffect } from 'react';
import { useThemeStore } from '@/stores/theme-store';
import { useMediaQuery } from './use-media-query';

/** Applies the resolved theme to <html> and returns it. Mount once near the root. */
export function useApplyTheme(): 'light' | 'dark' {
  const theme = useThemeStore((s) => s.theme);
  const systemDark = useMediaQuery('(prefers-color-scheme: dark)');
  const resolved = theme === 'system' ? (systemDark ? 'dark' : 'light') : theme;

  useEffect(() => {
    document.documentElement.classList.toggle('dark', resolved === 'dark');
  }, [resolved]);

  return resolved;
}
