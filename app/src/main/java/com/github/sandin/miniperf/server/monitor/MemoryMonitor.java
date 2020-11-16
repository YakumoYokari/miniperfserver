package com.github.sandin.miniperf.server.monitor;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.Memory;
import com.github.sandin.miniperf.server.proto.MemoryDetail;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.VirtualMemory;
import com.github.sandin.miniperf.server.util.ReflectionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Memory Monitor
 *
 * @see Memory
 * <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/ActivityThread.java;l=2739?q=ActivityThread.jav&ss=android">ActivityThread</a>
 */
public class MemoryMonitor implements IMonitor<Memory> {
    private static final String TAG = "MemoryMonitor";

    private final ActivityManager mActivityManager;

    private Context mContext;

    private Map<ProfileReq.DataType, Boolean> mDataTypes = new HashMap<>();

    public MemoryMonitor(Context context) {
        //Looper.prepare(); // TODO: java.lang.RuntimeException: Only one Looper may be created per thread
        //Context context = ActivityThread.systemMain().getSystemContext();
        //mActivityManager = new ServiceManager().getActivityManager();
        mContext = context;
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    }

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

    private long getVssMemory(int pid) throws IOException {
        Log.i(TAG, "start collect vss memory");
        String vssSystemFilePath = "/proc/" + pid + "/status";
        long vss = 0;
        File file = new File(vssSystemFilePath);
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                String vssStr = "";
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("VmSize")) {
                        for (int i = 0; i < line.length(); i++) {
                            if (line.charAt(i) >= '0' && line.charAt(i) <= '9')
                                vssStr += line.charAt(i);
                        }
                    }
                }
                if (!vssStr.equals("")) {
                    vss = Integer.parseInt(vssStr);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                reader.close();
            }
        }
        return vss;
    }

    public Memory collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Log.v(TAG, "collect memory data: " + targetApp + ", timestamp=" + timestamp);
        Memory.Builder memoryBuilder = Memory.newBuilder();
        MemoryDetail.Builder memoryDetailBuilder = MemoryDetail.newBuilder();
        Log.i(TAG, "collect process memory info");
        //TODO can't get memory info
        Debug.MemoryInfo[] processMemoryInfo = mActivityManager.getProcessMemoryInfo(new int[]{targetApp.getPid()});
        Log.i(TAG, "process memory info size : " + processMemoryInfo.length);
        if (processMemoryInfo != null && processMemoryInfo.length > 0) {
            final Debug.MemoryInfo memoryInfo = processMemoryInfo[0];

            // memory
            memoryBuilder.setPss(memoryInfo.getTotalPss() / 1024);
            final Integer swap = ReflectionUtils.invokeMethod(memoryInfo.getClass(), "getTotalSwappedOut",
                    new Class<?>[]{}, memoryInfo, new Object[]{}, true);
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
                            new Class<?>[]{Integer.TYPE}, memoryInfo, new Object[]{i}, true);
                    final Integer otherPss = ReflectionUtils.invokeMethod(memoryInfo.getClass(), "getOtherPss",
                            new Class<?>[]{Integer.TYPE}, memoryInfo, new Object[]{i}, true);
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
        //vss
        long vss = getVssMemory(targetApp.getPid());
        memoryBuilder.setVirtualMemory(vss);
        Memory memory = memoryBuilder.build();
        Log.v(TAG, dumpMemory(memory));
        if (data != null) {
            data.setMemory(memory);
            data.setVirtualMemory(VirtualMemory.newBuilder().setVirtualMemory(Math.round((float) vss / 1024)).build());
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
