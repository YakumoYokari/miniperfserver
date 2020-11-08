package com.github.sandin.miniperf.app;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.github.sandin.miniperf.server.proto.AppInfo;
import com.github.sandin.miniperf.server.proto.GetScreenInfoRsp;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;


public class PhoneInfoManager {

    private static final String TAG = "MiniPerf";

    /**
     * 获取屏幕尺寸
     *
     * @param context 上下文
     * @return GetScreenInfoRsp 包括屏幕分辨率和尺寸(inch)
     */
    public static GetScreenInfoRsp getScreenInfo(Context context) {
        Log.i(TAG, "start to get screen info");
        int width, height;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display defaultDisplay = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
        //compute screen size
        double screenSize = Math.sqrt(
                Math.pow(((double) width / displayMetrics.xdpi), 2) + Math.pow(((double) height / displayMetrics.ydpi), 2)
        );
        return GetScreenInfoRsp.newBuilder()
                .setScreenSize(BigDecimal.valueOf(screenSize).setScale(2, 4).floatValue())
                .setHeight(height)
                .setWidth(width)
                .build();
    }


    /**
     * 获取内存阈值
     *
     * @param context 上下文
     * @return 内存阈值 单位为Mb
     */
    public static int getLMKThreshold(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(memoryInfo);
        return (int) ((memoryInfo.threshold / 1024) / 1024);
    }


    /**
     * 获取应用类表
     *
     * @param context 上下文
     * @return 应用列表
     */
    public static List<AppInfo> getAppInfoList(Context context) {
        ArrayList<AppInfo> appInfoList = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(0);
        Intent intent = new Intent("android.intent.action.MAIN", null);
        for (ApplicationInfo applicationInfo : installedApplications) {
            AppInfo.Builder appInfoBuilder = AppInfo.newBuilder();
            //跳过自己
            if (applicationInfo.packageName.equals(context.getPackageName())) {
                continue;
            }
            //跳过没有Activity的（无法启动）
            intent.setPackage(applicationInfo.packageName);
            if (packageManager.queryIntentActivities(intent, 0).isEmpty()) {
                continue;
            }
            //isSystemApp
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                appInfoBuilder.setIsSystemApp(true);
            }
            //packageName
            appInfoBuilder.setPackageName(applicationInfo.packageName);
            //pid
            appInfoBuilder.setPid(applicationInfo.uid);
            //version
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(applicationInfo.packageName, 0);
                String versionName;
                if (packageInfo.versionName == null) {
                    versionName = "";
                } else {
                    versionName = packageInfo.versionName;
                }
                appInfoBuilder.setVersion(versionName);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            //label
            appInfoBuilder.setLabel(applicationInfo.loadLabel(packageManager).toString());

            //icon
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Drawable applicationIcon = packageManager.getApplicationIcon(applicationInfo);
            Bitmap bitmap = null;
            getIcon:
            {
                if (applicationIcon instanceof BitmapDrawable) {
                    final BitmapDrawable bitmapDrawable = (BitmapDrawable) applicationIcon;
                    if (bitmapDrawable.getBitmap() != null) {
                        bitmap = bitmapDrawable.getBitmap();
                        break getIcon;
                    }
                }
                if (applicationIcon.getIntrinsicWidth() > 0 && applicationIcon.getIntrinsicHeight() > 0) {
                    bitmap = Bitmap.createBitmap(applicationIcon.getIntrinsicWidth(), applicationIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                } else {
                    bitmap = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888);
                }
                final Canvas canvas = new Canvas(bitmap);
                applicationIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                applicationIcon.draw(canvas);
            }
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
            //格式转换
            appInfoBuilder.setIcon(ByteString.copyFrom(byteArrayOutputStream.toByteArray()));

            //debuggable
            if ((applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                appInfoBuilder.setIsDebuggable(true);
            }

            //process
            @SuppressLint("WrongConstant") PackageInfo packageArchiveInfo = packageManager.getPackageArchiveInfo(applicationInfo.sourceDir, 15);
            TreeSet<String> set = new TreeSet<>();
            if (packageArchiveInfo.activities != null) {
                for (ActivityInfo activityInfo : packageArchiveInfo.activities) {
                    if (activityInfo.processName != null) {
                        set.add(activityInfo.processName);
                    }
                }
            }
            if (packageArchiveInfo.receivers != null) {
                for (ActivityInfo activityInfo2 : packageArchiveInfo.receivers) {
                    if (activityInfo2.processName != null) {
                        set.add(activityInfo2.processName);
                    }
                }
            }
            if (packageArchiveInfo.providers != null) {
                for (ProviderInfo providerInfo : packageArchiveInfo.providers) {
                    if (providerInfo.processName != null) {
                        set.add(providerInfo.processName);
                    }
                }
            }
            if (packageArchiveInfo.services != null) {
                for (final ServiceInfo serviceInfo : packageArchiveInfo.services) {
                    if (serviceInfo.processName != null) {
                        set.add(serviceInfo.processName);
                    }
                }
            }
            set.remove(applicationInfo.packageName);
            for (String s : set) {
                appInfoBuilder.addProcessList(s);
            }

            //build
            AppInfo appInfo = appInfoBuilder.build();
            appInfoList.add(appInfo);
        }
        return appInfoList;
    }


}
