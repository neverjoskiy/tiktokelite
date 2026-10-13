package com.neverocheg.elite;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Short counts: 100,8K instead of 100,8 тыс.
 *
 * TikTok spells compact numbers out in the phone's language, which in Russian
 * and Ukrainian is a word where English has a letter. Every count on screen
 * is found where it is drawn and shortened the English way, whatever the
 * language around it: thousands are K, millions M, billions B, and the number
 * itself is left exactly as it was, comma and all.
 *
 * Nothing is moved and nothing is replaced, and what TikTok writes next is
 * shortened when it arrives: the text a view had is kept on the view, so a
 * count TikTok writes again is shortened again, and a shortened count is
 * left alone.
 */
final class Counts {

    private Counts() {}

    static final String KEY = "short_counts";

    private static volatile Boolean cached;

    public static boolean isEnabled() {
        Boolean known = cached;
        if (known != null) return known;
        try {
            Context context = Margy.context();
            if (context == null) return true;  // too early to know; not remembered
            boolean on = context.getSharedPreferences(Margy.PREFS,
                    Context.MODE_PRIVATE).getBoolean(KEY, true);
            cached = on;
            return on;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static void setEnabled(boolean enabled) {
        cached = enabled;
        try {
            Context context = Margy.context();
            if (context == null) return;
            SharedPreferences prefs = context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY, enabled).apply();
            android.app.Activity here = Watch.here();
            if (here == null) return;
            View root = here.getWindow() == null ? null : here.getWindow().getDecorView();
            if (root == null) return;
            if (enabled) sweep(root);
            else unshorten(root);
        } catch (Throwable ignored) {
        }
    }

    /** Put back what was shortened, for the switch being turned off. */
    private static void unshorten(View view) {
        if (view == null) return;
        try {
            if (view instanceof TextView) {
                TextView text = (TextView) view;
                Object was = text.getTag(WAS);
                if (was instanceof String) {
                    CharSequence shown = text.getText();
                    String shorted = format((String) was);
                    if (shorted != null && shown != null && shown.toString().equals(shorted)) {
                        text.setText((String) was);
                    }
                    text.setTag(WAS, null);
                }
                return;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) unshorten(group.getChildAt(i));
            }
        } catch (Throwable ignored) {
        }
    }

    /** A number and a word for thousand, million or billion, in any of our languages. */
    private static final Pattern COMPACT = Pattern.compile(
            "^(\\d{1,3}(?:[.,]\\d+)?)[\\s\\u00A0]+([A-Za-z\\u0400-\\u045F\\u0401\\u0451.]+)$");

    /** What was on a view before it was shortened, so a shortened count is left alone. */
    private static final int WAS = 0x436E7431;

    /** The shortened form of a compact count, or null when it is short already. */
    static String format(String text) {
        if (text == null) return null;
        Matcher match = COMPACT.matcher(text.trim());
        if (!match.matches()) return null;
        String word = match.group(2).replace(".", "").toLowerCase(java.util.Locale.ROOT);
        String letter;
        if (word.equals("тыс") || word.equals("тис")) letter = "K";
        else if (word.equals("млн")) letter = "M";
        else if (word.equals("млрд")) letter = "B";
        else return null;
        return match.group(1) + letter;
    }

    static void shorten(TextView view) {
        if (view == null) return;
        try {
            CharSequence shown = view.getText();
            String text = shown == null ? "" : shown.toString();
            Object was = view.getTag(WAS);
            if (was instanceof String) {
                String original = (String) was;
                String shorted = format(original);
                if (shorted != null && text.equals(shorted)) return;  // already shortened
                if (text.equals(original)) {
                    if (shorted != null) view.setText(shorted);
                    return;
                }
                // TikTok wrote something new: look at it below
            }
            String shorted = format(text);
            if (shorted != null && !shorted.equals(text)) {
                view.setTag(WAS, text);
                view.setText(shorted);
            }
        } catch (Throwable ignored) {
        }
    }

    /** How often a screen is walked again while things move about. */
    private static final long QUIET = 400L;

    private static final int SEEN = 0x436E7432;

    private static WeakReference<ViewTreeObserver> watched = new WeakReference<ViewTreeObserver>(null);

    static void attach(Activity activity) {
        if (activity == null) return;
        try {
            // the mod's own screens have no counts worth shortening
            if (activity.getClass().getName().startsWith("com.neverocheg.elite.")) return;
            View root = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
            if (root == null) return;
            ViewTreeObserver next = root.getViewTreeObserver();
            ViewTreeObserver old = watched.get();
            if (next == old) return;
            if (old != null && old.isAlive()) old.removeOnGlobalLayoutListener(layout);
            watched = new WeakReference<ViewTreeObserver>(next);
            if (next.isAlive()) next.addOnGlobalLayoutListener(layout);
        } catch (Throwable error) {
            Diary.detail("counts: " + error);
        }
    }

    private static final ViewTreeObserver.OnGlobalLayoutListener layout =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    try {
                        android.app.Activity here = Watch.here();
                        if (here == null || !isEnabled()) return;
                        View root = here.getWindow() == null ? null : here.getWindow().getDecorView();
                        if (root == null) return;
                        Object at = root.getTag(SEEN);
                        long now = android.os.SystemClock.uptimeMillis();
                        if (at instanceof Long && now - (Long) at < QUIET) return;
                        root.setTag(SEEN, now);
                        sweep(root);
                    } catch (Throwable ignored) {
                    }
                }
            };

    static void sweep(View root) {
        walk(root, 0, new int[]{4000});
    }

    private static void walk(View view, int depth, int[] left) {
        if (view == null || depth > 24 || left[0]-- <= 0) return;
        try {
            if (view.getVisibility() != View.VISIBLE) return;
            if (view instanceof TextView && !(view instanceof EditText)
                    && !(view instanceof android.widget.Button)) {
                shorten((TextView) view);
                return;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i), depth + 1, left);
            }
        } catch (Throwable ignored) {
        }
    }
}
