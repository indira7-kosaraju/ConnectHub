import type { BrowseParams } from './api/circles';
import type { MemberStatus } from './api/schema';

/** Central query-key factory so invalidation stays consistent. */
export const qk = {
  me: ['me'] as const,
  myCircles: ['me', 'circles'] as const,
  suggestions: (limit: number) => ['me', 'suggestions', limit] as const,
  allSuggestions: ['me', 'suggestions'] as const,
  circles: ['circles'] as const,
  trending: ['circles', 'trending'] as const,
  browse: (p: BrowseParams) => ['circles', 'browse', p] as const,
  circle: (slug: string) => ['circle', slug] as const,
  members: (slug: string, status: MemberStatus) => ['circle', slug, 'members', status] as const,
  profile: (username: string) => ['profile', username] as const,
  profileCircles: (username: string) => ['profile', username, 'circles'] as const,
  interests: ['interests'] as const,
  /** Every post list lives under ['posts', …] so one helper can patch a post everywhere. */
  posts: ['posts'] as const,
  feed: ['posts', 'feed'] as const,
  circlePosts: (slug: string) => ['posts', 'circle', slug] as const,
  userPosts: (username: string) => ['posts', 'user', username] as const,
  saved: ['posts', 'saved'] as const,
  post: (id: string) => ['post', id] as const,
  comments: (postId: string) => ['comments', postId] as const,
  momentStrip: ['moments', 'strip'] as const,
  conversations: ['conversations'] as const,
  conversation: (id: string) => ['conversation', id] as const,
  messages: (id: string) => ['messages', id] as const,
  unreadMessages: ['unread', 'messages'] as const,
  notifications: ['notifications'] as const,
  unreadNotifications: ['unread', 'notifications'] as const,
  contacts: (q: string) => ['contacts', q] as const,
  rewind: ['moments', 'rewind'] as const,
};
