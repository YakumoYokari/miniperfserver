package com.github.sandin.miniperfserver;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.genymobile.scrcpy.wrappers.ActivityThread;
import com.genymobile.scrcpy.wrappers.Process;
import com.github.sandin.miniperfserver.bean.TargetApp;
import com.github.sandin.miniperfserver.monitor.MemoryMonitor;
import com.github.sandin.miniperfserver.proto.GetMemoryUsageReq;
import com.github.sandin.miniperfserver.proto.GetMemoryUsageRsp;
import com.github.sandin.miniperfserver.proto.Memory;
import com.github.sandin.miniperfserver.proto.MiniPerfServerProtocol;
import com.github.sandin.miniperfserver.server.SocketServer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * MiniPerfServer
 */
public class MiniPerfServer implements SocketServer.Callback {
    private static final String TAG = "MiniPerfServer";

    /**
     * Unix socket name
     */
    private static final String UNIX_DOMAIN_SOCKET_NAME = "miniperfserver";

    @NonNull
    private final Context mContext;

    @Nullable
    private MemoryMonitor mMemoryMonitor;

    private MiniPerfServer(@NonNull Context context) {
        mContext = context;
    }

    public void run() {
        SocketServer server = new SocketServer(mContext, UNIX_DOMAIN_SOCKET_NAME, this);
        server.start();
    }

    @Override
    public byte[] onMessage(byte[] msg) {
        try {
            MiniPerfServerProtocol request = MiniPerfServerProtocol.parseFrom(msg);
            if (request.getGetMemoryUsageReq() != null) {
                return handleGetMemoryUsageReq(request.getGetMemoryUsageReq());
            } else {
                // TODO: other requests
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] handleGetMemoryUsageReq(GetMemoryUsageReq request) {
        if (mMemoryMonitor == null) {
            mMemoryMonitor = new MemoryMonitor(mContext);
        }
        try {
            Memory memory = mMemoryMonitor.collect(mContext, new TargetApp(null, request.getPid()), 0);
            return MiniPerfServerProtocol.newBuilder().setGetMemoryUsageRsp(GetMemoryUsageRsp.newBuilder().setMemory(memory)).build().toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
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
        Looper.prepareMainLooper();
        Process.setArgV0("MiniPerfServer");
        Context context = ActivityThread.systemMain().getSystemContext();
        if (context == null) {
            Log.e(TAG, "Can not get system context!");
            return;
        }

        MiniPerfServer app = new MiniPerfServer(context);
        app.run();
    }
}
