package com.github.sandin.miniperf.server.util;

public class ConvertUtils {
    public static int kHz2MHz(int value) {
        return Math.round((float) value / 1000);
    }

    public static int kb2Mb(int kb) {
        return Math.round((float) kb / 1024);
    }

}
