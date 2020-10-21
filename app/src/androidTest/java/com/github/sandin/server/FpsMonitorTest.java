package com.github.sandin.server;

import com.github.sandin.miniperf.server.monitor.FpsMonitor;
import com.github.sandin.miniperf.server.monitor.GpuMonitor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

@RunWith(JUnit4.class)
public class FpsMonitorTest {

    private static final String TAG = "FpsMonitorTest";
    private FpsMonitor mFpsMonitor;

    @Before
    public void setUp() {
        mFpsMonitor = new FpsMonitor();
    }

    @After
    public void tearDown() {
        mFpsMonitor = null;
    }

//    @Test
//    public void getLayerNameTest() throws IOException {
//        boolean hasLayerName = mFpsMonitor.getLayerName("tv.danmaku.bili");
//        Assert.assertNotEquals(false,hasLayerName);
//    }
}
