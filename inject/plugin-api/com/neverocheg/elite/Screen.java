package com.neverocheg.elite;

import android.app.Activity;

/** Declared for the api build. The real one is in the patched apk. */
public final class Screen {
    private Screen() {}

    public static Activity now() {
        return null;
    }

    public static void offer(String label, Runnable action) {
    }

    public static void offer(String label, float where, Runnable action) {
    }

    public static void progress(String what, int percent) {
    }

    public static void progressGone() {
    }
}
