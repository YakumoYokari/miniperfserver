package com.github.sandin.miniperf.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;

public class MainActivity extends Activity {

    private static final String TAG = "MiniPerfApp";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MiniPerf app start!");
        this.startService(new Intent(this, ViewService.class));
        requestBackgroundResident();
        finish();
    }

    /**
     * 请求后台常驻
     */
    private void requestBackgroundResident() {
        Log.i(TAG, "request background resident");
        if (Build.VERSION.SDK_INT >= 23 && !((PowerManager) getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}