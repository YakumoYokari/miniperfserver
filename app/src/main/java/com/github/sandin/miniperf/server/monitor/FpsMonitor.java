package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.sandin.miniperf.server.bean.FpsInfo;
import com.github.sandin.miniperf.server.bean.JankInfo;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.FPS;
import com.github.sandin.miniperf.server.proto.FrameTime;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
//TODO bug
public class FpsMonitor implements IMonitor<FpsInfo> {

    private static final String TAG = "FpsMonitor";
    private final static String SERVICE_NAME = "SurfaceFlinger";
    private String mLayerName;
    private long mRefreshPeriod;
    private long mLatestSeen = 0;
    private List<Long> mElapsedTimes = new ArrayList<>();

    private boolean getLayerName(@NonNull String packageName) {
        List<String> result = ReadSystemInfoUtils.readInfoFromDumpsys(SERVICE_NAME, new String[]{"--list"});
        for (String line : result) {
            if (line.startsWith(packageName + "/" + packageName)) {
                mLayerName = line.trim();
                return true;
            }
        }
        return false;
    }

    private List<String> getFramesData(@NonNull String layerName) {
        List<String> timestamps = new LinkedList<>();
        if (layerName != null) {
            timestamps = ReadSystemInfoUtils.readInfoFromDumpsys(SERVICE_NAME, new String[]{"--latency", mLayerName});
            if (timestamps.size() > 0) {
                mRefreshPeriod = Long.parseLong(timestamps.get(0));
                Log.i(TAG, "refresh period is " + mRefreshPeriod);
            }
        }
        return timestamps;
    }

    private boolean sample(long readyTimeStamp, long presentTimeStamp) {
        if (presentTimeStamp == Long.MAX_VALUE || readyTimeStamp == Long.MAX_VALUE) {
            return false;
        } else if (presentTimeStamp < mLatestSeen) {
            return false;
        } else if (presentTimeStamp == mLatestSeen) {
            return true;
        } else {
            mElapsedTimes.add(presentTimeStamp);
            mLatestSeen = presentTimeStamp;
            return false;
        }
    }

    //TODO 极少数情况下frametime数量无法与fps对应上 可能筛选算法存在问题
    private List<Long> getFrameTimes(@NonNull String packageName) {
        boolean hasLayerName = getLayerName(packageName);
        if (!hasLayerName) {
            Log.e(TAG, "application hasn't start or package name is error!");
            return null;
        }
        List<String> framesData = getFramesData(mLayerName);
        if (framesData.size() == 1) {
            Log.e(TAG, "can't get frames data !");
            return null;
        }
        mElapsedTimes.clear();
        boolean overlap = false;
        for (String line : framesData) {
            String[] parts = line.split("\t");
            if (parts.length == 3) {
                if (sample(Long.parseLong(parts[2]), Long.parseLong(parts[1]))) {
                    overlap = true;
                }
            }
        }
        if (!overlap)
            Log.e(TAG, "No overlap with previous poll, we missed some frames!");
        List<Long> frameTimes = new ArrayList<>();
        if (mElapsedTimes.size() > 0) {
            for (int i = 0; i < mElapsedTimes.size() - 1; i++) {
                frameTimes.add((mElapsedTimes.get(i + 1) - mElapsedTimes.get(i)) / 10000);
            }
        }
        Log.i(TAG, "Elapsed time : " + mElapsedTimes.toString());
        return frameTimes;
    }

    private JankInfo checkJank(List<Long> frameTimes) {
        JankInfo jankInfo = new JankInfo();
        int jank = 0;
        int bigJank = 0;
        long first_3s_frame_time = 0;
        long first_2s_frame_time = 0;
        long first_1s_frame_time = 0;
        for (Long frameTime : frameTimes) {
            Double time = (double) frameTime;
            if (first_1s_frame_time != 0 || first_2s_frame_time != 0 || first_3s_frame_time != 0) {
                double average = (first_1s_frame_time + first_2s_frame_time + first_3s_frame_time) / (3.0 * 2.0) + 2.0;
                if ((time.compareTo(average) > 0) && (time.compareTo(8533.333333333333) > 0)) {
                    jank++;
                    if (time.compareTo(12700.0) > 0)
                        bigJank++;
                    first_1s_frame_time = 0;
                    first_2s_frame_time = 0;
                    first_3s_frame_time = 0;
                }
            } else {
                first_3s_frame_time = first_2s_frame_time;
                first_2s_frame_time = first_1s_frame_time;
                first_1s_frame_time = frameTime;
            }
        }
        jankInfo.setJank(jank);
        jankInfo.setBigJank(bigJank);
        return jankInfo;
    }

    @Override
    public FpsInfo collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.i(TAG, "start collect fps info");
        FpsInfo fpsInfo = new FpsInfo();
        List<Long> frameTimes = getFrameTimes(targetApp.getPackageName());
        JankInfo jankInfo = checkJank(frameTimes);
        float fps = ((float) (mElapsedTimes.size() - 1)) / ((float) (mElapsedTimes.get(mElapsedTimes.size() - 1) - mElapsedTimes.get(0)) / (100 * 1000 * 10000));
        Log.i(TAG, "collect fps success : " + fps);
        fpsInfo.setFps(FPS.newBuilder().setFps(fps).build());
        fpsInfo.setFrameTime(FrameTime.newBuilder().addAllFrameTime(frameTimes).build());
        if (data != null) {
            data.setFps(FPS.newBuilder().setFps(fps).setJank(jankInfo.getJank()).setBigJank(jankInfo.getBigJank()))
                    .setFrameTime(FrameTime.newBuilder().addAllFrameTime(frameTimes));
        }
        Log.i(TAG, "collect fps info success : " + fpsInfo.toString());
        return fpsInfo;
    }
}
