package com.github.sandin.miniperf.server.session;

import android.content.Context;

import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.monitor.PerformanceMonitor;
import com.github.sandin.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.server.SocketServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Session Manager
 */
public class SessionManager {

    private static volatile SessionManager sInstance;

    private AtomicInteger mSessionIdGenerator = new AtomicInteger();

    private final Object mSessionsLock = new Object();
    private List<Session> mSessions = new ArrayList<>();

    public static SessionManager getInstance() {
        synchronized (SessionManager.class) {
            if (sInstance == null) {
                synchronized (SessionManager.class) {
                    sInstance = new SessionManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Create a new session, bind connection and monitor
     *
     * @param connection client connection
     * @param monitor profile monitor
     * @return the new session
     */
    public @Nullable Session createSession(@NonNull SocketServer.ClientConnection connection,
                                           @NonNull PerformanceMonitor monitor,
                                           @NonNull TargetApp targetApp,
                                           @NonNull List<ProfileReq.DataType> dataTypes) {
        int sessionId = mSessionIdGenerator.getAndIncrement();
        Session session = new Session(sessionId, connection, monitor);
        boolean success = session.start(targetApp, dataTypes);
        if (success) {
            synchronized (mSessionsLock) {
                mSessions.add(session);
            }
            return session;
        }
        return null;
    }


    /**
     * Destroy a session
     *
     * @param session the session
     */
    public void destroySession(@NonNull Context context, @NonNull Session session) {
        session.stop();
        synchronized (mSessionsLock) {
            mSessions.remove(session);
        }
    }

    /**
     * Get session by id
     *
     * @param sessionId session id
     * @return the session or null
     */
    public @Nullable Session getSession(int sessionId) {
        synchronized (mSessionsLock) {
            for (Session session : mSessions) {
                if (sessionId == session.getSessionId()) {
                    return session;
                }
            }
        }
        return null;
    }

}
