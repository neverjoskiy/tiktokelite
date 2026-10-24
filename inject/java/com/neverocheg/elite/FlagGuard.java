package com.neverocheg.elite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

/**
 * The way back from a flag that stops TikTok starting.
 *
 * A developer's answer for a flag is applied before the first screen is drawn,
 * and the only place to take it back is a screen inside the app. One wrong
 * answer could leave nothing to do but clear the app's data. So every start
 * with such answers in force is counted, and the count is wiped once the app
 * has stayed up for a while; three starts in a row that did not last that long
 * and the answers are left out, until somebody says to try them again.
 *
 * Only the main process counts. TikTok starts several others beside it, and
 * each would add one to a count that only the main one ever gets to wipe.
 *
 * And only a start somebody saw counts, or one that crashed. The main process
 * is also started in the background, for a push or a sync, and the system
 * ends it again in seconds; three of those used to hold the answers back as
 * if TikTok had fallen over three times ("flags: held back"), with no crash.
 */
final class FlagGuard {

    private FlagGuard() {}

    // not under the mod's own name: the settings screen watches those files
    // for changes, and this one changes by itself
    private static final String FILE = "flagguard_margyt";
    private static final String STARTS = "starts";
    private static final int TOO_MANY = 3;
    private static final long LASTED = 8000L;

    private static volatile SharedPreferences counting;
    private static volatile int countedFrom;
    private static volatile boolean counted;

    /** 0 not asked yet, 1 held back, 2 let through. One answer a process. */
    private static volatile int answer;

    /** Whether the developer's answers may be applied in this process. */
    static boolean allows() {
        int known = answer;
        if (known != 0) return known == 2;
        Context context = Margy.context();
        if (context == null) return false;
        try {
            final SharedPreferences prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
            int starts = prefs.getInt(STARTS, 0);
            if (starts >= TOO_MANY) {
                answer = 1;
                Diary.note("flags: held back, TikTok did not stay up with them");
                return false;
            }
            if (context.getPackageName().equals(processName())) {
                counting = prefs;
                countedFrom = starts;
                // a crash before any screen is up counts as well
                final Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                    count();
                    if (before != null) before.uncaughtException(thread, error);
                });
            }
            answer = 2;
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    /** A TikTok screen came up: this start counts, and is wiped if the app stays up. */
    static void shown() {
        final SharedPreferences prefs = counting;
        if (prefs == null || counted) return;
        count();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> prefs.edit().putInt(STARTS, 0).apply(), LASTED);
    }

    private static synchronized void count() {
        SharedPreferences prefs = counting;
        if (prefs == null || counted) return;
        counted = true;
        // written at once: what this guards against is the process dying
        try { prefs.edit().putInt(STARTS, countedFrom + 1).commit(); } catch (Throwable ignored) { }
    }

    static boolean heldBack() {
        return answer == 1;
    }

    /** Somebody changed the answers, or asked for them back: start counting again. */
    static void again() {
        answer = 2;
        Context context = Margy.context();
        if (context == null) return;
        try {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(STARTS, 0).apply();
        } catch (Throwable ignored) { }
    }

    private static String processName() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 28) return android.app.Application.getProcessName();
            return (String) Class.forName("android.app.ActivityThread")
                    .getMethod("currentProcessName").invoke(null);
        } catch (Throwable error) {
            return null;
        }
    }
}
