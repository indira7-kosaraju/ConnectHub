package com.connecthub.notification;

/** Mirrors the PostgreSQL {@code notification_type} enum. */
public enum NotificationType {
    reaction,
    comment,
    reply,
    circle_request,
    circle_approved,
    message;

    /** Key in {@code users.notification_prefs} that controls this type. */
    public String prefKey() {
        return switch (this) {
            case reaction -> "reactions";
            case comment -> "comments";
            case reply -> "replies";
            case circle_request, circle_approved -> "circles";
            case message -> "messages";
        };
    }
}
