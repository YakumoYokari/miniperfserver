package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.Memory;
import com.github.sandin.miniperf.server.proto.MemoryDetail;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.VirtualMemory;
import com.github.sandin.miniperf.server.util.ConvertUtils;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memory Monitor
 * Memory mb
 * Memory kb
 *
 * @see Memory
 * <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/ActivityThread.java;l=2739?q=ActivityThread.jav&ss=android">ActivityThread</a>
 */
public class MemoryMonitor implements IMonitor<Memory> {
    private static final String TAG = "MemoryMonitor";

    private Map<ProfileReq.DataType, Boolean> mDataTypes = new HashMap<>();


    /**
     * dump memoryInfo
     *
     * @param memory
     * @return
     */
    public static final String dumpMemory(Memory memory) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Memory");
        sb.append(", pss=").append(memory.getPss());
        sb.append(", swap=").append(memory.getSwap());
        sb.append(", virtualMemory=").append(memory.getVirtualMemory());
        if (memory.getMemoryDetail() != null) {
            sb.append(", memoryDetail.gl=").append(memory.getMemoryDetail().getGl());
            sb.append(", memoryDetail.gfx=").append(memory.getMemoryDetail().getGfx());
            sb.append(", memoryDetail.unknown=").append(memory.getMemoryDetail().getUnknown());
            sb.append(", memoryDetail.nativePass=").append(memory.getMemoryDetail().getNativePss());
        }
        sb.append("]");
        return sb.toString();
    }

    //kb -> mb 四舍五入


    private int getLineDataByIndex(String line, int index) {
        return Integer.parseInt(line.split("\\s+")[index]);
    }

    public Memory collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Memory.Builder memoryBuilder = Memory.newBuilder();
        MemoryDetail.Builder detailBuilder = MemoryDetail.newBuilder();
        int pid = targetApp.getPid();
        List<String> meminfoResult = ReadSystemInfoUtils.readInfoFromDumpsys("meminfo", new String[]{String.valueOf(pid), "--local"});
        int gl = 0, gfx = 0, unknow = 0, pss = 0, nativePss = 0;
        for (String line : meminfoResult) {
            //gfx
            if (line.startsWith("Gfx dev"))
                gfx = getLineDataByIndex(line, 2);
            //gl
            if (line.startsWith("GL mtrack"))
                gl = getLineDataByIndex(line, 2);
            //unknow
            if (line.startsWith("Unknown"))
                unknow = getLineDataByIndex(line, 1);
            //pss
            if (line.startsWith("TOTAL:"))
                pss = getLineDataByIndex(line, 1);
            //native
            if (line.startsWith("Native Heap:"))
                nativePss = getLineDataByIndex(line, 2);
        }
        memoryBuilder.setPss(ConvertUtils.kb2Mb(pss));
        detailBuilder.setUnknown(unknow).setGfx(gfx).setGl(gl).setNativePss(nativePss);
        //vss & swap kb
        List<String> vssAndSwapResult = ReadSystemInfoUtils.readInfoFromSystemFile("/proc/" + pid + "/status");
        int vss = 0;
        int swap = 0;
        for (String line : vssAndSwapResult) {
            //vss
            if (line.startsWith("VmSize:")) {
                vss = getLineDataByIndex(line, 1);
            }
            //swap
            if (line.startsWith("VmSwap:")) {
                swap = getLineDataByIndex(line, 1);
            }
        }
        memoryBuilder.setVirtualMemory(ConvertUtils.kb2Mb(vss)).setSwap(ConvertUtils.kb2Mb(swap));
        memoryBuilder.setMemoryDetail(detailBuilder);
        Memory memory = memoryBuilder.build();

        Log.v(TAG, dumpMemory(memory));
        if (data != null) {
            data.setMemory(memory);
            data.setVirtualMemory(VirtualMemory.newBuilder().setVirtualMemory(ConvertUtils.kb2Mb(vss)).build());
        }
        return memory;
    }

    private boolean isDataTypeEnabled(ProfileReq.DataType dataType) {
        return mDataTypes.containsKey(dataType) && mDataTypes.get(dataType);
    }

    @Override
    public void setInterestingFields(Map<ProfileReq.DataType, Boolean> dataTypes) {
        mDataTypes.clear();
        mDataTypes.putAll(dataTypes);
    }

}
