package com.github.sandin.miniperf.server.monitor;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Performance Monitor
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private final int mIntervalMs;
    private final int mScreenshotIntervalMs;
    private final List<IMonitor> mMonitors = new ArrayList<>();
    private List<Callback> mCallback = new ArrayList<>();
    //记录Monitor是否启动,启动value为true，key为dataType
    private Map<ProfileReq.DataType, Boolean> mMonitorStatus = new HashMap<>();
    private FpsMonitor mFpsMonitor;
    @Nullable
    private Thread mThread;
    /**
     * Target application
     */
    @Nullable
    private TargetApp mTargetApp;
    private boolean mIsRunning = false;
    //TODO use context
    private Context mContext;

    /**
     * Constructor
     *
     * @param intervalMs           interval time in ms
     * @param screenshotIntervalMs screenshot interval time in ms
     */
    public PerformanceMonitor(Context context, int intervalMs, int screenshotIntervalMs) {
        mContext = context;
        mIntervalMs = intervalMs;
        mScreenshotIntervalMs = screenshotIntervalMs;
        //init monitor status
        for (ProfileReq.DataType dataType : ProfileReq.DataType.values()) {
            mMonitorStatus.put(dataType, false);
        }
    }

    public void registerType(ProfileReq.DataType dataType) {
        mMonitorStatus.put(dataType, true);
    }

    public void unregisterType(ProfileReq.DataType dataType) {
        mMonitorStatus.put(dataType, false);
    }

    public void setMonitorFiledStatus(ProfileReq.DataType dataType, Boolean state) {
        mMonitorStatus.put(dataType, state);
    }

    public boolean getMonitorFiledStatus(ProfileReq.DataType dataType) {
        return mMonitorStatus.get(dataType);
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
            Log.i(TAG, "now data type is : " + dataType.name());
            switch (dataType) {
                //TODO
                case CPU_USAGE:
                    registerMonitor(new CpuMonitor(targetApp.getPid()));
                    Log.i(TAG, "cpu usage monitor register success");
                    break;
                case CORE_FREQUENCY:
                    break;
                case GPU_USAGE:
                    registerMonitor(new GpuMonitor());
                    Log.i(TAG, "gpu usage monitor register success");
                    break;
                case GPU_FREQ:
                    break;
                case FPS:
                    if (mMonitorStatus.get(ProfileReq.DataType.FRAME_TIME)) {
                        mFpsMonitor.addNeedDataType(dataType);
                    } else {
                        mFpsMonitor = new FpsMonitor();
                        mFpsMonitor.addNeedDataType(dataType);
                        registerMonitor(mFpsMonitor);
                        registerType(dataType);
                        Log.i(TAG, "fps monitor register success");
                    }
                    break;
                case NETWORK_USAGE:
                    break;
                case SCREEN_SHOT:
                    registerMonitor(new ScreenshotMonitor());
                    Log.i(TAG, "screenshot temperature monitor register success");
                    break;
                case MEMORY:
                    registerMonitor(new MemoryMonitor());
                    registerType(dataType);
                    Log.i(TAG, "memory monitor register success");
                    break;
                case BATTERY:
                    registerMonitor(new BatteryMonitor(mContext, null));
                    registerType(dataType);
                    Log.i(TAG, "battery monitor register success");
                    break;
                case CPU_TEMPERATURE:
                    registerMonitor(new CpuTemperatureMonitor());
                    registerType(dataType);
                    Log.i(TAG, "cpu temperature monitor register success");
                    break;
                case FRAME_TIME:
                    if (mMonitorStatus.get(ProfileReq.DataType.FPS)) {
                        mFpsMonitor.addNeedDataType(dataType);
                    } else {
                        mFpsMonitor = new FpsMonitor();
                        mFpsMonitor.addNeedDataType(dataType);
                        registerMonitor(mFpsMonitor);
                        registerType(dataType);
                        Log.i(TAG, "frame times monitor register success");
                    }
                    break;
                case ANDROID_MEMORY_DETAIL:
                    break;
                case CORE_USAGE:
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
            mMonitors.clear();

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
                Log.v(TAG, "collect data: " + data.build().toString());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return data.build();
    }

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

    private class MonitorWorker implements Runnable {
        private long mFirstTime = 0;
        private long mTickCount = 0;

        @Override
        public void run() {
            mFirstTime = SystemClock.uptimeMillis();
            mIsRunning = AndroidProcessUtils.checkAppIsRunning(mContext, mTargetApp.getPackageName());
            while (mIsRunning) {
                long startTime = SystemClock.uptimeMillis();
//                ProfileNtf collectData = collectData(startTime - mFirstTime);
                ProfileNtf collectData = collectData(System.currentTimeMillis());
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
            stop();
        }
    }

}
