package com.neverocheg.elite;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ghost mode bytecode interceptors. Ported from LotusTok (TT Extras 1.0.4) and
 * adapted to TikTokElite's naming.
 *
 * Each channel intercepts a real TikTok call and swallows it when the
 * corresponding switch is on. Nothing here talks to the network by itself;
 * unrelated operations delegate unchanged.
 *
 * NOTE: the obfuscated class and method names below (X.12Qu, X.0CPd, LIZIZ,
 * LIZJ, LLJJIII, LL, X.0TL0, X.06WV, X.0Rub, IIMCoreProxyService, X.0RiV,
 * X.19ZK, X.19X3, StoryApi, IStoryApi, ProfileViewerApiService,
 * IProfileViewerApi) come from LotusTok's build of TikTok. TikTokElite's
 * target APK may use different names; they must be re-anchored in
 * margyt/dexpatch.py and here before the channel works.
 */
final class GhostMode {

    interface Flags { boolean on(String key); }
    interface Notice { void log(String message); }

    private final ImEventGate transport;
    private final Flags flags;
    private final Notice notice;
    private final List<GhostReflect.Patch> patches = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> status = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> blocked = new LinkedHashMap<>();
    private final Map<Object, Boolean> installed = Collections.synchronizedMap(new IdentityHashMap<>());
    private volatile boolean closed;
    private volatile Object activityDelegate;

    GhostMode(Flags f, Notice n) {
        flags = f;
        notice = n;
        transport = new ImEventGate(this::enabled, this::hit, message -> n.log(message));
        for (String k : new String[]{"online", "stories", "read", "typing", "profiles"})
            blocked.put(k, new AtomicInteger());
    }

    boolean enabled(String channel) {
        return !closed && flags.on("ghost") && flags.on("ghost_" + channel);
    }

    private void hit(String key) {
        blocked.get(key).incrementAndGet();
    }

    private interface Work { void run() throws Exception; }

    private void attempt(String key, Work work) {
        try {
            work.run();
            status.put(key, "точка отправки подключена");
        } catch (Exception e) {
            String s = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (!s.equals(status.put(key, s))) notice.log("Ghost " + key + ": " + s);
        }
    }

    /** Intercept the lazy initializer without constructing TikTok services
     * before Application.onCreate. */
    void bootstrap() {
        if (closed || !flags.on("ghost")) return;
        attempt("online", () -> {
            Object cache = GhostReflect.stat("X.12Qu", "LIZIZ");
            Field value = GhostReflect.field(cache.getClass(), "LLJJIII");
            synchronized (cache) {
                Object current = value.get(cache);
                Object sentinel = GhostReflect.stat("X.0CPd", "LIZ");
                if (current != sentinel) {
                    activityDelegate = current;
                    return;
                }
                Field initializer = GhostReflect.field(cache.getClass(), "LL");
                Object original = initializer.get(cache);
                Class<?> fn = GhostReflect.type("kotlin.jvm.functions.Function0");
                Object wrapper = Proxy.newProxyInstance(fn.getClassLoader(),
                        new Class<?>[]{fn}, (p, m, a) -> {
                            Object result = GhostReflect.invoke(m, original, a);
                            if (!m.getName().equals("invoke") || closed) return result;
                            activityDelegate = result;
                            Class<?> service = GhostReflect.type(
                                    "com.ss.android.ugc.aweme.im.activity.status.sdk.api.ActivityStatusService");
                            Object adapter = Proxy.newProxyInstance(service.getClassLoader(),
                                    new Class<?>[]{service}, (p2, method, args) -> {
                                        if (enabled("online") && method.getName().equals("LIZJ"))
                                            attempt("online", this::online);
                                        Object answer = GhostReflect.invoke(method, result, args);
                                        if (enabled("online") && method.getName().equals("LIZIZ"))
                                            attempt("online", this::online);
                                        return answer;
                                    });
                            synchronized (patches) {
                                if (closed) return result;
                                patches.add(new GhostReflect.Patch(cache, value, adapter, result));
                            }
                            return adapter;
                        });
                patch(cache, initializer, wrapper);
            }
        });
    }

    /** Re-apply every enabled channel once a second while the screen is up. */
    void apply() {
        if (closed || !flags.on("ghost")) return;
        if (enabled("read") || enabled("typing")) {
            try {
                transport.install();
            } catch (Exception e) {
                transport.status = "ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                notice.log("IM outbound: " + e);
            }
        }
        if (enabled("typing")) {
            attempt("typing", () -> lazy(GhostReflect.stat("X.0TL0", "LIZIZ"),
                    "com.ss.android.ugc.aweme.im.chatroom.api.typingindicator.ITypingStatusSenderTimer",
                    "typing", "LIZ", false));
        }
        if (enabled("profiles")) {
            attempt("profiles", () -> api(
                    "com.ss.android.ugc.profile.business.ci.viewer.api.ProfileViewerApiService",
                    "com.ss.android.ugc.profile.business.ci.viewer.api.IProfileViewerApi",
                    "profiles", "reportView"));
        }
        if (enabled("stories")) {
            attempt("stories", () -> api(
                    "com.ss.android.ugc.aweme.story.api.StoryApi",
                    "com.ss.android.ugc.aweme.story.api.IStoryApi", "stories", "reportStoryViewed"));
        }
        if (enabled("online")) attempt("online", this::online);
        if (enabled("read")) attempt("read", this::read);
    }

