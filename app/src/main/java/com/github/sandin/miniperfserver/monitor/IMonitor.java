package com.github.sandin.miniperfserver.monitor;

import android.content.Context;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.proto.ProfileNtf;

/**
 * Interface of all monitor
 * <p>
 * T: data type
 */
public interface IMonitor<T> {

    /**
     * Collect data
     *
     * @param context   system context
     * @param targetApp target application
     */
    T collect(Context context, TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception;

}
