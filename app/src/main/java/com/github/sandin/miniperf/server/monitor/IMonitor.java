package com.github.sandin.miniperf.server.monitor;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;

import java.util.Map;

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

    /**
     * Set interesting data fields
     *
     * @param dataTypes data types
     */
    void setInterestingFields(Map<ProfileReq.DataType, Boolean> dataTypes);

}
