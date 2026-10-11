package com.neverocheg.elite;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.View;
import android.view.Window;

/**
 * HDR video at the brightness of everything else.
 *
 * The video still plays. What goes away is the extra brightness: a headroom
 * of one, which means "no more than white".
 *
 * Asking the window is not enough, and that is why this looked switched off
 * when it was on. TikTok decodes video in its own native player -- there is
 * not one call to `MediaCodec.configure` in the whole apk -- and hands the
 * frames to a surface of its own. A surface is composited on its own layer,
 * where the window's headroom does not reach, so the layer goes on asking the
 * display for the bright end while everything around it is dimmed to make
 * room.
 *
 * So each surface is told as well, and told again when the feed puts a new one
 * on screen. That needs Android 15 for a surface and Android 14 for the older
 * way of saying the same thing. Below those there is nothing an app can ask.
 */
public final class Hdr {

    static final String KEY = "no_hdr";

    /** No more brightness than anything else on screen. */
    private static final float FLAT = 1.0f;

    private static volatile Boolean cached;

    private Hdr() {}

    public static boolean isEnabled() {
        Boolean known = cached;
        if (known != null) return known;
        SharedPreferences prefs = prefs();
        if (prefs == null) return false;
        boolean on = prefs.getBoolean(KEY, false);
        cached = on;
        return on;
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        SharedPreferences prefs = prefs();
        if (prefs != null) prefs.edit().putBoolean(KEY, enabled).apply();
    }

    /** Whether this phone can actually be told to flatten it. */
    public static boolean reachable() {
        return Build.VERSION.SDK_INT >= 34;
    }

    private static SharedPreferences prefs() {
        Context context = Margy.context();
        if (context == null) return null;
        try {
            return context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ------------------------------------------------------------ the window


    /** Set on any window this has touched, so turning it off can undo it. */
    private static volatile boolean touched;

    /**
     * Flatten HDR on this window, or leave the window alone.
     *
     * Leaving it alone is the point of the second half. Asking for
     * COLOR_MODE_HDR when the setting is off is not "as it was": it puts the
     * window into a mode TikTok never asked for, and every ordinary colour in
     * it gets mapped through that -- which is why screens went pale and white
     * came out tinted.
     */
    public static void apply(Activity activity) {
        if (activity == null) return;
        try {
            Window window = activity.getWindow();
            if (window == null) return;
            if (!isEnabled()) {
                if (touched && Build.VERSION.SDK_INT >= 35) {
                    // zero is "whatever you like", which is where it started
                    window.setDesiredHdrHeadroom(0f);
                }
                return;
            }

            touched = true;
            if (Build.VERSION.SDK_INT >= 35) {
                window.setDesiredHdrHeadroom(FLAT);
            }
            window.setColorMode(ActivityInfo.COLOR_MODE_DEFAULT);
            flatten(window.getDecorView());
            watch(window.getDecorView());
        } catch (Throwable error) {
            Diary.note("hdr: " + error);
        }
    }

    // ------------------------------------------------------- the surfaces

    /** Our own tag, so a screen is only watched once. */
    private static final int WATCHED = 0x4D617248;

    /** How often the tree is walked again while things move about. */
    private static final long QUIET = 400;

    private static volatile long walked;

    /**
     * Tell every surface on this screen to stay at the plain brightness.
     *
     * Kept shallow and cheap: the walk stops at views that cannot hold one,
     * and it is only ever done while the setting is on.
     */
    static void flatten(View view) {
        if (view == null || Build.VERSION.SDK_INT < 34) return;
        try {
            if (view instanceof android.view.SurfaceView) {
                android.view.SurfaceView surface = (android.view.SurfaceView) view;
                if (Build.VERSION.SDK_INT >= 35) {
                    surface.setDesiredHdrHeadroom(FLAT);
                } else {
                    android.view.SurfaceControl control = surface.getSurfaceControl();
                    if (control != null && control.isValid()) {
                        new android.view.SurfaceControl.Transaction()
                                .setExtendedRangeBrightness(control, FLAT, FLAT)
                                .apply();
                    }
                }
                return;
            }
            if (view instanceof android.view.ViewGroup) {
                android.view.ViewGroup group = (android.view.ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    flatten(group.getChildAt(i));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Walk the screen again when it changes, because the feed recycles.
     *
     * One listener per window, throttled: a video that scrolls into view
     * brings a surface that has never been told anything.
     */
    private static void watch(final View decor) {
        if (decor == null || Build.VERSION.SDK_INT < 34) return;
        try {
            if (decor.getTag(WATCHED) != null) return;
            decor.setTag(WATCHED, Boolean.TRUE);
            decor.getViewTreeObserver().addOnGlobalLayoutListener(
                    new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (!isEnabled()) return;
                            long now = android.os.SystemClock.uptimeMillis();
                            if (now - walked < QUIET) return;
                            walked = now;
                            flatten(decor);
                        }
                    });
        } catch (Throwable error) {
            Diary.note("hdr: " + error);
        }
    }


}
