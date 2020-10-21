package com.github.sandin.miniperf.server.monitor;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.ProfileNtf;

/**
 * Interface of all monitor
 * <p>
 * T: data type
 */
public interface IMonitor<T> {

    /**
     * Collect data
     *
     * @param targetApp target application
     */
    T collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception;

}
