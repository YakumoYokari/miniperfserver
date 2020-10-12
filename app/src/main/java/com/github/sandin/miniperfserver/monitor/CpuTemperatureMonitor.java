package com.github.sandin.miniperfserver.monitor;

import android.content.Context;
import android.util.Log;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.proto.Temp;

import java.io.File;
import java.util.Scanner;

public class CpuTemperatureMonitor implements IMonitor<Temp> {
    private static final String TAG = "CpuTemperatureMonitor";
    private String[] mTemperatureSettingFilePaths = {"/sys/kernel/debug/tegra_thermal/temp_tj",
            "/sys/devices/platform/s5p-tmu/curr_temp",
            "/sys/devices/virtual/thermal/thermal_zone1/temp",
            "/sys/devices/system/cpu/cpufreq/cput_attributes/cur_temp",
            "/sys/devices/virtual/hwmon/hwmon2/temp1_input",
            "/sys/devices/platform/coretemp.0/temp2_input",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
            "/sys/class/thermal/thermal_zone7/temp",
            "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone7/temp",
            "/sys/devices/platform/s5p-tmu/temperature",
            "/sys/devices/w1 bus master/w1_master_attempts",
            "/sys/class/thermal/thermal_zone0/temp"
    };

    private int getCpuTemperature() {
        int temp = 0;
        for (String path : mTemperatureSettingFilePaths) {
            File temperatureSettingFile = new File(path);
            try (Scanner scanner = new Scanner(temperatureSettingFile)) {
                if (!scanner.hasNext())
                    break;
                String line = scanner.nextLine();
                Log.v(TAG, path + " " + line);
                temp = Integer.parseInt(line);
                if (temp != 0) {
                    if (temp >= 100)
                        temp = Math.round((float) temp / 1000L);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return temp;
    }

    @Override
    public Temp collect(Context context, TargetApp targetApp, long timestamp) throws Exception {
        Log.v(TAG, "collect cpu temperature data: timestamp=" + timestamp);
        int cpuTemperature = getCpuTemperature();
        Log.v(TAG, ": cpuTemp " + cpuTemperature);
        return Temp.newBuilder().setTemp(cpuTemperature).build();
    }
}
