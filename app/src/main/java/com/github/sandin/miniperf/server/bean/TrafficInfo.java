package com.github.sandin.miniperf.server.bean;

public class TrafficInfo {
    private long upload;
    private long download;

    public TrafficInfo(long upload, long download) {
        this.upload = upload;
        this.download = download;
    }

    public long getUpload() {
        return upload;
    }

    public void setUpload(long upload) {
        this.upload = upload;
    }

    public long getDownload() {
        return download;
    }

    public void setDownload(long download) {
        this.download = download;
    }
}
