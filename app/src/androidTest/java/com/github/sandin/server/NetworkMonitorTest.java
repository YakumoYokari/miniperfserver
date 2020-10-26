package com.github.sandin.server;

import android.content.Context;
import android.net.TrafficStats;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.monitor.NetworkMonitor;
import com.github.sandin.miniperf.server.proto.Network;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;

import org.junit.After;
import org.junit.Assert;
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
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mUid = AndroidProcessUtils.getUid(mContext, PACKAGE_NAME);
        mPid = AndroidProcessUtils.getPid(mContext, PACKAGE_NAME);
        mTargetApp = new TargetApp(PACKAGE_NAME, mPid);
        mNetworkMonitor = new NetworkMonitor(mContext);
    }

    @After
    public void tearDown() {
        mNetworkMonitor = null;
    }

    @Test
    public void collectTest() throws Exception {
        int pid = AndroidProcessUtils.getPid(mContext, PACKAGE_NAME);
//        int uid = 10057;
        TargetApp targetApp = new TargetApp(PACKAGE_NAME, pid);
        Network network = mNetworkMonitor.collect(targetApp, System.currentTimeMillis(), null);
        Log.v(TAG, NetworkMonitor.dumpTraffics(network));
//        Assert.assertNotNull(network);
//        long uidRxBytes = TrafficStats.getUidRxBytes(uid);
//        Assert.assertNotEquals(0, uidRxBytes);
    }
}
