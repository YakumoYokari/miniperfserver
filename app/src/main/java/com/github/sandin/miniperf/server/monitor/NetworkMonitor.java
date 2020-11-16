package com.github.sandin.miniperf.server.monitor;

import android.annotation.SuppressLint;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.bean.TrafficInfo;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.Network;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.List;
import java.util.Map;

/**
 * Network Monitor
 * 三种收集信息方式
 * 1、getTrafficsFromNetworkStatsManager 通过NetworkStatsManager获取
 * 仅支持Api>= 23,且目前发现部分安卓6存在权限问题,且有些手机上数据刷新率过低
 * 2、getTrafficsFromTrafficStats 通过TrafficsStats获取
 * 3、getTrafficsFromSystemFile 通过解析 /proc/net/xt_qtaguid/stats 高版本安卓会移除
 * return -1为不支持
 * return b/s
 */
public class NetworkMonitor implements IMonitor<Network> {

    private static final String TAG = "NetworkMonitor";
    private final String SERVICE_NAME = "netstats";
    private Context mContext;
    private long lastRxBytes = 0;
    private long lastTxBytes = 0;
    private String mSupportMethod;


    public NetworkMonitor(Context context) {
        mContext = context;
    }

    /**
     * dump traffic info
     *
     * @param network
     * @return string of trafficInfo
     */
    public static String dumpTraffics(Network network) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Traffics");
        sb.append(", download=").append(network.getDownload());
        sb.append(", upload=").append(network.getUpload());
        sb.append("]");
        return sb.toString();
    }

    //return -1为不支持
    @SuppressLint("NewApi")
    public TrafficInfo getTrafficsFromNetworkStatsManager(int uid) throws RemoteException {
        NetworkStatsManager mNetworkStatsManager = (NetworkStatsManager) mContext.getSystemService(Context.NETWORK_STATS_SERVICE);
        System.out.println("get traffics info from statsManager");
        String subId = AndroidProcessUtils.getSubscriberId(mContext);
        long totalRx = 0;
        long totalTx = 0;
        NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();
        int networkType = getNetworkType();
        //Unsupported
        if (networkType == -1) {
            return new TrafficInfo(-1, -1);
        }
        System.out.println("network type : " + networkType);
        Log.i(TAG, "network type : " + networkType);
        NetworkStats networkStats = mNetworkStatsManager
                .querySummary(networkType, subId, System.currentTimeMillis() - 24 * 60 * 60 * 1000, System.currentTimeMillis());
        System.out.println(uid);
        do {
            networkStats.getNextBucket(summaryBucket);
            int summaryUid = summaryBucket.getUid();
            Log.i(TAG, "summaryUid : " + summaryUid);
            if (uid == summaryUid) {
                long summaryRx = summaryBucket.getRxBytes();
                long summaryTx = summaryBucket.getTxBytes();
                totalRx += summaryRx;
                totalTx += summaryTx;
            }
        } while (networkStats.hasNextBucket());
        System.out.println("summaryRx : " + totalRx + " summaryTx : " + totalTx);
        return new TrafficInfo(totalTx, totalRx);
    }


    /**
     * 获取网络类型
     * 网络不可用时会返回null
     *
     * @see <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/ConnectivityService.java;l=1406;drc=aeab3f7d9fe6cfce81baf4e2e6b4696d210875f2">ConnectivityService</a>
     */
    @SuppressLint("MissingPermission")
    private int getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null) {
            //无可用网络
            return -1;
        }
        return cm.getActiveNetworkInfo().getType();
    }


    public TrafficInfo getTrafficsFromTrafficStats(int uid) {
        System.out.println("get traffics info from TrafficStats");
        Network.Builder network = Network.newBuilder();
        long download = TrafficStats.getUidRxBytes(uid);
        long upload = TrafficStats.getUidTxBytes(uid);
        //-1为不支持
        return new TrafficInfo(upload, download);
    }


    /*
    1 idx : 序号
    2 iface ： 代表流量类型（rmnet表示2G/3G, wlan表示Wifi流量,lo表示本地流量）
    3 acct_tag_hex ：线程标记（用于区分单个应用内不同模块/线程的流量）
    4 uid_tag_int ： 应用uid,据此判断是否是某应用统计的流量数据
    5 cnt_set ： 应用前后标志位：1：前台， 0：后台
    6 rx_btyes ： receive bytes 接受到的字节数
    7 rx_packets : 接收到的任务包数
    8 tx_bytes ： transmit bytes 发送的总字节数
    9 tx_packets ： 发送的总包数
    10 rx_tcp_types ： 接收到的tcp字节数
    11 rx_tcp_packets ： 接收到的tcp包数
    12 rx_udp_bytes ： 接收到的udp字节数
    13 rx_udp_packets ： 接收到的udp包数
    14 rx_other_bytes ： 接收到的其他类型字节数
    15 rx_other_packets ： 接收到的其他类型包数
    16 tx_tcp_bytes ： 发送的tcp字节数
    17 tx_tcp_packets ： 发送的tcp包数
    18 tx_udp_bytes ： 发送的udp字节数
    19 tx_udp_packets ： 发送的udp包数
    20 tx_other_bytes ： 发送的其他类型字节数
    21 tx_other_packets ： 发送的其他类型包数
    */
    public TrafficInfo getTrafficsFromSystemFile(int uid) {
        System.out.println("get traffics info from system file");
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.NETWORK_SYSTEM_FILE_PATHS);
        long rxBytes = 0, txBytes = 0;
        Log.i(TAG, "collect traffics info size : " + content.size());
        if (content.size() > 1) {
            for (String line : content) {
                if (line.equals(content.get(0)))
                    continue;
                String[] parts = line.split("\\s+");
                int lineUid = Integer.parseInt(parts[3]);
                if ((lineUid == uid) && (!parts[1].equals("lo"))) {
                    rxBytes += Long.parseLong(parts[5]);
                    txBytes += Long.parseLong(parts[7]);
                }
            }
        }
        System.out.println("collect traffic data from system file result : rx:" + rxBytes + " tx: " + txBytes);
        return new TrafficInfo(txBytes, rxBytes);
    }

    private void checkSupportMethod(int uid) {
        TrafficInfo trafficsFromSystemFile = getTrafficsFromSystemFile(uid);
        if (trafficsFromSystemFile.getUpload() != 0 && trafficsFromSystemFile.getDownload() != 0) {
            mSupportMethod = "getTrafficsFromSystemFile";
        } else {
            TrafficInfo trafficsFromTrafficStats = getTrafficsFromTrafficStats(uid);
            if (trafficsFromTrafficStats.getDownload() != -1 && trafficsFromTrafficStats.getUpload() != -1) {
                mSupportMethod = "getTrafficsFromTrafficStats";
            } else if (Build.VERSION.SDK_INT >= 23) {
                try {
                    TrafficInfo trafficsFromNetworkStatsManager = getTrafficsFromNetworkStatsManager(uid);
                    if (trafficsFromNetworkStatsManager.getUpload() > 0 && trafficsFromNetworkStatsManager.getDownload() > 0)
                        mSupportMethod = "getTrafficsFromNetworkStatsManager";
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mSupportMethod == null)
            mSupportMethod = "unsupported";
    }


    @Override
    public Network collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect traffics data: timestamp=" + timestamp);
        int uid = AndroidProcessUtils.getUid(mContext, targetApp.getPackageName());
        System.out.println("collect app uid : " + uid);
        checkSupportMethod(uid);
        Log.v(TAG, "this phone chose collect method is " + mSupportMethod);
        System.out.println("this phone chose collect method is " + mSupportMethod);
        Network.Builder networkBuilder = Network.newBuilder();
        TrafficInfo collectInfo;
        switch (mSupportMethod) {
            case "getTrafficsFromSystemFile":
                collectInfo = getTrafficsFromSystemFile(uid);
                break;
            case "getTrafficsFromNetworkStatsManager":
                collectInfo = getTrafficsFromNetworkStatsManager(uid);
                break;
            case "getTrafficsFromTrafficStats":
                collectInfo = getTrafficsFromTrafficStats(uid);
                break;
            case "unsupported":
                return networkBuilder.setDownload(-1).setUpload(-1).build();
            default:
                collectInfo = null;
        }

        if (collectInfo != null) {
            if (collectInfo.getDownload() >= 0 && collectInfo.getUpload() >= 0) {
                if (lastRxBytes == 0 || lastTxBytes == 0) {
                    networkBuilder.setUpload(0);
                    networkBuilder.setDownload(0);
                } else {
                    networkBuilder.setDownload((int) (collectInfo.getDownload() - lastRxBytes));
                    networkBuilder.setUpload((int) (collectInfo.getUpload() - lastTxBytes));
                }
                lastRxBytes = collectInfo.getDownload();
                lastTxBytes = collectInfo.getUpload();
            }
        } else {
            //UnSupported
            networkBuilder.setUpload(-1);
            networkBuilder.setDownload(-1);
            return networkBuilder.build();
        }
        Network network = networkBuilder.build();
        if (data != null) {
            data.setNetwork(network);
        }
        return network;
    }

    @Override
    public void setInterestingFields(Map<ProfileReq.DataType, Boolean> dataTypes) {
        // pass
    }
}
