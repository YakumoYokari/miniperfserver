package com.genymobile.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public final class SurfaceControl {

    private static final Class<?> CLASS;

    // see <https://android.googlesource.com/platform/frameworks/base.git/+/pie-release-2/core/java/android/view/SurfaceControl.java#305>
    public static final int POWER_MODE_OFF = 0;
    public static final int POWER_MODE_NORMAL = 2;

    static {
        try {
            CLASS = Class.forName("android.view.SurfaceControl");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static Method getBuiltInDisplayMethod;
    private static Method setDisplayPowerModeMethod;
    private static Method screenshotMethod;

    private SurfaceControl() {
        // only static methods
    }

    public static void openTransaction() {
        try {
            CLASS.getMethod("openTransaction").invoke(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void closeTransaction() {
        try {
            CLASS.getMethod("closeTransaction").invoke(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplayProjection(IBinder displayToken, int orientation, Rect layerStackRect, Rect displayRect) {
        try {
            CLASS.getMethod("setDisplayProjection", IBinder.class, int.class, Rect.class, Rect.class)
                    .invoke(null, displayToken, orientation, layerStackRect, displayRect);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) {
        try {
            CLASS.getMethod("setDisplayLayerStack", IBinder.class, int.class).invoke(null, displayToken, layerStack);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) {
        try {
            CLASS.getMethod("setDisplaySurface", IBinder.class, Surface.class).invoke(null, displayToken, surface);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static IBinder createDisplay(String name, boolean secure) {
        try {
            return (IBinder) CLASS.getMethod("createDisplay", String.class, boolean.class).invoke(null, name, secure);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Method getGetBuiltInDisplayMethod() throws NoSuchMethodException {
        if (getBuiltInDisplayMethod == null) {
            // the method signature has changed in Android Q
            // <https://github.com/Genymobile/scrcpy/issues/586>
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                getBuiltInDisplayMethod = CLASS.getMethod("getBuiltInDisplay", int.class);
            } else {
                getBuiltInDisplayMethod = CLASS.getMethod("getInternalDisplayToken");
            }
        }
        return getBuiltInDisplayMethod;
    }

    public static IBinder getBuiltInDisplay() {

        try {
            Method method = getGetBuiltInDisplayMethod();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // call getBuiltInDisplay(0)
                return (IBinder) method.invoke(null, 0);
            }

            // call getInternalDisplayToken()
            return (IBinder) method.invoke(null);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Method getSetDisplayPowerModeMethod() throws NoSuchMethodException {
        if (setDisplayPowerModeMethod == null) {
            setDisplayPowerModeMethod = CLASS.getMethod("setDisplayPowerMode", IBinder.class, int.class);
        }
        return setDisplayPowerModeMethod;
    }

    public static boolean setDisplayPowerMode(IBinder displayToken, int mode) {
        try {
            Method method = getSetDisplayPowerModeMethod();
            method.invoke(null, displayToken, mode);
            return true;
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void destroyDisplay(IBinder displayToken) {
        try {
            CLASS.getMethod("destroyDisplay", IBinder.class).invoke(null, displayToken);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }


    /**
     *
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/SurfaceControl.java;bpv=1;bpt=1
     *
     * 11.0 R /10.0 Q
     * public static Bitmap screenshot(Rect sourceCrop, int width, int height, boolean useIdentityTransform, int rotation)
     * public static Bitmap screenshot(Rect sourceCrop, int width, int height, int rotation)
     *
     * 9.0 P
     * public static Bitmap screenshot(Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform, int rotation)
     * public static Bitmap screenshot(Rect sourceCrop, int width, int height, int rotation)
     *
     * 8.0 O/7.0 N
     * public static Bitmap screenshot(Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform, int rotation)
     * public static Bitmap screenshot(int width, int height)
     *
     * 6.0 M
     * public static Bitmap screenshot(Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform, int rotation)
     * public static Bitmap screenshot(int width, int height)
     *
     * 5.1 L
     * public static Bitmap screenshot(Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform, int rotation)
     * public static Bitmap screenshot(int width, int height)
     *
     * 4.4
     * public static Bitmap screenshot(int width, int height, int minLayer, int maxLayer)
     * public static Bitmap screenshot(int width, int height)
     *
     * @return
     * @throws NoSuchMethodException
     */
    private static Method getScreenshotMethod() throws NoSuchMethodException {
        if (screenshotMethod == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                screenshotMethod = CLASS.getMethod("screenshot", int.class, int.class);
            } else {
                screenshotMethod = CLASS.getMethod("screenshot", Rect.class, int.class, int.class, int.class);
            }
        }
        return screenshotMethod;
    }

    public static Bitmap screenshot(int width, int height, int rotation) {
        try {
            Method method = getScreenshotMethod();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return (Bitmap) method.invoke(null, width, height);
            } else {
                return (Bitmap) method.invoke(null, new Rect(), width, height, rotation);
            }
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }
}
