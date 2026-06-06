package com.cyclinglab.platform.review;

import java.util.Locale;

/** Scope of a review: a single ISO week or a longer training phase. */
public enum ReviewScope {
    WEEK,
    PHASE;

    public static ReviewScope fromCode(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("scope is required");
        }
        try {
            return ReviewScope.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown review scope: " + s);
        }
    }
}