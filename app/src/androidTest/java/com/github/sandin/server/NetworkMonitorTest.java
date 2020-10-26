package com.github.sandin.server;

import android.content.Context;
import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.bean.TrafficInfo;
import com.github.sandin.miniperf.server.monitor.NetworkMonitor;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import androidx.test.platform.app.InstrumentationRegistry;

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
        mUid = AndroidProcessUtils.getUid(mContext,PACKAGE_NAME);
        mPid = AndroidProcessUtils.getPid(mContext,PACKAGE_NAME);
        mTargetApp = new TargetApp(PACKAGE_NAME, mPid);
        mNetworkMonitor = new NetworkMonitor(mContext);
    }

    @After
    public void tearDown() {
        mNetworkMonitor = null;
    }

    @Test
    public void collectTest() throws Exception {
        String packageName = "com.tencent.tmgp.jxqy";
        int pid = 20363;
        TargetApp targetApp = new TargetApp(packageName,pid);
        TrafficInfo trafficInfo = mNetworkMonitor.collect(targetApp,System.currentTimeMillis(),null);
        Log.v(TAG, NetworkMonitor.dumpTraffics(trafficInfo));
        Assert.assertNotNull(trafficInfo);
    }
}
