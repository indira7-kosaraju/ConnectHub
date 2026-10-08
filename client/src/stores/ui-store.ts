import { create } from 'zustand';

interface UiState {
  composerOpen: boolean;
  /** Circle to preselect in the composer (e.g. when opened from a circle page). */
  composerCircleId: string | null;
  openComposer: (circleId?: string) => void;
  closeComposer: () => void;
  momentComposerOpen: boolean;
  setMomentComposerOpen: (open: boolean) => void;
}

export const useUiStore = create<UiState>()((set) => ({
  composerOpen: false,
  composerCircleId: null,
  openComposer: (circleId) => set({ composerOpen: true, composerCircleId: circleId ?? null }),
  closeComposer: () => set({ composerOpen: false }),
  momentComposerOpen: false,
  setMomentComposerOpen: (momentComposerOpen) => set({ momentComposerOpen }),
}));
