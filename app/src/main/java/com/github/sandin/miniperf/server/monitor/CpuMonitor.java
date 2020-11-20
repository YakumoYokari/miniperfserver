package com.github.sandin.miniperf.server.monitor;

import android.util.Log;

import com.github.sandin.miniperf.server.bean.CpuInfo;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.CoreUsage;
import com.github.sandin.miniperf.server.proto.CpuFreq;
import com.github.sandin.miniperf.server.proto.CpuUsage;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.io.FileReader;

public class CpuMonitor implements IMonitor<CpuInfo> {

    private static final String TAG = "CpuMonitor";

    private CPUStat stat;

    private Map<ProfileReq.DataType, Boolean> mDataTypes = new HashMap<>();

    public CpuMonitor(int pid) {
        stat = new CPUStat(pid);
    }


    @Override
    public CpuInfo collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        CpuInfo cpuInfo = new CpuInfo();
        cpu_fetch_loop(targetApp.getPackageName(), targetApp.getPid());
        //PerfDog两种统计方式都有。CPU Usage默认为未规范化CPU利用率。建议使用规范化CPU利用率作为衡量性能指标。 与性能狗相同,后期可改  --https://bbs.perfdog.qq.com/detail-146.html
//        if (stat.allow_normalization) {
//            cpuInfo.setCpuUsage(CpuUsage.newBuilder().setAppUsage(stat.normalized_app_usage).setTotalUsage(stat.normalized_usage).build());
//            data.setCpuUsage(CpuUsage.newBuilder().setAppUsage(stat.normalized_app_usage).setTotalUsage(stat.normalized_usage));
//            Log.i(TAG, "CPUI app:"+stat.normalized_app_usage);
//            Log.i(TAG, "CPUI :"+stat.normalized_usage);
//        } else {

        cpuInfo.setCpuUsage(CpuUsage.newBuilder().setAppUsage(stat.app_usage).setTotalUsage(stat.usage).build());
        data.setCpuUsage(CpuUsage.newBuilder().setAppUsage(stat.app_usage).setTotalUsage(stat.usage));


        CoreUsage.Builder a = CoreUsage.newBuilder();
        CpuFreq.Builder b = CpuFreq.newBuilder();
        for (int i = 0; i < stat.cores; ++i) {
            a.addCoreUsage(stat.usage_per_cpu[i]);
            b.addCpuFreq((int) stat.current_freq[i]);
        }
        cpuInfo.setCoreUsage(a.build());
        data.setCoreUsage(a.build());
        cpuInfo.setCpuFreq(b.build());
        data.setCpuFreq(b.build());

        return cpuInfo;
    }


