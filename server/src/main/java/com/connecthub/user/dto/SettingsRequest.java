package com.connecthub.user.dto;

import java.util.Map;

/** Partial update of account settings: null fields are unchanged. */
public record SettingsRequest(Boolean privateProfile, Map<String, Boolean> notificationPrefs) {
}
