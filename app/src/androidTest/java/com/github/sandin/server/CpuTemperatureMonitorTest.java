package com.github.sandin.server;

import android.content.Context;

import com.github.sandin.miniperf.server.monitor.CpuTemperatureMonitor;
import com.github.sandin.miniperf.server.proto.Temp;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(JUnit4.class)
public class CpuTemperatureMonitorTest {
    private Context mContext;
    private CpuTemperatureMonitor mCpuTemperatureMonitor;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mCpuTemperatureMonitor = new CpuTemperatureMonitor();
    }

    @After
    public void tearDown() {
        mCpuTemperatureMonitor = null;
    }

    @Test
    public void collectTest() throws Exception {
        Temp temp = mCpuTemperatureMonitor.collect(null, 0,null);
        Assert.assertNotEquals(0, temp.getTemp());
    }
}
