package com.github.sandin.miniperf.server.bean;

import com.github.sandin.miniperf.server.proto.FPS;
import com.github.sandin.miniperf.server.proto.FrameTime;

public class FpsInfo {
    private FPS fps;
    private FrameTime frameTime;

    public FPS getFps() {
        return fps;
    }

    public void setFps(FPS fps) {
        this.fps = fps;
    }

    public FrameTime getFrameTime() {
        return frameTime;
    }

    public void setFrameTime(FrameTime frameTime) {
        this.frameTime = frameTime;
    }

    @Override
    public String toString() {
        return "FpsInfo{" +
                "fps=" + fps +
                ", frameTime=" + frameTime +
                '}';
    }
}
