package com.wetalk.utils;

import java.util.HashMap;
import java.util.Map;

public class ThreadLocalUtil {

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = ThreadLocal.withInitial(HashMap::new);

    public static void set(String key, Object value) {
        THREAD_LOCAL.get().put(key, value);
    }

    public static Object get(String key) {
        return THREAD_LOCAL.get().get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> type) {
        return (T) THREAD_LOCAL.get().get(key);
    }

    public static Long getUserId() {
        return get("userId", Long.class);
    }

    public static String getUsername() {
        return get("username", String.class);
    }

    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
