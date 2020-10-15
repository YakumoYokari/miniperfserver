package com.github.sandin.miniperfserver.monitor;

import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.proto.Network;
import com.github.sandin.miniperfserver.util.AndroidProcessUtils;

/**
 * Network Monitor
 * return b/s
 */
public class NetworkMonitor implements IMonitor<Network> {

    private static final String TAG = "NetworkMonitor";
    private NetworkStatsManager mNetworkStatsManager;
    private Context mContext;

    public NetworkMonitor(Context context) {
        mContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mNetworkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        }
    }




    @Override
    public Network collect(Context context, TargetApp targetApp, long timestamp) throws Exception {
        int uid = AndroidProcessUtils.getUid(context, targetApp.getPackageName());
        Log.v(TAG, String.valueOf(uid));
        return null;
    }
}
