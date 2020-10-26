package com.github.sandin.server;

import com.github.sandin.miniperf.server.monitor.GpuFreqMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GpuFreqMonitorTest {

    private static final String TAG = "GpuMonitor";
    private GpuFreqMonitor mGpuFreqMonitor;

    @Before
    public void setUp() {
        mGpuFreqMonitor = new GpuFreqMonitor();
    }

    @After
    public void tearDown() {
        mGpuFreqMonitor = null;
    }

    @Test
    public void collectTest() throws Exception {

    }
}
