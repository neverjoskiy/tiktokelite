package com.neverocheg.elite;

/** Read the current comment model, never a nickname or arbitrary row label. */
final class CommentText {
    private static final String MODEL = "com.ss.android.ugc.aweme.comment.model.Comment";
    private static final String CELL = "com.ss.android.ugc.aweme.commentv2.commentlist.powercell.BaseCommentCell";
    private CommentText() {}
    static String from(Object listener) {
        Object model=NativeRead.get(listener,"D6");if(model!=null){String text=find(model,new java.util.IdentityHashMap<Object,Boolean>(),0);if(text!=null)return text;}
        return find(listener,new java.util.IdentityHashMap<Object,Boolean>(),0);
    }
    /** The Comment model behind a cell or a listener, or null. */
    static Object model(Object listener) {
        if (listener == null) return null;
        Object direct = NativeRead.get(listener, "D6");
        if (direct != null && MODEL.equals(direct.getClass().getName())) return direct;
        Object found = direct != null ? model(direct, new java.util.IdentityHashMap<Object,Boolean>(), 0) : null;
        if (found != null) return found;
        return model(listener, new java.util.IdentityHashMap<Object,Boolean>(), 0);
    }
    private static Object model(Object value, java.util.IdentityHashMap<Object,Boolean> seen, int depth) {
        if (value==null || depth>4 || seen.size()>128 || seen.put(value,Boolean.TRUE)!=null) return null;
        Class<?> type=value.getClass();
        if (MODEL.equals(type.getName())) return value;
        String name=type.getName();
        if (name.startsWith("java.") || name.startsWith("android.")
                || name.contains(".profile.model.User")) return null;
        for (Class<?> owner=type;owner!=null && owner!=Object.class;owner=owner.getSuperclass()) {
            if (CELL.equals(owner.getName())) {
                for (java.lang.reflect.Method method:owner.getDeclaredMethods()) {
                    if (method.getParameterTypes().length==0 && MODEL.equals(method.getReturnType().getName())) {
                        try {method.setAccessible(true);Object got=method.invoke(value);if(got!=null)return got;}
                        catch (Throwable ignored) { }
                    }
                }
            }
            for (java.lang.reflect.Field field:owner.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    Object got=model(field.get(value),seen,depth+1);
                    if (got!=null) return got;
                } catch (Throwable ignored) { }
            }
        }
        return null;
    }
    private static String find(Object value, java.util.IdentityHashMap<Object,Boolean> seen, int depth) {
        if (value==null || depth>4 || seen.size()>128 || seen.put(value,Boolean.TRUE)!=null) return null;
        Class<?> type=value.getClass();
        if (MODEL.equals(type.getName())) {
            try {
                Object body=type.getMethod("getText").invoke(value);
                return body instanceof String && ((String)body).trim().length()>0 ? (String)body : null;
            } catch (Throwable ignored) {return null;}
        }
        String name=type.getName();
        if (name.startsWith("java.") || name.startsWith("android.")
                || name.contains(".profile.model.User")) return null;
        for (Class<?> owner=type;owner!=null && owner!=Object.class;owner=owner.getSuperclass()) {
            if (CELL.equals(owner.getName())) {
                for (java.lang.reflect.Method method:owner.getDeclaredMethods()) {
                    if (method.getParameterTypes().length==0 && MODEL.equals(method.getReturnType().getName())) {
                        try {method.setAccessible(true);return find(method.invoke(value),seen,depth+1);}
                        catch (Throwable ignored) { }
                    }
                }
            }
            for (java.lang.reflect.Field field:owner.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    String body=find(field.get(value),seen,depth+1);
                    if (body!=null) return body;
                } catch (Throwable ignored) { }
            }
        }
        return null;
    }
}
