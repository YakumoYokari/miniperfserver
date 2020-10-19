package com.github.sandin.miniperfserver.monitor;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.proto.Memory;
import com.github.sandin.miniperfserver.proto.MemoryDetail;
import com.github.sandin.miniperfserver.proto.ProfileNtf;
import com.github.sandin.miniperfserver.util.ReflectionUtils;

/**
 * Memory Monitor
 *
 * @see Memory
 * <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/ActivityThread.java;l=2739?q=ActivityThread.jav&ss=android">ActivityThread</a>
 */
public class MemoryMonitor implements IMonitor<Memory> {
    private static final String TAG = "MemoryMonitor";

    private final ActivityManager mActivityManager;

    public MemoryMonitor(Context context) {
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    public Memory collect(Context context, TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect memory data: " + targetApp + ", timestamp=" + timestamp);
        Memory.Builder memoryBuilder = Memory.newBuilder();
        MemoryDetail.Builder memoryDetailBuilder = MemoryDetail.newBuilder();

        Debug.MemoryInfo[] processMemoryInfo = mActivityManager.getProcessMemoryInfo(new int[]{ targetApp.getPid() });
        if (processMemoryInfo != null && processMemoryInfo.length > 0) {
            final Debug.MemoryInfo memoryInfo = processMemoryInfo[0];

            // memory
            memoryBuilder.setPss(memoryInfo.getTotalPss() / 1024);
            final Integer swap = ReflectionUtils.invokeMethod(memoryInfo.getClass(), "getTotalSwappedOut",
                    new Class<?>[] {}, memoryInfo, new Object[] {}, true);
            memoryBuilder.setSwap(swap != null ? swap / 1024 : 0);
            //memoryBuilder.setVirtualMemory(virtualMemory); // TODO: virtualMemory

            // memory detail
            Integer numOtherStats = ReflectionUtils.getFieldValue(memoryInfo.getClass(), "NUM_OTHER_STATS", memoryInfo, true);
            int otherStatsNum = numOtherStats != null ? numOtherStats : 0;
            int unknownPss = memoryInfo.otherPss;
            Log.v(TAG, "memoryInfo.otherPss: " + memoryInfo.otherPss);
            int gl = -1;
            int gfx = -1;
            if (otherStatsNum > 0) {
                for (int i = 0; i < otherStatsNum; i++) {
                    final String otherLabel = ReflectionUtils.invokeMethod(memoryInfo.getClass(), "getOtherLabel",
                            new Class<?>[] { Integer.TYPE }, memoryInfo, new Object[] { i }, true);
                    final Integer otherPss = ReflectionUtils.invokeMethod(memoryInfo.getClass(), "getOtherPss",
                            new Class<?>[] { Integer.TYPE }, memoryInfo, new Object[] { i }, true);
                    Log.v(TAG, i + "/" + otherStatsNum + " otherLabel=" + otherLabel + ", otherPss=" + otherPss);
                    if (otherLabel != null && otherPss != null) {
                        unknownPss -= otherPss;
                        switch (otherLabel) {
                            case "GL":
                            case "GL mtrack":
                                gl = otherPss;
                                Log.v(TAG, "gl: " + gl);
                                break;
                            case "Gfx dev":
                                gfx = otherPss;
                                Log.v(TAG, "gfx: " + gfx);
                                break;
                        }
                    }
                }
            }
            if (gfx != -1) {
                memoryDetailBuilder.setGfx(gfx);
            }
            if (gl != -1) {
                memoryDetailBuilder.setGl(gl);
            }
            memoryDetailBuilder.setUnknown(unknownPss);
            memoryDetailBuilder.setNativePss(memoryInfo.nativePss);
        }
        memoryBuilder.setMemoryDetail(memoryDetailBuilder);
        Memory memory = memoryBuilder.build();
        Log.v(TAG, dumpMemory(memory));
        return memory;
    }

    /**
     * dump memoryInfo
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
}
