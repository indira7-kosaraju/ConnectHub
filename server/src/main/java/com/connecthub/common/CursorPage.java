package com.connecthub.common;

import java.util.List;

/**
 * A page of results for keyset pagination. {@code nextCursor} is null when there are no more items.
 */
public record CursorPage<T>(List<T> items, String nextCursor) {
}
