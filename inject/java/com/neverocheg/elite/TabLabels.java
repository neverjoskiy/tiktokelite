package com.neverocheg.elite;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

/**
 * Bottom-bar tabs without their words: icons only, nudged down a little.
 *
 * Every label in the app passes through {@link Badge#setText}, so the tabs
 * are recognised there by their short words. The hiding itself waits for the
 * next layout: only then is it known whether the word sits at the foot of
 * the screen (a tab) or somewhere above it (a header with the same word,
 * which keeps its own).
 */
final class TabLabels {

    private TabLabels() {}

    private static final int DONE = 0x5461624C; // "TabL"

    static void gone(final TextView view, CharSequence text) {
        if (view == null || text == null) return;
        int n = text.length();
        if (n == 0 || n > 10 || view.getTag(DONE) != null) return;
        if (!isTab(text.toString().trim().toLowerCase(Locale.ROOT))) return;
        view.post(new Runnable() {
            @Override public void run() {
                try {
                    if (view.getTag(DONE) != null) return;
                    CharSequence showing = view.getText();
                    if (showing == null
                            || !isTab(showing.toString().trim().toLowerCase(Locale.ROOT))) return;
                    int[] at = new int[2];
                    view.getLocationOnScreen(at);
                    int height = view.getResources().getDisplayMetrics().heightPixels;
                    if (height <= 0 || at[1] < height * 0.6) return;
                    view.setTag(DONE, Boolean.TRUE);
                    view.setVisibility(View.GONE);
                    Object parent = view.getParent();
                    if (parent instanceof ViewGroup) {
                        ViewGroup group = (ViewGroup) parent;
                        float dy = 3 * view.getResources().getDisplayMetrics().density;
                        for (int i = 0; i < group.getChildCount(); i++) {
                            View child = group.getChildAt(i);
                            if (child != view && child.getVisibility() == View.VISIBLE) {
                                child.setTranslationY(child.getTranslationY() + dy);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static boolean isTab(String label) {
        return label.equals("главная") || label.equals("home") || label.equals("головна")
                || label.equals("друзья") || label.equals("friends") || label.equals("друзі")
                || label.equals("входящие") || label.equals("inbox") || label.equals("вхідні")
                || label.equals("профиль") || label.equals("profile") || label.equals("профіль")
                || label.equals("магазин") || label.equals("shop") || label.equals("крамниця");
    }
}
