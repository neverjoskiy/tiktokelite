package com.neverocheg.elite;

import android.content.Context;
import android.content.SharedPreferences;

/** Hand TikTok an empty application list instead of the phone's own. */
public final class Packages {

    private Packages() {}

    static final String KEY = "block_apps";

    private static volatile Boolean cached;

    public static boolean blocks() {
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

    public static void setBlocked(boolean on) {
        cached = on;
        try {
            Context context = Margy.context();
            if (context == null) return;
            SharedPreferences prefs = context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY, on).apply();
        } catch (Throwable ignored) {
        }
    }

    public static java.util.List installedPackages(android.content.pm.PackageManager pm, int flags) {
        if (blocks()) return new java.util.ArrayList();
        return pm.getInstalledPackages(flags);
    }

    public static java.util.List installedApplications(android.content.pm.PackageManager pm, int flags) {
        if (blocks()) return new java.util.ArrayList();
        return pm.getInstalledApplications(flags);
    }
}
