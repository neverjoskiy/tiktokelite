package com.neverocheg.elite;

import android.content.Context;
import android.content.SharedPreferences;

/** Hide the read receipts sent from direct messages. */
public final class Read {

    private Read() {}

    static final String KEY = "hide_read";

    private static volatile Boolean cached;

    public static boolean hides() {
        Boolean known = cached;
        if (known != null) return known;
        try {
            Context context = Margy.context();
            if (context == null) return false;
            boolean on = context.getSharedPreferences(Margy.PREFS,
                    Context.MODE_PRIVATE).getBoolean(KEY, false);
            cached = on;
            return on;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setHidden(boolean on) {
        cached = on;
        try {
            Context context = Margy.context();
            if (context == null) return;
            SharedPreferences prefs = context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY, on).apply();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Asked by TikTok's read-state sender before it posts the request: false
     * swallows it. The request body is already built by then and only goes to
     * the server, so not sending is the whole of it; the local unread counters
     * live on an earlier path and keep working.
     */
    public static boolean sends() {
        return !hides();
    }
}
