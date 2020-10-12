package com.github.sandin.miniperfserver.monitor;

import android.content.Context;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.genymobile.scrcpy.wrappers.ServiceManager;
import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.proto.Power;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BatteryMonitor implements IMonitor<Power> {

    private static final String TAG = "BatteryMonitor";
    private final BatteryManager mBatteryManager;
    //collect data from server or dex or app, default use dex
    private String mSource;
    private String[] currentSettingFilePaths = {
            "/sys/class/power_supply/battery/current_now",
            "/sys/class/power_supply/battery/batt_current_now",
            "/sys/class/power_supply/battery/batt_current"
    };
    private String[] voltageSettingFilePaths = {"/sys/class/power_supply/battery/voltage_now"};

    public BatteryMonitor(Context context) {
        this.mBatteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    }

    /**
     * Constructor
     *
     * @param context system context
     * @param source  data source
     */
    public BatteryMonitor(Context context, String source) {
        this.mBatteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        this.mSource = source;
    }

    public static String dumpPower(Power power) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Power");
        sb.append(", current=").append(power.getCurrent());
        sb.append(", voltage=").append(power.getVoltage());
        sb.append("]");
        return sb.toString();
    }

    @VisibleForTesting
    private int getBatteryInfoFromSettingFile(String[] paths) throws Exception {
        int info = 0;
        FileReader fileReader = null;
        BufferedReader br = null;
        for (String path : paths) {
            File settingFile = new File(path);
            if (settingFile != null) {
                fileReader = new FileReader(settingFile);
                br = new BufferedReader(fileReader);
                info = Integer.parseInt(br.readLine());
                info /= 1000; //u -> m
                if (info != 0) {
                    break;
                }
            }
        }
        close(br, fileReader);
        return info;
    }

    /**
     * get voltage from dump
     */
    @VisibleForTesting
    private int getVoltageFromDump() throws IOException {
        IBinder batteryService = ServiceManager.getService("battery");
        if (batteryService != null) {
            ParcelFileDescriptor[] pipe = new ParcelFileDescriptor[0];
            BufferedReader reader = null;
            try {
                pipe = ParcelFileDescriptor.createPipe();
                batteryService.dump(pipe[1].getFileDescriptor(), new String[0]);
                //first read, second write
                reader = new BufferedReader(new InputStreamReader(new ParcelFileDescriptor.AutoCloseInputStream(pipe[0])));
                String line = null;
                while (!line.startsWith("voltage:") && reader.ready()) {
                    line = reader.readLine().trim();
                }
                if (line != null) {
                    int voltage = Integer.parseInt(line.substring(8).trim());
                    return voltage;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(reader, pipe[0], pipe[1]);
            }
        }
        return 0;
    }

    private void close(Closeable... needCloseObjects) throws IOException {
        for (Closeable needCloseObject : needCloseObjects) {
            if (needCloseObject != null) {
                needCloseObject.close();
            }
        }
    }


    @Override
    public Power collect(Context context, TargetApp targetApp, long timestamp) throws Exception {
        Log.v(TAG, "collect battery data: timestamp=" + timestamp);
        if (Build.VERSION.SDK_INT < 21) {
            return Power.getDefaultInstance();
        }
        Power.Builder powerBuilder = Power.newBuilder();
        int current;
        int voltage;
        switch (mSource) {
            case "server":
                current = getBatteryInfoFromSettingFile(currentSettingFilePaths);
                voltage = getBatteryInfoFromSettingFile(voltageSettingFilePaths);
                break;
            case "app":
                current = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                voltage = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("voltage", -1);
                break;
            default:
                current = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                voltage = getVoltageFromDump();
                break;
        }
        if (Math.abs(current) > 10000) {
            current /= 1000;
        }
        if (Math.abs(voltage) > 10000) {
            voltage /= 1000;
        }
        powerBuilder.setVoltage(voltage);
        powerBuilder.setCurrent(current);
        Power power = powerBuilder.build();
        Log.v(TAG, dumpPower(power));
        return power;
    }
}
