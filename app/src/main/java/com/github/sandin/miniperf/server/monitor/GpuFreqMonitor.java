package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.GpuFreq;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.List;

/*
 *  Gpu Monitor
 * */
public class GpuFreqMonitor implements IMonitor<GpuFreq> {

    private static final String TAG = "GpuMonitor";

//    /**
//     * dump gpu info
//     *
//     * @param gpuInfo
//     * @return string of gpu info
//     */
//    public static String dumpGpuInfo(GpuInfo gpuInfo) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("[GpuInfo");
//        sb.append(", GpuUsage=").append(gpuInfo.getGpuUsage().getGpuUsage()).append("%");
//        sb.append(", GpuClock=").append(gpuInfo.getGpuFreq().getGpuFreq());
//        sb.append("]");
//        return sb.toString();
//    }

    private int getGpuClock() {
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.sGpuClockSystemFilePaths);
        int gpuClock = 0;
        if (content.size() > 0)
            gpuClock = Integer.parseInt(content.get(0)) / 1000000;//hz -> mhz
        return gpuClock;
    }


    @Override
    public GpuFreq collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect gpu freq data: timestamp=" + timestamp);
        int gpuClock = getGpuClock();
        GpuFreq gpuFreq = GpuFreq.newBuilder().setGpuFreq(gpuClock).build();
        if (data != null) {
            data.setGpuFreq(gpuFreq);
            Log.i(TAG, "collect gpu info success : " + data.getGpuFreq() + " " + data.getGpuUsage());
        }
        return gpuFreq;
    }
}
