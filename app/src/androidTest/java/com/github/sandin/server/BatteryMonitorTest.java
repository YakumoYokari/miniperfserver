package com.github.sandin.server;


import android.content.Context;
import android.os.BatteryManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.sandin.miniperf.server.monitor.BatteryMonitor;
import com.github.sandin.miniperf.server.proto.Power;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BatteryMonitorTest {

    private Context mContext;
    private BatteryMonitor mBatteryMonitor;
    private String[] mSources = {"server", "dex"};

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mBatteryMonitor = new BatteryMonitor(mContext, null);
    }

    @After
    public void tearDown() {
        mBatteryMonitor = null;
    }

    @Test
    public void getPowerInfoFromServerTest() {

    }

    @Test
    public void collectTest() throws Exception {
        mBatteryMonitor = new BatteryMonitor(mContext, null);
        Power power = mBatteryMonitor.collect(null, 0, null);
        BatteryMonitor.dumpPower(power);
        Assert.assertNotNull(power);
        Assert.assertNotEquals(0,power.getCurrent());
        Assert.assertNotEquals(0,power.getVoltage());
    }

    @Test
    public void collectByContextTest() {
        BatteryManager batteryManager = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
        int intProperty = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        Assert.assertNotNull(intProperty);
    }

}
