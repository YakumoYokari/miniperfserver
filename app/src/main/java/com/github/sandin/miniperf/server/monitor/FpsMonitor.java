package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.FPS;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.util.AdbUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;

public class FpsMonitor implements IMonitor<FPS> {

    private static final String TAG = "FpsMonitor";
    private String mLayerName;
    private long mRefreshPeriod;
    private long mLatestSeen = 0;
    private List<Long> mElapsedTimes = new ArrayList<>();

    private boolean getLayerName(String packageName) throws IOException {
        String command = "dumpsys SurfaceFlinger --list";
        List<String> result = AdbUtils.executeCommand(command);
        for (String line : result) {
            if (line.startsWith(packageName + "/" + packageName)) {
                mLayerName = line.trim();
                return true;
            }
        }
        return false;
    }

    private List<String> getFramesData(@NonNull String layerName) throws IOException {
        List<String> timestamps = new LinkedList<>();
        if (layerName != null) {
            String command = "dumpsys SurfaceFlinger --latency " + mLayerName;
            timestamps = AdbUtils.executeCommand(command);
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

    @Override
    public FPS collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        FPS.Builder builder = FPS.newBuilder();
        boolean hasLayerName = getLayerName(targetApp.getPackageName());
        if (hasLayerName) {
            Log.e(TAG, "App not started or package name error");
            return null;
        }
        List<Long> frameTimes = new LinkedList<>();
        List<String> framesData = getFramesData(mLayerName);
        if (framesData != null && framesData.size() > 1) {
            mRefreshPeriod = Integer.parseInt(framesData.get(0));
            boolean overlap = false;
            for (int i = 1; i < framesData.size(); i++) {
                String[] parts = framesData.get(i).split("\t");
                if (parts.length == 3)
                    if (sample(Long.parseLong(parts[2]), Long.parseLong(parts[1])))
                        overlap = true;

            }
            if (!overlap) {
                Log.e(TAG, "No overlap with previous poll");
            }
            long prevPresentTime = 0;
            for (Long presentTime : mElapsedTimes) {
                if (prevPresentTime == 0) {
                    prevPresentTime = presentTime;
                    continue;
                }
                long presentTimeDiff = presentTime - prevPresentTime;
                frameTimes.add(presentTimeDiff);
            }
        }
        //TODO
        float fps = 60;
        builder.setFps(60);
        data.setFps(FPS.newBuilder().setFps(60).setJank(0).setBigJank(0).build());
        return builder.build();
    }
}
