package com.github.sandin.miniperfserver.monitor;

import android.util.Log;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.data.DataSource;
import com.github.sandin.miniperfserver.proto.ProfileNtf;
import com.github.sandin.miniperfserver.proto.Temp;
import com.github.sandin.miniperfserver.util.ReadSystemInfoUtils;

import java.util.List;

public class CpuTemperatureMonitor implements IMonitor<Temp> {
    private static final String TAG = "CpuTemperatureMonitor";

    private int getCpuTemperature() {
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.CPU_TEMPERATURE_SYSTEM_FILE_PATHS);
        int temperature = 0;
        if (content.size() > 0) {
            temperature = Integer.parseInt(content.get(0));
            if (temperature >= 100) {
                temperature = Math.round((float) temperature / 1000);
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
        data.setTemp(temp);
        return temp;
    }
}
