package com.connecthub.user;

/**
 * Mirrors the PostgreSQL {@code auth_provider} enum. Constants are lowercase because Hibernate
 * binds {@code name()} directly to the database enum label.
 */
public enum AuthProvider {
    local,
    google
}
