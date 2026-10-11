package com.neverocheg.elite;

import android.view.Choreographer;

/**
 * How long the app stopped for, written down.
 *
 * "It freezes" is not something anyone can act on, and neither is a guess
 * about why. The main thread draws a frame every sixteen milliseconds when
 * all is well, so a gap much larger than that is a stall somebody felt, and
 * the diary can say how long and how often.
 *
 * The cost is one comparison per frame. It says nothing at all unless a stall
 * is long enough to notice, and it gives up after a few so the diary does not
 * turn into a list of them.
 */
public final class Lag {

    /** Long enough that a person notices, rather than merely a dropped frame. */
    private static final long BAD = 700;

    /** Enough to show a pattern without filling the diary. */
    private static final int ENOUGH = 6;

    private static long lastFrame;
    private static int told;
    private static long worst;

    private Lag() {}

    public static void watch() {
        try {
            Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long nanos) {
                    long now = nanos / 1000000L;
                    long since = lastFrame == 0 ? 0 : now - lastFrame;
                    lastFrame = now;
                    if (since > BAD) {
                        if (since > worst) worst = since;
                        if (told < ENOUGH) {
                            told++;
                            Diary.note("lag: the app stopped for " + since + " ms");
                        }
                    }
                    Choreographer.getInstance().postFrameCallback(this);
                }
            });
        } catch (Throwable error) {
            Diary.note("lag: " + error);
        }
    }

    /** The longest stall this run, for the settings to show. */
    public static long worst() {
        return worst;
    }
}
