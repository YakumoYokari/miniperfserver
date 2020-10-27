package com.github.sandin.miniperf.server.monitor;

import android.content.Context;
import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.proto.Network;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Network Monitor
 * return b/s
 */
public class NetworkMonitor implements IMonitor<Network> {

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
    21 tx_other_packets ： 发送的其他类型包数*/
    private static final String TAG = "NetworkMonitor";
    //private NetworkStatsManager mNetworkStatsManager;
    private Context mContext;
    private int lastRxBytes = 0;
    private int lastTxBytes = 0;

    public NetworkMonitor(Context context) {
        mContext = context;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            mNetworkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
//        }
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

    public Network getTraffics(int uid) throws IOException {
//        List<String> content = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.NETWORK_SYSTEM_FILE_PATHS);
        File networkFile = new File(DataSource.NETWORK_SYSTEM_FILE_PATHS);
        Network.Builder networkBuilder = Network.newBuilder();
        int rxBytes = 0, txBytes = 0;
        List<String> content = new LinkedList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(networkFile));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("line: " + line);
                content.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
        Log.i(TAG, "collect traffics info size : " + content.size());
        if (content.size() > 1) {
            Log.i(TAG, content.toString());
            Log.i(TAG, "content size is : " + content.size());
            for (String line : content) {
                String[] parts = line.split("\t");
                if (Integer.parseInt(parts[3]) == uid && parts[1] != "lo") {
                    rxBytes += Integer.parseInt(parts[5]);
                    txBytes += Integer.parseInt(parts[7]);
                }
            }
        }
        if (lastRxBytes == 0 || lastTxBytes == 0) {
            lastRxBytes = rxBytes;
            lastTxBytes = txBytes;
            networkBuilder.setDownload(0);
            networkBuilder.setUpload(0);
        }
        networkBuilder.setDownload(rxBytes - lastRxBytes);
        networkBuilder.setUpload(txBytes - lastTxBytes);
        return networkBuilder.build();
    }

//    //TODO getSubscribeId和使用networkStatsManager时出现权限问题
//    @SuppressLint("MissingPermission")
//    private TrafficInfo getTraffics(final int uid) throws RemoteException {
//        TrafficInfo trafficInfo = new TrafficInfo();
//        if (Build.VERSION.SDK_INT < 23) {
//        long download = TrafficStats.getUidRxBytes(uid);
//        long upload = TrafficStats.getUidTxBytes(uid);
//        trafficInfo.setDownload(download);
//        trafficInfo.setUpload(upload);
//        } else {
//            String subscriberId = AndroidProcessUtils.getSubscriberId(mContext);
//            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//            int netType = cm.getActiveNetworkInfo().getType();
//            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
//            NetworkStats networkStats = mNetworkStatsManager.querySummary(netType, subscriberId, System.currentTimeMillis() - 60 * 1000, System.currentTimeMillis());
//            long summaryRx = 0;
//            long summaryTx = 0;
//            while (networkStats.hasNextBucket()) {
//                networkStats.getNextBucket(bucket);
//                int summaryUid = bucket.getUid();
//                if (uid == summaryUid) {
//                    summaryRx += bucket.getRxBytes();
//                    summaryTx += bucket.getTxBytes();
//                }
//            }
//            trafficInfo.setDownload(summaryRx);
//            trafficInfo.setUpload(summaryTx);
//        }
//        return trafficInfo;
//    }

    @Override
    public Network collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect traffics data: timestamp=" + timestamp);
        int uid = AndroidProcessUtils.getUid(mContext, targetApp.getPackageName());
        Network traffics = getTraffics(uid);
        Log.v(TAG, dumpTraffics(traffics));
        if (data != null) {
            data.setNetwork(Network.newBuilder()
                    .setDownload(traffics.getDownload())
                    .setUpload(traffics.getUpload())
                    .build()
            );
        }
        return traffics;
    }
}
