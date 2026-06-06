package com.cyclinglab.platform.plan;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure helpers for ISO 8601 week arithmetic. Stateless and side-effect free so
 * it can be unit-tested without Spring.
 */
public final class IsoWeek {

    private static final WeekFields ISO = WeekFields.ISO;

    private IsoWeek() {}

    /**
     * Returns the seven {@link LocalDate}s (Mon..Sun) for the given ISO
     * {@code year + week}. ISO weeks always contain Jan 4, so we use that as
     * a fixed reference point and walk back to the Monday of week 1.
     */
    public static List<LocalDate> datesOf(int isoYear, int isoWeek) {
        if (isoWeek < 1 || isoWeek > 53) {
            throw new IllegalArgumentException("isoWeek out of range: " + isoWeek);
        }
        // Jan 4 is always in ISO week 1 of the given year.
        LocalDate jan4 = LocalDate.of(isoYear, 1, 4);
        LocalDate weekOneMonday = jan4.with(DayOfWeek.MONDAY);
        LocalDate weekMonday = weekOneMonday.plusWeeks(isoWeek - 1L);
        // Sanity check: the resulting date must still be in the same ISO
        // week-based year. This catches "2026 week 53" inputs that don't
        // actually exist (2026 only has 52 weeks).
        int backYear = weekMonday.get(ISO.weekBasedYear());
        if (backYear != isoYear) {
            throw new IllegalArgumentException(
                "isoWeek " + isoWeek + " does not exist in year " + isoYear
            );
        }
        List<LocalDate> out = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            out.add(weekMonday.plusDays(i));
        }
        return out;
    }

    /** Weekday in ISO numbering: Mon=1 .. Sun=7. */
    public static int weekday(LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    /** Convenience: returns the ISO {@code (year, week)} pair for a date. */
    public static int[] of(LocalDate date) {
        int y = date.get(ISO.weekBasedYear());
        int w = date.get(ISO.weekOfWeekBasedYear());
        return new int[] { y, w };
    }

    /** Reference to the standard ISO WeekFields, useful for callers/tests. */
    public static WeekFields fields() {
        return ISO;
    }

    /** Suppresses the unused-locale warning in {@link #fields()}. */
    @SuppressWarnings("unused")
    private static final Locale ROOT = Locale.ROOT;
}