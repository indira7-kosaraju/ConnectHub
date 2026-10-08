/**
 * Envelope shared by every REST response. Domain DTO types are generated from the
 * OpenAPI spec into `src/types/openapi.d.ts` via `npm run gen:api`.
 */
export interface ApiError {
  code: string;
  message: string;
  fields?: Record<string, string>;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ApiError | null;
  timestamp: string;
}

export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  hasNext: boolean;
}
