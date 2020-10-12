package com.github.sandin.miniperfserver;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.sandin.miniperfserver.monitor.BatteryMonitor;
import com.github.sandin.miniperfserver.proto.Power;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class BatteryMonitorTest {

    private Context mContext;
    private BatteryMonitor mBatteryMonitor;
    private String[] mSources = {"server", "dex", "app"};

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mBatteryMonitor = new BatteryMonitor(mContext, null);
    }

    @After
    public void tearDown() {
        mBatteryMonitor = null;
    }

//    @Test
//    public void getVoltageFromDumpTest() throws IOException {
//        int voltage = mBatteryMonitor.getVoltageFromDump();
//        Assert.assertNotEquals(0, voltage);
//    }
//
//    @Test
//    public void getCurrentFromSettingFileTest() {
//        int current = mBatteryMonitor.getCurrentFromSettingFile();
//        Assert.assertNotEquals(0, current);
//    }
//
//    @Test
//    public void getVoltageFromSettingFileTest() {
//        int voltage = mBatteryMonitor.getVoltageFromSettingFile();
//        Assert.assertNotEquals(0, voltage);
//    }

    @Test
    public void collectTest() throws Exception {
        for (String source : mSources) {
            mBatteryMonitor = new BatteryMonitor(mContext, source);
            Power power = mBatteryMonitor.collect(mContext, null, 0);
            BatteryMonitor.dumpPower(power);
            Assert.assertNotNull(power);
        }
    }

}
