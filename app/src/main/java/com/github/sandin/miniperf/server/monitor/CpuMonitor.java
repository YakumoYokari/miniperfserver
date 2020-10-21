package com.github.sandin.miniperf.server.monitor;

import com.github.sandin.miniperf.server.bean.CpuInfo;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.server.proto.CpuUsage;
import com.github.sandin.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.util.AndroidProcessUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class CpuMonitor implements IMonitor<CpuInfo> {

    private CPUStat stat;

    @Override
    public CpuInfo collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        CpuInfo cpuInfo = new CpuInfo();
        cpu_fetch_loop(targetApp.getPackageName());
        if (stat.allow_normalization) {
            cpuInfo.setCpuUsage(CpuUsage.newBuilder().setAppUsage(stat.normalized_usage).setAppUsage(stat.normalized_app_usage).build());
            data.setCpuUsage(CpuUsage.newBuilder().setAppUsage(stat.normalized_usage).setAppUsage(stat.normalized_app_usage));
        } else {
            cpuInfo.setCpuUsage(CpuUsage.newBuilder().setAppUsage(stat.usage).setAppUsage(stat.app_usage).build());
            data.setCpuUsage(CpuUsage.newBuilder().setAppUsage(stat.usage).setAppUsage(stat.app_usage));
        }
        return cpuInfo;
    }


