package com.github.sandin.miniperfserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketClient {

    private int mSocketPort;

    private Socket mSocket;
    private DataInputStream mSocketInputStream;
    private DataOutputStream mSocketOutputStream;

    public SocketClient(String host, int socketPort) throws IOException {
        mSocketPort = socketPort;

        mSocket = new Socket(host, socketPort);
        mSocketInputStream = new DataInputStream(mSocket.getInputStream());
        mSocketOutputStream = new DataOutputStream(mSocket.getOutputStream());
    }

    public void sendMessage(byte[] message) throws IOException {
        mSocketOutputStream.writeInt(message.length);
        mSocketOutputStream.write(message);
    }

    public byte[] readMessage() throws IOException {
        int length = mSocketInputStream.readInt();
        byte[] buffer = new byte[length];
        mSocketInputStream.readFully(buffer);
        return buffer;
    }

    public void close() throws IOException {
        if (mSocket != null) {
            mSocket.close();
        }
    }

}
