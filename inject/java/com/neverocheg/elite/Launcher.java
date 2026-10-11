package com.neverocheg.elite;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

/**
 * Which icon the app wears.
 *
 * Android decides that from the component the home screen opens, and there is
 * no way to change one while the app runs -- so the build ships one component
 * per icon, all pointing at the same activity, all switched off. Choosing an
 * icon turns one on and turns the others off, and because they all lead to the
 * same place the app opens exactly as it did.
 *
 * TikTok's own entry is itself one of these -- an alias in front of
 * MainActivity -- so the default is not a special case: it is just the one the
 * build found already there.
 *
 * The launcher usually notices within a second or two. Some of them want the
 * home screen redrawn before they will, which is a launcher's business and not
 * something an app is allowed to insist on.
 */
public final class Launcher {

    private Launcher() {}

    public static final String KEY = "icon";

    /** The empty name is TikTok's own, the one the build did not add. */
    public static final String DEFAULT = "";

    public static String[] all() {
        String[] out = new String[Shots.KEYS.length + 1];
        out[0] = DEFAULT;
        System.arraycopy(Shots.KEYS, 0, out, 1, Shots.KEYS.length);
        return out;
    }

    public static String nameOf(String which) {
        int at = indexOf(which);
        return at < 0 ? Text.ICON_DEFAULT : Shots.LABELS[at];
    }

    public static String chosen() {
        SharedPreferences prefs = prefs();
        String which = prefs == null ? DEFAULT : prefs.getString(KEY, DEFAULT);
        return indexOf(which) < 0 ? DEFAULT : which;
    }

    /**
     * Turn on the one that was chosen, and everything else off.
     *
     * DONT_KILL_APP matters: without it Android stops the app the moment one
     * of its components is switched, which from the inside looks like the mod
     * crashing the phone's TikTok for changing a picture.
     */
    public static void choose(Context context, String which) {
        if (context == null) return;
        if (indexOf(which) < 0) which = DEFAULT;
        try {
            PackageManager packages = context.getPackageManager();
            String self = context.getPackageName();

            for (int i = 0; i < Shots.COMPONENTS.length; i++) {
                boolean on = Shots.KEYS[i].equals(which);
                set(packages, self, Shots.COMPONENTS[i], on);
            }
            set(packages, self, Shots.DEFAULT, DEFAULT.equals(which));

            SharedPreferences prefs = prefs();
            if (prefs != null) prefs.edit().putString(KEY, which).apply();
            Diary.note("icon: " + (DEFAULT.equals(which) ? "back to TikTok's" : which));
        } catch (Throwable error) {
            Diary.note("icon: " + error);
        }
    }

    private static void set(PackageManager packages, String self,
                            String component, boolean on) {
        packages.setComponentEnabledSetting(
                new ComponentName(self, component),
                on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                   : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /** The picture for the settings to show, decoded once and kept. */
    public static Bitmap preview(String which) {
        int at = indexOf(which);
        String[] png = at < 0 ? Shots.DEFAULT_PNG : Shots.PNG[at];
        if (which == null) return null;
        synchronized (drawn) {
            Bitmap known = drawn.get(which);
            if (known != null) return known;
            try {
                StringBuilder whole = new StringBuilder();
                for (String piece : png) whole.append(piece);
                byte[] raw = Base64.decode(whole.toString(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                if (bitmap != null) drawn.put(which, bitmap);
                return bitmap;
            } catch (Throwable error) {
                Diary.note("icon: " + error);
                return null;
            }
        }
    }

    private static final Map<String, Bitmap> drawn = new HashMap<String, Bitmap>();

    private static int indexOf(String which) {
        if (which == null) return -1;
        for (int i = 0; i < Shots.KEYS.length; i++) {
            if (Shots.KEYS[i].equals(which)) return i;
        }
        return -1;
    }

    private static SharedPreferences prefs() {
        Context context = Margy.context();
        if (context == null) return null;
        return context.getSharedPreferences(Margy.PREFS, Context.MODE_PRIVATE);
    }
}
