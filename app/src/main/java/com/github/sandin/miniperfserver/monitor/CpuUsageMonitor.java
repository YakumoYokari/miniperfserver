package com.github.sandin.miniperfserver.monitor;

import android.content.Context;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.proto.CpuUsage;

public class CpuUsageMonitor implements IMonitor<CpuUsage>{



    @Override
    public CpuUsage collect(Context context, TargetApp targetApp, long timestamp) throws Exception {
        return null;
    }
}
