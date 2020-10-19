package com.github.sandin.miniperfserver;

import com.github.sandin.miniperfserver.monitor.BatteryMonitor;
import com.github.sandin.miniperfserver.monitor.MemoryMonitor;
import com.github.sandin.miniperfserver.proto.AppInfo;
import com.github.sandin.miniperfserver.proto.GetAppInfoReq;
import com.github.sandin.miniperfserver.proto.GetAppInfoRsp;
import com.github.sandin.miniperfserver.proto.GetBatteryInfoReq;
import com.github.sandin.miniperfserver.proto.GetBatteryInfoRsp;
import com.github.sandin.miniperfserver.proto.GetMemoryUsageReq;
import com.github.sandin.miniperfserver.proto.GetMemoryUsageRsp;
import com.github.sandin.miniperfserver.proto.Memory;
import com.github.sandin.miniperfserver.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperfserver.proto.Power;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class SocketServerUnitTest {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 6100;

    private SocketClient mClient;

    @Before
    public void setUp() throws Exception {
        mClient = new SocketClient(HOST, PORT);
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
        int pid = 26077;

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
        System.out.println("power"+ BatteryMonitor.dumpPower(power));
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

}