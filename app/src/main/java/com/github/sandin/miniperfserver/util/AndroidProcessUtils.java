package com.github.sandin.miniperfserver.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import com.genymobile.scrcpy.wrappers.ServiceManager;

import java.util.List;

/**
 * Android Process Utils
 */
public final class AndroidProcessUtils {

    private AndroidProcessUtils() {
        // static functions only
    }

    /**
     * get phone subscriber id
     * @param context
     * @return SubscriberId
     */
    public static String getSubscriberId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSubscriberId();
    }

    /**
     * get application's pid
     *
     * @param packageName
     * @return use application's package name to get pid
     */
    public static int getPid(String packageName) {
        ActivityManager am = (ActivityManager) ServiceManager.getService(Context.ACTIVITY_SERVICE);
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

    /**
     * get application's uid
     *
     * @param packageName
     * @return
     */
    public static int getUid(String packageName) {
        try {
            return ((PackageManager)ServiceManager.getService("package")).getApplicationInfo(packageName, 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

}
