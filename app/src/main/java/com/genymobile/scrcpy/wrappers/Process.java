package com.genymobile.scrcpy.wrappers;

import android.annotation.SuppressLint;

/**
 * @link https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/os/Process.java
 */
@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class Process {

    private static final Class<?> CLASS;

    static {
        try {
            CLASS = Class.forName("android.os.Process");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private Process() {
        // only static methods
    }

    public static void setArgV0(String text) {
        try {
            CLASS.getMethod("setArgV0", String.class).invoke(null, text);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
