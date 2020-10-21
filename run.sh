adb forward tcp:45455 localabstract:miniperfserver > /dev/null 2>&1
adb push app/build/intermediates/dex/debug/mergeDexDebug/classes.dex /data/local/tmp/MiniPerfServer.dex > /dev/null 2>&1
adb shell mkdir /data/local/tmp/dalvik-cache > /dev/null 2>&1
adb shell ANDROID_DATA=/data/local/tmp app_process -Djava.class.path=/data/local/tmp/MiniPerfServer.dex /data/local/tmp com.github.sandin.miniperfserver.MiniPerfServer $@
