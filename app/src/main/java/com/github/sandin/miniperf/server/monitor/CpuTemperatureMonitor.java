package com.github.sandin.miniperf.server.monitor;

import android.os.Build;
import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.Temp;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.List;
import java.util.Map;

public class CpuTemperatureMonitor implements IMonitor<Temp> {
    private static final String TAG = "CpuTemperatureMonitor";

    private int getCpuTemperatureFromSystemFile() {
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.CPU_TEMPERATURE_SYSTEM_FILE_PATHS);
        if (content.size() == 0) {
            content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.CPU_TEMPERATURE_SYSTEM_FILE_PATHS_SPARE);
        }
        System.out.println("content : " + content.toString());
        int temperature = 0;
        int count = 0;
        if (content.size() > 0) {
            for (String line : content) {
                if (!line.equals("")) {
                    System.out.println("line : " + line);
                    int temp = Integer.parseInt(line);
                    temp = Math.abs(temp);
                    if (temp >= 100 && temp < 1000) {
                        temp = (Math.round((float) temp / 10));
                    } else if (temp >= 1000 && temp < 10000) {
                        temp = (Math.round((float) temp / 100));
                    } else if (temp >= 1000) {
                        temp = (Math.round((float) temp / 1000));
                    }
                    System.out.println("line temp : " + temp);
                    temperature += temp;
                    count++;
                    System.out.println("now total : " + temp);
                    System.out.println("now count : " + count);
                }
            }
        }
        if (count != 0 && temperature != 0)
            temperature = Math.round((float) temperature / count);
        return temperature;
    }

    private int getCpuTemperatureFromHardwareProperties() {
        List<String> hardware_properties = ReadSystemInfoUtils.readInfoFromDumpsys("hardware_properties", new String[0]);
        int temperature = -1;
        if (hardware_properties.size() > 0) {
            String line = hardware_properties.get(1);
            int index = line.indexOf('[');
            String[] temps = line.substring(index + 1, line.length() - 1).split(",\\s+");
            int count = 0;
            float total = 0;
            for (String temp : temps) {
                total += Float.parseFloat(temp);
                count++;
            }
            temperature = Math.round(total / count);
        }
        return temperature;
    }

    @Override
    public Temp collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect cpu temperature data: timestamp=" + timestamp);
        int cpuTemperature;
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                cpuTemperature = getCpuTemperatureFromHardwareProperties();
                //TODO java.lang.RuntimeException: Bad file descriptor
            } catch (Exception e) {
                e.printStackTrace();
                cpuTemperature = getCpuTemperatureFromSystemFile();
            }
            if (cpuTemperature == -1)
                cpuTemperature = getCpuTemperatureFromSystemFile();
        } else {
            cpuTemperature = getCpuTemperatureFromSystemFile();
        }
        Log.v(TAG, ": cpuTemp " + cpuTemperature);
        Temp temp = Temp.newBuilder().setTemp(cpuTemperature).build();
        if (data != null)
            data.setTemp(temp);
        return temp;
    }

    @Override
    public void setInterestingFields(Map<ProfileReq.DataType, Boolean> dataTypes) {
        // pass
    }
}
