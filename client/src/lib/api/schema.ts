import type { components } from '@/types/openapi';

/** Friendly aliases for the generated OpenAPI schemas (`npm run gen:api`). */
type Schemas = components['schemas'];

export type MeResponse = Schemas['MeResponse'];
export type AuthResponse = Schemas['AuthResponse'];
export type AuthProviders = Schemas['AuthProvidersResponse'];
export type Interest = Schemas['InterestDto'];
export type UsernameAvailability = Schemas['UsernameAvailability'];
export type RegisterRequest = Schemas['RegisterRequest'];
export type LoginRequest = Schemas['LoginRequest'];
export type UserSummary = Schemas['UserSummary'];
export type CircleSummary = Schemas['CircleSummary'];
export type CircleDetail = Schemas['CircleDetail'];
export type ViewerMembership = Schemas['ViewerMembership'];
export type MemberDto = Schemas['MemberDto'];
export type JoinResult = Schemas['JoinResult'];
export type ProfileResponse = Schemas['ProfileResponse'];
export type CreateCircleRequest = Schemas['CreateCircleRequest'];
export type UpdateCircleRequest = Schemas['UpdateCircleRequest'];
export type UpdateProfileRequest = Schemas['UpdateProfileRequest'];
export type MemberStatus = MemberDto['status'];
export type PostDto = Schemas['PostDto'];
export type PostImage = Schemas['PostImageDto'];
export type VibeMix = Schemas['VibeMix'];
export type ReactionType = NonNullable<PostDto['myReaction']>;
export type ReactionResult = Schemas['ReactionResult'];
export type CommentDto = Schemas['CommentDto'];
export type ReportReason = Schemas['ReportRequest']['reason'];
export type MomentDto = Schemas['MomentDto'];
export type MomentGroup = Schemas['MomentGroup'];
export type MomentBackground = NonNullable<MomentDto['background']>;
export type ConversationDto = Schemas['ConversationDto'];
export type MessageDto = Schemas['MessageDto'];
export type ReadReceipt = Schemas['ReadReceipt'];
/** STOMP-only payload (not part of the REST spec): /user/queue/typing. */
export interface TypingEvent {
  conversationId: string;
  userId: string;
  typing: boolean;
}
export type NotificationDto = Schemas['NotificationDto'];
export type NotificationType = NotificationDto['type'];
export type SearchResults = Schemas['SearchResults'];
export type NotificationPrefs = MeResponse['notificationPrefs'];
