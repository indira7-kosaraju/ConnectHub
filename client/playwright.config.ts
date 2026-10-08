import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests run against a real API (dev profile, seeded data) behind the Vite proxy.
 *  - Locally: start the API and `npm run dev`, then `npm run e2e` (reuses http://localhost:5173).
 *  - CI: builds the app and serves it with `vite preview` on :4173 (proxying /api and /ws to :8080).
 */
const baseURL = process.env.E2E_BASE_URL ?? (process.env.CI ? 'http://localhost:4173' : 'http://localhost:5173');

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  workers: process.env.CI ? 2 : 4,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.CI
    ? { command: 'npm run preview -- --port 4173 --strictPort', url: 'http://localhost:4173', reuseExistingServer: false, timeout: 60_000 }
    : undefined,
});
