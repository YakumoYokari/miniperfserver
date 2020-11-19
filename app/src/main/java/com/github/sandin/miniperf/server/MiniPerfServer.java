package com.github.sandin.miniperf.server;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.genymobile.scrcpy.wrappers.ActivityThread;
import com.genymobile.scrcpy.wrappers.Process;
import com.github.sandin.miniperf.app.BuildConfig;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.bean.TrafficInfo;
import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.monitor.AppListMonitor;
import com.github.sandin.miniperf.server.monitor.BatteryMonitor;
import com.github.sandin.miniperf.server.monitor.CpuTemperatureMonitor;
import com.github.sandin.miniperf.server.monitor.GpuFreqMonitor;
import com.github.sandin.miniperf.server.monitor.GpuUsageMonitor;
import com.github.sandin.miniperf.server.monitor.MemoryMonitor;
import com.github.sandin.miniperf.server.monitor.NetworkMonitor;
import com.github.sandin.miniperf.server.monitor.PerformanceMonitor;
import com.github.sandin.miniperf.server.monitor.ScreenshotMonitor;
import com.github.sandin.miniperf.server.proto.AppInfo;
import com.github.sandin.miniperf.server.proto.CheckDeviceRsp;
import com.github.sandin.miniperf.server.proto.EmptyRsp;
import com.github.sandin.miniperf.server.proto.GetAppInfoRsp;
import com.github.sandin.miniperf.server.proto.GetBatteryInfoRsp;
import com.github.sandin.miniperf.server.proto.GetMemoryUsageReq;
import com.github.sandin.miniperf.server.proto.GetMemoryUsageRsp;
import com.github.sandin.miniperf.server.proto.GpuFreq;
import com.github.sandin.miniperf.server.proto.GpuUsage;
import com.github.sandin.miniperf.server.proto.Memory;
import com.github.sandin.miniperf.server.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperf.server.proto.Power;
import com.github.sandin.miniperf.server.proto.ProcessFoundNTF;
import com.github.sandin.miniperf.server.proto.ProcessNotFoundNTF;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.ProfileRsp;
import com.github.sandin.miniperf.server.proto.StopProfileRsp;
import com.github.sandin.miniperf.server.proto.Temp;
import com.github.sandin.miniperf.server.proto.ToggleInterestingFiledNTF;
import com.github.sandin.miniperf.server.server.SocketServer;
import com.github.sandin.miniperf.server.session.Session;
import com.github.sandin.miniperf.server.session.SessionManager;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;
import com.github.sandin.miniperf.server.util.ArgumentParser;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.util.List;


/**
 * MiniPerfServer
 */
