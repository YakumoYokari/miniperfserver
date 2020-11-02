package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.Temp;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CpuTemperatureMonitor implements IMonitor<Temp> {
    private static final String TAG = "CpuTemperatureMonitor";

    private int getCpuTemperature() {
        List<String> content = new LinkedList<>();
        content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.CPU_TEMPERATURE_SYSTEM_FILE_PATHS);
        System.out.println("content : " + content.toString());
        //TODO 临时处理方法 荣耀30lite
        if (content.size() == 0 || content == null) {
            content = ReadSystemInfoUtils.readInfoFromSystemFile(new String[]{"/sys/devices/virtual/thermal/thermal_zone1/temp", "/sys/class/thermal/thermal_zone1/temp"});
        }
        int temperature = 0;
//        int[] counts = new int[100];
        int count = 0;
        if (content.size() > 0) {
            for (String line : content) {
                if (!line.equals("")) {
                    int temp = Integer.parseInt(line);
                    //TODO 某些机型的配置文件读出来只需要除10 已修复 待回测
                    temp = Math.abs(temp);
                    if (temp >= 100 && temp < 1000) {
                        temp = (Math.round((float) temp / 10));
                    } else if (temp >= 1000) {
                        temp = (Math.round((float) temp / 1000));
                    }
//                    counts[temp]++;
                    temperature += temp;
                    count++;
                }
            }
        }
        if (count != 0 && temperature != 0)
            temperature = Math.round((float) temperature / count);
//        int maxCount = 0;
//        int maxIndex = 0;
//        for (int i = 0; i < 100; i++) {
//            if (counts[i] > maxCount) {
//                maxCount = counts[i];
//                maxIndex = i;
//            }
//        }
//        temperature = maxIndex;
        return temperature;
    }

    @Override
    public Temp collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect cpu temperature data: timestamp=" + timestamp);
        int cpuTemperature = getCpuTemperature();
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
