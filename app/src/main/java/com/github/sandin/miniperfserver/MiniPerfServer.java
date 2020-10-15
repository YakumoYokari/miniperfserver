package com.github.sandin.miniperfserver;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.genymobile.scrcpy.wrappers.ActivityThread;
import com.genymobile.scrcpy.wrappers.Process;
import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.monitor.BatteryMonitor;
import com.github.sandin.miniperfserver.monitor.MemoryMonitor;
import com.github.sandin.miniperfserver.monitor.ScreenshotMonitor;
import com.github.sandin.miniperfserver.proto.GetBatteryInfoReq;
import com.github.sandin.miniperfserver.proto.GetBatteryInfoRsp;
import com.github.sandin.miniperfserver.proto.GetMemoryUsageReq;
import com.github.sandin.miniperfserver.proto.GetMemoryUsageRsp;
import com.github.sandin.miniperfserver.proto.Memory;
import com.github.sandin.miniperfserver.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperfserver.proto.Power;
import com.github.sandin.miniperfserver.server.SocketServer;
import com.github.sandin.miniperfserver.util.ArgumentParser;

/**
 * MiniPerfServer
 */
public class MiniPerfServer implements SocketServer.Callback {
    private static final String TAG = "MiniPerfServer";

    /**
     * Unix socket name
     */
    private static final String UNIX_DOMAIN_SOCKET_NAME = "miniperfserver";

    @NonNull
    private final Context mContext;

    @Nullable
    private MemoryMonitor mMemoryMonitor;

    @Nullable
    private BatteryMonitor mBatteryMonitor;

    private MiniPerfServer(@NonNull Context context) {
        mContext = context;
    }

    /**
     * EnterPoint
     *
     * <pre>
     *      ANDROID_DATA=/data/local/tmp \
     *      app_process -Djava.class.path=/data/local/tmp/MiniPerfServer.dex \
     *      /data/local/tmp \
     *      com.github.sandin.miniperfserver.MiniPerfServer
     * </pre>
     */
    public static void main(String[] args) {
        Log.i(TAG, "launch, version " + BuildConfig.VERSION_NAME);
        Looper.prepareMainLooper();
        Process.setArgV0("MiniPerfServer");

        ArgumentParser argumentParser = new ArgumentParser();
        argumentParser.addArg("test", "test mode", false);
        argumentParser.addArg("command", "command for test mode", true);
        ArgumentParser.Arguments arguments = argumentParser.parse(args);

        // ONLY FOR TEST
        if (arguments.has("test" )) {
            try {
                test(arguments);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        Context context = ActivityThread.systemMain().getSystemContext();
        if (context == null) {
            Log.e(TAG, "Can not get system context!");
            return;
        }
        MiniPerfServer app = new MiniPerfServer(context);
        app.run(arguments);
    }

    public static void test(ArgumentParser.Arguments arguments) throws Exception {
        String command = arguments.getAsString("command", null);
        if (command != null) {
            switch (command) {
                case "screenshot":
                    ScreenshotMonitor screenshotMonitor = new ScreenshotMonitor();
                    screenshotMonitor.takeScreenshot(System.out);
            }
        } else {
            System.out.println("[Error] no command");
        }
    }

    public void run(ArgumentParser.Arguments arguments) {
        SocketServer server = new SocketServer(mContext, UNIX_DOMAIN_SOCKET_NAME, this);
        server.start();
    }

    @Override
    public byte[] onMessage(byte[] msg) {
        try {
            MiniPerfServerProtocol request = MiniPerfServerProtocol.parseFrom(msg);
            return handleRequestMessage(request);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] handleRequestMessage(MiniPerfServerProtocol request) {
        // TODO: other requests
        switch (request.getProtocolCase()){
            case GETMEMORYUSAGEREQ:
                return handleGetMemoryUsageReq(request.getGetMemoryUsageReq());
            case GETBATTERYINFOREQ:
                return handleGetBatteryInfoReq(request.getGetBatteryInfoReq());
        }
        return null;
    }

    private byte[] handleGetMemoryUsageReq(GetMemoryUsageReq request) {
        if (mMemoryMonitor == null) {
            mMemoryMonitor = new MemoryMonitor(mContext);
        }
        try {
            Memory memory = mMemoryMonitor.collect(mContext, new TargetApp(null, request.getPid()), 0);
            return MiniPerfServerProtocol.newBuilder().setGetMemoryUsageRsp(GetMemoryUsageRsp.newBuilder().setMemory(memory)).build().toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] handleGetBatteryInfoReq(GetBatteryInfoReq request) {
        if (mBatteryMonitor == null) {
            mBatteryMonitor = new BatteryMonitor(mContext, null);
        }
        try {
            Power power = mBatteryMonitor.collect(mContext, null, 0);
            return MiniPerfServerProtocol.newBuilder().setGetBatteryInfoRsp(GetBatteryInfoRsp.newBuilder().setPower(power)).build().toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
