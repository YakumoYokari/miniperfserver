package com.github.sandin.miniperf.server.session;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.monitor.PerformanceMonitor;
import com.github.sandin.miniperf.server.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.server.SocketServer;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * Profile Session
 */
public final class Session implements PerformanceMonitor.Callback {

    private int mSessionId;

    private SocketServer.ClientConnection mConnection;

    private PerformanceMonitor mMonitor;

    /**
     * Session
     *
     * @param sessionId session id
     * @param connection client connection
     * @param monitor profile monitor
     */
    public Session(int sessionId,
                   @NonNull SocketServer.ClientConnection connection,
                   @NonNull PerformanceMonitor monitor) {
        this.mSessionId = sessionId;
        this.mConnection = connection;
        this.mMonitor = monitor;
    }

    /**
     * Start a session
     *
     * @return success/fail
     */
    public boolean start(@NonNull TargetApp targetApp,
                         @NonNull List<ProfileReq.DataType> dataTypes) {
        mMonitor.registerCallback(this);
        return mMonitor.start(targetApp, dataTypes);
    }

    /**
     * Stop the session
     */
    public void stop() {
        mMonitor.unregisterCallback(this);
        mMonitor.stop();
    }

    /**
     * implement of Monitor's Callback, bridge between {@link SocketServer.ClientConnection} and {@link PerformanceMonitor}
     * receive new data from monitor, and send it to the client
     *
     * +---------+   data   +---------+   bytes  +--------+
     * | Monitor |  +-----> | Session |  +-----> | Client |
     * +---------+          +---------+          +--------+
     */
    @Override
    public void onUpdate(ProfileNtf data) {
        try {
            MiniPerfServerProtocol response = MiniPerfServerProtocol.newBuilder().setProfileNtf(data).build();
            mConnection.sendMessage(response.toByteArray());
        } catch (IOException e) {
            e.printStackTrace(); // TODO: socket closed
        }
    }

    public int getSessionId() {
        return mSessionId;
    }

    public void setSessionId(int sessionId) {
        this.mSessionId = sessionId;
    }

    public SocketServer.ClientConnection getConnection() {
        return mConnection;
    }

    public void setConnection(SocketServer.ClientConnection connection) {
        this.mConnection = connection;
    }

    public PerformanceMonitor getMonitor() {
        return mMonitor;
    }

    public void setMonitor(PerformanceMonitor monitor) {
        this.mMonitor = monitor;
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId=" + mSessionId +
                ", connection=" + mConnection +
                ", monitor=" + mMonitor +
                '}';
    }
}
