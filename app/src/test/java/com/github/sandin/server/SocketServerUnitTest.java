package com.github.sandin.server;

import com.github.sandin.miniperf.server.monitor.BatteryMonitor;
import com.github.sandin.miniperf.server.monitor.MemoryMonitor;
import com.github.sandin.miniperf.server.proto.AppInfo;
import com.github.sandin.miniperf.server.proto.CheckDeviceReq;
import com.github.sandin.miniperf.server.proto.CoreUsage;
import com.github.sandin.miniperf.server.proto.CpuFreq;
import com.github.sandin.miniperf.server.proto.CpuUsage;
import com.github.sandin.miniperf.server.proto.FPS;
import com.github.sandin.miniperf.server.proto.FrameTime;
import com.github.sandin.miniperf.server.proto.GetAppInfoReq;
import com.github.sandin.miniperf.server.proto.GetBatteryInfoReq;
import com.github.sandin.miniperf.server.proto.GetBatteryInfoRsp;
import com.github.sandin.miniperf.server.proto.GetMemoryUsageReq;
import com.github.sandin.miniperf.server.proto.GetMemoryUsageRsp;
import com.github.sandin.miniperf.server.proto.GpuFreq;
import com.github.sandin.miniperf.server.proto.GpuUsage;
import com.github.sandin.miniperf.server.proto.Memory;
import com.github.sandin.miniperf.server.proto.MemoryDetail;
import com.github.sandin.miniperf.server.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperf.server.proto.Network;
import com.github.sandin.miniperf.server.proto.Power;
import com.github.sandin.miniperf.server.proto.ProfileApp;
import com.github.sandin.miniperf.server.proto.ProfileAppInfo;
import com.github.sandin.miniperf.server.proto.ProfileNTFACK;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.StopProfileReq;
import com.github.sandin.miniperf.server.proto.Temp;
import com.github.sandin.miniperf.server.proto.ToggleInterestingFiledNTF;

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
    private final String mHost;
    private final int mPort;
    private SocketClient mClient;

    public SocketServerUnitTest(String host, int port) {
        mHost = host;
        mPort = port;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"127.0.0.1", 45455},     // USB Mode: unix domain socket -> adb forward -> 45455
