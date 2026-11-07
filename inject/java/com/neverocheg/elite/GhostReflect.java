package com.neverocheg.elite;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Reflection helpers for Ghost mode: read fields, static fields, call no-arg
 * methods, and patch a field with a proxy while remembering the previous value
 * so it can be restored when the mode is switched off.
 */
final class GhostReflect {

    private GhostReflect() {}

    static Class<?> type(String name) throws Exception {
        return Class.forName(name, false, GhostReflect.class.getClassLoader());
    }

    /** Find a field by name, walking up the superclass chain. */
    static Field field(Class<?> c, String name) throws Exception {
        for (Class<?> p = c; p != null; p = p.getSuperclass()) {
            try {
                Field f = p.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    static Object get(Object o, String name) throws Exception {
        return field(o.getClass(), name).get(o);
    }

    static Object stat(String cls, String field) throws Exception {
        return field(type(cls), field).get(null);
    }

    static Object call(Object o, String name) throws Exception {
        return o.getClass().getMethod(name).invoke(o);
    }

    static Object invoke(Method m, Object o, Object[] args) throws Throwable {
        try {
            return m.invoke(o, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
        }
    }

    /** Patch a field with a replacement value, remembering the previous one. */
    static final class Patch {
        final Object owner, previous, replacement;
        final Field field;

        Patch(Object owner, Field f, Object value) throws Exception {
            this(owner, f, value, f.get(owner));
        }

        Patch(Object owner, Field f, Object value, Object restoreValue) throws Exception {
            this.owner = owner;
            this.field = f;
            previous = restoreValue;
            replacement = value;
            f.set(owner, value);
            if (f.get(owner) != value) {
                throw new IllegalStateException("write rejected: " + f.getName());
            }
        }

        void restore() throws Exception {
            if (field.get(owner) == replacement) {
                field.set(owner, previous);
            }
        }
    }

    /** Build a proxy for `face` that delegates to `delegate`, but lets
     * `suppress` cancel a call and `substitute` provide the answer instead. */
    static Object proxy(Class<?> face, Object delegate,
                        java.util.function.BiFunction<Method, Object[], Boolean> suppress,
                        java.util.function.Function<Method, Object> substitute) {
        return Proxy.newProxyInstance(face.getClassLoader(),
                new Class<?>[]{face}, (p, m, a) -> {
                    if (m.getDeclaringClass() == Object.class) {
                        switch (m.getName()) {
                            case "equals": return p == a[0];
                            case "hashCode": return System.identityHashCode(p);
                            case "toString": return "TikTokElite ghost adapter";
                        }
                    }
                    if (suppress.apply(m, a)) return substitute.apply(m);
                    return invoke(m, delegate, a);
                });
    }
}