//    public void kill(String packagename) throws NameNotFoundException {
//        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        am.forceStopPackage(packagename);
//        return;
//    }

    private void cpu_fetch_loop(String packageName, int ppid) throws FileNotFoundException, InterruptedException {
//        Log.i(TAG, "pid"+ppid);
//        int pid = AndroidProcessUtils.getPid(packageName);
        int pid = ppid;
        Log.i(TAG, "cpu_fetch_loop: packageName" + packageName);
        //stat = new CPUStat(pid);
        //Thread.sleep(1000);
        //SimpleTimer st = new SimpleTimer();
        stat.update();
        stat.print();
        //st.Println("CPUSTAT");
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

        File cur_freq_file[];
				
        private static final String[] EMPTY_STRING_ARRAY = new String[0];

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
            int offline = 0;
            for (int i = 0; i < cores; ++i) {
                String Path="/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
                List<String> str = ReadSystemInfoUtils.readInfoFromSystemFile(Path);
                Log.i(TAG, "CPUStat123: "+"   "+i+"    "+str.toString());
                if(str.size()>0){
                    long tmp = Long.valueOf(str.get(0));
                    max_freq[i] = tmp;
                    Log.i(TAG, "CPUStat123433:"+ max_freq[i]);
                }else{
                    offline++;
                    //Log.i(TAG, "CPUStat1234:"+offline);
                }
            }
            Log.i(TAG, "CPUStat: 11111111");
            if (offline == cores)
                allow_normalization = false;

            normalized_usage_per_cpu = new float[cores];

            cur_freq_file = new File[cores];

            for (int i = 0; i < cores; ++i){
                String filename = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq";
                cur_freq_file[i] = new File(filename);
            }
						
            have_time_in_state = true; // guess
            have_current_freq = true; // guess

            System.out.println("[DEBUG] allow_normalization = " + allow_normalization);
        }

        private String[] apache_split(String str){
            if (str == null) {
                return null;
            }
            final int len = str.length();
            if (len == 0) {
                return EMPTY_STRING_ARRAY;
            }
            final List<String> list = new ArrayList<String>();
            int i = 0;
            int start = 0;
            boolean match = false;
            boolean lastMatch = false;
            while (i < len) {
                if (str.charAt(i) == ' ') {
                    if (match) {
                        list.add(str.substring(start, i));
                        match = false;
                        lastMatch = true;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
            if (match && lastMatch) {
                list.add(str.substring(start, i));
            }
            return list.toArray(EMPTY_STRING_ARRAY);
        }

        private String _read_file(String path){
            FileReader fr = null;

            try {
                StringBuilder sb = new StringBuilder();

                // Create an input stream
                fr = new FileReader(path);

                int data;
                // Read a character and append it to string builder one by one
                while ((data = fr.read()) != -1) {
                    sb.append((char) data);
                }
                // System.out.println(sb.toString());
                return sb.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    // Close the input stream
                    if (fr != null) fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private boolean _read_app(List<String> str){
            if (str.size()==0) return false;
            String line = str.get(0);
            Log.i(TAG, "_read_app: str: "+line);
            // System.out.println("[DEBUG]" + line);
            int pos = line.lastIndexOf(')');
            if (pos + 2 >= line.length()) return false;
            line = line.substring(pos + 2);
            String[] tokens = apache_split(line);
            current_app.utime = Long.parseLong(tokens[11]);
            current_app.stime = Long.parseLong(tokens[12]);
            // System.out.println("[DEBUG] current: utime=" + current_app.utime + "; stime=" + current_app.stime);
            return true;
        }


        private boolean _read_max_freq(int x){
            String filename = "/sys/devices/system/cpu/cpu" + x + "/cpufreq/cpuinfo_max_freq";
            File f = new File(filename);
            BufferedReader reader = null;
            try{
                reader =new BufferedReader(new FileReader(f));
                int tmp =Integer.parseInt(reader.readLine());
                max_freq[x] = tmp;
            }catch (FileNotFoundException e){
                return true;
            }
            catch (IOException e){
            } finally {
                if(reader != null){
                    try{
                        reader.close();
                    }catch (IOException ignore){
                    }
                }
            }
            // System.out.println("[DEBUG] max_freq[" + x +"] = " + current_freq[x] + "/" + max_freq[x] + " from " + filename);
            return true;
        }

        private int bytes2int(byte[] b, int n){
            int ret = 0;
            for(int i = 0; i < n && (b[i] >= '0' && b[i] <= '9'); ++ i){
                ret = ret * 10 + (b[i] - '0');
            }
            return ret;
        }

        private int _read_all_cur_freq() {
            int cores_read = 0;
            FileInputStream files[] = new FileInputStream[cores];
            for(int i=0; i<cores; ++i){
                try {
                    files[i] = new FileInputStream(cur_freq_file[i]);
                } catch (FileNotFoundException e) {
                    //e.printStackTrace();
                }
            }

            for(int i=0; i<cores; ++i){
                byte[] buf = new byte[20];
                current_freq[i] = 0;
                if (files[i] == null) continue;
                try {
                    int read = files[i].read(buf);
                    current_freq[i] = bytes2int(buf, read);
                    cores_read ++;
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }

            for (int i = 0; i < cores; ++i){
                if (files[i] != null){
                    try {
                        files[i].close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            }

            for (int i = 0; i < cores; ++i){
                if (current_freq[i] > 0 && max_freq[i] == 0){
                    _read_max_freq(i);
                }
            }

            return cores_read;
        }
        private boolean _read_current_freq(int x){
            String filename = "/sys/devices/system/cpu/cpu" + x + "/cpufreq/scaling_cur_freq";
            File f = new File(filename);
            //BufferedReader reader = null;
            FileInputStream ff = new FileInputStream(f);
            current_freq[x] = 0;
            try{
                reader =new BufferedReader(new FileReader(f));
                //Thread.sleep(20);
                int tmp =Integer.parseInt(reader.readLine());
                current_freq[x] = tmp;
            }  catch(FileNotFoundException e){
                // e.printStackTrace();
                current_freq[x] = 0;
                return true;
            }catch (IOException e)
            {
            } finally {
                if(reader != null){
                    try{
                        reader.close();
                    }catch (IOException ignore){

                    }
                }
            }
            if (current_freq[x] > 0 && max_freq[x] == 0){
                _read_max_freq(x);
            }
            System.out.println("[DEBUG] current_freq[" + x +"] = " + current_freq[x] + "/" + max_freq[x] + " from " + filename);
            return true;
        }

        private boolean _read_time_in_state(int x){
            String tis = "/sys/devices/system/cpu/cpu"+ x + "/cpufreq/stats/time_in_state";
            List<String> str = ReadSystemInfoUtils.readInfoFromSystemFile(tis);
            long sum = 0;
            long total_tic = 0;
            if(str.size()>0){
                for(String line:str){
                    String[] toks = apache_split(line);
                    if (toks.length < 2) break;
                    sum += Long.parseLong(toks[0]) * Long.parseLong(toks[1]);
                    total_tic += Long.parseLong(toks[1]);
                }
            } else{
                return false;
            }
            if (sum == 0 || total_tic == 0) return false;
            current_time_in_state[x].weighted_sum = sum;
            current_time_in_state[x].total_time = total_tic;
            // System.out.println("[DEBUG] total_tic[" + x + "] = " + total_tic);
            return true;
        }

        private boolean _read_cpu(List<String> str){
            if (str.size()==0) return false;
            String line = str.get(0);
            Log.i(TAG, "_read_cpu: str:"+line);
            String[] tokens = apache_split(line);
            // System.out.println(tokens[0]);
            if (tokens.length >= 8 && tokens[0].equals("cpu")){
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
            System.out.println("[DEBUG] cpu = " + line);
            return true;
        }


        private int _read_cpux(String str){
            String line = str;
            Log.i(TAG, "_read_cpux: str"+line);
            String[] tokens = apache_split(line);
            if (tokens.length < 8){
                return -1;
            }
            if (!tokens[0].startsWith("cpu")){
                return -1;
            }
            int x = Integer.parseInt(tokens[0].substring(3));

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

            // System.out.println("[DEBUG] total_tic[" + x + "] = " + current_per_cpu[x].total);
            // System.out.println("[DEBUG] cpu[" + x + "] = " + line);
            return x;
        }

        private boolean read() throws FileNotFoundException {
            if (have_current_freq){
                if (_read_all_cur_freq() == 0){
                    have_current_freq = false;
                }
            }
            
            String pid_stat = "/proc/" + pid + "/stat";
            List<String> str = ReadSystemInfoUtils.readInfoFromSystemFile(pid_stat);
            if (!_read_app(str)) return false;
            String stat = "/proc/stat";
            // String stat = _read_file("/proc/stat");
            // System.out.println(stat);
            List<String> str1 = ReadSystemInfoUtils.readInfoFromSystemFile(stat);
            
            try{
                if (! _read_cpu(str1)) return false;
                boolean[] processed = new boolean[cores];
                for (int i =1;i<str1.size();i++){
                    int x = _read_cpux(str1.get(i));
                    if(x<0) break;
                    processed[x] = true;
                }
                for (int x = 0; x < cores; ++x){
                    if (processed[x]) continue;
                    current_per_cpu[x].user = last_per_cpu[x].user;
                    current_per_cpu[x].nice = last_per_cpu[x].nice;
                    current_per_cpu[x].system = last_per_cpu[x].system;
                    current_per_cpu[x].idle = last_per_cpu[x].idle;
                    current_per_cpu[x].iowait = last_per_cpu[x].iowait;
                    current_per_cpu[x].irq = last_per_cpu[x].irq;
                    current_per_cpu[x].softirq = last_per_cpu[x].softirq;
                    current_per_cpu[x].total = last_per_cpu[x].total;
                    current_per_cpu[x].u1 = last_per_cpu[x].u1;
                    current_per_cpu[x].u2 = last_per_cpu[x].u2;
                }
                /*
                for (int i = 0; i < cores; ++i){
                    if (! _read_cpux(i, scn)) return false;
                }
                */
            } finally {
            }

            if (allow_normalization) {
                for(int i = 0; i < cores; ++i){
                    if (have_time_in_state){
                        if (!_read_time_in_state(i)){
                            have_time_in_state = false;
                            break;
                        }
                    }
                }
            }



            return true;
        }

        public void update(){
            try {
                if (!read()){
                    System.out.println("read error");
                    return;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            // 1. calculate usage
            if (current.idle >= last.idle &&  (current.idle - last.idle) / 8 < 0x7c){
              if (current.total - last.total == 0){
                  usage = 0.0f;
              }
              else {
                  usage = (current.u2 - last.u2) * 100.0f / (current.total - last.total);
              }
            }
            
            for (int i = 0; i < cores; ++i){
              if (current_per_cpu[i].idle >= last_per_cpu[i].idle &&  (current_per_cpu[i].idle - last_per_cpu[i].idle) / 8 < 0x7c){
                if (current_per_cpu[i].total - last_per_cpu[i].total == 0){
                    usage_per_cpu[i] = 0.0f;
                } else {
                    usage_per_cpu[i] = (current_per_cpu[i].u2 - last_per_cpu[i].u2) * 100.0f / (current_per_cpu[i].total - last_per_cpu[i].total);
                    // System.out.println("current " + i + " u2=" + (current_per_cpu[i].u2 - last_per_cpu[i].u2) + " t=" + (current_per_cpu[i].total - last_per_cpu[i].total));
                }
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
            
            if (current.idle >= last.idle &&  (current.idle - last.idle) / 8 < 0x7c){
              if (current.total - last.total == 0){
                  app_usage = 0.0f;
              } else {
                  app_usage = ((current_app.stime + current_app.utime) - (last_app.stime + last_app.utime)) * 100f / (current.total - last.total);
              }
            }
            if (allow_normalization) {
                float den = 0;
                float num = 0;
                float R0 = 0.0f;
                long maxf = 0;
                for (int i = 0; i < cores; ++i){
                    maxf = Math.max(maxf, max_freq[i]);
                }
                for (int i = 0; i < cores; ++i){
                    float Ri = 1;
                    if (have_time_in_state){
                        if (current_time_in_state[i].total_time - last_time_in_state[i].total_time == 0){
                            Ri = 0.0f;
                        } else {
                            Ri = (current_time_in_state[i].weighted_sum - last_time_in_state[i].weighted_sum) * 1f / ((current_time_in_state[i].total_time - last_time_in_state[i].total_time) * max_freq[i]);
                        }
                    } else if (have_current_freq){

                        if (max_freq[i] > 0){
                            Ri = current_freq[i] * 1f / max_freq[i];
                        } else {
                            Ri = 0;
                        }
                        R0 += Ri;
                    }
                    normalized_usage_per_cpu[i] = usage_per_cpu[i] * Ri;
                    den += (current_per_cpu[i].total - last_per_cpu[i].total) * maxf;
                    num += (current_per_cpu[i].u2 - last_per_cpu[i].u2) * current_freq[i];
                }
                R0 /= cores;
                System.out.println("[DEBUG] have_time_in_state=" + have_time_in_state + "; have_current_freq=" + have_current_freq + "; R0=" + R0);
                // System.out.println("[DEBUG] all_cpu_weighted_total=" + den + "; all_cpu_weighted_sum=" + num);
                if (den > 0){
                    normalized_usage = num / den * 100f;
                    normalized_app_usage = app_usage * normalized_usage / usage;
                }
                if (R0 > 0.001){
                    normalized_usage = usage * R0;
                    normalized_app_usage = app_usage * normalized_usage / usage;
                }
            }

            // 2. current <-> last
            {
                ProcStat t = last;
                last = current;
                current = t;
                for (int i = 0; i < cores; ++i){
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
            if (allow_normalization){
                TimeInState[] t;
                t = last_time_in_state;
                last_time_in_state = current_time_in_state;
                current_time_in_state = t;
            }
        }

        public void print(){
            //System.out.print("idle: " + last.idle + " ");
            //System.out.print("total: " + last.total + " ");
            System.out.print("[RAW] ");
            System.out.print("usage: " + Math.round(usage) + " ");
            System.out.print("app_usage: " + Math.round(app_usage) + " ");
            for (int i = 0; i < cores; ++i){
                System.out.print("usage[" + i + "]: " + Math.round(usage_per_cpu[i]) + " ");
            }
            System.out.println("");
            if (allow_normalization){
                System.out.print("[NORM] ");
                System.out.print("usage: " + Math.round(normalized_usage) + " ");
                System.out.print("app_usage: " + Math.round(normalized_app_usage) + " ");
                for (int i = 0; i < cores; ++i){
                    System.out.print("usage[" + i + "]: " + Math.round(normalized_usage_per_cpu[i]) + " ");
                }
                System.out.println("");
            }
            if (have_current_freq){
                System.out.print("[CLOCK] ");
                for (int i = 0; i < cores; ++i){
                    System.out.print("freq[" + i + "]: " + Math.round(current_freq[i]) + " ");
                }
                System.out.println("");
            }
        }
    }

    private boolean isDataTypeEnabled(ProfileReq.DataType dataType) {
        return mDataTypes.containsKey(dataType) && mDataTypes.get(dataType);
    }

    @Override
    public void setInterestingFields(Map<ProfileReq.DataType, Boolean> dataTypes) {
        mDataTypes.clear();
        mDataTypes.putAll(dataTypes);
    }
}

