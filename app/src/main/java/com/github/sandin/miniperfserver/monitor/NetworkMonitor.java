package com.github.sandin.miniperfserver.monitor;

import android.annotation.SuppressLint;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.bean.TrafficInfo;
import com.github.sandin.miniperfserver.proto.Network;
import com.github.sandin.miniperfserver.proto.ProfileNtf;
import com.github.sandin.miniperfserver.util.AndroidProcessUtils;

/**
 * Network Monitor
 * return b/s
 */
public class NetworkMonitor implements IMonitor<TrafficInfo> {

    private static final String TAG = "NetworkMonitor";
    private NetworkStatsManager mNetworkStatsManager;
    private Context mContext;

    public NetworkMonitor(Context context) {
        mContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mNetworkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        }
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

    //TODO getSubscribeId和使用networkStatsManager时出现权限问题
    @SuppressLint("MissingPermission")
    private TrafficInfo getTraffics(final int uid) throws RemoteException {
        TrafficInfo trafficInfo = new TrafficInfo();
        if (Build.VERSION.SDK_INT < 23) {
            long download = TrafficStats.getUidRxBytes(uid);
            long upload = TrafficStats.getUidTxBytes(uid);
            trafficInfo.setDownload(download);
            trafficInfo.setUpload(upload);
        } else {
            String subscriberId = AndroidProcessUtils.getSubscriberId(mContext);
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            int netType = cm.getActiveNetworkInfo().getType();
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            NetworkStats networkStats = mNetworkStatsManager.querySummary(netType, subscriberId, System.currentTimeMillis() - 60 * 1000, System.currentTimeMillis());
            long summaryRx = 0;
            long summaryTx = 0;
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket);
                int summaryUid = bucket.getUid();
                if (uid == summaryUid) {
                    summaryRx += bucket.getRxBytes();
                    summaryTx += bucket.getTxBytes();
                }
            }
            trafficInfo.setDownload(summaryRx);
            trafficInfo.setUpload(summaryTx);
        }
        return trafficInfo;
    }

    @Override
    public TrafficInfo collect(Context context, TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect traffics data: timestamp=" + timestamp);
        int uid = AndroidProcessUtils.getUid(context, targetApp.getPackageName());
        TrafficInfo trafficInfo = getTraffics(uid);
        Log.v(TAG, dumpTraffics(trafficInfo));
        data.setNetwork(Network.newBuilder().setDownload((int) trafficInfo.getDownload()).setUpload((int) trafficInfo.getUpload()).build());
        return trafficInfo;
    }
}
