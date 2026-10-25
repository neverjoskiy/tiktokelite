package com.neverocheg.elite;

import android.content.Context;
import android.content.SharedPreferences;

import com.ss.android.ugc.aweme.feed.model.Aweme;

/**
 * Leaving old posts out of the feed: nothing older than a few hours or days.
 *
 * Counted back from now, from six hours to a week. Fixed calendar dates were
 * tried and dropped: the feed is made of recent posts, and asking it for only
 * last year's left TikTok with not one video to show. A person's own posts,
 * live rooms and adverts are never left out.
 */
final class FeedDates {

    private FeedDates() {}

    /** The ages on offer, in hours. Zero is off. */
    static final int[] HOURS = {0, 6, 12, 24, 48, 72, 120, 168};

    private static final String KEY = "date_filter_hours";
    private static volatile int hours = -1;

    static int hours() {
        int known = hours;
        if (known >= 0) return known;
        SharedPreferences p = prefs();
        if (p == null) return 0;
        int saved = p.getInt(KEY, 0);
        known = 0;
        for (int one : HOURS) if (one == saved) known = saved;
        hours = known;
        return known;
    }

    static void setHours(int value) {
        int chosen = 0;
        for (int one : HOURS) if (one == value) chosen = value;
        hours = chosen;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(KEY, chosen).apply();
    }

    static boolean active() { return hours() > 0; }

    /** Whether a post made at `created` (seconds) is older than allowed. */
    static boolean leavesOut(long created) {
        int limit = hours();
        if (limit <= 0 || created <= 0) return false;
        if (created > 100000000000L) created /= 1000L;
        return System.currentTimeMillis() / 1000L - created > limit * 3600L;
    }

    /** A post's age in seconds, read through the model's own getter; zero when unknown. */
    static long created(Object item) {
        Object post = item instanceof Aweme ? item : NativeRead.get(item, "getAweme");
        if (post == null) return 0;
        return NativeRead.number(NativeRead.get(post, "getCreateTime"));
    }

    /** "6 hours", "3 days", or "off". */
    static String describe(int value) {
        if (value <= 0) return Text.DATE_FILTER_OFF;
        if (value < 24) return Text.dateFilterRecent(value, 0);
        return Text.dateFilterRecent(value / 24, 1);
    }

    private static SharedPreferences prefs() {
        Context c = Margy.context();
        return c == null ? null : c.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
    }
}
