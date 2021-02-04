# MiniPerf Server 

## TODO

1. android 11 监控网络信息
2. Cpu温度监控 更好的解决方案
3. 低性能手机存在连不上和无法获取应用列表的问题（多试几次可以解决）
4. 目前发现在极度卡顿的情况下会出现帧耗时个数与fps不匹配
## 技术实现
### FPS

1. 获取layerName：

   先执行`dumpsys SurfaceFlinger --list`

   laynerName分为三种情况：

   ```
   //packageName为应用包名
   // "SurfaceView - <packageName>/<activityName>#0"            
   // "SurfaceView - <packageName>/<activityName>"
   // "<packageName>/<activityName>"
   ```

   并根据android版本分为两种情况，并赋予不同的权重，如果多个同时存在，则按照权重选择

   ```
   //Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
   // "SurfaceView - <packageName>/<activityName>#0"            
   // "SurfaceView - <packageName>/<activityName>"
   // "<packageName>/<activityName>"
   "SurfaceView - " + packageName + "/" 100
   packageName + "/" 80
   packageName 60
   
   //Build.VERSION.SDK_INT < Build.VERSION_CODES.N
   // "SurfaceView"
   // "<packageName>/<activityName>"
   packageName + "/" 80
   packageName 60
   ```

2.通过`dumpsys SurfaceFlinger --latency layerName  `获取frametimes，数据经过筛选后保存下来，保存逻辑如下

   ```java
   /*
       https://cs.android.com/android/platform/superproject/+/master:tools/test/graphicsbenchmark/performance_tests/hostside/src/com/android/game/qualification/metric/GameQualificationFpsCollector.java;drc=master;l=181?q=GameQualificationFps&ss=android
       */
       private boolean sample(long readyTimeStamp, long presentTimeStamp) {
           if (presentTimeStamp == Long.MAX_VALUE || readyTimeStamp == Long.MAX_VALUE) {
               return false;
           } else if (presentTimeStamp < mLatestSeen) {
               return false;
           } else if (presentTimeStamp == mLatestSeen) {
               return true;
           } else {
               mElapsedTimes.add(presentTimeStamp);
               mLatestSeen = presentTimeStamp;
               return false;
           }
       }
   ```

   dumpsys所得的数据中，第三列为readyTimeStamp，第二列为presentTimeStamp

   

3.从第二次数据开始，每一次的frametime与上一次的相减即得到每一帧的帧耗时

4.计算jank，计算逻辑如下

```java
 private JankInfo checkJank(List<Long> frameTimes) {
        JankInfo jankInfo = new JankInfo();
        int jank = 0;
        int bigJank = 0;
        long first_3s_frame_time = -1;
        long first_2s_frame_time = -1;
        long first_1s_frame_time = -1;
        for (Long frameTime : frameTimes) {
            Double time = (double) frameTime;
            if (first_1s_frame_time != -1 && first_2s_frame_time != -1 && first_3s_frame_time != -1) {
                double average = (first_1s_frame_time + first_2s_frame_time + first_3s_frame_time) / 3.0 * 2.0 + 2.0;
                if ((average > 0) && (time > 8533.333333333333)) {
                    jank++;
                    if (time > 12700)
                        bigJank++;
                    first_1s_frame_time = -1;
                    first_2s_frame_time = -1;
                    first_3s_frame_time = -1;
                }
            } else {
                first_3s_frame_time = first_2s_frame_time;
                first_2s_frame_time = first_1s_frame_time;
                first_1s_frame_time = frameTime;
            }
        }
        jankInfo.setJank(jank);
        jankInfo.setBigJank(bigJank);
        return jankInfo;
    }
```





### CPU

皆为读取配置文件获取

### cpu total usage

```
/proc/stat
/sys/devices/system/cpu/cpu%d/cpufreq/stats/time_in_state
```

### cpu app usage

```
/proc/<pid>/stat
/sys/devices/system/cpu/cpu%d/cpufreq/stats/time_in_state
```

### cpu clock

```
/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_min_freq
/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq
/sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq
```

### cpu core usage

```
/proc/stat
/sys/devices/system/cpu/cpu%d/cpufreq/stats/time_in_state
```



