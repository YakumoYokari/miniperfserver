package com.github.sandin.miniperf.server.monitor;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import com.genymobile.scrcpy.wrappers.ServiceManager;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.Power;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

public class BatteryMonitor implements IMonitor<Power> {

    private static final String TAG = "BatteryMonitor";
    private final BatteryManager mBatteryManager;
    //collect data from server or dex or app, default use dex
    private String mSource;

    /**
     * Constructor
     */
    public BatteryMonitor() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param source  data source
     */
    public BatteryMonitor(String source) {
        this.mBatteryManager = (BatteryManager) ServiceManager.getService(Context.BATTERY_SERVICE);
        this.mSource = source;
    }

    /**
     * dump power info
     *
     * @param power
     * @return string of power info
     */
    public static String dumpPower(Power power) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Power");
        sb.append(", current=").append(power.getCurrent());
        sb.append(", voltage=").append(power.getVoltage());
        sb.append("]");
        return sb.toString();
    }

    //读取配置文件出来的单位为μ
    private static int micro2Milli(int micro) {
        return Math.round((float) micro / 1000);
    }

    /**
     * get voltage from dump
     */
    @VisibleForTesting
    private int getVoltageFromDump() {
        int voltage = 0;
        List<String> content = ReadSystemInfoUtils.readInfoFromDumpsys("battery");
        if (content.size() > 0) {
            for (String line : content) {
                if (line.startsWith("voltage:"))
                    voltage = Integer.parseInt(line.substring(8).trim());
            }
        }
        return voltage;
    }

    @VisibleForTesting
    private Power getPowerInfoFromServer() {
        int voltage = 0;
        int current = 0;
        List<String> currentContent = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.CURRENT_SYSTEM_FILE_PATHS);
        List<String> voltageContent = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.VOLTAGE_SYSTEM_FILE_PATHS);
        if (currentContent.size() > 0 && voltageContent.size() > 0) {
            voltage = micro2Milli(Integer.parseInt(voltageContent.get(0)));
            current = micro2Milli(Math.abs(Integer.parseInt(currentContent.get(0))));
        }
        return Power.newBuilder().setCurrent(current).setVoltage(voltage).build();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @VisibleForTesting
    private Power getPowerInfoFromApp() {
        int current = micro2Milli(Math.abs(mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)));
        int voltage = 0; //FIXME: mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("voltage", -1);
        return Power.newBuilder().setCurrent(current).setVoltage(voltage).build();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @VisibleForTesting
    private Power getPowerInfoFromDex() {
        int current = micro2Milli(Math.abs(mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)));
        int voltage = getVoltageFromDump();
        return Power.newBuilder().setCurrent(current).setVoltage(voltage).build();
    }

    @Override
    public Power collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect battery data: timestamp=" + timestamp);
        if (Build.VERSION.SDK_INT < 21) {
            return Power.getDefaultInstance();
        }
        Power power;
        switch (mSource) {
            case "server":
                power = getPowerInfoFromServer();
                break;
            case "app":
                power = getPowerInfoFromApp();
                break;
            default:
                power = getPowerInfoFromDex();
                break;
        }
        data.setPower(power);
        Log.v(TAG, dumpPower(power));
        return power;
    }
}
