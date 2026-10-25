package com.neverocheg.elite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.app.Activity;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import java.lang.ref.WeakReference;

/**
 * The feed's progress bar as Material 3 Expressive draws one.
 *
 * The part already watched is a wave in the accent colour, drawn after the
 * official Compose tokens: a 4dp round stroke, a 40dp wavelength, and the
 * wave lying inside a 10dp container so nothing reaches outside the line
 * TikTok had already booked. What this adds over both TikTok and the
 * official default is the part the brief asked for: the wave is barely
 * there while the video starts and both its height and its pace grow as
 * the video is watched, so the last fifth of a video visibly hurries.
 *
 * Only the picture of TikTok's bar is swapped. The bar itself, its
 * dragging and its progress all keep working; putting TikTok's own picture
 * back is one call, and the switch in the settings goes through here first.
 *
 * Off unless switched on. Not built for Lite.
 */
final class WavySeek {

    private WavySeek() {}

    public static final String KEY = "wavy_seek";

    private static final int WATCHED = 0x4D617257;  // "MarW"
    private static WeakReference<SeekBar> bar = new WeakReference<>(null);
    private static Drawable original;
    private static long looked;

    private static volatile Boolean cached;

    static void reloadSettings() {
        cached = null;
        // the next look at the screen will read the switch again
        looked = 0;
    }