public class MiniPerfServer implements SocketServer.Callback {
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
        argumentParser.addArg("pkg", "package name", true);

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
//        String packageName = arguments.getAsString("pkg", null);
        String packageName = "com.xiaomi.market";
        System.out.println("test package name is : " + packageName);
        while (!AndroidProcessUtils.checkAppIsRunning(mContext, packageName)) {
            System.out.println("wait for app start");
            Thread.sleep(500);
        }
        int pid = AndroidProcessUtils.getPid(mContext, packageName);
        int uid = AndroidProcessUtils.getUid(mContext, packageName);
        TargetApp targetApp = new TargetApp(packageName, pid);
        String command = arguments.getAsString("command", null);
        if (command != null) {
            switch (command) {
                case "memory":
                    System.out.println("start test memory");
                    MemoryMonitor memoryMonitor = new MemoryMonitor();
                    while (true) {
                        Memory memory = memoryMonitor.collect(targetApp, System.currentTimeMillis(), null);
                        String dumpMemory = MemoryMonitor.dumpMemory(memory);
                        System.out.println(dumpMemory);
                        Thread.sleep(1000);
                    }
                case "screenshot":
                    ScreenshotMonitor screenshotMonitor = new ScreenshotMonitor();
                    screenshotMonitor.takeScreenshot(System.out);
                    break;
                case "network":
                    //Tx : send ,Rx : recv
                    NetworkMonitor networkMonitor = new NetworkMonitor(mContext);
                    while (true) {
//                        Network network = networkMonitor.collect(targetApp, System.currentTimeMillis(), null);
//                        System.out.println(network.getUpload());
//                        System.out.println(network.getDownload());
                        TrafficInfo traffics = networkMonitor.getTrafficsFromDumpsys(uid);
                        System.out.println("rx : " + traffics.getDownload());
                        System.out.println("tx : " + traffics.getUpload());
                        Thread.sleep(500);
                    }
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
                case "cputemp":
                    final CpuTemperatureMonitor cpuTemperatureMonitor = new CpuTemperatureMonitor();
                    while (true) {
                        Temp temp = cpuTemperatureMonitor.collect(targetApp, System.currentTimeMillis(), null);
                        System.out.println("temp : " + temp);
                        Thread.sleep(1000);
                    }
                case "gpu":
                    GpuFreqMonitor gpuFreqMonitor = new GpuFreqMonitor();
                    GpuUsageMonitor gpuUsageMonitor = new GpuUsageMonitor();
                    GpuFreq gpuFreq = gpuFreqMonitor.collect(null, System.currentTimeMillis(), null);
                    GpuUsage usage = gpuUsageMonitor.collect(null, System.currentTimeMillis(), null);
                    System.out.println("gpu freq : " + gpuFreq.getGpuFreq());
                    System.out.println("gpu usage : " + usage.getGpuUsage());
                    break;

                case "battery":
                    BatteryMonitor batteryMonitor = new BatteryMonitor(mContext, null);
                    Power power = batteryMonitor.collect(null, System.currentTimeMillis(), null);
                    System.out.println(power.getCurrent());
                    System.out.println(power.getVoltage());
                    System.out.println(Build.BRAND);
                    break;
                case "alive":
//                    while (true) {
//                        boolean appIsRunning = AndroidProcessUtils.checkAppIsRunning(mContext, packageName);
//                        System.out.println(appIsRunning);
//                        Thread.sleep(500);
//                    }
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
            Log.i(TAG, "recv message: " + request.toString() + ", client=" + clientConnection.getClientName());
            return handleRequestMessage(clientConnection, request);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] handleRequestMessage(SocketServer.ClientConnection clientConnection, MiniPerfServerProtocol request) throws Exception {
        switch (request.getProtocolCase()) {
            // TODO: other requests
            case PROFILEREQ:
                Log.i(TAG, "handleRequestMessage: PROFILEREQ");
                return handleProfileReq(clientConnection, request.getProfileReq());
            case PROFILENTFACK:
                Log.i(TAG, "handleRequestMessage: PROFILENTFACK");
                return null; // TODO:
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
                Log.i(TAG, "handleRequestMessage: TOGGLEINTERESTINGFILEDNTF");
                return handleToggleInterestingFiledNtf(request.getToggleInterestingFiledNTF());
            case CHECKDEVICEREQ:
                return handleCheckDeviceReq();
            default:
                Log.i(TAG, "handleRequestMessage: Unknown protocol " + request.getProtocolCase());
                break;
        }
        return null;
    }

    private byte[] handleToggleInterestingFiledNtf(ToggleInterestingFiledNTF request) {
        int dataTypeNum = request.getDataType();
        ProfileReq.DataType dataType = ProfileReq.DataType.forNumber(dataTypeNum);
        PerformanceMonitor monitor = session.getMonitor();
        monitor.toggleInterestingDataTypes(dataType);
        return null;
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
        List<ProfileReq.DataType> dataTypes = request.getDataTypesList();
        Log.i(TAG, "recv profile data types : " + dataTypes.toString());
        int errorCode = 0;
        int sessionId = 0;
        boolean appIsRunning = AndroidProcessUtils.checkAppIsRunning(mContext, packageName);
        Log.i(TAG, "now app state is : " + appIsRunning);
        //waiting for app start
        while (!appIsRunning) {
            try {
                Log.i(TAG, "wait for app start");
                clientConnection.sendMessage(
                        MiniPerfServerProtocol.newBuilder().setProcessNotFoundNTF(
                                ProcessNotFoundNTF.newBuilder()
                        ).build().toByteArray()
                );
                Thread.sleep(500);
                appIsRunning = AndroidProcessUtils.checkAppIsRunning(mContext, packageName);
                Log.i(TAG, "now app state is : " + appIsRunning);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //found process
        clientConnection.sendMessage(
                MiniPerfServerProtocol.newBuilder().setProcessFoundNTF(
                        ProcessFoundNTF.newBuilder()
                ).build().toByteArray()
        );
        int pid = AndroidProcessUtils.getPid(mContext, packageName);
        Log.d(TAG, "application pid is : " + pid);
        targetApp.setPid(pid);
        PerformanceMonitor performanceMonitor = new PerformanceMonitor(mContext, 1000, 2000);
        session = SessionManager.getInstance().createSession(clientConnection, performanceMonitor, targetApp, dataTypes);
        if (session != null) {
            sessionId = session.getSessionId();
        } else {
            errorCode = -2; // TODO: errorCode enum
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

    private byte[] handleCheckDeviceReq() throws Exception {
        List<String> gpuFreqContent = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.GPU_CLOCK_SYSTEM_FILE_PATHS);
        List<String> gpuUsageContent = ReadSystemInfoUtils.readInfoFromSystemFile(DataSource.GPU_USAGE_SYSTEM_FILE_PATHS);
        CpuTemperatureMonitor cpuTemperatureMonitor = new CpuTemperatureMonitor();
        Temp temp = cpuTemperatureMonitor.collect(null, System.currentTimeMillis(), null);
        System.out.println("get temp success : " + temp);
        CheckDeviceRsp.Builder rspBuilder = CheckDeviceRsp.newBuilder();
        if (gpuFreqContent.size() > 0) {
            rspBuilder.setGpuFreq(true);
        } else {
            rspBuilder.setGpuFreq(false);
        }
        if (gpuUsageContent.size() > 0) {
            rspBuilder.setGpuUsage(true);
        } else {
            rspBuilder.setGpuUsage(false);
        }
        if (temp.getTemp() >= 0 && temp.getTemp() <= 100) {
            rspBuilder.setCpuTemperature(true);
        } else {
            rspBuilder.setCpuTemperature(false);
        }
        System.out.println(rspBuilder.build());
        return MiniPerfServerProtocol.newBuilder().setCheckDeviceRsp(rspBuilder).build().toByteArray();
    }
}
