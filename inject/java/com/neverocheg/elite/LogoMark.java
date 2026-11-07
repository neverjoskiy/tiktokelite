package com.neverocheg.elite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

/**
 * The mod's logo mark at the head of the settings screen: the crown icon and
 * the name, centred.
 *
 * The crown is TikTokElite's own launcher icon (baked into Shots.java by the
 * build from icon_out/), drawn through Launcher.preview() so the user can
 * replace the icon file without touching this code. The name is the mod's own
 * title string. Nothing is stored here; the picture is decoded once and kept.
 */
public final class LogoMark extends View {

    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap crown;
    private long last;
    private float turn;

    public LogoMark(Context context) {
        super(context);
        crown = Launcher.preview(Launcher.DEFAULT);
        ink.setTextAlign(Paint.Align.CENTER);
        ink.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (crown == null) crown = Launcher.preview(Launcher.DEFAULT);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();

        // slow idle turn so the title page feels alive
        long now = SystemClock.uptimeMillis();
        if (last != 0) turn += (now - last) * 0.05f;
        last = now;
        if (turn > 360f) turn -= 360f;

        int cx = w / 2;
        int side = Math.min(w, h) / 3;
        if (side < 24) side = 24;

        if (crown != null) {
            canvas.save();
            canvas.translate(cx, h / 2 - side / 4);
            canvas.rotate(turn);
            float scale = side / (float) Math.max(crown.getWidth(), crown.getHeight());
            canvas.scale(scale, scale);
            canvas.drawBitmap(crown,
                    -crown.getWidth() / 2f, -crown.getHeight() / 2f, ink);
            canvas.restore();
        }

        String name = Version.LITE ? "TikeLite" : "TikTokElite";
        ink.setColor(0xFFFFFFFF);
        ink.setTextSize(side * 0.42f);
        Paint.FontMetrics metrics = ink.getFontMetrics();
        float textY = h / 2 + side / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(name, cx, textY, ink);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, Math.round(56f * (w / 360f)));
    }
}
