package com.github.sandin.server;

import android.content.Context;

import com.github.sandin.miniperf.server.server.SocketServer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class SocketServerTest {
    private static final String SOCKET_NAME = "testsocketserver";

    private Context mContext;
    private SocketServer mSocketServer;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mSocketServer = new SocketServer(SOCKET_NAME, new SocketServer.Callback() {
            @Override
            public byte[] onMessage(SocketServer.ClientConnection clientConnection, byte[] msg) {
                return null;
            }
        }, 3);
    }

    @After
    public void tearDown() {
        mSocketServer.stop();
    }

    @Test
    public void pingServer() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        Thread serverThread = new Thread() {
            @Override
            public void run() {
                countDownLatch.countDown();
                mSocketServer.start();
            }
        };
        serverThread.start();

        Thread clientThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1);
                    LocalSocketClient client = new LocalSocketClient(SOCKET_NAME);
                    byte[] request = "ping".getBytes();
                    System.out.println("request: " + "ping");
                    client.sendMessage(request);
                    byte[] response = client.readMessage();
                    Assert.assertEquals("pong", new String(response));
                    System.out.println("response: " + new String(response));
                    client.close();

                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            }
        };
        clientThread.start();

        countDownLatch.await();

    }

}