//                { "10.11.252.38", 43300 }, // Wifi Mode: <phone ip>:<server port>
        });
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
        int pid = 6958;

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
        String packageName = "com.tencent.tmgp.jx3m";
        //packageName = "com.xsj.jxsj3.xsj";
        //packageName = "cn.testplus.qc_agent";
        int pid = 3104;
        String processName = packageName;
        ProfileApp app = ProfileApp.newBuilder()
                .setAppInfo(ProfileAppInfo.newBuilder().setPackageName(packageName).setProcessName(processName).setUserId(pid))
                .build();
        System.out.println("profile app : " + app.toString());
        MiniPerfServerProtocol request = MiniPerfServerProtocol.newBuilder()
                .setProfileReq(ProfileReq.newBuilder().setProfileApp(app)
                        .addDataTypes(ProfileReq.DataType.MEMORY)
                        .addDataTypes(ProfileReq.DataType.CPU_USAGE)
                        .addDataTypes(ProfileReq.DataType.CORE_FREQUENCY)
                        .addDataTypes(ProfileReq.DataType.GPU_USAGE)
                        .addDataTypes(ProfileReq.DataType.GPU_FREQ)
                        .addDataTypes(ProfileReq.DataType.FPS)
                        .addDataTypes(ProfileReq.DataType.NETWORK_USAGE)
                        .addDataTypes(ProfileReq.DataType.BATTERY)
                        .addDataTypes(ProfileReq.DataType.CPU_TEMPERATURE)
                        .addDataTypes(ProfileReq.DataType.FRAME_TIME)
                        .addDataTypes(ProfileReq.DataType.ANDROID_MEMORY_DETAIL)
                        .addDataTypes(ProfileReq.DataType.CORE_USAGE)
                )
                .build();
        System.out.println("send request: " + request);
        mClient.sendMessage(request.toByteArray());
        //ProfileRsp
        byte[] profileRspBytes = mClient.readMessage();
        System.out.println("recv response bytes length: " + profileRspBytes.length);
        MiniPerfServerProtocol profileRsp = MiniPerfServerProtocol.parseFrom(profileRspBytes);
        System.out.println("recv response: " + profileRsp);
        Assert.assertNotNull(profileRsp);
        profileRsp = MiniPerfServerProtocol.parseFrom(profileRspBytes);
        System.out.println("recv response: " + profileRsp);

        String[] a = {"cpuUsage", "cpuFreq", "GpuUsage", "GpuFreq", "Fps", "Network", "Memory", "Power", "Temp", "FrameTime", "MemoryDetail", "CoreUsage"};
        int[] ishas = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,};

        for (int i = 0; i < 50; i++) {

            // read profile message
            byte[] bytes = mClient.readMessage();

            // send toggle dataType message

            if (i % 10 == 0) {
                System.out.println("send toggle dataType request");
                MiniPerfServerProtocol toggleRequest = MiniPerfServerProtocol.newBuilder().setToggleInterestingFiledNTF(
                        ToggleInterestingFiledNTF.newBuilder().setDataType(ProfileReq.DataType.MEMORY.getNumber())).build();
                mClient.sendMessage(toggleRequest.toByteArray());
            }


            // send ack request message
            MiniPerfServerProtocol ackRequest = MiniPerfServerProtocol.newBuilder().setProfileNTFACK(ProfileNTFACK.newBuilder()).build();
            mClient.sendMessage(ackRequest.toByteArray());

            MiniPerfServerProtocol response = MiniPerfServerProtocol.parseFrom(bytes);
            System.out.println("recv response: " + response);
            ProfileNtf profileNtf = response.getProfileNtf();
            Assert.assertNotNull(profileNtf);
            System.out.println(profileNtf.toString());
            CpuUsage cpuUsage = profileNtf.getCpuUsage();
            CpuFreq cpuFreq = profileNtf.getCpuFreq();
            GpuUsage gpuUsage = profileNtf.getGpuUsage();
            GpuFreq gpuFreq = profileNtf.getGpuFreq();
            FPS fps = profileNtf.getFps();
            Network network = profileNtf.getNetwork();
            Memory memory = profileNtf.getMemory();
            Power power = profileNtf.getPower();
            Temp temp = profileNtf.getTemp();
            FrameTime frameTime = profileNtf.getFrameTime();
            MemoryDetail memoryDetail = profileNtf.getMemory().getMemoryDetail();
            CoreUsage coreUsage = profileNtf.getCoreUsage();
            //System.out.println("123:"+profileNtf.getTemp().getTemp()+"   "+(profileNtf.getTemp().getTemp()>10&&ishas[0]==0)+"   ***"+ishas[8]);

            ishas[0] = ((profileNtf.getCpuUsage().getAppUsage() != 0 || profileNtf.getCpuUsage().getTotalUsage() != 0) && ishas[0] == 0 ? 1 : ishas[0]);
            //ishas[1] = (profileNtf.getCpuFreq().getCpuFreq(0)!=0&&ishas[1]==0?1:ishas[1]);
            ishas[2] = (profileNtf.getGpuUsage().getGpuUsage() != 0 && ishas[2] == 0 ? 1 : ishas[2]);
            ishas[3] = (profileNtf.getGpuFreq().getGpuFreq() > 0 && ishas[3] == 0 ? 1 : ishas[3]);
            ishas[4] = ((profileNtf.getFps().getFps() != 0 || profileNtf.getFps().getBigJank() != 0 || profileNtf.getFps().getJank() != 0) && ishas[4] == 0 ? 1 : ishas[4]);
            ishas[5] = ((profileNtf.getNetwork().getDownload() > 0 || profileNtf.getNetwork().getUpload() > 0) && ishas[5] == 0 ? 1 : ishas[5]);
            ishas[6] = ((profileNtf.getMemory().getPss() != 0 || profileNtf.getMemory().getSwap() != 0 || profileNtf.getMemory().getVirtualMemory() != 0) && ishas[6] == 0 ? 1 : ishas[6]);
            ishas[7] = ((profileNtf.getPower().getCurrent() != 0 || profileNtf.getPower().getVoltage() != 0) && ishas[7] == 0 ? 1 : ishas[7]);
            ishas[8] = (profileNtf.getTemp().getTemp() > 10 && ishas[8] == 0 ? 1 : ishas[8]);
            ishas[9] = (profileNtf.getFrameTime().getFrameTimeCount() != 0 && ishas[9] == 0 ? 1 : ishas[9]);
            ishas[10] = ((profileNtf.getMemory().getMemoryDetail().getGfx() != 0 || profileNtf.getMemory().getMemoryDetail().getGl() != 0 || profileNtf.getMemory().getMemoryDetail().getNativePss() != 0 || profileNtf.getMemory().getMemoryDetail().getUnknown() != 0) && ishas[10] == 0 ? 1 : ishas[10]);
            ishas[11] = (profileNtf.getCoreUsage().getCoreUsageCount() != 0 && ishas[11] == 0 ? 1 : ishas[11]);

            if (profileNtf.getCpuUsage().getAppUsage() > 100 || profileNtf.getCpuUsage().getAppUsage() < 0 || profileNtf.getCpuUsage().getTotalUsage() > 100 || profileNtf.getCpuUsage().getTotalUsage() < 0) {
                ishas[0] = 3;
            }
            if (profileNtf.getGpuUsage().getGpuUsage() < 0 || profileNtf.getGpuUsage().getGpuUsage() > 100) {
                ishas[2] = 3;
            }


        }
        System.out.println();
        for (int i = 0; i < ishas.length; i++) {
            if (ishas[i] != 1) {
                System.out.println(a[i]);
            }
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

    @Test
    public void checkTest() throws IOException {
        MiniPerfServerProtocol request = MiniPerfServerProtocol.newBuilder().setCheckDeviceReq(CheckDeviceReq.newBuilder()).build();
        System.out.println("send request: " + request);
        mClient.sendMessage(request.toByteArray());
        byte[] rsp = mClient.readMessage();
        System.out.println("recv response bytes length: " + rsp.length);
        MiniPerfServerProtocol response = MiniPerfServerProtocol.parseFrom(rsp);
        System.out.println("recv response: " + response);
    }


}