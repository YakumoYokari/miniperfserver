package com.github.sandin.miniperfserver;

import android.util.Log;

import com.github.sandin.miniperfserver.bean.GpuInfo;
import com.github.sandin.miniperfserver.monitor.GpuMonitor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GpuMonitorTest {

    private static final String TAG = "GpuMonitor";
    private GpuMonitor mGpuMonitor;

    @Before
    public void setUp() {
        mGpuMonitor = new GpuMonitor();
    }

    @After
    public void tearDown() {
        mGpuMonitor = null;
    }

    @Test
    public void collectTest() throws Exception {
        GpuInfo gpuInfo = mGpuMonitor.collect(null, null, 0,null);
        Assert.assertNotNull(gpuInfo);
        Log.v(TAG, GpuMonitor.dumpGpuInfo(gpuInfo));
    }
}
