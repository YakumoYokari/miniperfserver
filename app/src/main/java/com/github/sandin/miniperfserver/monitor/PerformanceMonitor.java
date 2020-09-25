package com.github.sandin.miniperfserver.monitor;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.github.sandin.miniperfserver.bean.TargetApp;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * The Performance Monitor
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";

    private final Context mContext;
    private final int mIntervalMs;
    private final int mScreenshotIntervalMs;

    private final List<IMonitor> mMonitors = new ArrayList<>();

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
     * @param context system context
     * @param intervalMs interval time in ms
     * @param screenshotIntervalMs screenshot interval time in ms
     */
    public PerformanceMonitor(Context context, int intervalMs, int screenshotIntervalMs) {
        mContext = context;
        mIntervalMs = intervalMs;
        mScreenshotIntervalMs = screenshotIntervalMs;

        registerMonitor(new MemoryMonitor(context));
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

    public void start(TargetApp targetApp) {
        if (mIsRunning) {
            Log.w(TAG, "server has already been started!");
            return;
        }
        mTargetApp = targetApp;
        mIsRunning = true;
        mThread = new Thread(new MonitorWorker());
        mThread.start();
    }

    public void stop() {
        if (mIsRunning) {
            mIsRunning = false;

            if (mThread != null) {
                try {
                    mThread.join();
                } catch (InterruptedException ignore) {
                }
                mThread = null;
            }
        }
    }

    private void collectData(long timestamp) {
        try {
            for (IMonitor<?> monitor : mMonitors) {
                Object data = monitor.collect(mContext, mTargetApp, timestamp);
                //Log.v(TAG, "collect data: " + data);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private class MonitorWorker implements Runnable {
        private long mFirstTime = 0;
        private long mTickCount = 0;

        @Override
        public void run() {
            mFirstTime = SystemClock.uptimeMillis();
            while (mIsRunning) {
                long startTime = SystemClock.uptimeMillis();
                collectData(startTime - mFirstTime);
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
