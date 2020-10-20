package com.github.sandin.miniperfserver;

import android.content.Context;
import android.net.TrafficStats;

import androidx.test.platform.app.InstrumentationRegistry;

import com.github.sandin.miniperfserver.monitor.CpuTemperatureMonitor;
import com.github.sandin.miniperfserver.proto.Temp;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
        Temp temp = mCpuTemperatureMonitor.collect(mContext, null, 0,null);
        Assert.assertNotEquals(0, temp.getTemp());
    }
}
