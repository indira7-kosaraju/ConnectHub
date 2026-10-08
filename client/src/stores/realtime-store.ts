import { create } from 'zustand';

export type ConnectionStatus = 'idle' | 'connecting' | 'connected' | 'reconnecting';

interface RealtimeState {
  status: ConnectionStatus;
  /** conversationId → epoch ms until which the other person counts as typing. */
  typing: Record<string, number>;
  /** The thread currently on screen (incoming messages there are marked read, not toasted). */
  activeConversationId: string | null;
  setStatus: (s: ConnectionStatus) => void;
  setTyping: (conversationId: string, typing: boolean) => void;
  setActiveConversation: (id: string | null) => void;
}

export const useRealtimeStore = create<RealtimeState>()((set) => ({
  status: 'idle',
  typing: {},
  activeConversationId: null,
  setStatus: (status) => set({ status }),
  setTyping: (conversationId, typing) =>
    set((s) => {
      const next = { ...s.typing };
      if (typing) next[conversationId] = Date.now() + 6000;
      else delete next[conversationId];
      return { typing: next };
    }),
  setActiveConversation: (activeConversationId) => set({ activeConversationId }),
}));
