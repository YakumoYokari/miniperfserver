package com.github.sandin.server;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LocalSocketClient {

    private String mSocketName;

    private LocalSocket mSocket;
    private DataInputStream mSocketInputStream;
    private DataOutputStream mSocketOutputStream;

    public LocalSocketClient(String socketName) throws IOException {
        mSocketName = socketName;

        mSocket = new LocalSocket();
        mSocket.connect(new LocalSocketAddress(mSocketName));
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
