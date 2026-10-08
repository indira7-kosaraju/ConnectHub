import type { NotificationDto } from '@/lib/api/schema';
import { VIBE_META } from '@/types/vibe';

/** Human sentence for a notification (without the actor's name, which is rendered bold separately). */
export function notificationText(n: NotificationDto): string {
  switch (n.type) {
    case 'reaction': {
      const vibe = n.reaction ? VIBE_META[n.reaction] : null;
      return vibe ? `felt ${vibe.glyph} ${vibe.label} about your post` : 'added a vibe to your post';
    }
    case 'comment':
      return 'commented on your post';
    case 'reply':
      return 'replied to your comment';
    case 'circle_request':
      return `asked to join ${n.circleName ?? 'your circle'}`;
    case 'circle_approved':
      return `approved your request to join ${n.circleName ?? 'a circle'}`;
    case 'message':
      return 'sent you a message';
  }
}

/** Where tapping a notification goes. Messages resolve to a conversation separately. */
export function notificationHref(n: NotificationDto): string | null {
  switch (n.type) {
    case 'reaction':
    case 'comment':
    case 'reply':
      return n.postId ? `/p/${n.postId}` : null;
    case 'circle_request':
    case 'circle_approved':
      return n.circleSlug ? `/c/${n.circleSlug}` : null;
    case 'message':
      return null;
  }
}
