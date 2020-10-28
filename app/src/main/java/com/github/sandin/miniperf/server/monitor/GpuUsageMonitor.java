package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.GpuUsage;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.List;
import java.util.Map;

public class GpuUsageMonitor implements IMonitor<GpuUsage> {

    private static final String TAG = "GpuMonitor";

    private int getGpuUsage() {
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.GPU_USAGE_SYSTEM_FILE_PATHS);
        System.out.println(content.toString());
        float usagePercentage = 0;
        if (content.size() > 0) {
            String line = content.get(0).trim();
            int firstIndex = line.indexOf(" ");
            int lastIndex = line.lastIndexOf(" ");
            int used = Integer.parseInt(line.substring(0, firstIndex));
            int total = Integer.parseInt(line.substring(lastIndex + 1));
            if (used != 0 && total != 0)
                usagePercentage = ((float) used / total) * 100;
        }
        return Math.round(usagePercentage);
    }

    @Override
    public GpuUsage collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        int gpuUsage = getGpuUsage();
        Log.i(TAG, "collect gpu usage : " + gpuUsage);
        GpuUsage usage = GpuUsage.newBuilder().setGpuUsage(gpuUsage).build();
        if (data != null) {
            data.setGpuUsage(usage);
        }
        return usage;
    }

    @Override
    public void setInterestingFields(Map<ProfileReq.DataType, Boolean> dataTypes) {
        // pass
    }
}
