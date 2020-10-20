package com.github.sandin.miniperfserver.monitor;

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
     * @param targetApp target application
     */
    T collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception;

}
