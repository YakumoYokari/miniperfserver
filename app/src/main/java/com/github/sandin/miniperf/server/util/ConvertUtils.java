package com.github.sandin.miniperf.server.util;

public class ConvertUtils {
    public static int kHz2MHz(int value) {
        return Math.round((float) value / 1000);
    }

    public static int kb2Mb(int kb) {
        return Math.round((float) kb / 1024);
    }

    public static int micro2Milli(int micro) {
        return Math.round((float) micro / 1000);
    }
}
