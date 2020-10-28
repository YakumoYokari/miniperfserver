package com.github.sandin.miniperf.server;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.genymobile.scrcpy.wrappers.ActivityThread;
import com.genymobile.scrcpy.wrappers.Process;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.monitor.AppListMonitor;
import com.github.sandin.miniperf.server.monitor.BatteryMonitor;
import com.github.sandin.miniperf.server.monitor.MemoryMonitor;
import com.github.sandin.miniperf.server.monitor.NetworkMonitor;
import com.github.sandin.miniperf.server.monitor.PerformanceMonitor;
import com.github.sandin.miniperf.server.monitor.ScreenshotMonitor;
import com.github.sandin.miniperf.server.proto.AppInfo;
import com.github.sandin.miniperf.server.proto.EmptyRsp;
import com.github.sandin.miniperf.server.proto.GetAppInfoRsp;
import com.github.sandin.miniperf.server.proto.GetBatteryInfoRsp;
import com.github.sandin.miniperf.server.proto.GetMemoryUsageReq;
import com.github.sandin.miniperf.server.proto.GetMemoryUsageRsp;
import com.github.sandin.miniperf.server.proto.Memory;
import com.github.sandin.miniperf.server.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperf.server.proto.Power;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.ProfileRsp;
import com.github.sandin.miniperf.server.proto.StopProfileRsp;
import com.github.sandin.miniperf.server.proto.ToggleInterestingFiledNTF;
import com.github.sandin.miniperf.server.server.SocketServer;
import com.github.sandin.miniperf.server.session.Session;
import com.github.sandin.miniperf.server.session.SessionManager;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;
import com.github.sandin.miniperf.server.util.ArgumentParser;

import java.util.List;

import androidx.annotation.Nullable;


/**
 * MiniPerfServer
 */
public class MiniPerfServer implements SocketServer.Callback  {
    private static final String TAG = "MiniPerfServer";

    /**
     * Unix domain socket name
     */
    private static final String UNIX_DOMAIN_SOCKET_NAME = "miniperfserver";

    /**
     * Normal socket port
     */
    private static final int NORMAL_SOCKET_PORT = 43300;

    //TODO use context
    private static Context mContext;

    @Nullable
    private MemoryMonitor mMemoryMonitor;

    @Nullable
    private BatteryMonitor mBatteryMonitor;

    @Nullable
    private AppListMonitor mAppListMonitor;

    private Session session;

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

        ArgumentParser argumentParser = new ArgumentParser();
        argumentParser.addArg("test", "test mode", false);
        argumentParser.addArg("app", "app mode", false);
        argumentParser.addArg("command", "command for test mode", true);
        ArgumentParser.Arguments arguments = argumentParser.parse(args);

        boolean isApp = arguments.has("app");
        if (!isApp) {
            Looper.prepareMainLooper();
            Process.setArgV0("MiniPerfServer");

            //TODO use context
            mContext = ActivityThread.systemMain().getSystemContext();

            // ONLY FOR TEST
            if (arguments.has("command")) {
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
        }

        MiniPerfServer app = new MiniPerfServer();
        app.run(arguments);
    }

    public static void test(ArgumentParser.Arguments arguments) throws Exception {
        String packageName = "com.xiaomi.shop";
        int pid = 5658;
        int uid = AndroidProcessUtils.getUid(mContext,packageName);
        TargetApp targetApp = new TargetApp(packageName, uid);
        String command = arguments.getAsString("command", null);
        if (command != null) {
            switch (command) {
                case "screenshot":
                    ScreenshotMonitor screenshotMonitor = new ScreenshotMonitor();
                    screenshotMonitor.takeScreenshot(System.out);
                    break;
                case "network":
                    NetworkMonitor networkMonitor = new NetworkMonitor(mContext);
                    networkMonitor.collect(targetApp,System.currentTimeMillis(),null);
                    break;
                case "appinfo":
                    AppListMonitor appListMonitor = new AppListMonitor(mContext);
                    List<AppInfo> appList = appListMonitor.getAppInfo();
                    for (AppInfo app : appList) {
                        System.out.println("app: " + app.getLabel());
                        System.out.println("package name: " + app.getPackageName());
                        System.out.println("version: " + app.getVersion());
                        System.out.println("uid: " + app.getPid());
                        System.out.println("system app: " + app.getIsSystemApp());
                        System.out.println("");
                    }
                    break;
                default:
                    System.out.println("[Error] unknown command: " + command);
                    break;
            }
        } else {
            System.out.println("[Error] no command");
        }
    }

