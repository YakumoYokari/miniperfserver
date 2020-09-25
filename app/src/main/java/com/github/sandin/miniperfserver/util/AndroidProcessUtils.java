package com.github.sandin.miniperfserver.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * Android Process Utils
 */
public final class AndroidProcessUtils {

    private AndroidProcessUtils() {
        // static functions only
    }

    public static int getPid(Context context,  String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> pids = am.getRunningAppProcesses();
        int pid = -1;
        if (pids != null) {
            for (int i = 0; i < pids.size(); i++) {
                ActivityManager.RunningAppProcessInfo info = pids.get(i);
                if (info.processName.equalsIgnoreCase(packageName)) {
                    pid = info.pid;
                }
            }
        }
        return pid;
    }

}
