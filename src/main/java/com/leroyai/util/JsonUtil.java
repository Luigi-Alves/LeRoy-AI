package com.leroyai.util;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public final class JsonUtil {

    private static final Gson GSON = new Gson();

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }
}
