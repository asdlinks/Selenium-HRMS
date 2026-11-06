package com.mywehr.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Date formatting for the three representations this app mixes:
 *
 *   - native <input type="date"> typed as mm/dd/yyyy in a Chrome en-US profile
 *   - grid / calendar text such as "26 Sept 2026"
 *   - holiday cards such as "Fri Oct 02 2026"
 *
 * Tests build dates relative to today rather than hardcoding them, so the
 * suite stays green tomorrow.
 */
public final class DateUtils {

    /** What a native date input expects when typed into, in an en-US profile. */
    public static final DateTimeFormatter BROWSER_DATE_INPUT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);

    /** ISO form, used when a date is set through the DOM value property. */
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Grid rendering, e.g. "26 Sept 2026". */
    public static final DateTimeFormatter GRID_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private DateUtils() {
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate daysFromToday(int days) {
        return LocalDate.now().plusDays(days);
    }

    public static String forDateInput(LocalDate date) {
        return date.format(BROWSER_DATE_INPUT);
    }

    public static String forDateInput(int daysFromToday) {
        return forDateInput(daysFromToday(daysFromToday));
    }

    public static String iso(LocalDate date) {
        return date.format(ISO_DATE);
    }

    public static String iso(int daysFromToday) {
        return iso(daysFromToday(daysFromToday));
    }

    /**
     * A future weekday, skipping Saturday and Sunday.
     *
     * Leave applied on a weekly off behaves differently (the app treats those
     * days as non-working), so tests that assert on leave-day arithmetic need
     * a working day to be meaningful.
     */
    public static LocalDate nextWorkingDay(int minimumDaysAhead) {
        LocalDate candidate = daysFromToday(minimumDaysAhead);
        while (candidate.getDayOfWeek().getValue() >= 6) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    /** Parses the mm/dd/yyyy form the leave drawer is filled with. */
    public static LocalDate fromDateInput(String value) {
        return LocalDate.parse(value, BROWSER_DATE_INPUT);
    }

    /**
     * Day and abbreviated month as the grid shows them, e.g. "28 Sep".
     *
     * Deliberately three letters: the app renders September as "Sept" while
     * the JDK says "Sep", and "28 Sep" is a prefix of both.
     */
    public static String gridDayMonth(LocalDate date) {
        return date.getDayOfMonth() + " "
                + date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
                        .substring(0, 3);
    }

    public static String currentYear() {
        return String.valueOf(LocalDate.now().getYear());
    }

    /** Compact stamp used to keep generated test data unique. */
    public static String uniqueStamp() {
        return String.valueOf(System.currentTimeMillis() % 100000000L);
    }
}