### GPU

获取GPU频率及使用率目前仅支持部分高通手机

#### GPU Clock

读取

- `/sys/class/kgsl/kgsl-3d0/gpuclk(常用)`
- `/sys/kernel/gpu/gpu_clock`

单位为hz

#### GPU Usage

读取

- `/sys/class/kgsl/kgsl-3d0/gpubusy(常用)`
- `/sys/kernel/gpu/gpu_clock`

读取出的结果存在两个值，第一个值代表已使用，第二个值代表总共，所以(第一个值/第二个值)*100即为使用率(%)

### Memory

最开始时通过ActivityManager.getProcessMemoryInfo()来获取应用的内存信息，但是发现如果两次检测时间过近会导致无法正常获取内存数据，经排查后发现是存在一个时间间隔得设置所导致的（<https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ActivityManagerConstants.java;l=122?q=MEMORY_INFO_THROTTLE_TIME&ss=android%2Fplatform%2Fsuperproject>）

故修改为以下方案

#### Virtual memory && Swap

通过读取 `/proc/{pid}/status`，`VmSize`行即为virtual memory, `VmSwap`行即为swap内存，单位都为kb

#### Native Pss && Ps && Gfx && GL && Unknow

通过`dumpsys meminfo {pid} --local`来获取某个应用的内存详情，其中：

- `Gfx dev`  -> Gfx
- `GL mtrack`  -> GL
- `Unknown`  -> Unknow
- `TOTAL`  -> Pss
- `Native Heap`  -> Native Pss

单位都为kb

**注意：通过dumpsys meminfo获取到的数据中，有些手机会同时存在`TOTAL/TOTAL:`和`Native Heap/Native Heap:`，两者含义是相同的，但是存在部分oppo手机（目前仅发现）dump出的信息中仅有TOTAL和Native Heap，所以选择获取前者的数据 **

### Network

存在两种获取方式

计算方法为：得出当前应用总接收的字节数和总发送的字节数，与上一秒的求差

#### 解析 /proc/net/xt_qtaguid/stats系统文件

文件内每列的含义如下

```
 0 idx : 序号
 1 iface ： 代表流量类型（rmnet表示2G/3G, wlan表示Wifi流量,lo表示本地流量）
 2 acct_tag_hex ：线程标记（用于区分单个应用内不同模块/线程的流量）
 3 uid_tag_int ： 应用uid,据此判断是否是某应用统计的流量数据
 4 cnt_set ： 应用前后标志位：1：前台， 0：后台
 5 rx_btyes ： receive bytes 接受到的字节数
 6 rx_packets : 接收到的任务包数
 7 tx_bytes ： transmit bytes 发送的总字节数
 8 tx_packets ： 发送的总包数
 9  rx_tcp_types ： 接收到的tcp字节数
 10 rx_tcp_packets ： 接收到的tcp包数
 11 rx_udp_bytes ： 接收到的udp字节数
 12 rx_udp_packets ： 接收到的udp包数
 13 rx_other_bytes ： 接收到的其他类型字节数
 14 rx_other_packets ： 接收到的其他类型包数
 15 tx_tcp_bytes ： 发送的tcp字节数
 16 tx_tcp_packets ： 发送的tcp包数
 17 tx_udp_bytes ： 发送的udp字节数
 18 tx_udp_packets ： 发送的udp包数
 19 tx_other_bytes ： 发送的其他类型字节数
 20 tx_other_packets ： 发送的其他类型包数
```

通过uid来筛选需要获取的应用的流量信息，累加rx_bytes和tx_bytes即可得到当前总的接收和发送的字节数（需剔除本地回环流量，即iface列为lo）

#### 通过调用NetworkStatusManager中的api

