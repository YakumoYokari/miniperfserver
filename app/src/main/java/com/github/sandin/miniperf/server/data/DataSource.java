package com.github.sandin.miniperf.server.data;

public final class DataSource {
    //TODO经过对比 0 和 7 是最准的 但是无法发现与核有无关系
    public static final String[] CPU_TEMPERATURE_SYSTEM_FILE_PATHS = {
//            "/sys/kernel/debug/tegra_thermal/temp_tj",
//            "/sys/devices/platform/s5p-tmu/curr_temp",
//            "/sys/devices/virtual/thermal/thermal_zone1/temp",//常用
//            "/sys/devices/system/cpu/cpufreq/cput_attributes/cur_temp",
//            "/sys/devices/virtual/hwmon/hwmon2/temp1_input",
//            "/sys/devices/platform/coretemp.0/temp2_input",
//            "/sys/devices/virtual/thermal/thermal_zone0/temp",
//            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
            "/sys/class/thermal/thermal_zone7/temp",//目前看来最准确的
//            "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
//            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone7/temp",//目前看来最准确的
//            "/sys/devices/platform/s5p-tmu/temperature",
//            "/sys/devices/w1 bus master/w1_master_attempts",
//            "/sys/class/thermal/thermal_zone0/temp"
    };

    //TODO 暂未发现使用的温度传感器和cpu核有关系 此为备用数据源
    public static final String[] CPU_TEMPERATURE_SYSTEM_FILE_PATHS_SPARE = {
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone0/temp"
    };

    public static final String[] CURRENT_SYSTEM_FILE_PATHS = {
            "/sys/class/power_supply/battery/current_now",
//            "/sys/class/power_supply/battery/batt_current_now",
//            "/sys/class/power_supply/battery/batt_current"
    };

    public static final String[] VOLTAGE_SYSTEM_FILE_PATHS = {
            "/sys/class/power_supply/battery/voltage_now"
    };

    public static final String[] GPU_USAGE_SYSTEM_FILE_PATHS = {
            "/sys/class/kgsl/kgsl-3d0/gpubusy",//高通常见
            "/sys/kernel/gpu/gpu_busy"

    };

    public static final String[] GPU_CLOCK_SYSTEM_FILE_PATHS = {
            "/sys/class/kgsl/kgsl-3d0/gpuclk",//高通
            "/sys/kernel/gpu/gpu_clock"
    };

    public static final String NETWORK_SYSTEM_FILE_PATHS = "/proc/net/xt_qtaguid/stats";


}
