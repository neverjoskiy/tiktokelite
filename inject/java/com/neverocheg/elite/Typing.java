package com.neverocheg.elite;

import android.content.Context;
import android.content.SharedPreferences;

/** Hide the typing status sent from direct messages. */
public final class Typing {

    private Typing() {}

    static final String KEY = "hide_typing";

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
     * Asked by TikTok's typing sender before it sends: false swallows the event.
     *
     * The sender answers nothing and takes a conversation id, so there is no
     * result to rebuild -- not sending is the whole of it.
     */
    public static boolean sends() {
        return !hides();
    }
}
