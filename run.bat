adb forward tcp:6100 localabstract:miniperfserver
adb push app\build\intermediates\dex\debug\mergeDexDebug\classes.dex /data/local/tmp/MiniPerfServer.dex
adb shell mkdir /data/local/tmp/dalvik-cache
adb shell ANDROID_DATA=/data/local/tmp app_process -Djava.class.path=/data/local/tmp/MiniPerfServer.dex /data/local/tmp com.github.sandin.miniperfserver.MiniPerfServer
