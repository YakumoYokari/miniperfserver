package com.genymobile.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.content.Context;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/ActivityThread.java">ActivityThread</a>
 */
@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class ActivityThread {

    private static final Class<?> CLASS;

    private final Object mSystemMainInstance;

    static {
        try {
            CLASS = Class.forName("android.app.ActivityThread");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private ActivityThread(Object systemMainInstance) {
        mSystemMainInstance = systemMainInstance;
    }

    public static ActivityThread systemMain() {
        try {
            Object systemMainInstance = CLASS.getMethod("systemMain").invoke(null);
            return new ActivityThread(systemMainInstance);
            /*
            Constructor<?> activityThreadConstructor = CLASS.getDeclaredConstructor();
            activityThreadConstructor.setAccessible(true);
            Object activityThread = activityThreadConstructor.newInstance();
            return new ActivityThread(activityThread);
             */
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public Context getSystemContext() {
        try {
            return (Context) CLASS.getMethod("getSystemContext").invoke(mSystemMainInstance);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
