package com.github.sandin.miniperfserver.bean;

import java.io.Serializable;

public class TargetApp implements Serializable {

    private int pid;

    private String packageName;

    public TargetApp() {
    }

    public TargetApp(String packageName, int pid) {
        this.packageName = packageName;
        this.pid = pid;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return "TargetApp{" +
                "pid=" + pid +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}
