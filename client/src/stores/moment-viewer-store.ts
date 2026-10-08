import { create } from 'zustand';
import type { MomentGroup } from '@/lib/api/schema';

interface ViewerState {
  /** Snapshot of the groups being played (kept stable while the viewer is open). */
  groups: MomentGroup[] | null;
  startGroup: number;
  startMoment: number;
  /** Rewind playback: no "seen" tracking. */
  archive: boolean;
  open: (groups: MomentGroup[], startGroup?: number, opts?: { startMoment?: number; archive?: boolean }) => void;
  close: () => void;
}

export const useMomentViewer = create<ViewerState>()((set) => ({
  groups: null,
  startGroup: 0,
  startMoment: 0,
  archive: false,
  open: (groups, startGroup = 0, opts) => {
    // Start at the first unseen moment of the chosen person, unless told otherwise.
    const g = groups[startGroup];
    const firstUnseen = g ? g.moments.findIndex((m) => !m.seen) : -1;
    set({
      groups,
      startGroup,
      startMoment: opts?.startMoment ?? (firstUnseen >= 0 ? firstUnseen : 0),
      archive: opts?.archive ?? false,
    });
  },
  close: () => set({ groups: null }),
}));
