import { api } from './client';
import type {
  AuthProviders,
  AuthResponse,
  Interest,
  LoginRequest,
  MeResponse,
  RegisterRequest,
  UsernameAvailability,
} from './schema';

export const authApi = {
  register: (body: RegisterRequest) => api.post<AuthResponse>('/auth/register', body),
  login: (body: LoginRequest) => api.post<AuthResponse>('/auth/login', body),
  logout: () => api.post<null>('/auth/logout'),
  providers: () => api.get<AuthProviders>('/auth/providers'),
};

export const meApi = {
  get: () => api.get<MeResponse>('/me'),
  usernameAvailability: (username: string, signal?: AbortSignal) =>
    api.get<UsernameAvailability>('/me/username-availability', { params: { username }, signal }),
  setUsername: (username: string) => api.put<MeResponse>('/me/username', { username }),
  setInterests: (interestIds: string[]) => api.put<MeResponse>('/me/interests', { interestIds }),
  uploadAvatar: (file: Blob) => {
    const form = new FormData();
    form.append('file', file, 'avatar.jpg');
    return api.post<MeResponse>('/me/avatar', form);
  },
  completeOnboarding: () => api.post<MeResponse>('/me/onboarding/complete'),
};

export const interestsApi = {
  list: () => api.get<Interest[]>('/interests'),
};

/** Full-page navigation to start Google sign-in (handled by Spring Security). */
export const googleSignInUrl = (apiUrl: string) => `${apiUrl}/oauth2/authorization/google`;
