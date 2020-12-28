package com.github.sandin.miniperf.server.monitor;

import android.content.Context;
import android.util.Log;

import com.genymobile.scrcpy.wrappers.NetworkStatusManager;
import com.genymobile.scrcpy.wrappers.ServiceManager;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.bean.TrafficInfo;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.Network;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Network Monitor
 * 两种收集信息方式
 * 1、getTrafficsFromNetworkStatsManager 通过NetworkStatsManager获取 仅支持Api>= 23
 * 2、getTrafficsFromSystemFile 通过解析 /proc/net/xt_qtaguid/stats 高版本安卓会移除
 * return -1为不支持
 * return b/s
 */
public class NetworkMonitor implements IMonitor<Network> {

    private static final String TAG = "NetworkMonitor";
    private final String SERVICE_NAME = "netstats";
    private long lastRxBytes = 0;
    private long lastTxBytes = 0;
    private Context mContext;
    private boolean supportReadSystemFile;


    public NetworkMonitor(Context context) {
        mContext = context;
        File systemFile = new File(DataSource.NETWORK_SYSTEM_FILE_PATHS);
        supportReadSystemFile = systemFile.exists();
        if (supportReadSystemFile) {
            if (ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.NETWORK_SYSTEM_FILE_PATHS).size() <= 0)
                supportReadSystemFile = !supportReadSystemFile;
        }
        System.out.println("support read system file : " + supportReadSystemFile);
        Log.i(TAG, "support read system file : " + supportReadSystemFile);
    }

    /**
     * dump traffic info
     *
     * @param trafficInfo
     * @return string of trafficInfo
     */
    public static String dumpTraffics(TrafficInfo trafficInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Traffics");
        sb.append(", download=").append(trafficInfo.getDownload());
        sb.append(", upload=").append(trafficInfo.getUpload());
        sb.append("]");
        return sb.toString();
    }

    public TrafficInfo getTrafficsFromNetstats(int uid) {
        final int TYPE_RX_BYTES = 0;
        final int TYPE_TX_BYTES = 2;
        NetworkStatusManager networkStatusManager = new ServiceManager().getNetworkStatusManager();
        long rx = networkStatusManager.getUidStats(uid, TYPE_RX_BYTES);
        long tx = networkStatusManager.getUidStats(uid, TYPE_TX_BYTES);
        return new TrafficInfo(tx, rx);
    }


    /*
    0 idx : 序号
    1 iface ： 代表流量类型（rmnet表示2G/3G, wlan表示Wifi流量,lo表示本地流量）
    2 acct_tag_hex ：线程标记（用于区分单个应用内不同模块/线程的流量）
    3 uid_tag_int ： 应用uid,据此判断是否是某应用统计的流量数据
    4 cnt_set ： 应用前后标志位：1：前台， 0：后台
    5 rx_btyes ： receive bytes 接受到的字节数
    6 rx_packets : 接收到的任务包数
    7 tx_bytes ： transmit bytes 发送的总字节数
    8 tx_packets ： 发送的总包数
    9  rx_tcp_types ： 接收到的tcp字节数
    10 rx_tcp_packets ： 接收到的tcp包数
    11 rx_udp_bytes ： 接收到的udp字节数
    12 rx_udp_packets ： 接收到的udp包数
    13 rx_other_bytes ： 接收到的其他类型字节数
    14 rx_other_packets ： 接收到的其他类型包数
    15 tx_tcp_bytes ： 发送的tcp字节数
    16 tx_tcp_packets ： 发送的tcp包数
    17 tx_udp_bytes ： 发送的udp字节数
    18 tx_udp_packets ： 发送的udp包数
    19 tx_other_bytes ： 发送的其他类型字节数
    20 tx_other_packets ： 发送的其他类型包数
    */
    public TrafficInfo getTrafficsFromSystemFile(int uid) {
        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.NETWORK_SYSTEM_FILE_PATHS);
        long rxBytes = 0, txBytes = 0;
        if (content.size() > 1) {
            for (String line : content) {
                if (line.equals(content.get(0)))
                    continue;
                String[] parts = line.split("\\s+");
                long lineUid = Long.parseLong(parts[3]);
                if ((lineUid == uid) && (!parts[1].equals("lo"))) {
                    rxBytes += Long.parseLong(parts[5]);
                    txBytes += Long.parseLong(parts[7]);
                }
            }
        }
        System.out.println("collect traffic data from system file result : rx:" + rxBytes + " tx: " + txBytes);
        return new TrafficInfo(txBytes, rxBytes);
    }


    @Override
    public Network collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        int uid = AndroidProcessUtils.getUid(mContext, targetApp.getPackageName());
        System.out.println("collect application uid : " + uid);
        TrafficInfo traffics;
        if (supportReadSystemFile)
            traffics = getTrafficsFromSystemFile(uid);
        else
            traffics = getTrafficsFromNetstats(uid);
        Network.Builder networkBuilder = Network.newBuilder();
        if (traffics != null) {
            System.out.println("now traffics info : " + traffics.getDownload() + " " + traffics.getUpload());
            Log.i(TAG, "last traffic info : " + lastRxBytes + " " + lastTxBytes);
            Log.i(TAG, "now traffics info : " + traffics.getDownload() + " " + traffics.getUpload());
            //first collect
            if (lastTxBytes == 0 && lastRxBytes == 0) {
                networkBuilder.setUpload(0).setDownload(0);
            } else {
                networkBuilder.setDownload((int) (traffics.getDownload() - lastRxBytes)).setUpload((int) (traffics.getUpload() - lastTxBytes));
            }
            lastTxBytes = traffics.getUpload();
            lastRxBytes = traffics.getDownload();
            System.out.println("now last bytes is : " + lastRxBytes + " " + lastTxBytes);
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
