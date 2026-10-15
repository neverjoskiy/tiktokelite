package com.neverocheg.elite;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Videos already watched, remembered so the feed can skip them.
 *
 * Marked when a video starts playing, kept in memory and in one preference
 * string, oldest dropped past the cap. Recording stops when the watch
 * history itself is switched off; filtering runs only on its own switch,
 * which is off unless turned on.
 */
public final class Watched {

    private Watched() {}

    static final String KEY = "hide_watched";
    static final String SEEN = "watched_ids";
    static final String NO_HISTORY = "no_history";

    private static final int CAP = 3000;
    private static final long SAVE_EVERY = 30000L;

    private static final java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<String>();
    private static volatile boolean loaded;
    private static volatile Boolean cached;
    private static volatile Boolean historyCached;
    private static long savedAt;

    private static SharedPreferences prefs() {
        try {
            Context context = Margy.context();
            if (context == null) return null;
            return context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean hides() {
        Boolean known = cached;
        if (known != null) return known;
        try {
            SharedPreferences prefs = prefs();
            boolean on = prefs != null && prefs.getBoolean(KEY, false);
            cached = on;
            return on;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setHidden(boolean on) {
        cached = on;
        try {
            SharedPreferences prefs = prefs();
            if (prefs == null) return;
            prefs.edit().putBoolean(KEY, on).apply();
        } catch (Throwable ignored) {
        }
    }

    public static boolean historyOff() {
        Boolean known = historyCached;
        if (known != null) return known;
        try {
            SharedPreferences prefs = prefs();
            boolean on = prefs != null && prefs.getBoolean(NO_HISTORY, false);
            historyCached = on;
            return on;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setHistoryOff(boolean on) {
        historyCached = on;
        try {
            SharedPreferences prefs = prefs();
            if (prefs == null) return;
            prefs.edit().putBoolean(NO_HISTORY, on).apply();
        } catch (Throwable ignored) {
        }
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            SharedPreferences prefs = prefs();
            if (prefs == null) return;
            String saved = prefs.getString(SEEN, "");
            if (saved == null || saved.isEmpty()) return;
            for (String aid : saved.split(",")) {
                if (!aid.isEmpty()) ids.add(aid);
            }
            while (ids.size() > CAP) ids.remove(ids.iterator().next());
        } catch (Throwable ignored) {
        }
    }

    private static void save() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - savedAt < SAVE_EVERY) return;
        savedAt = now;
        try {
            SharedPreferences prefs = prefs();
            if (prefs == null) return;
            StringBuilder out = new StringBuilder();
            for (String aid : ids) {
                if (out.length() > 0) out.append(',');
                out.append(aid);
            }
            prefs.edit().putString(SEEN, out.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    public static boolean seen(String aid) {
        if (aid == null || aid.isEmpty()) return false;
        synchronized (ids) {
            load();
            return ids.contains(aid);
        }
    }

    /** A video started playing: remember it unless the history is switched off. */
    public static void saw(String aid) {
        if (aid == null || aid.isEmpty() || historyOff()) return;
        synchronized (ids) {
            load();
            if (!ids.add(aid)) return;
            while (ids.size() > CAP) ids.remove(ids.iterator().next());
            save();
        }
    }
}
