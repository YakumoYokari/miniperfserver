package com.github.sandin.miniperf.server.monitor;

import android.os.SystemClock;
import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * The Performance Monitor
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";

    /**
     * The callback of monitor
     */
    public interface Callback {

        /**
         * On new data update
         *
         * @param data the new data
         */
        void onUpdate(ProfileNtf data);

    }

    private final int mIntervalMs;
    private final int mScreenshotIntervalMs;
    private final List<IMonitor> mMonitors = new ArrayList<>();

    private List<Callback> mCallback = new ArrayList<>();

    @Nullable
    private Thread mThread;

    /**
     * Target application
     */
    @Nullable
    private TargetApp mTargetApp;

    private boolean mIsRunning = false;

    /**
     * Constructor
     *
     * @param intervalMs           interval time in ms
     * @param screenshotIntervalMs screenshot interval time in ms
     */
    public PerformanceMonitor(int intervalMs, int screenshotIntervalMs) {
        mIntervalMs = intervalMs;
        mScreenshotIntervalMs = screenshotIntervalMs;
    }

    /**
     * Register a data callback
     *
     * @param callback callback
     */
    public void registerCallback(Callback callback) {
        mCallback.add(callback);
    }

    /**
     * Unregister a data callback
     *
     * @param callback callback
     */
    public void unregisterCallback(Callback callback) {
        mCallback.remove(callback);
    }

    /**
     * Notify all callbacks
     *
     * @param data the new data
     */
    private void notifyCallbacks(ProfileNtf data) {
        for (Callback callback : mCallback) {
            callback.onUpdate(data);
        }
    }

    /**
     * Register a monitor
     *
     * @param monitor monitor
     */
    public void registerMonitor(IMonitor monitor) {
        mMonitors.add(monitor);
    }

    /**
     * Unregister a monitor
     *
     * @param monitor monitor
     */
    public void unregisterMonitor(IMonitor monitor) {
        mMonitors.remove(monitor);
    }

    /**
     * Start profile a app
     *
     * @param targetApp target app
     * @param dataTypes profile data types
     * @return success/fail
     */
    public boolean start(TargetApp targetApp, List<ProfileReq.DataType> dataTypes) {
        if (mIsRunning) {
            Log.w(TAG, "server has already been started!");
            return false;
        }
        mTargetApp = targetApp;
        mIsRunning = true;

        for (ProfileReq.DataType dataType : dataTypes) {
            switch (dataType.name()) {
                case "FPS":
                    registerMonitor(new FpsMonitor());
                    break;
                case "MEMORY":
                    registerMonitor(new MemoryMonitor());
                    break;
                case "BATTERY":
                    registerMonitor(new BatteryMonitor(null));
                    break;
                case "CPU_TEMPERATURE":
                    registerMonitor(new CpuTemperatureMonitor());
                    break;
            }
        }

        mThread = new Thread(new MonitorWorker());
        mThread.start();
        return true;
    }

    public void stop() {
        if (mIsRunning) {
            mIsRunning = false;

            // TODO: unregisterMonitors

            if (mThread != null) {
                try {
                    mThread.join();
                } catch (InterruptedException ignore) {
                }
                mThread = null;
            }
        }
    }

    private ProfileNtf collectData(long timestamp) {
        ProfileNtf.Builder data = ProfileNtf.newBuilder();
        data.setTimestamp(timestamp);
        try {
            for (IMonitor<?> monitor : mMonitors) {
                monitor.collect(mTargetApp, timestamp, data);
                Log.v(TAG, "collect data: " + data);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return data.build();
    }

    private class MonitorWorker implements Runnable {
        private long mFirstTime = 0;
        private long mTickCount = 0;

        @Override
        public void run() {
            mFirstTime = SystemClock.uptimeMillis();
            while (mIsRunning) {
                long startTime = SystemClock.uptimeMillis();
                ProfileNtf collectData = collectData(startTime - mFirstTime);
                notifyCallbacks(collectData); // send data

                mTickCount++;
                long costTime = SystemClock.uptimeMillis() - startTime;
                long sleepTime = mIntervalMs - costTime - 2;  // | costTime | sleepTime |
                //long nextTickTime = mFirstTime + (mIntervalMs * mTickCount);
                //long sleepTime = nextTickTime - SystemClock.uptimeMillis();
                if (sleepTime > 0) {
                    SystemClock.sleep(sleepTime);
                } else {
                    Log.w(TAG, "Collect data take too many time, no need to sleep, cost time=" + costTime);
                }
            }
        }
    }

}