    public void run(ArgumentParser.Arguments arguments) {
        // The normal socket server
        new Thread() {
            @Override
            public void run() {
                SocketServer server = new SocketServer(NORMAL_SOCKET_PORT, MiniPerfServer.this, 3);
                server.start(); // block op
            }
        }.start();

        // Unix domain socket server
        SocketServer server = new SocketServer(UNIX_DOMAIN_SOCKET_NAME, this, 3);
        server.start(); // block op
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
            // TODO: other requests
            case PROFILEREQ:
                return handleProfileReq(clientConnection, request.getProfileReq());
            case GETMEMORYUSAGEREQ:
                return handleGetMemoryUsageReq(request.getGetMemoryUsageReq());
            case GETBATTERYINFOREQ:
                return handleGetBatteryInfoReq();
            case GETAPPINFOREQ:
                return handleGetAppInfoReq();
            case HELLOREQ:
                return handleHelloReq();
            case STOPPROFILEREQ:
                return handleStopProfileReq();
            case TOGGLEINTERESTINGFILEDNTF:
                handleToggleInterestingFiledNtf(request.getToggleInterestingFiledNTF());
        }
        return null;
    }

    private void handleToggleInterestingFiledNtf(ToggleInterestingFiledNTF request) {
        int dataTypeNum = request.getDataType();
        ProfileReq.DataType dataType = ProfileReq.DataType.forNumber(dataTypeNum);
        PerformanceMonitor monitor = session.getMonitor();
        monitor.toggleInterestingDataTypes(dataType);
    }

    private byte[] handleStopProfileReq() {
        if (session != null) {
            session.getMonitor().stop();
            session.getConnection().close();
            session = null;
        }
        return MiniPerfServerProtocol.newBuilder().setStopProfileRsp(StopProfileRsp.newBuilder()).build().toByteArray();
    }

    private byte[] handleProfileReq(SocketServer.ClientConnection clientConnection, ProfileReq request) {
        //只能存在一条长链接
        if (session != null) {
            return MiniPerfServerProtocol.newBuilder().setProfileRsp(
                    ProfileRsp.newBuilder()
                            .setTimestamp(System.currentTimeMillis())
                            .setErrorCode(-1)
                            .setSessionId(session.getSessionId()))
                    .build().toByteArray();
        }
        TargetApp targetApp = new TargetApp();
        String packageName = request.getProfileApp().getAppInfo().getPackageName();
        targetApp.setPackageName(packageName);
        //TODO userid is pid
        //targetApp.setPid(request.getProfileApp().getAppInfo().getUserId());
        targetApp.setPid(AndroidProcessUtils.getPid(mContext, packageName));
        List<ProfileReq.DataType> dataTypes = request.getDataTypesList();
        Log.i(TAG, "recv profile data types : " + dataTypes.toString());
        int errorCode = 0;
        int sessionId = 0;
        PerformanceMonitor performanceMonitor = new PerformanceMonitor(mContext, 1000, 2000);
        session = SessionManager.getInstance().createSession(clientConnection, performanceMonitor, targetApp, dataTypes);
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

    private byte[] handleGetBatteryInfoReq() {
        if (mBatteryMonitor == null) {
            mBatteryMonitor = new BatteryMonitor(mContext, null);
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
            mAppListMonitor = new AppListMonitor(mContext);
        }
        try {
            List<AppInfo> appInfoList = mAppListMonitor.collect(null, 0, null);
            return MiniPerfServerProtocol.newBuilder().setGetAppInfoRsp(GetAppInfoRsp.newBuilder().addAllAppInfo(appInfoList)).build().toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] handleHelloReq() {
        MiniPerfServerProtocol emptyRsp = MiniPerfServerProtocol.newBuilder().setEmptyRsp(EmptyRsp.newBuilder()).build();
        return emptyRsp.toByteArray();
    }
}
