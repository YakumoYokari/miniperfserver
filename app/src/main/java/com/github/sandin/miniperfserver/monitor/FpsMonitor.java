package com.github.sandin.miniperfserver.monitor;

import android.content.Context;
import android.util.Log;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.proto.FPS;
import com.github.sandin.miniperfserver.util.AdbUtils;

import java.io.IOException;
import java.util.List;

public class FpsMonitor implements IMonitor<FPS> {

    private static final String TAG = "FpsMonitor";
    private String mLayerName;

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

    @Override
    public FPS collect(Context context, TargetApp targetApp, long timestamp) throws Exception {
        boolean hasLayerName = getLayerName(targetApp.getPackageName());
        if (hasLayerName) {
            Log.e(TAG, "App not started or package name error");
            return null;
        }
        String command = "dumpsys SurfaceFlinger --latency " + mLayerName;
        List<String> result = AdbUtils.executeCommand(command);
        if (result.size() == 1)
            return null;
        else if (result.get(1).length() == 3) {
            for (String line : result) {
                String[] timeStamps = line.split(" ");
                long t0 = Long.parseLong(timeStamps[0]);//start time
                long t1 = Long.parseLong(timeStamps[1]);//vsync
                long t2 = Long.parseLong(timeStamps[2]);//finish time
            }
        }
        return null;
    }
}
