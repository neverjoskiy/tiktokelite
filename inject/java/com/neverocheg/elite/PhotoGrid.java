package com.neverocheg.elite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Set;

/**
 * The photos of a post as small pictures, each with a tick, for the download sheet.
 *
 * A tap ticks or unticks a photo; `changed` runs after every tap so the sheet can
 * say how many are ticked. The pictures are fetched in the background from the
 * same addresses the download uses, decoded small, and kept for a while so that
 * opening the sheet again on the same post shows them at once.
 */
final class PhotoGrid {

    private PhotoGrid() {}

    private static final int PER_LINE = 4;
    private static final java.util.concurrent.ExecutorService LOADER =
            java.util.concurrent.Executors.newFixedThreadPool(3, run -> {
                Thread thread = new Thread(run, "MargyT photos");
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });
    private static final android.util.LruCache<String, Bitmap> KEPT =
            new android.util.LruCache<String, Bitmap>(12 * 1024 * 1024) {
                @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
            };
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static View make(Context c, Skin skin, List<DownloadQuality.Choice> photos, Set<Integer> picked, Runnable changed) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        int across = Math.min(dp(c, 480), c.getResources().getDisplayMetrics().widthPixels - dp(c, 24)) - dp(c, 28);
        int gap = dp(c, 6);
        int width = (across - gap * (PER_LINE - 1)) / PER_LINE;
        int height = Math.round(width * 4f / 3f);
        LinearLayout line = null;
        for (int i = 0; i < photos.size(); i++) {
            if (i % PER_LINE == 0) {
                line = new LinearLayout(c);
                line.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams where = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                where.topMargin = i == 0 ? 0 : gap;
                box.addView(line, where);
            }
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(width, height);
            size.leftMargin = i % PER_LINE == 0 ? 0 : gap;
            line.addView(tile(c, skin, photos.get(i), i, picked, changed), size);
        }
        return box;
    }

    private static View tile(Context c, Skin skin, DownloadQuality.Choice photo, int index,
                             Set<Integer> picked, Runnable changed) {
        final float radius = dp(c, 14);
        FrameLayout tile = new FrameLayout(c);
        tile.setClipToOutline(true);
        tile.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        tile.setBackgroundColor((skin.text & 0xFFFFFF) | 0x14000000);
        tile.setContentDescription(photo.label);
        tile.setClickable(true);
        tile.setFocusable(true);

        ImageView image = new ImageView(c);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(image, new FrameLayout.LayoutParams(-1, -1));

        // the photo's number, small, so the order in the post can still be told
        TextView number = new TextView(c);
        number.setText(String.valueOf(index + 1));
        number.setTextColor(Color.WHITE);
        number.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        number.setShadowLayer(dp(c, 3), 0, 0, 0xAA000000);
        FrameLayout.LayoutParams corner = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        corner.leftMargin = dp(c, 7);
        corner.bottomMargin = dp(c, 4);
        tile.addView(number, corner);

        final Tick tick = new Tick(c);
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(dp(c, 24), dp(c, 24), Gravity.TOP | Gravity.END);
        top.topMargin = dp(c, 6);
        top.rightMargin = dp(c, 6);
        tile.addView(tick, top);

        final Ring ring = new Ring(c, radius);
        tile.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        tick.on = ring.on = picked.contains(index);

        Motion.touch(tile);
        tile.setOnClickListener(v -> {
            boolean on = !picked.contains(index);
            if (on) picked.add(index); else picked.remove(index);
            tick.on = ring.on = on;
            tick.invalidate();
            ring.invalidate();
            tile.setContentDescription(photo.label + (on ? " ✓" : ""));
            try { changed.run(); } catch (Throwable ignored) {}
        });
        load(photo, image, Math.max(1, dp(c, 120)));
        return tile;
    }

    private static void load(DownloadQuality.Choice photo, ImageView into, int target) {
        final String key = photo.url;
        Bitmap kept = key == null ? null : KEPT.get(key);
        if (kept != null) { into.setImageBitmap(kept); return; }
        into.setTag(key);
        try {
            LOADER.execute(() -> {
                Bitmap picture = null;
                for (String address : photo.candidates()) {
                    picture = decode(Net.bytes(address), target);
                    if (picture != null) break;
                }
                if (picture == null) return;
                final Bitmap done = picture;
                if (key != null) KEPT.put(key, done);
                MAIN.post(() -> { if (key != null && key.equals(into.getTag())) into.setImageBitmap(done); });
            });
        } catch (Throwable error) {
            Diary.note("photo grid: " + error);
        }
    }

    /** The picture shrunk by halves until it is no smaller than `target` on its short side. */
    static Bitmap decode(byte[] data, int target) {
        if (data == null || data.length == 0) return null;
        try {
            BitmapFactory.Options size = new BitmapFactory.Options();
            size.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, size);
            int shortSide = Math.min(size.outWidth, size.outHeight);
            if (shortSide <= 0) return null;
            int sample = 1;
            while (shortSide / (sample * 2) >= target) sample *= 2;
            BitmapFactory.Options read = new BitmapFactory.Options();
            read.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, read);
        } catch (Throwable error) {
            return null;
        }
    }

    /** A circle in the corner: empty and ringed when the photo is not ticked, filled with a tick when it is. */
    private static final class Tick extends View {
        boolean on;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mark = new Path();

        Tick(Context c) { super(c); }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) / 2f;
            float unit = getResources().getDisplayMetrics().density;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(on ? Accent.colour() : 0x55000000);
            canvas.drawCircle(w / 2f, h / 2f, r - unit, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(unit * 1.8f);
            paint.setColor(Color.WHITE);
            if (!on) {
                canvas.drawCircle(w / 2f, h / 2f, r - unit * 1.5f, paint);
                return;
            }
            int accent = Accent.colour();
            int red = (accent >> 16) & 0xFF, green = (accent >> 8) & 0xFF, blue = accent & 0xFF;
            paint.setColor((red * 299 + green * 587 + blue * 114) / 1000 > 150 ? 0xFF111111 : Color.WHITE);
            paint.setStrokeWidth(unit * 2.2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            mark.reset();
            mark.moveTo(w * .29f, h * .52f);
            mark.lineTo(w * .44f, h * .66f);
            mark.lineTo(w * .72f, h * .36f);
            canvas.drawPath(mark, paint);
        }
    }

    /** The accent drawn round a ticked photo. */
    private static final class Ring extends View {
        boolean on;
        private final float radius;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Ring(Context c, float radius) {
            super(c);
            this.radius = radius;
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (!on) return;
            float unit = getResources().getDisplayMetrics().density, half = unit * 1.5f;
            paint.setStrokeWidth(half * 2);
            paint.setColor(Accent.colour());
            canvas.drawRoundRect(new RectF(half, half, getWidth() - half, getHeight() - half), radius - half, radius - half, paint);
        }
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
