import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { fileURLToPath, URL } from 'node:url';

const API_TARGET = process.env.API_PROXY_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  // sockjs-client references Node's `global`
  define: { global: 'globalThis' },
  server: {
    port: 5173,
    // Same-origin in dev, mirroring the Vercel rewrites in prod, so the refresh cookie is first-party.
    proxy: {
      ...Object.fromEntries(['/api', '/oauth2', '/login/oauth2', '/uploads'].map((path) => [path, { target: API_TARGET, xfwd: true }])),
      '/ws': { target: API_TARGET, ws: true, xfwd: true },
    },
  },
  // `vite preview` (used by e2e tests) reuses server.proxy.
  build: {
    target: 'es2022',
    rollupOptions: {
      output: {
        // Rolldown's native splitting: page code stays split per route; a stable React vendor
        // chunk caches across deploys; all icons share one chunk instead of dozens of tiny ones.
        codeSplitting: {
          groups: [
            { name: 'react', test: /node_modules[\\/](react|react-dom|react-router|react-router-dom|@remix-run|scheduler)[\\/]/, priority: 20 },
            { name: 'icons', test: /node_modules[\\/]lucide-react[\\/]/, priority: 10 },
          ],
        },
      },
    },
  },
});
