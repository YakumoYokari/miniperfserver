package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import com.github.sandin.miniperf.server.bean.GpuInfo;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.server.proto.GpuFreq;
import com.github.sandin.server.proto.GpuUsage;
import com.github.sandin.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.List;

/*
 *  Gpu Monitor
 * */
public class GpuMonitor implements IMonitor<GpuInfo> {

    private static final String TAG = "GpuMonitor";


    /**
     * dump gpu info
     *
     * @param gpuInfo
     * @return string of gpu info
     */
    public static String dumpGpuInfo(GpuInfo gpuInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("[GpuInfo");
        sb.append(", GpuUsage=").append(gpuInfo.getGpuUsage().getGpuUsage()).append("%");
        sb.append(", GpuClock=").append(gpuInfo.getGpuFreq().getGpuFreq());
        sb.append("]");
        return sb.toString();
    }

    private int getGpuClock() {
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.sGpuClockSystemFilePaths);
        int gpuClock = 0;
        if (content.size() > 0)
            gpuClock = Integer.parseInt(content.get(0)) / 1000000;//hz -> mhz
        return gpuClock;
    }

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
    public GpuInfo collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect gpu data: timestamp=" + timestamp);
        GpuInfo gpuInfo = new GpuInfo();
        gpuInfo.setGpuUsage(GpuUsage.newBuilder().setGpuUsage(getGpuUsage()).build());
        gpuInfo.setGpuFreq(GpuFreq.newBuilder().setGpuFreq(getGpuClock()).build());
        Log.v(TAG, dumpGpuInfo(gpuInfo));
        data.setGpuFreq(gpuInfo.getGpuFreq());
        data.setGpuUsage(gpuInfo.getGpuUsage());
        return gpuInfo;
    }
}
