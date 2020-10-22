package com.github.sandin.miniperf.server.bean;

public class JankInfo {
    private int Jank;
    private int bigJank;

    public int getJank() {
        return Jank;
    }

    public void setJank(int jank) {
        Jank = jank;
    }

    public int getBigJank() {
        return bigJank;
    }

    public void setBigJank(int bigJank) {
        this.bigJank = bigJank;
    }

    @Override
    public String toString() {
        return "JankInfo{" +
                "Jank=" + Jank +
                ", bigJank=" + bigJank +
                '}';
    }
}
