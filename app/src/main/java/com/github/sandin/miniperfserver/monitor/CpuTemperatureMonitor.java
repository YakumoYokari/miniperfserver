package com.github.sandin.miniperfserver.monitor;

import android.content.Context;
import android.util.Log;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.data.DataSource;
import com.github.sandin.miniperfserver.proto.Temp;
import com.github.sandin.miniperfserver.util.ReadSystemInfoUtils;

import java.util.List;

public class CpuTemperatureMonitor implements IMonitor<Temp> {
    private static final String TAG = "CpuTemperatureMonitor";

    private int getCpuTemperature() {
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.sCpuTemperatureSystemFilePaths);
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
    public Temp collect(Context context, TargetApp targetApp, long timestamp) throws Exception {
        Log.v(TAG, "collect cpu temperature data: timestamp=" + timestamp);
        int cpuTemperature = getCpuTemperature();
        Log.v(TAG, ": cpuTemp " + cpuTemperature);
        return Temp.newBuilder().setTemp(cpuTemperature).build();
    }
}
