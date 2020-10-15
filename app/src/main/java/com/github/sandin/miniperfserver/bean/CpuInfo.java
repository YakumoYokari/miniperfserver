package com.github.sandin.miniperfserver.bean;

import com.github.sandin.miniperfserver.proto.CoreUsage;
import com.github.sandin.miniperfserver.proto.CpuFreq;
import com.github.sandin.miniperfserver.proto.CpuUsage;

public class CpuInfo {
    private CpuUsage cpuUsage;
    private CpuFreq cpuFreq;
    private CoreUsage coreUsage;

    public CpuUsage getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(CpuUsage cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public CpuFreq getCpuFreq() {
        return cpuFreq;
    }

    public void setCpuFreq(CpuFreq cpuFreq) {
        this.cpuFreq = cpuFreq;
    }

    public CoreUsage getCoreUsage() {
        return coreUsage;
    }

    public void setCoreUsage(CoreUsage coreUsage) {
        this.coreUsage = coreUsage;
    }
}
