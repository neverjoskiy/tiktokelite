package com.neverocheg.elite;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Takes back every repost the person has made, one after another.
 *
 * Nothing here talks to TikTok's servers by itself. The list of reposts and the
 * taking back of one are TikTok's own calls -- the ones its repost tab and its
 * repost button make -- and they are called the way the app calls them, so they
 * are signed and sent by the app's own network stack, as the person's own taps
 * would be. What the mod adds is only the loop, and the manners of a loop: a
 * pause between two calls, a stop when the server starts refusing, and a button
 * that ends it.
 *
 * NOTE: the obfuscated class and method names below (UpvoteServiceImpl, X.19ZK,
 * LJFF, LIZLLL, X.0pPL, LLLLL, X.19X3, LJ) come from LotusTok's build of TikTok.
 * TikTokElite's target APK may use different names; they must be re-anchored in
 * margyt/dexpatch.py and here before the channel works.
 */
public final class RepostPurge {

    private RepostPurge() {}

    /** Reposts asked for at a time, and the most pages that are ever read. */
    private static final int PAGE = 50, PAGES = 400;
    /** The wait between two reposts taken back. */
    private static final long PAUSE = 1500L;
    /** After this many refusals in a row the server is not asked again. */
    private static final int REFUSALS = 3;

    private static volatile boolean counting, running, cancelled;

    private static final Runnable STOP = new Runnable() {
        @Override public void run() {
            cancelled = true;
        }
    };

    private static final class Entry {
        String item = "", repost = "";
        int carrier;
        long time;
    }

    private static long rangeFrom, rangeTo = Long.MAX_VALUE;
    private static volatile int doneNow, totalNow;
    private static volatile String phase = "";

    private static final String NOTE_WARNING = "Иногда удаляются не все репосты: TikTok может отказать или пропустить часть. "
            + "Если что-то осталось, запустите удаление ещё раз.";

    // ------------------------------------------------------------ the question

    /** The settings row: which reposts, then count them, ask, and only on a yes take them back. */
    static void begin(final Activity activity) {
        if (counting || running) {
            Popup.ask(activity, Text.REPOST_PURGE,
                    Text.REPOST_PURGE_COUNTING + (totalNow > 0 ? "\n" + doneNow + " / " + totalNow : ""),
                    Text.REPOST_PURGE_STOPPED, new Runnable() {
                        @Override public void run() {
                            cancelled = true;
                        }
                    },
                    Text.CLOSE, null, null, null);
            return;
        }
        Popup.ask(activity, Text.REPOST_PURGE, Text.REPOST_PURGE_WARNING,
                Text.REPOST_PURGE_ASK_ALL, new Runnable() {
                    @Override public void run() {
                        rangeFrom = 0;
                        rangeTo = Long.MAX_VALUE;
                        count(activity);
                    }
                },
                Text.REPOST_PURGE_ASK_PERIOD, new Runnable() {
                    @Override public void run() {
                        pick(activity);
                    }
                },
                null, null);
    }

