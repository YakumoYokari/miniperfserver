package com.github.sandin.miniperfserver;

import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Process;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.monitor.NetworkMonitor;
import com.github.sandin.miniperfserver.proto.Network;
import com.github.sandin.miniperfserver.util.AndroidProcessUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NetworkMonitorTest {
    private static final String TAG = "NetworkMonitorTest";
    private static final String PACKAGE_NAME = "com.xiaomi.shop";
    private Context mContext;
    private NetworkMonitor mNetworkMonitor;
    private TargetApp mTargetApp;
    private int mPid;
    private int mUid;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mNetworkMonitor = new NetworkMonitor();
        mUid = AndroidProcessUtils.getUid(mContext, PACKAGE_NAME);
        mPid = AndroidProcessUtils.getPid(mContext,PACKAGE_NAME);
        mTargetApp = new TargetApp(PACKAGE_NAME, mPid);
    }

    @Test
    public void collectTest() throws Exception {

    }
}
