package com.github.sandin.miniperf.server.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import java.util.List;

/**
 * Android Process Utils
 * TODO use context
 */
public final class AndroidProcessUtils {

    private AndroidProcessUtils() {
        // static functions only
    }

    /**
     * get phone subscriber id
     *
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
    public static int getPid(Context context, String packageName) {
        //ActivityManager am = (ActivityManager) ServiceManager.getService(Context.ACTIVITY_SERVICE);
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

    /**
     * get application's uid
     *
     * @param packageName
     * @return
     */
    public static int getUid(Context context, String packageName) {
        try {
            //return ((PackageManager) ServiceManager.getService("package")).getApplicationInfo(packageName, 0).uid;
            return context.getPackageManager().getApplicationInfo(packageName, 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * ????????????????????????????????????
     *
     * @param context     ?????????
     * @param packageName
     * @return true ?????????????????????false ??????????????????
     */
    public static boolean checkAppIsRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> lists;
        if (am != null) {
            lists = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo appProcess : lists) {
                if (appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
