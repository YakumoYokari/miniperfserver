package com.genymobile.scrcpy.wrappers;

import android.os.IInterface;

import java.lang.reflect.InvocationTargetException;

public class NetworkStatusManager {
    private final IInterface manager;

    public NetworkStatusManager(IInterface manager) {
        this.manager = manager;
    }

    /*
    TYPE_RX_BYTES = 0
    TYPE_RX_PACKETS = 1
    TYPE_TX_BYTES = 2
    TYPE_TX_PACKETS = 3
    TYPE_TCP_RX_PACKETS = 4
    TYPE_TCP_TX_PACKETS = 5
    */
    public long getUidStats(int uid, int type) {
        try {
            return (long) manager.getClass().getMethod("getUidStats", new Class[]{int.class, int.class}).invoke(manager, uid, type);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
