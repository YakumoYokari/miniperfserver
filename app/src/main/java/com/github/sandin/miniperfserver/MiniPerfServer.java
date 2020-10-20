package com.github.sandin.miniperfserver;

import android.os.Looper;
import android.util.Log;

import com.genymobile.scrcpy.wrappers.Process;
import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.monitor.AppListMonitor;
import com.github.sandin.miniperfserver.monitor.BatteryMonitor;
import com.github.sandin.miniperfserver.monitor.MemoryMonitor;
import com.github.sandin.miniperfserver.monitor.PerformanceMonitor;
import com.github.sandin.miniperfserver.monitor.ScreenshotMonitor;
import com.github.sandin.miniperfserver.proto.AppInfo;
import com.github.sandin.miniperfserver.proto.GetAppInfoRsp;
import com.github.sandin.miniperfserver.proto.GetBatteryInfoReq;
import com.github.sandin.miniperfserver.proto.GetBatteryInfoRsp;
import com.github.sandin.miniperfserver.proto.GetMemoryUsageReq;
import com.github.sandin.miniperfserver.proto.GetMemoryUsageRsp;
import com.github.sandin.miniperfserver.proto.Memory;
import com.github.sandin.miniperfserver.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperfserver.proto.Power;
import com.github.sandin.miniperfserver.proto.ProfileReq;
import com.github.sandin.miniperfserver.proto.ProfileRsp;
import com.github.sandin.miniperfserver.server.SocketServer;
import com.github.sandin.miniperfserver.session.Session;
import com.github.sandin.miniperfserver.session.SessionManager;
import com.github.sandin.miniperfserver.util.ArgumentParser;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * MiniPerfServer
 */
public class MiniPerfServer implements SocketServer.Callback {
    private static final String TAG = "MiniPerfServer";

    /**
     * Unix socket name
     */
    private static final String UNIX_DOMAIN_SOCKET_NAME = "miniperfserver";

    @Nullable
    private MemoryMonitor mMemoryMonitor;

    @Nullable
    private BatteryMonitor mBatteryMonitor;

    @Nullable
    private AppListMonitor mAppListMonitor;

    private MiniPerfServer() {
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
        if (arguments.has("test")) {
            try {
                test(arguments);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        /*
        Context context = ActivityThread.systemMain().getSystemContext();
        if (context == null) {
            Log.e(TAG, "Can not get system context!");
            return;
        }
         */
        MiniPerfServer app = new MiniPerfServer();
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
        SocketServer server = new SocketServer(UNIX_DOMAIN_SOCKET_NAME, this);
        server.start();
    }

    @Override
    public byte[] onMessage(SocketServer.ClientConnection clientConnection, byte[] msg) {
        try {
            MiniPerfServerProtocol request = MiniPerfServerProtocol.parseFrom(msg);
            return handleRequestMessage(clientConnection, request);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] handleRequestMessage(SocketServer.ClientConnection clientConnection, MiniPerfServerProtocol request) {
        switch (request.getProtocolCase()) {
            case PROFILEREQ:
                return handleProfileReq(clientConnection, request.getProfileReq());
            case GETMEMORYUSAGEREQ:
                return handleGetMemoryUsageReq(request.getGetMemoryUsageReq());
            case GETBATTERYINFOREQ:
                return handleGetBatteryInfoReq(request.getGetBatteryInfoReq());
            case GETAPPINFOREQ:
                return handleGetAppInfoReq();
            // TODO: other requests
        }
        return null;
    }

    private byte[] handleProfileReq(SocketServer.ClientConnection clientConnection, ProfileReq request) {
        TargetApp targetApp = new TargetApp();
        targetApp.setPackageName(request.getProfileApp().getAppInfo().getPackageName());
        targetApp.setPid(request.getProfileApp().getPidName().getPid());
        List<ProfileReq.DataType> dataTypes = request.getDataTypesList();

        int errorCode = 0;
        int sessionId = 0;
        PerformanceMonitor performanceMonitor = new PerformanceMonitor(1000, 2000);
        Session session = SessionManager.getInstance().createSession(clientConnection, performanceMonitor, targetApp, dataTypes);
        if (session != null) {
            sessionId = session.getSessionId();
        } else {
            errorCode = -1; // TODO: errorCode enum
        }
        return MiniPerfServerProtocol.newBuilder().setProfileRsp(
                ProfileRsp.newBuilder()
                        .setTimestamp(System.currentTimeMillis())
                        .setErrorCode(errorCode)
                        .setSessionId(sessionId)
        ).build().toByteArray();
    }

    private byte[] handleGetMemoryUsageReq(GetMemoryUsageReq request) {
        if (mMemoryMonitor == null) {
            mMemoryMonitor = new MemoryMonitor();
        }
        try {
            Memory memory = mMemoryMonitor.collect(new TargetApp(null, request.getPid()), 0, null);
            return MiniPerfServerProtocol.newBuilder().setGetMemoryUsageRsp(GetMemoryUsageRsp.newBuilder().setMemory(memory)).build().toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] handleGetBatteryInfoReq(GetBatteryInfoReq request) {
        if (mBatteryMonitor == null) {
            mBatteryMonitor = new BatteryMonitor(null);
        }
        try {
            Power power = mBatteryMonitor.collect(null, 0, null);
            return MiniPerfServerProtocol.newBuilder().setGetBatteryInfoRsp(GetBatteryInfoRsp.newBuilder().setPower(power)).build().toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] handleGetAppInfoReq() {
        if (mAppListMonitor == null) {
            mAppListMonitor = new AppListMonitor();
        }
        try {
            List<AppInfo> appInfoList = mAppListMonitor.collect(null, 0, null);
            return MiniPerfServerProtocol.newBuilder().setGetAppInfoRsp(GetAppInfoRsp.newBuilder().addAllAppInfo(appInfoList)).build().toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