    /** The two dates, from and to, each day taken whole. */
    private static void pick(final Activity activity) {
        final java.util.Calendar now = java.util.Calendar.getInstance();
        final java.util.Calendar ago = java.util.Calendar.getInstance();
        ago.add(java.util.Calendar.MONTH, -1);
        final android.app.DatePickerDialog from = new android.app.DatePickerDialog(activity,
                new android.app.DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
                        final java.util.Calendar start = java.util.Calendar.getInstance();
                        start.set(year, month, day, 0, 0, 0);
                        start.set(java.util.Calendar.MILLISECOND, 0);
                        android.app.DatePickerDialog to = new android.app.DatePickerDialog(activity,
                                new android.app.DatePickerDialog.OnDateSetListener() {
                                    @Override public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
                                        java.util.Calendar end = java.util.Calendar.getInstance();
                                        end.set(year, month, day, 23, 59, 59);
                                        end.set(java.util.Calendar.MILLISECOND, 999);
                                        long a = start.getTimeInMillis(), b = end.getTimeInMillis();
                                        rangeFrom = Math.min(a, b);
                                        rangeTo = Math.max(a, b);
                                        count(activity);
                                    }
                                }, now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH),
                                now.get(java.util.Calendar.DAY_OF_MONTH));
                        to.setTitle(Text.REPOST_PURGE_ASK_PERIOD);
                        to.show();
                    }
                }, ago.get(java.util.Calendar.YEAR), ago.get(java.util.Calendar.MONTH),
                ago.get(java.util.Calendar.DAY_OF_MONTH));
        from.setTitle(Text.REPOST_PURGE_ASK_PERIOD);
        from.show();
    }

    private static boolean narrowed() {
        return rangeFrom != 0 || rangeTo != Long.MAX_VALUE;
    }

    private static String period() {
        java.text.DateFormat format = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM);
        return format.format(new java.util.Date(rangeFrom)) + " — " + format.format(new java.util.Date(rangeTo));
    }

    private static void count(final Activity activity) {
        final String uid = Account.id();
        if (uid == null || uid.length() == 0) {
            android.widget.Toast.makeText(activity, Text.REPOST_PURGE_WARNING, android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        counting = true;
        cancelled = false;
        phase = Text.REPOST_PURGE_COUNTING;
        doneNow = 0;
        totalNow = 0;
        final Waiting waiting = screen(activity, Text.REPOST_PURGE_COUNTING, STOP);
        waiting.dialog.show();
        status(Text.REPOST_PURGE_COUNTING, 0, 0, true);
        new Thread(new Runnable() {
            @Override public void run() {
                List<Entry> found = null;
                String failure = null;
                try {
                    found = list(uid, activity, waiting);
                } catch (Throwable error) {
                    failure = String.valueOf(error);
                    Diary.note("repost purge: list failed, " + error);
                }
                final List<Entry> entries = filtered(found);
                final String failed = failure;
                counting = false;
                final Activity here = Watch.here() != null ? Watch.here() : activity;
                here.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            if (waiting.dialog.isShowing()) waiting.dialog.dismiss();
                        } catch (Throwable ignored) {
                        }
                        if (cancelled) {
                            status(Text.REPOST_PURGE_STOPPED, 0, 0, false);
                            return;
                        }
                        if (failed != null) {
                            status(Text.REPOST_PURGE_COUNTING, 0, 0, false);
                            Popup.show(here, Text.REPOST_PURGE,
                                    "Не удалось получить список репостов. Откройте TikTok, зайдите в свой профиль и попробуйте ещё раз.");
                        } else if (entries.isEmpty()) {
                            status(Text.REPOST_PURGE_COUNTING, 0, 0, false);
                            Popup.show(here, Text.REPOST_PURGE, narrowed()
                                    ? Text.REPOST_PURGE_ASK_PERIOD
                                    : Text.REPOST_PURGE_CONFIRM_ALL);
                        } else {
                            confirm(here, entries);
                        }
                    }
                });
            }
        }, "margyt-repost-count").start();
    }

    /** Only the reposts of the period; a repost whose date TikTok did not give is not guessed at. */
    private static List<Entry> filtered(List<Entry> all) {
        List<Entry> kept = new ArrayList<Entry>();
        if (all == null) return kept;
        for (Entry entry : all) {
            if (!narrowed() || (entry.time > 0 && entry.time >= rangeFrom && entry.time <= rangeTo)) kept.add(entry);
        }
        return kept;
    }

    private static void confirm(final Activity activity, final List<Entry> entries) {
        Popup.ask(activity,
                Text.REPOST_PURGE_CONFIRM_ALL,
                (narrowed() ? period() + "\n" : "") + Text.REPOST_PURGE_COUNT + entries.size()
                        + "\n\n" + Text.REPOST_PURGE_WARNING
                        + "\n" + Text.REPOST_PURGE_HIDE,
                Text.REPOST_PURGE_ASK_ALL, new Runnable() {
                    @Override public void run() {
                        remove(activity, entries);
                    }
                },
                Text.REPOST_PURGE_ASK_PERIOD, null,
                null, null);
    }

    // ------------------------------------------------------------ TikTok's calls

    /** One page as TikTok answered it. */
    private static final class Page {
        List<?> posts;
        boolean more;
        Throwable failure;
    }

    /**
     * Every repost of the account, a page after a page.
     *
     * The next page starts where what came back ends, so a page the server cut short
     * -- a video that is gone, one that is private -- is read again from its end and
     * cannot leave a gap; a repost seen twice is kept once.
     */
    private static List<Entry> list(String uid, final Activity activity, final Waiting waiting) throws Throwable {
        final Object service = Class.forName("com.ss.android.ugc.aweme.upvote.service.UpvoteServiceImpl")
                .getConstructor().newInstance();
        final Method fetch = service.getClass().getMethod("LJFF", String.class, long.class, int.class, Integer.class);
        final Method first = Class.forName("X.19ZK").getMethod("LIZLLL");
        Map<String, Entry> out = new LinkedHashMap<String, Entry>();

        long offset = 0;
        int pages = 0, barren = 0;
        while (pages < PAGES && !cancelled) {
            Page page = page(service, fetch, first, uid, offset);
            if (page.failure != null) {
                if (pages == 0) throw page.failure;
                Diary.note("repost purge: page " + pages + " failed, " + page.failure);
                break;
            }
            pages++;
            if (page.posts == null || page.posts.isEmpty()) break;
            int before = out.size();
            take(out, page);
            offset += page.posts.size();
            barren = out.size() == before ? barren + 1 : 0;
            if (barren >= 3) break;
            shown(activity, waiting, out.size());
        }
        Diary.note("repost purge: " + out.size() + " reposts found in " + pages + " pages");
        return new ArrayList<Entry>(out.values());
    }

    private static Page page(Object service, Method fetch, Method first, String uid, long offset) {
        Page page = new Page();
        try {
            Object answer = call(first, call(fetch, service, uid, offset, PAGE, null));
            page.posts = answer == null ? null : (List<?>) NativeRead.field(answer, "repostList");
            page.more = answer != null && Boolean.TRUE.equals(NativeRead.field(answer, "hasMore"));
        } catch (Throwable error) {
            page.failure = error;
        }
        return page;
    }

    private static void take(Map<String, Entry> out, Page page) {
        if (page.posts == null) return;
        for (Object post : page.posts) {
            Entry entry = entry(post);
            if (entry == null) continue;
            String key = entry.item + "/" + entry.repost;
            if (out.containsKey(key)) continue;
            out.put(key, entry);
        }
    }

    /** What has been counted so far, under the ring. */
    private static void shown(Activity activity, final Waiting waiting, final int counted) {
        doneNow = counted;
        status(Text.REPOST_PURGE_COUNTING + " " + counted, 0, 0, true);
        try {
            activity.runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (!cancelled) waiting.words.setText(Text.REPOST_PURGE_COUNTING + "\n" + counted);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static long lastNote;

    /** The line in the notification shade: what is being done, and how far it has got. */
    private static void status(String text, int at, int of, boolean going) {
        long now = SystemClock.uptimeMillis();
        if (going && now - lastNote < 800L) return;
        lastNote = now;
        try {
            Context context = Margy.context();
            if (context == null) return;
            android.app.NotificationManager manager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            android.app.Notification.Builder builder;
            String channel = "margyt_purge";
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(new android.app.NotificationChannel(channel,
                        Text.REPOST_PURGE, android.app.NotificationManager.IMPORTANCE_LOW));
                builder = new android.app.Notification.Builder(context, channel);
            } else {
                builder = new android.app.Notification.Builder(context);
            }
            builder.setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setContentTitle(Text.REPOST_PURGE)
                    .setContentText(text)
                    .setOnlyAlertOnce(true)
                    .setOngoing(going);
            if (going) {
                if (of > 0) builder.setProgress(of, at, false);
                else builder.setProgress(0, 0, true);
            }
            manager.notify(4711, builder.build());
        } catch (Throwable ignored) {
        }
    }

    private static Entry entry(Object post) {
        String item = NativeRead.aid(post);
        Object info = NativeRead.get(post, "getUpvoteInfo");
        Entry entry = new Entry();
        entry.item = item;
        if (info != null) {
            Object made = NativeRead.field(info, "createTime");
            if (made instanceof Number) {
                long time = ((Number) made).longValue();
                entry.time = time > 0 && time < 100000000000L ? time * 1000L : time;
            }
            String own = NativeRead.string(NativeRead.field(info, "itemId"));
            if (own.length() > 0) entry.item = own;
            entry.repost = NativeRead.string(NativeRead.field(info, "upvoteId"));
            Object carrier = NativeRead.field(info, "carrierType");
            if (carrier instanceof Number) entry.carrier = ((Number) carrier).intValue();
        }
        return entry.item.length() == 0 ? null : entry;
    }

    /** TikTok's call to take one repost back; it returns once the server has answered. */
    private static void take(Entry entry, Object operator, Method delete, Method wait) throws Throwable {
        Object response = call(wait, call(delete, operator, entry.carrier, entry.item, entry.repost));
        Diary.note("repost purge: " + entry.item + " -> " + describe(response));
    }

    static Object call(Method method, Object on, Object... arguments) throws Throwable {
        try {
            return method.invoke(on, arguments);
        } catch (InvocationTargetException error) {
            throw error.getCause() == null ? error : error.getCause();
        }
    }

    static String describe(Object thing) {
        if (thing == null) return "null";
        String text = String.valueOf(thing);
        return thing.getClass().getSimpleName() + (text.length() > 160 ? text.substring(0, 160) : text);
    }

    // ------------------------------------------------------------ taking back

    private static void remove(final Activity activity, final List<Entry> entries) {
        if (running) return;
        running = true;
        cancelled = false;

        phase = Text.REPOST_PURGE_DELETING;
        doneNow = 0;
        totalNow = entries.size();
        final Waiting waiting = screen(activity, Text.REPOST_PURGE_DELETING, STOP);
        final Dialog dialog = waiting.dialog;
        dialog.show();
        status(Text.REPOST_PURGE_DELETING, 0, entries.size(), true);

        new Thread(new Runnable() {
            @Override public void run() {
                int done = 0, refused = 0;
                String why = null;
                try {
                    Object operator = Class.forName("X.0pPL").getConstructor().newInstance();
                    Method delete = operator.getClass().getMethod("LLLLL", Integer.class, String.class, String.class);
                    Method wait = Class.forName("X.19X3").getMethod("LJ");
                    for (Entry entry : entries) {
                        if (cancelled) break;
                        try {
                            take(entry, operator, delete, wait);
                            done++;
                            doneNow = done;
                            refused = 0;
                            status(done + " / " + entries.size(), done, entries.size(), true);
                            final int shownDone = done;
                            try {
                                activity.runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        waiting.words.setText(Text.REPOST_PURGE_DELETING + "\n" + shownDone + " / " + entries.size());
                                    }
                                });
                            } catch (Throwable ignored) {
                            }
                        } catch (Throwable error) {
                            refused++;
                            Diary.note("repost purge: refused, " + error);
                            if (refused >= REFUSALS) {
                                why = String.valueOf(error);
                                break;
                            }
                        }
                        long until = SystemClock.uptimeMillis() + PAUSE;
                        while (!cancelled && SystemClock.uptimeMillis() < until) Thread.sleep(50);
                    }
                } catch (Throwable error) {
                    why = String.valueOf(error);
                    Diary.note("repost purge: " + error);
                }
                final int removed = done;
                final boolean stopped = why != null;
                final boolean cancelledNow = cancelled;
                running = false;
                String message = Text.REPOST_PURGE_RESULT + removed + " / " + entries.size();
                if (stopped) message += "\n" + Text.REPOST_PURGE_REFUSED;
                else if (cancelledNow) message += "\n" + Text.REPOST_PURGE_STOPPED;
                else if (removed < entries.size()) message += "\n" + Text.REPOST_PURGE_PARTIAL;
                message += "\n\n" + Text.REPOST_PURGE_WARNING;
                status(Text.REPOST_PURGE_RESULT + removed + " / " + entries.size(), 0, 0, false);
                final String result = message;
                final Activity here = Watch.here() != null ? Watch.here() : activity;
                here.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        try {
                            if (dialog.isShowing()) dialog.dismiss();
                        } catch (Throwable ignored) {
                        }
                        if (here.isFinishing()) return;
                        Popup.show(here, Text.REPOST_PURGE, result);
                    }
                });
            }
        }, "margyt-repost-purge").start();
    }

    // ------------------------------------------------------------ the screen

    /** A whole page: the turning ring in the middle, what is being done under it, and the way out. */
    static Waiting screen(final Activity activity, String text, final Runnable stop) {
        Skin skin = Skin.remembered(activity);
        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_NoActionBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(skin.page));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(skin.page);
        page.setPadding(dp(activity, 32), dp(activity, 32), dp(activity, 32), dp(activity, 32));

        Ring ring = new Ring(activity);
        page.addView(ring, new LinearLayout.LayoutParams(dp(activity, 56), dp(activity, 56)));

        final TextView words = new TextView(activity);
        words.setText(text);
        words.setTextColor(skin.text);
        words.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        words.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams under = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        under.topMargin = dp(activity, 24);
        page.addView(words, under);

        TextView cancel = new TextView(activity);
        cancel.setText(Text.REPOST_PURGE_CANCEL);
        cancel.setTextColor(Accent.colour() | 0xFF000000);
        cancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        cancel.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        cancel.setGravity(Gravity.CENTER);
        cancel.setMinHeight(dp(activity, 48));
        cancel.setMinWidth(dp(activity, 120));
        cancel.setPadding(dp(activity, 24), 0, dp(activity, 24), 0);
        android.graphics.drawable.GradientDrawable outline = new android.graphics.drawable.GradientDrawable();
        outline.setColor(Color.TRANSPARENT);
        outline.setStroke(dp(activity, 1), (skin.text & 0x00FFFFFF) | 0x66000000);
        outline.setCornerRadius(dp(activity, 24));
        cancel.setBackground(outline);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                stop.run();
                words.setText(Text.REPOST_PURGE_STOPPING);
                v.setEnabled(false);
                v.setAlpha(0.5f);
            }
        });
        LinearLayout.LayoutParams below = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        below.topMargin = dp(activity, 40);
        page.addView(cancel, below);

        TextView hide = new TextView(activity);
        hide.setText(Text.REPOST_PURGE_HIDE);
        hide.setTextColor(Accent.colour() | 0xFF000000);
        hide.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        hide.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        hide.setGravity(Gravity.CENTER);
        hide.setMinHeight(dp(activity, 48));
        hide.setPadding(dp(activity, 24), 0, dp(activity, 24), 0);
        hide.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dialog.dismiss();
            }
        });
        LinearLayout.LayoutParams under2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        under2.topMargin = dp(activity, 12);
        page.addView(hide, under2);

        dialog.setContentView(page);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
            @Override public void onCancel(android.content.DialogInterface d) {
                stop.run();
            }
        });
        return new Waiting(dialog, words);
    }

    /** The page that is on the screen, and the words under its ring. */
    static final class Waiting {
        final Dialog dialog;
        final TextView words;

        Waiting(Dialog dialog, TextView words) {
            this.dialog = dialog;
            this.words = words;
        }
    }

    /**
     * Material 3 Expressive's wavy circular progress indicator, the indeterminate one:
     * a pale ring for the track, and on it an arc drawn as a wave, with rounded ends,
     * that grows from its tail and lets the tail catch up while the whole of it turns.
     */
    private static final class Ring extends View {
        private static final long SPIN = 1568L, CYCLE = 1333L, RIPPLE = 900L;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final Path wave = new Path();

        Ring(Context context) {
            super(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        private static float ease(float t) {
            return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
        }

        @Override protected void onDraw(Canvas canvas) {
            float unit = getResources().getDisplayMetrics().density;
            float stroke = 4f * unit, amplitude = 1.6f * unit;
            float centre = getWidth() / 2f;
            float radius = centre - stroke / 2f - amplitude;
            int accent = Accent.colour() | 0xFF000000;

            long now = SystemClock.uptimeMillis();
            float turn = (now % SPIN) / (float) SPIN * 360f;
            float within = (now % CYCLE) / (float) CYCLE;
            long laps = now / CYCLE;
            float head = within < 0.5f ? 250f * ease(within * 2f) : 250f;
            float tail = within < 0.5f ? 0f : 250f * ease((within - 0.5f) * 2f);
            float start = turn + tail + (laps % 360) * 250f;
            float sweep = 30f + head - tail;

            float gap = (float) Math.toDegrees((stroke * 1.6f) / radius);
            paint.setStrokeWidth(stroke);
            paint.setColor((accent & 0x00FFFFFF) | 0x38000000);
            box.set(centre - radius, centre - radius, centre + radius, centre + radius);
            float room = 360f - sweep - 2f * gap;
            if (room > 0f) canvas.drawArc(box, start + sweep + gap, room, false, paint);

            int waves = Math.max(6, Math.round((float) (2 * Math.PI * radius) / (14f * unit)));
            float ripple = (now % RIPPLE) / (float) RIPPLE * (float) (2 * Math.PI);
            wave.rewind();
            for (float a = 0f; a <= sweep; a += 2f) {
                double angle = Math.toRadians(start + a);
                double along = (angle - Math.toRadians(start)) * waves - ripple;
                float edge = Math.min(1f, Math.min(a, sweep - a) / 14f);
                float r = radius + amplitude * edge * (float) Math.sin(along);
                float x = centre + r * (float) Math.cos(angle), y = centre + r * (float) Math.sin(angle);
                if (a == 0f) wave.moveTo(x, y);
                else wave.lineTo(x, y);
            }
            paint.setColor(accent);
            canvas.drawPath(wave, paint);
            postInvalidateOnAnimation();
        }
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
