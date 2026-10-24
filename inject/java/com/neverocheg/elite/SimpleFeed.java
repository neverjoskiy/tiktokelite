package com.neverocheg.elite;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The simplified feed: less on top of the video.
 *
 * Gone are the For You / Following tabs above (the search button stays), the
 * author's picture with its follow button, the line naming the sound and the
 * spinning record at the bottom right. The buttons on the right are a little
 * smaller and the caption is smaller and shorter. The idea is the TikTok You
 * mod's.
 *
 * Everything is found by the resource names TikTok 47.2.41 gives these views,
 * on layout, and what was changed is remembered so that switching it off puts
 * every view back the way it was.
 */
final class SimpleFeed {

    private SimpleFeed() {}

    static final String KEY = "simple_feed";
    private static final float RAIL = 0.84f, CAPTION = 0.88f;
    private static final int WATCHED = 0x7e0c033a;

    private static volatile Boolean on;
    private static final Map<View, Integer> hidden = new WeakHashMap<>();
    private static final Map<View, float[]> shrunk = new WeakHashMap<>();
    private static final Map<TextView, float[]> written = new WeakHashMap<>();
    private static long last;

    static boolean isOn() {
        Boolean known = on;
        if (known != null) return known;
        Context c = Margy.context();
        if (c == null) return false;
        known = c.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
        on = known;
        return known;
    }

    static void set(Context context, boolean value) {
        on = value;
        context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply();
        if (!value) restore();
    }

    /** Watch the main screen's layouts; called as each screen comes up. */
    static void attach(Activity activity) {
        if (activity == null || !activity.getClass().getName().equals("com.ss.android.ugc.aweme.main.MainActivity")) return;
        final View root = activity.getWindow().getDecorView();
        if (Boolean.TRUE.equals(root.getTag(WATCHED))) return;
        root.setTag(WATCHED, Boolean.TRUE);
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                if (!isOn()) return;
                long now = android.os.SystemClock.uptimeMillis();
                if (now - last < 120) { root.removeCallbacks(again); root.postDelayed(again, 130); return; }
                last = now;
                pass(root);
            }
            private final Runnable again = () -> { if (isOn()) { last = android.os.SystemClock.uptimeMillis(); pass(root); } };
        });
        if (isOn()) root.post(() -> pass(root));
    }

    private static void pass(View root) {
        long probe = Probe.start();
        try {
            walk(root, 0, new int[]{4000});
        } catch (Throwable error) {
            Diary.note("simple feed: " + error);
        } finally {
            Probe.stop(Probe.DIM_PASS, probe);
        }
    }

    private static void walk(View view, int depth, int[] budget) {
        if (view == null || depth > 48 || budget[0]-- <= 0) return;
        if (view.getVisibility() == View.GONE && !hidden.containsKey(view)) return;
        String name = Names.of(view);
        if (name.length() > 0) {
            switch (name) {
                case "uvh": top(view); return;              // the row of feed tabs
                case "title_shadow": hide(view, View.INVISIBLE); return;
                case "i_a": hide(up(view, 2), View.GONE); return;      // author picture and follow
                case "o6r": hide(up(view, 1), View.GONE); return;      // "♫ sound name"
                case "pnv": hide(up(view, 1), View.INVISIBLE); return; // the spinning record
                case "videomusiccoverblock": hide(view, View.INVISIBLE); return;
                case "desc": caption(view); return;
                case "lje": shrink(view, RAIL, 1f, 1f); break;         // the column of buttons
                case "bqx": shrink(view, CAPTION, 0f, 1f); break;      // name, caption and the rest
                default: break;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i), depth + 1, budget);
        }
    }

    /** The tabs go; search stays, and the LIVE button while the eye stands in it. */
    private static void top(View row) {
        if (!(row instanceof ViewGroup)) { hide(row, View.INVISIBLE); return; }
        ViewGroup group = (ViewGroup) row;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            String name = Names.of(child);
            // search stays, and LIVE while the eye stands in it
            if ("k_5".equals(name) || "k_2".equals(name) && Dim.isConfigured()) { show(child); continue; }
            hide(child, View.INVISIBLE);
        }
    }

    private static View up(View view, int steps) {
        View at = view;
        for (int i = 0; i < steps && at != null; i++) at = at.getParent() instanceof View ? (View) at.getParent() : null;
        return at;
    }

    private static void hide(View view, int how) {
        if (view == null) return;
        synchronized (hidden) { if (!hidden.containsKey(view)) hidden.put(view, view.getVisibility()); }
        if (view.getVisibility() != how) view.setVisibility(how);
    }

    private static void show(View view) {
        Integer was;
        synchronized (hidden) { was = hidden.remove(view); }
        if (was != null && view.getVisibility() != was) view.setVisibility(was);
    }

    private static void shrink(View view, float scale, float pivotX, float pivotY) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0) return;
        synchronized (shrunk) {
            if (!shrunk.containsKey(view)) shrunk.put(view, new float[]{view.getScaleX(), view.getScaleY(), view.getPivotX(), view.getPivotY()});
        }
        float px = view.getWidth() * pivotX, py = view.getHeight() * pivotY;
        if (view.getPivotX() != px) view.setPivotX(px);
        if (view.getPivotY() != py) view.setPivotY(py);
        if (view.getScaleX() != scale) view.setScaleX(scale);
        if (view.getScaleY() != scale) view.setScaleY(scale);
    }

    /** A smaller caption that stops at two lines. */
    private static void caption(View view) {
        if (!(view instanceof TextView)) return;
        TextView text = (TextView) view;
        float[] was;
        synchronized (written) {
            was = written.get(text);
            if (was == null) { was = new float[]{text.getTextSize(), text.getMaxLines()}; written.put(text, was); }
        }
        float size = was[0] * 0.9f;
        if (Math.abs(text.getTextSize() - size) > 0.5f && Math.abs(text.getTextSize() - was[0]) > 0.5f) {
            // TikTok set a size of its own since: that is the one to shrink
            was[0] = text.getTextSize();
            size = was[0] * 0.9f;
        }
        if (Math.abs(text.getTextSize() - size) > 0.5f) text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
        if (text.getMaxLines() > 2) text.setMaxLines(2);
    }

    /** Everything back as TikTok had it. */
    private static void restore() {
        java.util.List<Map.Entry<View, Integer>> views;
        synchronized (hidden) { views = new java.util.ArrayList<>(hidden.entrySet()); hidden.clear(); }
        for (Map.Entry<View, Integer> entry : views) {
            View view = entry.getKey();
            if (view != null) view.setVisibility(entry.getValue());
        }
        java.util.List<Map.Entry<View, float[]>> sizes;
        synchronized (shrunk) { sizes = new java.util.ArrayList<>(shrunk.entrySet()); shrunk.clear(); }
        for (Map.Entry<View, float[]> entry : sizes) {
            View view = entry.getKey();
            float[] was = entry.getValue();
            if (view == null) continue;
            view.setScaleX(was[0]); view.setScaleY(was[1]); view.setPivotX(was[2]); view.setPivotY(was[3]);
        }
        java.util.List<Map.Entry<TextView, float[]>> texts;
        synchronized (written) { texts = new java.util.ArrayList<>(written.entrySet()); written.clear(); }
        for (Map.Entry<TextView, float[]> entry : texts) {
            TextView text = entry.getKey();
            float[] was = entry.getValue();
            if (text == null) continue;
            text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, was[0]);
            text.setMaxLines((int) was[1]);
        }
    }
}
