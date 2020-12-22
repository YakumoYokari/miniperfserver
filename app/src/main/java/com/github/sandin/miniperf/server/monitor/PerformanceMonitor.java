package com.github.sandin.miniperf.server.monitor;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.ProcessNotFoundNTF;
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
    private static final String FPS_MONITOR = "fps";
    private static final String SCREENSHOT_MONITOR = "screenshot";
    private static final String MEMORY_MONITOR = "memory";
    private static final String CPU_MONITOR = "cpu";
    private static final String CPU_TEMPERATURE_MONITOR = "cpu_temperature";
    private static final String GPU_USAGE_MONITOR = "gpu_usage";
    private static final String GPU_FREQ_MONITOR = "gpu_freq";
    private static final String NETWORK_MONITOR = "network";
    private static final String BATTERY_MONITOR = "battery";
    /**
     * Interval time in Ms
     */
    private final int mIntervalMs;
    private final int mScreenshotIntervalMs;
    /**
     * Current monitors
     */
    private final Map<String, IMonitor<?>> mMonitors = new HashMap<>();
    /**
     * Data Callback
     */
    private final List<Callback> mCallback = new ArrayList<>();
    /**
     * Data Type List
     */
    private final Map<ProfileReq.DataType, Boolean> mDataTypes = new HashMap<>();
    /**
     * Loop thread
     * <p>
     * {@link PerformanceMonitor#mIntervalMs}
     */
    @Nullable
    private Thread mLoopThread;
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

    private void notifySendCloseNtf(ProcessNotFoundNTF ntf) {
        for (Callback callback : mCallback) {
            callback.sendAppClosedNTF(ntf);
        }
    }

    /**
     * Register a monitor
     *
     * @param name    name
     * @param monitor monitor
     */
    private void registerMonitor(String name, IMonitor<?> monitor) {
        Log.i(TAG, "registerMonitor name=" + name);
        mMonitors.put(name, monitor);
    }

    /**
     * Unregister a monitor
     *
     * @param name monitor name
     */
    private void unregisterMonitor(String name) {
        Log.i(TAG, "unregisterMonitor name=" + name);
        mMonitors.remove(name);
    }

    /**
     * Is monitor registered or not
     *
     * @param name monitor name
     */
    private boolean isMonitorRegistered(String name) {
        return mMonitors.containsKey(name);
    }

    @VisibleForTesting
    public <T> T getMonitor(String name) {
        return (T) mMonitors.get(name);
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

        // init data types
        mDataTypes.clear();
        for (ProfileReq.DataType dataType : ProfileReq.DataType.values()) {
            mDataTypes.put(dataType, false);  // turn off the switch
        }

        // set up data types
        for (ProfileReq.DataType dataType : dataTypes) {
            Log.i(TAG, "now data type is : " + dataType.name());
            mDataTypes.put(dataType, true); // turn on the switch
        }
        setupMonitorsForDataTypes();

        mLoopThread = new Thread(new MonitorWorker());
        mLoopThread.start();
        return true;
    }

    /**
     * Toggle data types
     *
     * @param dataTypes need to toggle data types
     */
    public void toggleInterestingDataTypes(ProfileReq.DataType... dataTypes) {
        for (ProfileReq.DataType dataType : dataTypes) {
            if (mDataTypes.containsKey(dataType)) {
                mDataTypes.put(dataType, !mDataTypes.get(dataType)); // toggle
            } else {
                mDataTypes.put(dataType, true); // turn on
            }
            Log.i(TAG, "toggle dataType " + dataType + " -> " + mDataTypes.get(dataType));
        }
        setupMonitorsForDataTypes();
    }

    public boolean isDataTypeEnabled(ProfileReq.DataType dataType) {
        return mDataTypes.get(dataType);
    }

    private Map<ProfileReq.DataType, Boolean> getSubDataTypes(ProfileReq.DataType... dataTypes) {
        Map<ProfileReq.DataType, Boolean> copy = new HashMap<>();
        for (ProfileReq.DataType dataType : dataTypes) {
            copy.put(dataType, mDataTypes.get(dataType));
        }
        return copy;
    }

    private void setupMonitorsForDataTypes() {
        // cpu
        if (isDataTypeEnabled(ProfileReq.DataType.CPU_USAGE) || isDataTypeEnabled(ProfileReq.DataType.CORE_USAGE) || isDataTypeEnabled(ProfileReq.DataType.CORE_FREQUENCY)) {
            final CpuMonitor cpuMonitor;
            if (!isMonitorRegistered(CPU_MONITOR)) {
                cpuMonitor = new CpuMonitor(mTargetApp.getPid());
                registerMonitor(CPU_MONITOR, cpuMonitor);
            } else { // has already registered and just update fields
                cpuMonitor = getMonitor(CPU_MONITOR);
            }
            cpuMonitor.setInterestingFields(getSubDataTypes(ProfileReq.DataType.CPU_USAGE, ProfileReq.DataType.CORE_USAGE, ProfileReq.DataType.CORE_FREQUENCY));
        } else if (!isDataTypeEnabled(ProfileReq.DataType.CPU_USAGE) && !isDataTypeEnabled(ProfileReq.DataType.CORE_USAGE) && !isDataTypeEnabled(ProfileReq.DataType.CORE_FREQUENCY)) {
            if (isMonitorRegistered(CPU_MONITOR)) {
                unregisterMonitor(CPU_MONITOR);
            }
        }

        // screenshot
        if (isDataTypeEnabled(ProfileReq.DataType.SCREEN_SHOT)) {
            if (!isMonitorRegistered(SCREENSHOT_MONITOR)) {
                registerMonitor(SCREENSHOT_MONITOR, new ScreenshotMonitor());
            } // else has already registered and do nothing
        } else {
            if (isMonitorRegistered(SCREENSHOT_MONITOR)) {
                unregisterMonitor(SCREENSHOT_MONITOR);
            }
        }

        // fps
        if (isDataTypeEnabled(ProfileReq.DataType.FPS) || isDataTypeEnabled(ProfileReq.DataType.FRAME_TIME)) {
            final FpsMonitor fpsMonitor;
            if (!isMonitorRegistered(FPS_MONITOR)) {
                fpsMonitor = new FpsMonitor();
                registerMonitor(FPS_MONITOR, fpsMonitor);
            } else { // has already registered and just update fields
                fpsMonitor = getMonitor(FPS_MONITOR);
            }
            fpsMonitor.setInterestingFields(getSubDataTypes(ProfileReq.DataType.FPS, ProfileReq.DataType.FRAME_TIME));
        } else if (!isDataTypeEnabled(ProfileReq.DataType.FPS) && !isDataTypeEnabled(ProfileReq.DataType.FRAME_TIME)) {
            if (isMonitorRegistered(FPS_MONITOR)) {
                unregisterMonitor(FPS_MONITOR);
            }
        }

        // memory
        if (isDataTypeEnabled(ProfileReq.DataType.MEMORY) || isDataTypeEnabled(ProfileReq.DataType.ANDROID_MEMORY_DETAIL)) {
            final MemoryMonitor memoryMonitor;
            if (!isMonitorRegistered(MEMORY_MONITOR)) {
                memoryMonitor = new MemoryMonitor();
                registerMonitor(MEMORY_MONITOR, memoryMonitor);
            } else { // has already registered and just update fields
                memoryMonitor = getMonitor(MEMORY_MONITOR);
            }
            memoryMonitor.setInterestingFields(getSubDataTypes(ProfileReq.DataType.MEMORY, ProfileReq.DataType.ANDROID_MEMORY_DETAIL));
        } else if (!isDataTypeEnabled(ProfileReq.DataType.MEMORY) && !isDataTypeEnabled(ProfileReq.DataType.ANDROID_MEMORY_DETAIL)) {
            if (isMonitorRegistered(MEMORY_MONITOR)) {
                unregisterMonitor(MEMORY_MONITOR);
            }
        }

        //cpuTemp
        if (isDataTypeEnabled(ProfileReq.DataType.CPU_TEMPERATURE)) {
            if (!isMonitorRegistered(CPU_TEMPERATURE_MONITOR)) {
                registerMonitor(CPU_TEMPERATURE_MONITOR, new CpuTemperatureMonitor());
            } // else has already registered and do nothing
        } else {
            if (isMonitorRegistered(CPU_TEMPERATURE_MONITOR)) {
                unregisterMonitor(CPU_TEMPERATURE_MONITOR);
            }
        }

        // gpu
        if (isDataTypeEnabled(ProfileReq.DataType.GPU_USAGE)) {
            if (!isMonitorRegistered(GPU_USAGE_MONITOR)) {
                registerMonitor("gpu_usage", new GpuUsageMonitor());
            } // else has already registered and do nothing
        } else {
            if (isMonitorRegistered(GPU_USAGE_MONITOR)) {
                unregisterMonitor(GPU_USAGE_MONITOR);
            }
        }

        if (isDataTypeEnabled(ProfileReq.DataType.GPU_FREQ)) {
            if (!isMonitorRegistered(GPU_FREQ_MONITOR)) {
                registerMonitor(GPU_FREQ_MONITOR, new GpuFreqMonitor());
            } // else has already registered and do nothing
        } else {
            if (isMonitorRegistered(GPU_FREQ_MONITOR)) {
                unregisterMonitor(GPU_FREQ_MONITOR);
            }
        }

        // network
        if (isDataTypeEnabled(ProfileReq.DataType.NETWORK_USAGE)) {
            if (!isMonitorRegistered(NETWORK_MONITOR)) {
                registerMonitor(NETWORK_MONITOR, new NetworkMonitor(mContext));
            } // else has already registered and do nothing
        } else {
            if (isMonitorRegistered(NETWORK_MONITOR)) {
                unregisterMonitor(NETWORK_MONITOR);
            }
        }

        // battery
        if (isDataTypeEnabled(ProfileReq.DataType.BATTERY)) {
            if (!isMonitorRegistered(BATTERY_MONITOR)) {
                registerMonitor(BATTERY_MONITOR, new BatteryMonitor(mContext, null));
            } // else has already registered and do nothing
        } else {
            if (isMonitorRegistered(BATTERY_MONITOR)) {
                unregisterMonitor(BATTERY_MONITOR);
            }
        }
    }

    public void stop() {
        if (mIsRunning) {
            mIsRunning = false;

            // TODO: unregisterMonitors
            mMonitors.clear();
            //清除状态
            for (ProfileReq.DataType dataType : mDataTypes.keySet()) {
                mDataTypes.put(dataType, false);
            }

            if (mLoopThread != null) {
                try {
                    mLoopThread.join();
                } catch (InterruptedException ignore) {
                }
                mLoopThread = null;
            }
        }
    }

    private ProfileNtf collectData(long timestamp) {
        ProfileNtf.Builder data = ProfileNtf.newBuilder();
        data.setTimestamp(timestamp);
        for (Map.Entry<String, IMonitor<?>> entry : mMonitors.entrySet()) {
            IMonitor<?> monitor = entry.getValue();
            try {
                monitor.collect(mTargetApp, timestamp, data);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            Log.v(TAG, "collect data: " + data.build().toString());
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

        void sendAppClosedNTF(ProcessNotFoundNTF ntf);
    }

    private class MonitorWorker implements Runnable {
        private long mFirstTime = 0;
        private long mTickCount = 0;

        @Override
        public void run() {
            mFirstTime = SystemClock.uptimeMillis();
            while (mIsRunning) {
                Log.i(TAG, System.currentTimeMillis() + " now running state is : " + mIsRunning);
                long startTime = SystemClock.uptimeMillis();
                boolean appIsRunning = AndroidProcessUtils.checkAppIsRunning(mContext, mTargetApp.getPackageName());
                Log.i(TAG, System.currentTimeMillis() + " now app state is " + appIsRunning);
                if (!appIsRunning)
                    break;
                ProfileNtf collectData = collectData(System.currentTimeMillis());
                notifyCallbacks(collectData); // send data
                System.out.println();
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
            Log.i(TAG, "application is close !");
            //after app close , close server and clear monitors
            Log.i(TAG, "stop performance monitor");
            notifySendCloseNtf(ProcessNotFoundNTF.newBuilder().build());
            stop();
        }
    }

}