//    public void kill(String packagename) throws NameNotFoundException {
//        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        am.forceStopPackage(packagename);
//        return;
//    }

    private void cpu_fetch_loop(String packageName) throws FileNotFoundException, InterruptedException {
        int pid = AndroidProcessUtils.getPid(packageName);

        stat = new CPUStat(pid);
        while (true) {

            Thread.sleep(1000);
            SimpleTimer st = new SimpleTimer();
            stat.update();
            stat.print();
            st.Println("CPUSTAT");
            /*
            File myObj = new File("/proc/stat");
            Scanner myReader = new Scanner(myObj);
            try{
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    System.out.println(data);
                }
            } finally {
                myReader.close();
            }
            */
        }
    }

    static class SimpleTimer {
        private long begin;

        public SimpleTimer() {
            begin = System.nanoTime();
        }

        public void Println(String tag) {
            long elapsedTime = System.nanoTime() - begin;
            System.out.println("[SIMPLETIMER] " + tag + " costs: " + (elapsedTime * 1e-9) + " s");
        }
    }

    static class ProcStat {
        public long user;
        public long nice;
        public long system;
        public long idle;
        public long iowait;
        public long irq;
        public long softirq;
        public long u1;
        public long u2;
        public long total;
    }

    static class TimeInState {
        long weighted_sum; // Sum(frequency * time)
        long total_time;
    }

    static class AppStat {
        public long utime;
        public long stime;
    }

    static class CPUStat {
        int pid;
        int cores;
        boolean allow_normalization;
        boolean have_time_in_state;
        boolean have_current_freq;

        long[] max_freq;
        ProcStat last;
        ProcStat current;
        ProcStat[] last_per_cpu;
        ProcStat[] current_per_cpu;
        AppStat last_app;
        AppStat current_app;
        TimeInState[] last_time_in_state;
        TimeInState[] current_time_in_state;
        long[] current_freq;

        float usage;
        float[] usage_per_cpu;
        float app_usage;

        float normalized_usage;
        float[] normalized_usage_per_cpu;
        float normalized_app_usage;

        public CPUStat(int pid) {

            this.pid = pid;
            cores = Runtime.getRuntime().availableProcessors();
            last = new ProcStat();
            current = new ProcStat();
            // System.out.println("[DEBUG] cores = " + cores);
            last_per_cpu = new ProcStat[cores];
            for (int i = 0; i < cores; ++i) {
                last_per_cpu[i] = new ProcStat();
            }
            current_per_cpu = new ProcStat[cores];
            for (int i = 0; i < cores; ++i) {
                current_per_cpu[i] = new ProcStat();
            }

            last_app = new AppStat();
            current_app = new AppStat();

            usage_per_cpu = new float[cores];

            last_time_in_state = new TimeInState[cores];
            current_time_in_state = new TimeInState[cores];
            for (int i = 0; i < cores; ++i) {
                last_time_in_state[i] = new TimeInState();
                current_time_in_state[i] = new TimeInState();
            }

            max_freq = new long[cores];
            current_freq = new long[cores];

            allow_normalization = true;
            for (int i = 0; i < cores; ++i) {
                File f = new File("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq");
                Scanner scn = null;
                try {
                    scn = new Scanner(f);
                    max_freq[i] = scn.nextLong();
                } catch (FileNotFoundException e) {
                    allow_normalization = false;
                } finally {
                    if (scn != null) {
                        scn.close();
                    }
                }
            }

            normalized_usage_per_cpu = new float[cores];

            have_time_in_state = true; // guess
            have_current_freq = true; // guess

            System.out.println("[DEBUG] allow_normalization = " + allow_normalization);
        }

        private boolean _read_app(Scanner scn) {
            if (!scn.hasNextLine()) return false;
            String line = scn.nextLine();
            // System.out.println("[DEBUG]" + line);
            int pos = line.lastIndexOf(')');
            if (pos + 2 >= line.length()) return false;
            line = line.substring(pos + 2);
            String[] tokens = line.split("\\s+");
            current_app.utime = Long.parseLong(tokens[11]);
            current_app.stime = Long.parseLong(tokens[12]);
            // System.out.println("[DEBUG] current: utime=" + current_app.utime + "; stime=" + current_app.stime);
            return true;
        }

        private boolean _read_current_freq(int x) {
            File f = new File("/sys/devices/system/cpu/cpu" + x + "/cpufreq/scaling_cur_freq");
            Scanner scn = null;
            try {
                scn = new Scanner(f);
                current_freq[x] = scn.nextLong();
            } catch (FileNotFoundException e) {
                return false;
            } finally {
                if (scn != null) {
                    scn.close();
                }
            }
            System.out.println("[DEBUG] current_freq[" + x + "] = " + current_freq[x] + "/" + max_freq[x]);
            return true;
        }

        private boolean _read_time_in_state(int x) {
            File tis = new File("/sys/devices/system/cpu/cpu" + x + "/cpufreq/stats/time_in_state");
            Scanner scn = null;
            long sum = 0;
            long total_tic = 0;
            try {
                scn = new Scanner(tis);
                while (scn.hasNextLine()) {
                    String line = scn.nextLine();
                    String[] toks = line.split("\\s+");
                    if (toks.length < 2) break;
                    sum += Long.parseLong(toks[0]) * Long.parseLong(toks[1]);
                    total_tic += Long.parseLong(toks[1]);
                }
            } catch (FileNotFoundException e) {
                return false;
            } finally {
                if (scn != null) scn.close();
            }
            if (sum == 0 || total_tic == 0) return false;
            current_time_in_state[x].weighted_sum = sum;
            current_time_in_state[x].total_time = total_tic;
            System.out.println("[DEBUG] total_tic[" + x + "] = " + total_tic);
            return true;
        }

        private boolean _read_cpu(Scanner scn) {
            if (!scn.hasNextLine()) return false;
            String line = scn.nextLine();
            String[] tokens = line.split("\\s+");
            // System.out.println(tokens[0]);
            if (tokens.length >= 8 && tokens[0].equals("cpu")) {
                current.user = Long.parseLong(tokens[1]);
                current.nice = Long.parseLong(tokens[2]);
                current.system = Long.parseLong(tokens[3]);
                current.idle = Long.parseLong(tokens[4]);
                current.iowait = Long.parseLong(tokens[5]);
                current.irq = Long.parseLong(tokens[6]);
                current.softirq = Long.parseLong(tokens[7]);
                current.total = current.user
                        + current.nice
                        + current.system
                        + current.idle
                        + current.iowait
                        + current.irq
                        + current.softirq;
                /*
                System.out.println("[DEBUG]" + current.user + "," + current.nice + "," + current.system
                    + "," + current.idle + "," + current.iowait + "," + current.irq + "," + current.softirq
                );
                */
                current.u1 = current.total - current.idle;

                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/os/ProcessCpuTracker.java;drc=3ceb4bd3edb81e5bd791a1c9835b9b0cc62c022d;l=903
                current.u2 = current.user + current.system + current.iowait + current.irq + current.softirq;
            }
            return true;
        }

        private boolean _read_cpux(int x, Scanner scn) {
            if (!scn.hasNextLine()) return false;
            String line = scn.nextLine();
            String[] tokens = line.split("\\s+");
            if (tokens.length >= 8 && tokens[0].equals("cpu" + x)) {
                current_per_cpu[x].user = Long.parseLong(tokens[1]);
                current_per_cpu[x].nice = Long.parseLong(tokens[2]);
                current_per_cpu[x].system = Long.parseLong(tokens[3]);
                current_per_cpu[x].idle = Long.parseLong(tokens[4]);
                current_per_cpu[x].iowait = Long.parseLong(tokens[5]);
                current_per_cpu[x].irq = Long.parseLong(tokens[6]);
                current_per_cpu[x].softirq = Long.parseLong(tokens[7]);
                current_per_cpu[x].total = current_per_cpu[x].user
                        + current_per_cpu[x].nice
                        + current_per_cpu[x].system
                        + current_per_cpu[x].idle
                        + current_per_cpu[x].iowait
                        + current_per_cpu[x].irq
                        + current_per_cpu[x].softirq;
                current_per_cpu[x].u1 = current_per_cpu[x].total - current_per_cpu[x].idle;
                current_per_cpu[x].u2 = current_per_cpu[x].user + current_per_cpu[x].system + current_per_cpu[x].iowait + current_per_cpu[x].irq + current_per_cpu[x].softirq;
            }
            // System.out.println("[DEBUG] total_tic[" + x + "] = " + current_per_cpu[x].total);
            return true;
        }

        private boolean read() throws FileNotFoundException {
            Scanner scn;

            File pid_stat = new File("/proc/" + pid + "/stat");
            scn = new Scanner(pid_stat);
            try {
                if (!_read_app(scn)) return false;
            } finally {
                scn.close();
            }

            File stat = new File("/proc/stat");
            scn = new Scanner(stat);
            try {
                if (!_read_cpu(scn)) return false;
                for (int i = 0; i < cores; ++i) {
                    if (!_read_cpux(i, scn)) return false;
                }
            } finally {
                scn.close();
            }

            if (allow_normalization) {
                for (int i = 0; i < cores; ++i) {
                    if (have_time_in_state) {
                        if (!_read_time_in_state(i)) {
                            have_time_in_state = false;
                            break;
                        }
                    }
                }
            }

            for (int i = 0; i < cores; ++i) {
                if (have_current_freq) {
                    if (!_read_current_freq(i)) {
                        have_current_freq = false;
                    }
                }
            }

            return true;
        }

        public void update() {
            try {
                if (!read()) {
                    System.out.println("read error");
                    return;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            // 1. calculate usage
            if (current.total - last.total == 0) {
                usage = 0.0f;
            } else {
                usage = (current.u2 - last.u2) * 100.0f / (current.total - last.total);
            }

            for (int i = 0; i < cores; ++i) {
                if (current_per_cpu[i].total - last_per_cpu[i].total == 0) {
                    usage_per_cpu[i] = 0.0f;
                } else {
                    usage_per_cpu[i] = (current_per_cpu[i].u2 - last_per_cpu[i].u2) * 100.0f / (current_per_cpu[i].total - last_per_cpu[i].total);
                }
            }

            // System.out.println("[DEBUG] current: utime=" + current_app.utime + "; stime=" + current_app.stime);
            // System.out.println("[DEBUG] last: utime=" + last_app.utime + "; stime=" + last_app.stime);

            /*
            usage = (current.u1 - last.u1) * 100.0f / (current.total - last.total);
            for (int i = 0; i < cores; ++i){
                usage_per_cpu[i] = (current_per_cpu[i].u1 - last_per_cpu[i].u1) * 100.0f / (current_per_cpu[i].total - last_per_cpu[i].total);
            }
            */
            if (current.total - last.total == 0) {
                app_usage = 0.0f;
            } else {
                app_usage = ((current_app.stime + current_app.utime) - (last_app.stime + last_app.utime)) * 100f / (current.total - last.total);
            }

            if (allow_normalization) {
                float den = 0;
                float num = 0;
                float R0 = 0.0f;
                long maxf = 0;
                for (int i = 0; i < cores; ++i) {
                    maxf = Math.max(maxf, max_freq[i]);
                }
                for (int i = 0; i < cores; ++i) {
                    float Ri = 1;
                    if (have_time_in_state) {
                        if (current_time_in_state[i].total_time - last_time_in_state[i].total_time == 0) {
                            Ri = 0.0f;
                        } else {
                            Ri = (current_time_in_state[i].weighted_sum - last_time_in_state[i].weighted_sum) * 1f / ((current_time_in_state[i].total_time - last_time_in_state[i].total_time) * max_freq[i]);
                        }
                    } else if (have_current_freq) {
                        Ri = current_freq[i] * 1f / max_freq[i];
                        R0 += Ri;
                    }
                    normalized_usage_per_cpu[i] = usage_per_cpu[i] * Ri;
                    den += (current_per_cpu[i].total - last_per_cpu[i].total) * maxf;
                    num += (current_per_cpu[i].u2 - last_per_cpu[i].u2) * current_freq[i];
                }
                R0 /= cores;
                System.out.println("[DEBUG] have_time_in_state=" + have_time_in_state + "; have_current_freq=" + have_current_freq + "; R0=" + R0);
                // System.out.println("[DEBUG] all_cpu_weighted_total=" + den + "; all_cpu_weighted_sum=" + num);
                if (den > 0) {
                    normalized_usage = num / den * 100f;
                    normalized_app_usage = app_usage * normalized_usage / usage;
                }
                if (R0 > 0.001) {
                    normalized_usage = usage * R0;
                    normalized_app_usage = app_usage * normalized_usage / usage;
                }
            }

            // 2. current <-> last
            {
                ProcStat t = last;
                last = current;
                current = t;
                for (int i = 0; i < cores; ++i) {
                    t = last_per_cpu[i];
                    last_per_cpu[i] = current_per_cpu[i];
                    current_per_cpu[i] = t;
                }
            }
            {
                AppStat t = last_app;
                last_app = current_app;
                current_app = t;
            }
            if (allow_normalization) {
                TimeInState[] t;
                t = last_time_in_state;
                last_time_in_state = current_time_in_state;
                current_time_in_state = t;
            }
        }

        public void print() {
            //System.out.print("idle: " + last.idle + " ");
            //System.out.print("total: " + last.total + " ");
            System.out.print("[RAW] ");
            System.out.print("usage: " + Math.round(usage) + " ");
            System.out.print("app_usage: " + Math.round(app_usage) + " ");
            for (int i = 0; i < cores; ++i) {
                System.out.print("usage[" + i + "]: " + Math.round(usage_per_cpu[i]) + " ");
            }
            System.out.println();
            if (allow_normalization) {
                System.out.print("[NORM] ");
                System.out.print("usage: " + Math.round(normalized_usage) + " ");
                System.out.print("app_usage: " + Math.round(normalized_app_usage) + " ");
                for (int i = 0; i < cores; ++i) {
                    System.out.print("usage[" + i + "]: " + Math.round(normalized_usage_per_cpu[i]) + " ");
                }
                System.out.println();
            }
        }
    }
}

