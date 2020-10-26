package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.Temp;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.List;

public class CpuTemperatureMonitor implements IMonitor<Temp> {
    private static final String TAG = "CpuTemperatureMonitor";

    private int getCpuTemperature() {
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.CPU_TEMPERATURE_SYSTEM_FILE_PATHS);
        int temperature = 0;
        if (content.size() > 0) {
            temperature = Integer.parseInt(content.get(0));
            //TODO 某些机型的配置文件读出来只需要除10 已修复 待回测
            if (temperature >= 100 && temperature < 1000) {
                temperature = Math.abs(Math.round((float) temperature / 10));
            } else if (temperature >= 1000) {
                temperature = Math.abs(Math.round((float) temperature / 1000));
            }
        }
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
}