```markdown
新实现遵循旧版 xt_qtaguid 模块实现，因此 TrafficController 和 NetworkStatsService 将使用旧版实现或新实现运行。如果应用使用公共 API，那么无论在后台使用 xt_qtaguid 还是 eBPF 工具，应该没有任何区别。

在 Android 9 版本中，xt_qtaguid 模块在所有设备上都处于开启状态，但直接读取 xt_qtaguid 模块 proc 文件的所有公共 API 都移到了 NetworkManagement 服务中。根据设备内核版本和初始 API 级别，NetworkManagement 服务能够知道 eBPF 工具是否处于开启状态，并选择正确的模块来获取每个应用的网络使用情况统计数据。sepolicy 会阻止 SDK 级别为 28 及以上的应用访问 xt_qtaguid proc 文件

<https://source.android.com/devices/tech/datausage/ebpf-traffic-monitor#legacy-xt_qtaguid-deprecation-process>
```

经过尝试后，直接使用NetworkManagement会存在刷新率过低得问题，TrafficStats在高版本android上会直接返回-1(UNSUPPORTED)。通过查看android10源码可知可以通过反射调用android.net.INetworkStatsService中的getUidStats(int uid,int type)来绕过判断，从而就不会返回-1

```
TYPE_RX_BYTES = 0
TYPE_RX_PACKETS = 1
TYPE_TX_BYTES = 2
TYPE_TX_PACKETS = 3
TYPE_TCP_RX_PACKETS = 4
TYPE_TCP_TX_PACKETS = 5
```

**但是，在android11中，将判断移至了getUidStats中，经过测试这个方法不再适用，后续需要另寻方案**

<https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/net/NetworkStatsService.java;l=1072?q=getUidStats&ss=android%2Fplatform%2Fsuperproject>

```java
//android 11
public long getUidStats(int uid, int type){
    final int callingUid = Binder.getCallingUid();
    if (callingUid != android.os.Process.SYSTEM_UID && callingUid != uid){
        return UNSUPPORTED;
    }
    return nativeGetUidStat(uid,type,checkBpfStatsEnable());
}
```



### Battery

android SDK_INT <21 时不支持获取电池信息

#### 电流

通过`BatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)`可以获取到原始的电流数据，对其进行绝对值处理后，此时存在两种情况：

1. 单位为mA，此时不需要再进行处理
2. 单位为μA，此时需要转换成mA

因手机厂商和android版本而异

#### 电压

通过dump battery获取,voltage行即为电压信息

获取到的数据单位即为mV，暂未发现需要进行处理的点



### ScreenShot

通过调用`display(android.hardware.display.IDisplayManager)`下的`getDisplayInfo(int displayId)`来获取屏幕尺寸及旋转状态，然后通过`SurfaceControl.screenshot(int width,int height,int rolation)`获取屏幕截图



### CPU Temperature

可以把常见数据源分成两类

```
//1
"/sys/class/thermal/thermal_zone7/temp"
"/sys/devices/virtual/thermal/thermal_zone7/temp"
//2
"/sys/devices/virtual/thermal/thermal_zone0/temp",
"/sys/class/thermal/thermal_zone0/temp",
"/sys/kernel/debug/tegra_thermal/temp_tj",
"/sys/devices/platform/s5p-tmu/curr_temp",
"/sys/devices/virtual/thermal/thermal_zone1/temp",//常用
"/sys/devices/system/cpu/cpufreq/cput_attributes/cur_temp",
"/sys/devices/virtual/hwmon/hwmon2/temp1_input",
"/sys/devices/platform/coretemp.0/temp2_input",
"/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
"/sys/class/thermal/thermal_zone1/temp",
"/sys/devices/platform/s5p-tmu/temperature",
"/sys/devices/w1 bus master/w1_master_attempts",
"/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
"/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
"/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
"/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
"/sys/devices/platform/tegra_tmon/temp1_input",
"/sys/class/hwmon/hwmon0/device/temp1_input",
"/sys/devices/virtual/thermal/thermal_zone1/temp",
"/sys/class/thermal/thermal_zone3/temp",
"/sys/class/thermal/thermal_zone4/temp",
"/sys/class/hwmon/hwmonX/temp1_input",
"/sys/devices/platform/s5p-tmu/curr_temp"
```

使用频率和准确率递减，读取文件出现的温度可能存在三种情况:

1. 大于等于100小于1000     			除10取绝对值
2. 大于等于1000小于10000            除100取绝对值
3. 大于10000                                    除1000取绝对值



