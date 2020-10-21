package com.github.sandin.miniperf.server.bean;

import com.github.sandin.server.proto.GpuFreq;
import com.github.sandin.server.proto.GpuUsage;

public class GpuInfo {
    private GpuUsage gpuUsage;
    private GpuFreq gpuFreq;

    public GpuUsage getGpuUsage() {
        return gpuUsage;
    }

    public void setGpuUsage(GpuUsage gpuUsage) {
        this.gpuUsage = gpuUsage;
    }

    public GpuFreq getGpuFreq() {
        return gpuFreq;
    }

    public void setGpuFreq(GpuFreq gpuFreq) {
        this.gpuFreq = gpuFreq;
    }
}