    private static Object proxy(Class<?> face, Object delegate,
                                java.util.function.BiFunction<Method, Object[], Boolean> suppress,
                                java.util.function.Function<Method, Object> substitute) {
        return Proxy.newProxyInstance(face.getClassLoader(), new Class<?>[]{face}, (p, m, a) -> {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "equals": return p == a[0];
                    case "hashCode": return System.identityHashCode(p);
                    case "toString": return "TikTokElite ghost adapter";
                }
            }
            if (suppress.apply(m, a)) return substitute.apply(m);
            return GhostReflect.invoke(m, delegate, a);
        });
    }

    private void patch(Object owner, Field field, Object value) throws Exception {
        synchronized (patches) {
            if (closed) throw new IllegalStateException("adapter stopped");
            patches.add(new GhostReflect.Patch(owner, field, value));
            installed.put(owner, Boolean.TRUE);
        }
    }

    private void lazy(Object lazy, String face, String channel, String method,
                      boolean answer) throws Exception {
        if (installed.containsKey(lazy)) return;
        Object original = GhostReflect.call(lazy, "getValue");
        Class<?> type = GhostReflect.type(face);
        if (original == null || !type.isInstance(original))
            throw new IllegalStateException("sender unavailable");
        Object value = proxy(type, original,
                (m, a) -> {
                    boolean stop = m.getName().equals(method) && enabled(channel);
                    if (stop) hit(channel);
                    return stop;
                }, m -> answer ? Boolean.TRUE : null);
        synchronized (lazy) {
            patch(lazy, GhostReflect.field(lazy.getClass(), "LLJJIII"), value);
        }
    }

    private void api(String implementation, String face, String channel, String method)
            throws Exception {
        Object service = GhostReflect.stat(implementation, "LIZIZ");
        if (installed.containsKey(service)) return;
        Field field = GhostReflect.field(service.getClass(), "LIZ");
        Object original = field.get(service);
        final Object canceled;
        if (channel.equals("profiles"))
            canceled = GhostReflect.type("X.19ZK")
                    .getMethod("LJJJJJ", Iterable.class).invoke(null, Collections.emptyList());
        else
            canceled = GhostReflect.type("X.19X3")
                    .getMethod("LJIILLIIL", Throwable.class)
                    .invoke(null, new CancellationException("TikTokElite: not sent"));
        Object value = proxy(GhostReflect.type(face), original,
                (m, a) -> {
                    boolean stop = m.getName().equals(method) && enabled(channel);
                    if (stop) hit(channel);
                    return stop;
                }, m -> canceled);
        patch(service, field, value);
    }

    private void online() throws Exception {
        Object service = activityDelegate != null
                ? activityDelegate
                : GhostReflect.type("X.12Qu").getMethod("LIZ").invoke(null);
        Object session = GhostReflect.call(service, "LJIIJJI");
        if (session == null) throw new IllegalStateException("ожидание входа в аккаунт");
        Object reporter = GhostReflect.call(session, "LJ");
        Object gate = GhostReflect.get(reporter, "LIZLLL");
        if (installed.containsKey(gate)) return;
        Field field = GhostReflect.field(gate.getClass(), "LIZ");
        Object original = field.get(gate);
        Object value = proxy(GhostReflect.type("X.1KSn"), original,
                (m, a) -> {
                    boolean stop = m.getName().equals("LIZ") && enabled("online");
                    if (stop) hit("online");
                    return stop;
                }, m -> Boolean.TRUE);
        patch(gate, field, value);
    }

    private void read() throws Exception {
        Object service = GhostReflect.type("X.06WV").getMethod("LIZ").invoke(null);
        Object kind = GhostReflect.stat("X.0Rub", "TIKTOK_SOCIAL_IM");
        Object client = GhostReflect.type("com.bytedance.ies.im.core.api.proxy.IIMCoreProxyService")
                .getMethod("LIZJ", GhostReflect.type("X.0Rub")).invoke(service, kind);
        Object sdk = GhostReflect.call(client, "LJIIJ");
        Object root = GhostReflect.get(sdk, "LIZ");
        Object nativeRoot = GhostReflect.call(root, "LJIIZILJ");
        Object domain = GhostReflect.call(nativeRoot, "getDomain");
        GhostReflect.call(domain, "LIZLLL");
        if (!domain.getClass().getName().equals("X.0RgK"))
            throw new IllegalStateException("неподдерживаемый тип сессии: " + domain.getClass().getName());
        lazy(GhostReflect.get(domain, "LJI"), "X.0RiV", "read", "LJLL", false);
    }

    String report() {
        StringBuilder s = new StringBuilder("призрак: ")
                .append(flags.on("ghost") ? "включён" : "выключен")
                .append("\nотправка IM: ").append(transport.report());
        for (String k : blocked.keySet())
            s.append("\n").append(k)
                    .append(": ").append(enabled(k)
                            ? status.getOrDefault(k, "ожидание подключения")
                            : "выключено")
                    .append("; остановлено: ").append(blocked.get(k).get());
        return s.toString();
    }

    void close() {
        closed = true;
        transport.close();
        synchronized (patches) {
            for (int i = patches.size() - 1; i >= 0; i--)
                try {
                    patches.get(i).restore();
                } catch (Exception e) {
                    notice.log("Ghost restore: " + e);
                }
            patches.clear();
            installed.clear();
        }
        activityDelegate = null;
    }
}
