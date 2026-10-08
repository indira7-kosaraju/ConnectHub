package com.connecthub.moderation;

/** Mirrors the PostgreSQL {@code report_reason} enum. */
public enum ReportReason {
    spam,
    harassment,
    hate,
    nudity,
    violence,
    misinformation,
    self_harm,
    other
}
