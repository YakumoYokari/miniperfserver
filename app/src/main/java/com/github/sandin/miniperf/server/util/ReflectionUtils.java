package com.github.sandin.miniperf.server.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class ReflectionUtils {

    private static final Map<String, Method> sMethodsCache = new HashMap<>();
    private static final Map<String, Field> sFieldsCache = new HashMap<>();

    private ReflectionUtils() {
        // static function only
    }

    public static final <T> T getFieldValue(Class<?> clazz,
                                            String fieldName,
                                            Object object,
                                            boolean useCache) {
        try {
            Field field = null;
            String cacheKey = clazz.getCanonicalName() + fieldName;
            if (useCache) {
                field = sFieldsCache.get(cacheKey);
            }
            if (field == null) {
                field = clazz.getDeclaredField(fieldName);
                if (useCache) {
                    sFieldsCache.put(cacheKey, field);
                }
            }
            if (field != null) {
                return (T)field.get(object);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final <T> T invokeMethod(Class<?> clazz,
                                           String methodName,
                                           Class<?>[] parameterTypes,
                                           Object object,
                                           Object[] args,
                                           boolean useCache) {
        try {
            Method method = null;
            String cacheKey = clazz.getCanonicalName() + methodName; // TODO: use parameterTypes
            if (useCache) {
                method = sMethodsCache.get(cacheKey);
            }
            if (method == null) {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                if (useCache) {
                    sMethodsCache.put(cacheKey, method);
                }
            }
            if (method != null) {
                return (T) method.invoke(object, args);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