    public static boolean enabled() {
        Boolean known = cached;
        if (known != null) return known.booleanValue();
        Context context = Margy.context();
        if (context == null) return false;
        boolean on;
        try {
            on = context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE)
                    .getBoolean(KEY, false);
        } catch (Throwable ignored) {
            return false;
        }
        cached = Boolean.valueOf(on);
        return on;
    }

    public static void setEnabled(boolean on) {
        cached = Boolean.valueOf(on);
        try {
            Context context = Margy.context();
            if (context != null) {
                context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY, on).apply();
            }
        } catch (Throwable ignored) {
        }
    }

    static void attach(Activity activity) {
        try {
            if (activity == null) return;
            String name = activity.getClass().getName();
            if (!name.endsWith(".MainActivity") && !name.endsWith(".SplashActivity")) return;
            final View root = activity.getWindow().getDecorView();
            if (Boolean.TRUE.equals(root.getTag(WATCHED))) {
                dress(root);
                return;
            }
            root.setTag(WATCHED, Boolean.TRUE);
            root.getViewTreeObserver().addOnGlobalLayoutListener(() -> dress(root));
            root.post(() -> dress(root));
        } catch (Throwable error) {
            Diary.note("wavy seek: " + error);
        }
    }

    private static void dress(View root) {
        try {
            SeekBar known = bar.get();
            if (known != null && (!known.isAttachedToWindow() || known.getRootView() != root)) {
                bar = new WeakReference<SeekBar>(null);
                known = null;
            }
            if (!enabled()) {
                if (known != null && known.getProgressDrawable() instanceof Wave && original != null) {
                    known.setProgressDrawable(original);
                }
                return;
            }            if (known == null) {
                long now = SystemClock.uptimeMillis();
                if (now - looked < 700) return;
                looked = now;
                View found = find(root, 0, new int[]{3000});
                if (!(found instanceof SeekBar)) return;
                known = (SeekBar) found;
                bar = new WeakReference<>(known);
            }
            if (!(known.getProgressDrawable() instanceof Wave)) {
                original = known.getProgressDrawable();
                known.setProgressDrawable(new Wave(known));
            }
        } catch (Throwable error) {
            Diary.note("wavy seek: " + error);
        }
    }

    /** The feed's bar, and how TikTok 47.2 names it inside the player. */
    private static View find(View view, int depth, int[] budget) {
        if (view == null || depth > 40 || budget[0]-- <= 0) return null;
        if (view instanceof SeekBar && "vpz".equals(Names.of(view))) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), depth + 1, budget);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * The wave itself. The numbers are Material 3 Expressive's: a 4dp stroke,
     * a 3dp amplitude and a 40dp wavelength -- and the container is 10dp so a
     * 3dp wave on a 4dp line is all the room there is, and nothing protrudes.
     */
    private static final class Wave extends Drawable {
        private final WeakReference<SeekBar> owner;
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float unit;
        /** Amplitude and phase as they are this frame, for the smooth approach. */
        private float amplitude, phase;
        /** Where the amplitude was last told to head, so a pause still settles it flat. */
        private float wanted;
        private long last;

        Wave(SeekBar seek) {
            owner = new WeakReference<>(seek);
            unit = seek.getResources().getDisplayMetrics().density;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
        }

        /** Whether the video is playing: TikTok keeps its answer in `getMPauseStatus`. */
        private java.lang.reflect.Method pausedAsk;
        private boolean asked;

        private boolean playing(SeekBar seek) {
            try {
                if (!asked) {
                    asked = true;
                    try {
                        pausedAsk = seek.getClass().getMethod("getMPauseStatus");
                    } catch (Throwable ignored) {
                    }
                }
                if (pausedAsk == null) return !seek.isPressed();
                return !Boolean.TRUE.equals(pausedAsk.invoke(seek)) && !seek.isPressed();
            } catch (Throwable ignored) {
                return !seek.isPressed();
            }
        }

        /**
         * The amplitude the brief asks for, a share of the official 3dp.
         *
         * Quiet at the start of a video -- a quarter of the way -- then a
         * climb that is gentle through the middle and steepest near the end,
         * where the whole line is still a wave and not a blur. The easing is
         * the square of how far the video has gone, which keeps the first
         * half quiet and spends the last quarter on the visible hurry.
         */
        private static float amplitudeShare(float progress) {
            return 0.45f + 0.55f * Math.max(0f, Math.min(1f, progress)) *
                    Math.max(0f, Math.min(1f, progress));
        }

        /** How fast the wave travels, in shares of one wavelength a second. */
        private static float speedShare(float progress) {
            return 0.8f + 1.4f * Math.max(0f, Math.min(1f, progress));
        }

        @Override public void draw(Canvas canvas) {
            SeekBar seek = owner.get();
            if (seek == null) return;
            android.graphics.Rect box = getBounds();
            float left = box.left, right = box.right, cy = box.exactCenterY();
            int max = Math.max(1, seek.getMax());
            float progress = Math.max(0f, Math.min(1f, seek.getProgress() / (float) max));
            long now = SystemClock.uptimeMillis();
            float dt = last == 0 ? 0f : Math.min(0.05f, (now - last) / 1000f);
            last = now;
            boolean moving = playing(seek);
            float unit3 = unit * 3f;

            // the wave grows as the video is watched and settles flat on a pause
            wanted = moving ? amplitudeShare(progress) : 0f;
            amplitude += (wanted - amplitude) * Math.min(1f, dt * 8f);
            float length = unit * 40f;
            if (amplitude > 0.01f) phase = (phase + dt * length * speedShare(progress)) % length;
            int accent = Accent.colour();
            float thick = unit * 4f, gap = unit * 4f;

            // what is left: a faint straight line to the end of the video
            float at = left + (right - left) * progress;
            line.setStrokeWidth(thick);
            line.setColor((accent & 0xFFFFFF) | 0x40000000);
            float restFrom = Math.min(right - thick / 2, at + gap);
            if (restFrom < right - thick / 2) {
                canvas.drawLine(restFrom, cy, right - thick / 2, cy, line);
            }

            // what has been watched: the wave
            float waveTo = at - gap;
            if (waveTo > left + thick / 2) {
                path.reset();
                float step = unit * 2f;
                boolean first = true;
                for (float x = left + thick / 2; x <= waveTo; x += step) {
                    float y = cy + (float) Math.sin((x + phase) / length * Math.PI * 2) * amplitude * unit3;
                    if (first) { path.moveTo(x, y); first = false; } else path.lineTo(x, y);
                }
                float y = cy + (float) Math.sin((waveTo + phase) / length * Math.PI * 2) * amplitude * unit3;
                path.lineTo(waveTo, y);
                line.setColor(accent);
                line.setStrokeWidth(thick);
                canvas.drawPath(path, line);
            }

            // keep drawing while the wave has somewhere to go, about thirty a second
            if (seek.isShown() && (Math.abs(amplitude - wanted) > 0.005f || (amplitude > 0.01f && moving))) {
                scheduleSelf(this::invalidateSelf, now + 33);
            }
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override protected boolean onLevelChange(int level) { invalidateSelf(); return true; }
    }
}
