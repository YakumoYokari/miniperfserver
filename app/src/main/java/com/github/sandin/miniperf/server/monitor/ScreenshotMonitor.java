package com.github.sandin.miniperf.server.monitor;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.genymobile.scrcpy.DisplayInfo;
import com.genymobile.scrcpy.Size;
import com.genymobile.scrcpy.wrappers.DisplayManager;
import com.genymobile.scrcpy.wrappers.ServiceManager;
import com.genymobile.scrcpy.wrappers.SurfaceControl;
import com.github.sandin.miniperf.server.bean.TargetApp;
import com.github.sandin.miniperf.server.proto.ProfileNtf;
import com.github.sandin.miniperf.server.proto.ProfileReq;
import com.github.sandin.miniperf.server.proto.Screenshot;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Screenshot Monitor
 */
public class
ScreenshotMonitor implements IMonitor<Screenshot> {
    private static final String TAG = "ScreenshotMonitor";

    int rotation =0;

    @Override
    public Screenshot collect(TargetApp targetApp, long timestamp, ProfileNtf.Builder data) throws Exception {
        Screenshot.Builder builder = Screenshot.newBuilder();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        takeScreenshot(byteArrayOutputStream);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (rotation == 2) {
                rotation = 3;
            } else if (rotation == 3) {
                rotation = 2;
            }
        }else
        {
            rotation = 0;
        }
        Screenshot screenshot = builder.setData(ByteString.copyFrom(byteArrayOutputStream.toByteArray())).setOrientationValue(rotation).build();
        if (data != null)
            data.setScreenshot(screenshot);
        return screenshot;
    }

    @Override
    public void setInterestingFields(Map<ProfileReq.DataType, Boolean> dataTypes) {
        // pass
    }

    public void takeScreenshot(OutputStream outputStream) throws Exception {
        DisplayManager displayMgr = new ServiceManager().getDisplayManager();
        DisplayInfo displayInfo = displayMgr.getDisplayInfo(displayMgr.getDisplayIds()[0]);
        Log.i(TAG, "displayInfo: " + displayInfo);

        Size size = displayInfo.getSize();
        int width = size.getWidth() / 3;
        int height = size.getHeight() / 3;
        rotation = displayInfo.getRotation();
        Log.i(TAG, "screen width: " + width + ", height: " + height + ", rotation:" + rotation);

        long start = System.nanoTime();
        Bitmap bitmap = SurfaceControl.screenshot(width, height, rotation);
        Log.i(TAG, "screenshot cost time: " + (System.nanoTime() - start));
        start = System.nanoTime();

        //OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("/data/local/tmp/test.jpg"));
        //OutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        //outputStream.close();
        Log.i(TAG, "bitmap width: " + bitmap.getWidth() + ", height: " + bitmap.getHeight());
        Log.i(TAG, "screenshot to jpg file cost time: " + (System.nanoTime() - start));
    }
}
