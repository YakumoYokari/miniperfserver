package com.github.sandin.miniperf.app;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.sandin.miniperf.server.proto.AppHelloRsp;
import com.github.sandin.miniperf.server.proto.AppInfo;
import com.github.sandin.miniperf.server.proto.GetAppInfoRsp;
import com.github.sandin.miniperf.server.proto.GetLMKThresholdRsp;
import com.github.sandin.miniperf.server.proto.GetScreenInfoRsp;
import com.github.sandin.miniperf.server.proto.MiniPerfAppProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class MiniPerfAppServer implements Runnable {

    private static final String TAG = "MiniPerfApp";
    private static final int PORT = 33333;
    private static final int BACKLOG = 128;

    private ServerSocket mServerSocket;
    private Context mContext;

    public MiniPerfAppServer(Context context) {
        this.mContext = context;
    }

    @Override
    public void run() {
        try {
            this.mServerSocket = new ServerSocket(PORT, BACKLOG);
            while (true) {
                new Thread(new Server(this.mContext, this.mServerSocket.accept()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Server implements Runnable {

        @NonNull
        private final DataInputStream mSocketInputStream;
        @NonNull
        private final DataOutputStream mSocketOutputStream;
        @NonNull
        private Context mContext;

        public Server(Context mContext, Socket socket) throws IOException {
            this.mContext = mContext;
            mSocketInputStream = new DataInputStream(socket.getInputStream());
            mSocketOutputStream = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            while (true) {
                try {
                    byte[] request = readMessage();
                    byte[] response = handleRequest(MiniPerfAppProtocol.parseFrom(request));
                    if (response != null)
                        sendMessage(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        private byte[] handleRequest(MiniPerfAppProtocol request) {
            switch (request.getProtocolCase()) {
                case APPHELLOREQ:
                    return AppHelloRsp.newBuilder().build().toByteArray();
                case GETSCREENINFOREQ:
                    GetScreenInfoRsp screenInfo = PhoneInfoManager.getScreenInfo(mContext);
                    return screenInfo.toByteArray();
                case GETLMKTHRESHOLDREQ:
                    int lmkThreshold = PhoneInfoManager.getLMKThreshold(mContext);
                    return GetLMKThresholdRsp.newBuilder()
                            .setMemoryThreshold(lmkThreshold)
                            .build()
                            .toByteArray();
                case GETAPPINFOREQ:
                    List<AppInfo> appList = PhoneInfoManager.getAppInfoList(mContext);
                    GetAppInfoRsp getAppInfoRsp = GetAppInfoRsp.newBuilder().addAllAppInfo(appList).build();
                    return getAppInfoRsp.toByteArray();
            }
            return null;
        }


        private void sendMessage(byte[] message) throws IOException {
            mSocketOutputStream.writeInt(message.length);
            mSocketOutputStream.write(message);
            mSocketOutputStream.flush();
        }

        private byte[] readMessage() throws IOException {
            int length = mSocketInputStream.readInt();
            byte[] buffer = new byte[length];
            mSocketInputStream.readFully(buffer);
            Log.v(TAG, "recv raw message, length=" + length);
            return buffer;
        }

    }
}
