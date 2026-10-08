import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { env } from '@/lib/env';
import { refreshAccessToken } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth-store';
import { useRealtimeStore } from '@/stores/realtime-store';

type Handler = (body: unknown) => void;

/** http(s)://host/ws → ws(s)://host/ws/websocket (Spring's raw-WebSocket path on a SockJS endpoint). */
function nativeUrl(): string {
  const url = new URL(env.wsUrl, window.location.origin);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.pathname = url.pathname.replace(/\/$/, '') + '/websocket';
  return url.toString();
}

const SOCKJS_AFTER_FAILURES = 2;

/**
 * One STOMP-over-SockJS connection per signed-in tab. The JWT goes in the CONNECT frame and is
 * refreshed before every (re)connect. Subscriptions are remembered and re-established on reconnect.
 */
class RealtimeClient {
  private client: Client | null = null;
  private handlers = new Map<string, Set<Handler>>();
  private active = new Map<string, StompSubscription>();
  /** Consecutive failed connects; after a few, fall back to SockJS (e.g. proxies that block WebSockets). */
  private failures = 0;
  private sockJs: (new (url: string) => WebSocket) | null = null;

  start() {
    if (this.client) return;
    const store = useRealtimeStore.getState();
    store.setStatus('connecting');
    this.client = new Client({
      webSocketFactory: () =>
        this.sockJs ? new this.sockJs(new URL(env.wsUrl, window.location.origin).toString()) : new WebSocket(nativeUrl()),
      reconnectDelay: 3000,
      heartbeatIncoming: 20_000,
      heartbeatOutgoing: 20_000,
      beforeConnect: async () => {
        let token = useAuthStore.getState().accessToken;
        if (!token) token = await refreshAccessToken();
        this.client!.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      },
      onConnect: () => {
        this.failures = 0;
        useRealtimeStore.getState().setStatus('connected');
        this.active.clear();
        this.handlers.forEach((_, destination) => this.attach(destination));
      },
      onStompError: (frame) => {
        // Usually an expired token on CONNECT: refresh so the automatic reconnect succeeds.
        if (/token/i.test(frame.headers.message ?? '')) void refreshAccessToken();
      },
      onWebSocketClose: () => {
        if (!this.client) return;
        useRealtimeStore.getState().setStatus('reconnecting');
        if (!this.client.connected && ++this.failures >= SOCKJS_AFTER_FAILURES && !this.sockJs) {
          // Loaded only when needed: SockJS emulates WebSockets over HTTP streaming/polling.
          void import('sockjs-client').then((m) => {
            this.sockJs = m.default as unknown as new (url: string) => WebSocket;
          });
        }
      },
    });
    this.client.activate();
  }

  async stop() {
    const c = this.client;
    this.client = null;
    this.active.clear();
    this.handlers.clear();
    useRealtimeStore.getState().setStatus('idle');
    await c?.deactivate();
  }

  get connected() {
    return !!this.client?.connected;
  }

  /** Subscribe to a destination; returns an unsubscribe function. */
  on(destination: string, handler: Handler): () => void {
    let set = this.handlers.get(destination);
    if (!set) {
      set = new Set();
      this.handlers.set(destination, set);
      if (this.connected) this.attach(destination);
    }
    set.add(handler);
    return () => {
      set!.delete(handler);
      if (set!.size === 0) {
        this.handlers.delete(destination);
        this.active.get(destination)?.unsubscribe();
        this.active.delete(destination);
      }
    };
  }

  /** Publish to an /app destination. Returns false if not connected (callers fall back to REST). */
  publish(destination: string, body: unknown): boolean {
    if (!this.client?.connected) return false;
    this.client.publish({ destination, body: JSON.stringify(body) });
    return true;
  }

  private attach(destination: string) {
    if (!this.client?.connected || this.active.has(destination)) return;
    const sub = this.client.subscribe(destination, (msg: IMessage) => {
      let body: unknown;
      try {
        body = JSON.parse(msg.body);
      } catch {
        return;
      }
      this.handlers.get(destination)?.forEach((h) => h(body));
    });
    this.active.set(destination, sub);
  }
}

export const realtime = new RealtimeClient();

export const QUEUES = {
  messages: '/user/queue/messages',
  read: '/user/queue/read',
  typing: '/user/queue/typing',
  notifications: '/user/queue/notifications',
  errors: '/user/queue/errors',
} as const;
