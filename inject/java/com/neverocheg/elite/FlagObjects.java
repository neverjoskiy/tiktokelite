package com.neverocheg.elite;

/** Whole settings objects for TikTok's object reader, built from the JSON the mod keeps. */
final class FlagObjects {
    private FlagObjects() {}

    static Object build(String json, Class<?> type) throws Exception {
        return NativeJson.decode(json, type);
    }
}
