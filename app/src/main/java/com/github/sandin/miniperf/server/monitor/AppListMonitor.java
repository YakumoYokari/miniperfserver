package com.github.sandin.miniperf.server.monitor;

import android.annotation.SuppressLint;
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
import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.AppInfo;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class AppListMonitor implements IMonitor<List<AppInfo>> {

    private static final String TAG = "AppListMonitor";
    private final PackageManager mPackageManager;
    private final Context mContext;

    public AppListMonitor(Context context) {
        mPackageManager = context.getPackageManager();
        mContext = context;
    }

    @Override
    public List<AppInfo> collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect application list data: timestamp=" + timestamp);
        return getAppInfo();
    }

    @Override
    public void setInterestingFields(Map<ProfileReq.DataType, Boolean> dataTypes) {
        // pass
    }

    public List<AppInfo> getAppInfo() {
        ArrayList<AppInfo> appInfoList = new ArrayList<>();
        List<ApplicationInfo> installedApplications = mPackageManager.getInstalledApplications(0);
        Intent intent = new Intent("android.intent.action.MAIN", null);
        for (ApplicationInfo applicationInfo : installedApplications) {
            AppInfo.Builder appInfoBuilder = AppInfo.newBuilder();
            //跳过自己
            if (applicationInfo.packageName.equals(mContext.getPackageName())) {
                continue;
            }
            //跳过没有Activity的（无法启动）
            intent.setPackage(applicationInfo.packageName);
            if (mContext.getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
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
                PackageInfo packageInfo = mPackageManager.getPackageInfo(applicationInfo.packageName, 0);
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
            appInfoBuilder.setLabel(applicationInfo.loadLabel(mContext.getPackageManager()).toString());

            //icon
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Drawable applicationIcon = mContext.getPackageManager().getApplicationIcon(applicationInfo);
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
            @SuppressLint("WrongConstant") PackageInfo packageArchiveInfo = mContext.getPackageManager().getPackageArchiveInfo(applicationInfo.sourceDir, 15);
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
