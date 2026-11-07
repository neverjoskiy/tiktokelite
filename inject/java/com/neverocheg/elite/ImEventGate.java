package com.neverocheg.elite;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Last shared outbound bridge of the native IM SDK. Handles the legacy mark-read
 * path too. Recognizes only explicit protocol commands; never inspects message
 * text or session secrets.
 *
 * When ghost read/typing is on, packets carrying MARK_CONVERSATION_READ or
 * SEND_INPUT_STATUS are held and cancelled locally instead of being sent.
 */
final class ImEventGate {

    interface Mode { boolean on(String channel); }
    interface Hit { void accept(String channel); }
    interface Notice { void log(String message); }

    private final Mode mode;
    private final Hit hit;
    private final Notice log;
    private final Map<Integer, String> commands = new HashMap<>();
    private final Map<String, String> denied = new LinkedHashMap<>();    private final AtomicInteger[] observed = {
            new AtomicInteger(), new AtomicInteger(),
            new AtomicInteger(), new AtomicInteger()};
    private GhostReflect.Patch patch;
    private volatile boolean closed;
    private Class<?> requestType, callbackType;
    private Field cmd, seq, body;
    String status = "не подключён";
    private volatile String callbackIssue;

    ImEventGate(Mode m, Hit h, Notice n) {
        mode = m;
        hit = h;
        log = n;
    }

    void install() throws Exception {
        if (patch != null) {
            status = "HTTP и WebSocket подключены";
            return;
        }
        Class<?> enums = GhostReflect.type("X.0S5V");
        for (String name : new String[]{
                "MARK_CONVERSATION_READ", "MARK_CONVERSATION_READ_V3",
                "BATCH_MARK_CONVERSATION_READ", "MARK_STRANGER_CONVERSATION_READ",
                "MARK_ALL_STRANGER_CONVERSATIONS_READ", "MARK_MULTI_STRANGER_CONVERSATIONS_READ"})
            commands.put(((Number) GhostReflect.call(
                    GhostReflect.field(enums, name).get(null), "getValue")).intValue(), "read");
        commands.put(((Number) GhostReflect.call(
                GhostReflect.field(enums, "SEND_INPUT_STATUS").get(null), "getValue")).intValue(),
                "typing");
        requestType = GhostReflect.type("com.bytedance.im.core.proto.Request");
        cmd = GhostReflect.field(requestType, "cmd");
        seq = GhostReflect.field(requestType, "sequence_id");
        body = GhostReflect.field(GhostReflect.type("X.0S7w"), "LIZLLL");
        callbackType = GhostReflect.type("X.0S6U");
        Object cache = GhostReflect.stat("X.0SLs", "LIZIZ");
        Object original = GhostReflect.call(cache, "getValue");
        Class<?> face = GhostReflect.type("com.bytedance.ies.im.core.api.proxy.IIMCoreDependProxyService");
        Object http = face.getMethod("LIZIZ").invoke(original);
        Object ws = face.getMethod("LIZJ").invoke(original);
        Object httpGate = outbound(GhostReflect.type("X.0S7u"), http,
                (m, a) -> {
                    if ((m.getName().equals("LJ") || m.getName().equals("LIZLLL")) && a != null && a.length == 2) {
                        Object request = body.get(a[0]);
                        if (requestType.isInstance(request)) {
                            Number command = (Number) cmd.get(request);
                            if (command != null && commands.containsKey(command.intValue())) {
                                Number sequence = (Number) seq.get(request);
                                return blocked(command.intValue(), sequence == null ? 0 : sequence.longValue());
                            }
                        }
                    }
                    return null;
                }, (m, a, channel) -> {
                    try {
                        Object callback = GhostReflect.get(a[1], "l0");
                        if (!callbackType.isInstance(callback))
                            throw new IllegalStateException("unexpected HTTP callback");
                        callbackType.getMethod("LIZ", Exception.class, String.class, int.class)
                                .invoke(callback, new CancellationException("TikTokElite: " + channel + " not sent"),
                                        "cancelled locally by TikTokElite", -1000);
                    } catch (Exception e) {
                        callbackIssue = "пакет удержан; отмена callback: " + e.getClass().getSimpleName();
                        log.log(callbackIssue);
                    }
                }, channel -> {
                    observed[channel.equals("read") ? 0 : 1].incrementAndGet();
                    hit.accept(channel);
                });
        Class<?> wsType = GhostReflect.type("com.bytedance.ies.im.core.api.depend.IWsDepend");
        Object wsGate = outbound(wsType, ws,
                (m, a) -> {
                    if (m.getName().equals("LIZIZ") && a != null && a.length == 1) {
                        Object message = a[0];
                        int service = ((Number) GhostReflect.get(message, "service")).intValue();
                        int method = ((Number) GhostReflect.get(message, "method")).intValue();
                        if (service == 5 && method == 1)
                            return blocked(commandHeader(GhostReflect.get(message, "msgHeaders")),
                                    ((Number) GhostReflect.get(message, "seqId")).longValue());
                    }
                    return null;
                }, (m, a, ch) -> {
                }, channel -> {
                    observed[channel.equals("read") ? 2 : 3].incrementAndGet();
                    hit.accept(channel);
                });
        Object adapter = GhostReflect.proxy(face, original,
                (m, a) -> m.getName().equals("LIZIZ") || m.getName().equals("LIZJ"),
                m -> m.getName().equals("LIZIZ") ? httpGate : wsGate);
        synchronized (cache) {
            patch = new GhostReflect.Patch(cache, GhostReflect.field(cache.getClass(), "LLJJIII"), adapter);
        }
        status = "HTTP и WebSocket подключены";
    }

    interface Decision { String channel(Method method, Object[] args) throws Exception; }

    interface Cancel { void run(Method method, Object[] args, String channel) throws Exception; }

    Object outbound(Class<?> face, Object delegate, Decision decision, Cancel cancel,
                    Hit observe) {
        return Proxy.newProxyInstance(face.getClassLoader(), new Class<?>[]{face}, (p, m, a) -> {
            String channel = closed ? null : decision.channel(m, a);
            if (channel != null) {
                cancel.run(m, a, channel);
                observe.accept(channel);
                return null;
            }
            return GhostReflect.invoke(m, delegate, a);
        });
    }

    private int commandHeader(Object headers) throws Exception {
        if (!(headers instanceof List)) return -1;
        for (Object header : (List<?>) headers)
            if ("cmd".equals(GhostReflect.get(header, "key")))
                try {
                    return Integer.parseInt(String.valueOf(GhostReflect.get(header, "value")));
                } catch (NumberFormatException ignored) {
                    return -1;
                }
        return -1;
    }

    synchronized String blocked(int command, long sequence) {
        String key = command + ":" + sequence;
        String old = sequence > 0 ? denied.get(key) : null;
        if (old != null) return old;
        String channel = commands.get(command);
        if (channel == null || !mode.on(channel)) return null;
        if (sequence > 0) denied.put(key, channel);
        if (denied.size() > 8192) denied.remove(denied.keySet().iterator().next());
        return channel;
    }

    String report() {
        return status + "; HTTP прочитано/печатает: " + observed[0].get() + "/" + observed[1].get()
                + "; WS: " + observed[2].get() + "/" + observed[3].get()
                + (callbackIssue == null ? "" : "; " + callbackIssue);
    }

    void close() {
        closed = true;
        if (patch != null) try {
            patch.restore();
        } catch (Exception e) {
            log.log("IM bridge restore: " + e);
        }
        patch = null;
        synchronized (this) {
            denied.clear();
        }
    }
}
