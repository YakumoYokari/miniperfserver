package com.github.sandin.server;

import android.content.Context;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.monitor.PerformanceMonitor;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PerformanceMonitorTest {
    private static final String TARGET_PACKAGE_NAME = "com.github.sandin.miniperfserver";

    private Context mContext;
    private PerformanceMonitor mPerformanceMonitor;
    private TargetApp mTargetApp;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mPerformanceMonitor = new PerformanceMonitor(mContext, 1000, 2 * 1000);

        int pid = AndroidProcessUtils.getPid(mContext, TARGET_PACKAGE_NAME);
        //assertTrue(pid != -1);
        mTargetApp = new TargetApp(TARGET_PACKAGE_NAME, pid);
    }

    @After
    public void tearDown() {
        mPerformanceMonitor = null;
    }

    @Test
    public void start() throws InterruptedException {
        List<ProfileReq.DataType> dataTypes = new ArrayList<>(); // TODO
        mPerformanceMonitor.start(mTargetApp, dataTypes);

        Thread.sleep(10 * 1000);

        mPerformanceMonitor.stop();
    }

    @Test
    public void toggleInterestingDataTypes() throws Exception {
        TargetApp targetApp = new TargetApp();
        List<ProfileReq.DataType> dataTypes = new ArrayList<>();
        dataTypes.add(ProfileReq.DataType.FPS);
        dataTypes.add(ProfileReq.DataType.FRAME_TIME);
        dataTypes.add(ProfileReq.DataType.MEMORY);
        dataTypes.add(ProfileReq.DataType.ANDROID_MEMORY_DETAIL);
        dataTypes.add(ProfileReq.DataType.CPU_USAGE);
        dataTypes.add(ProfileReq.DataType.CORE_FREQUENCY);
        dataTypes.add(ProfileReq.DataType.GPU_USAGE);
        dataTypes.add(ProfileReq.DataType.GPU_FREQ);
        dataTypes.add(ProfileReq.DataType.NETWORK_USAGE);
        dataTypes.add(ProfileReq.DataType.BATTERY);
        dataTypes.add(ProfileReq.DataType.CPU_TEMPERATURE);
        dataTypes.add(ProfileReq.DataType.CORE_USAGE);

        mPerformanceMonitor.start(targetApp, dataTypes);

        // fps & frameTime
        assertTrue(mPerformanceMonitor.isDataTypeEnabled(ProfileReq.DataType.FPS));
        assertTrue(mPerformanceMonitor.isDataTypeEnabled(ProfileReq.DataType.FRAME_TIME));
        assertNotNull(mPerformanceMonitor.getMonitor("fps"));

        // -> turn off "fps"
        mPerformanceMonitor.toggleInterestingDataTypes(ProfileReq.DataType.FPS);
        assertFalse(mPerformanceMonitor.isDataTypeEnabled(ProfileReq.DataType.FPS));
        assertNotNull(mPerformanceMonitor.getMonitor("fps"));

        // -> turn off "frameTime"
        mPerformanceMonitor.toggleInterestingDataTypes(ProfileReq.DataType.FRAME_TIME);
        assertFalse(mPerformanceMonitor.isDataTypeEnabled(ProfileReq.DataType.FRAME_TIME));
        assertNull(mPerformanceMonitor.getMonitor("fps")); // "fps" and "frameTime" both turn off, should unregister the FpsMonitor

        // memory & memoryDetail
        assertTrue(mPerformanceMonitor.isDataTypeEnabled(ProfileReq.DataType.MEMORY));
        assertTrue(mPerformanceMonitor.isDataTypeEnabled(ProfileReq.DataType.ANDROID_MEMORY_DETAIL));
        assertNotNull(mPerformanceMonitor.getMonitor("memory"));

        // -> turn off memory
        mPerformanceMonitor.toggleInterestingDataTypes(ProfileReq.DataType.MEMORY);
        assertFalse(mPerformanceMonitor.isDataTypeEnabled(ProfileReq.DataType.MEMORY));
        assertNotNull(mPerformanceMonitor.getMonitor("memory"));

        // -> turn off memory detail
        mPerformanceMonitor.toggleInterestingDataTypes(ProfileReq.DataType.ANDROID_MEMORY_DETAIL);
        assertFalse(mPerformanceMonitor.isDataTypeEnabled(ProfileReq.DataType.ANDROID_MEMORY_DETAIL));
        assertNull(mPerformanceMonitor.getMonitor("memory"));

        mPerformanceMonitor.stop();
    }

}