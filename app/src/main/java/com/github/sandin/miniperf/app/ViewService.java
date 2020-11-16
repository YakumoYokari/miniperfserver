package com.github.sandin.miniperf.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class ViewService extends Service {

    private static final String TAG = "MiniPerfApp";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //start server
        new Thread(new MiniPerfAppServer(getApplicationContext())).start();
        createNotificationBar();
    }

    private void createNotificationBar() {
        Log.i(TAG, "create notification bar");
        //target version  >= 26 需要添加channel
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.app_icon);
        builder.setContentTitle("MiniPerf");
        builder.setContentText("MiniPerf is running");
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        builder.setContentIntent(PendingIntent.getActivities(this, 0, new Intent[]{intent}, PendingIntent.FLAG_UPDATE_CURRENT));
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("1", "MiniPerf", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
            builder.setChannelId("1");
        }
        startForeground(100, builder.build());
    }
}
