package com.neverocheg.elite;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;

/**
 * Comments in Material 3 shapes.
 *
 * Every comment row is dressed where TikTok binds it, so recycling is not a
 * problem: a row that comes back gets looked at again. A row keeps a card
 * with a gap round it, and the sheet the rows are in gets round top corners.
 * Rows TikTok painted itself are left as they were.
 */
final class CommentSheets {

    private CommentSheets() {}

    static final String KEY = "comment_cards";

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
            context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, enabled).apply();
            android.app.Activity here = Watch.here();
            if (here == null || here.getWindow() == null) return;
            View root = here.getWindow().getDecorView();
            if (root == null) return;
            if (enabled) {
                // rows dress themselves as TikTok binds them again on scroll
                roundSheet(root);
            } else {
                undress(root, 0);
            }
        } catch (Throwable ignored) {
        }
    }

    /** The card behind a row, drawn a little inside the view. */
    private static final class Card extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float left, top, right, bottom, radius;

        void dress(int colour) {
            if (paint.getColor() != colour) {
                paint.setColor(colour);
                invalidateSelf();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            canvas.drawRoundRect(new RectF(
                    bounds.left + left, bounds.top + top,
                    bounds.right - right, bounds.bottom - bottom),
                    radius, radius, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /** What a dressed row carries: its card, so a foreign background is noticed. */
    private static final int DRESSED = 0x436D7431;
    /** A sheet whose top corners were taken off. */
    private static final int ROUNDED = 0x436D7432;

    /** A comment row TikTok has just bound: a card behind it, and the sheet rounded. */
    static void dress(View root) {
        if (root == null || !isEnabled()) return;
        try {
            Object worn = root.getTag(DRESSED);
            if (worn instanceof Card) {
                if (root.getBackground() == worn) return;  // still ours
            } else if (root.getBackground() != null) {
                return;  // painted by TikTok itself
            }
            float unit = root.getResources().getDisplayMetrics().density;
            int ground = Md3.behind(root);
            int shell = NavBar.mix(ground, Md3.ink(ground), 0.07f);
            Card card = worn instanceof Card ? (Card) worn : new Card();
            card.left = card.right = 8f * unit;
            card.top = card.bottom = 3f * unit;
            card.radius = 16f * unit;
            card.dress(shell);
            if (root.getBackground() != card) root.setBackground(card);
            root.setTag(DRESSED, card);
            roundSheet(root);
        } catch (Throwable error) {
            Diary.detail("comment cards: " + error);
        }
    }

    private static final ViewOutlineProvider TOP_ROUNDED = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            float unit = view.getResources().getDisplayMetrics().density;
            // roundRect rounds every corner; the bottom two are off the screen
            int over = Math.round(20f * unit);
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight() + over, over);
        }
    };

    /** The sheet a row sits in: the full-width container anchored to the screen's foot. */
    static void roundSheet(View root) {
        if (root == null || !isEnabled()) return;
        try {
            View decor = root.getRootView();
            if (decor == null || decor.getWidth() == 0) return;
            float unit = root.getResources().getDisplayMetrics().density;
            int[] at = new int[2], origin = new int[2];
            decor.getLocationOnScreen(origin);
            int foot = origin[1] + decor.getHeight();
            View view = root;
            for (int i = 0; i < 12 && view != null && view != decor; i++) {
                if (view.getTag(ROUNDED) == null && view.getWidth() >= decor.getWidth() * 0.9f) {
                    view.getLocationOnScreen(at);
                    int bottom = at[1] + view.getHeight();
                    if (bottom >= foot - Math.round(8 * unit)
                            && view.getHeight() > Math.round(200 * unit)
                            && view.getHeight() < decor.getHeight() * 0.95f) {
                        view.setTag(ROUNDED, Boolean.TRUE);
                        view.setOutlineProvider(TOP_ROUNDED);
                        view.setClipToOutline(true);
                        return;
                    }
                }
                Object parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } catch (Throwable error) {
            Diary.detail("comment cards: " + error);
        }
    }

    private static void undress(View view, int depth) {
        if (view == null || depth > 24) return;
        try {
            if (view.getTag(DRESSED) instanceof Card) {
                if (view.getBackground() instanceof Card) view.setBackground(null);
                view.setTag(DRESSED, null);
            }
            if (view.getTag(ROUNDED) != null) {
                view.setClipToOutline(false);
                view.setOutlineProvider(null);
                view.setTag(ROUNDED, null);
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) undress(group.getChildAt(i), depth + 1);
            }
        } catch (Throwable ignored) {
        }
    }
}
