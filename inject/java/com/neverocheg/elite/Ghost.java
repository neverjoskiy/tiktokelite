package com.neverocheg.elite;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

/**
 * Ghost mode: TikTok must not learn that you are online, that you read a chat,
 * that you are typing, or that you watched a story or visited a profile.
 *
 * Every flag is kept on its own, each switched independently. The main switch
 * gates everything; when it is off none of the others do anything.
 *
 * "Online" needs an app restart after switching: the activity-status service is
 * initialised once at startup and the patch lands on its lazy cache. Everything
 * else applies as soon as the setting changes.
 */
public final class Ghost {

    static final String KEY = "ghost";
    static final String ONLINE = "ghost_online", READ = "ghost_read", TYPING = "ghost_typing",
            STORIES = "ghost_stories", PROFILES = "ghost_profiles";

    private static GhostMode mode;
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static Activity shown;
    private static final Runnable tick = new Runnable() {
        @Override public void run() {
            if (shown == null) return;
            apply();
            main.postDelayed(this, 1000L);
        }
    };

    private Ghost() {}

    private static SharedPreferences prefs() {
        Context context = Margy.context();
        return context == null ? null : context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
    }

    static boolean on(String key) {
        SharedPreferences prefs = prefs();
        return prefs != null && prefs.getBoolean(key, false);
    }

    static void set(String key, boolean value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(key, value).apply();
        main.post(Ghost::apply);
    }

    static void start() {
        try {
            mode = new GhostMode(Ghost::on, message -> Diary.note("ghost: " + message));
            mode.bootstrap();
        } catch (Throwable error) {
            Diary.note("ghost start: " + error);
        }
    }

    static void resumed(Activity activity) {
        shown = activity;
        main.removeCallbacks(tick);
        apply();
        main.postDelayed(tick, 1000L);
    }

    static void paused(Activity activity) {
        if (shown == activity) {
            shown = null;
            main.removeCallbacks(tick);
        }
    }

    static void apply() {
        try {
            if (mode != null) mode.apply();
        } catch (Throwable error) {
            Diary.note("ghost apply: " + error);
        }
    }

    static String report() {
        return mode == null ? "не запущен" : mode.report();
    }
}
