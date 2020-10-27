package com.github.sandin.server;

import com.github.sandin.miniperf.server.monitor.BatteryMonitor;
import com.github.sandin.miniperf.server.monitor.MemoryMonitor;
import com.github.sandin.miniperf.server.proto.AppInfo;
import com.github.sandin.miniperf.server.proto.GetAppInfoReq;
import com.github.sandin.miniperf.server.proto.GetBatteryInfoReq;
import com.github.sandin.miniperf.server.proto.GetBatteryInfoRsp;
import com.github.sandin.miniperf.server.proto.GetMemoryUsageReq;
import com.github.sandin.miniperf.server.proto.GetMemoryUsageRsp;
import com.github.sandin.miniperf.server.proto.Memory;
import com.github.sandin.miniperf.server.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperf.server.proto.Power;
import com.github.sandin.miniperf.server.proto.ProfileApp;
import com.github.sandin.miniperf.server.proto.ProfileAppInfo;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.StopProfileReq;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(value = Parameterized.class)
public class SocketServerUnitTest {
    private static final String TAG = "SocketTest";

    private SocketClient mClient;

    private final String mHost;
    private final int mPort;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "127.0.0.1", 45455 },     // USB Mode: unix domain socket -> adb forward -> 45455
                //{ "10.11.251.98", 43300 }, // Wifi Mode: <phone ip>:<server port>
        });
    }

    public SocketServerUnitTest(String host, int port) {
        mHost = host;
        mPort = port;
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Socket Server: " + mHost + ":" + mPort);
        mClient = new SocketClient(mHost, mPort);
    }

    @Test
    public void pingServer() throws Exception {
        mClient.sendMessage("ping".getBytes());
        byte[] bytes = mClient.readMessage();
        System.out.println("response: " + new String(bytes));
        Assert.assertEquals("pong", new String(bytes));
    }

    @Test
    public void getMemory() throws Exception {
        int pid = 2318;

        MiniPerfServerProtocol request = MiniPerfServerProtocol.newBuilder().setGetMemoryUsageReq(
                GetMemoryUsageReq.newBuilder().setPid(pid)).build();
        System.out.println("send request: " + request);
        mClient.sendMessage(request.toByteArray());
        byte[] bytes = mClient.readMessage();
        MiniPerfServerProtocol response = MiniPerfServerProtocol.parseFrom(bytes);
        System.out.println("revc response: " + response);

        GetMemoryUsageRsp getMemoryUsageRsp = response.getGetMemoryUsageRsp();
        Assert.assertNotNull(getMemoryUsageRsp);

        Memory memory = getMemoryUsageRsp.getMemory();
        Assert.assertNotNull(memory);
        System.out.println("memory" + MemoryMonitor.dumpMemory(memory));
        Assert.assertTrue(memory.getPss() > 0);
    }

    @Test
    public void getBatteryInfo() throws IOException {
        MiniPerfServerProtocol request = MiniPerfServerProtocol.newBuilder().setGetBatteryInfoReq(GetBatteryInfoReq.newBuilder()).build();
        System.out.println("send request: " + request);
        mClient.sendMessage(request.toByteArray());
        byte[] bytes = mClient.readMessage();
        MiniPerfServerProtocol response = MiniPerfServerProtocol.parseFrom(bytes);
        System.out.println("recv response: " + response);

        GetBatteryInfoRsp rsp = response.getGetBatteryInfoRsp();
        Assert.assertNotNull(rsp);
        Power power = rsp.getPower();
        Assert.assertNotNull(power);
        System.out.println("power" + BatteryMonitor.dumpPower(power));
    }

    @Test
    public void getAppListTest() throws IOException {
        MiniPerfServerProtocol request = MiniPerfServerProtocol.newBuilder().setGetAppInfoReq(GetAppInfoReq.newBuilder()).build();
        System.out.println("send request: " + request);
        mClient.sendMessage(request.toByteArray());
        byte[] bytes = mClient.readMessage();
        MiniPerfServerProtocol response = MiniPerfServerProtocol.parseFrom(bytes);
        System.out.println("recv response: " + response);
        List<AppInfo> appInfoList = response.getGetAppInfoRsp().getAppInfoList();
        Assert.assertNotNull(appInfoList);
    }

    @Test
    public void profileReqTest() throws IOException {
        String packageName = "com.xiaomi.shop";
        int pid = 13642;
        String processName = packageName;
        ProfileApp app = ProfileApp.newBuilder()
                .setAppInfo(ProfileAppInfo.newBuilder().setPackageName(packageName).setProcessName(processName).setUserId(pid))
                .build();
        System.out.println("profile app : " + app.toString());
        MiniPerfServerProtocol request = MiniPerfServerProtocol.newBuilder()
                .setProfileReq(ProfileReq.newBuilder().setProfileApp(app)
//                        .addDataTypes(ProfileReq.DataType.SCREEN_SHOT)
//                        .addDataTypes(ProfileReq.DataType.CPU_TEMPERATURE)
//                        .addDataTypes(ProfileReq.DataType.CORE_FREQUENCY)
                                .addDataTypes(ProfileReq.DataType.FPS)
                                .addDataTypes(ProfileReq.DataType.FRAME_TIME)
//                        .addDataTypes(ProfileReq.DataType.MEMORY)
//                        .addDataTypes(ProfileReq.DataType.BATTERY)
//                        .addDataTypes(ProfileReq.DataType.GPU_USAGE)
//                        .addDataTypes(ProfileReq.DataType.GPU_FREQ)
                )
                .build();
        System.out.println("send request: " + request);
        mClient.sendMessage(request.toByteArray());
        //ProfileRsp
        byte[] profileRspBytes = mClient.readMessage();
        System.out.println("recv response bytes length: " + profileRspBytes.length);
        MiniPerfServerProtocol profileRsp = MiniPerfServerProtocol.parseFrom(profileRspBytes);
        System.out.println("recv response: " + profileRsp);
        while (true) {
            byte[] bytes = mClient.readMessage();
            MiniPerfServerProtocol response = MiniPerfServerProtocol.parseFrom(bytes);
            System.out.println("recv response: " + response);
            ProfileNtf profileNtf = response.getProfileNtf();
            Assert.assertNotNull(profileNtf);
            System.out.println(profileNtf.toString());
        }
    }

    @Test
    public void stopTest() throws IOException {
        MiniPerfServerProtocol request = MiniPerfServerProtocol.newBuilder().setStopProfileReq(StopProfileReq.newBuilder().build()).build();
        System.out.println("send request: " + request);
        mClient.sendMessage(request.toByteArray());
        byte[] rsp = mClient.readMessage();
        System.out.println("recv response bytes length: " + rsp.length);
        MiniPerfServerProtocol response = MiniPerfServerProtocol.parseFrom(rsp);
        System.out.println("recv response: " + response);
    }

}