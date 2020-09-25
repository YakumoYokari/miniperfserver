package com.genymobile.scrcpy.wrappers;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/os/ServiceManager.java">ServiceManager</a>
 */
@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class ServiceManager {

    public static final String PACKAGE_NAME = "com.android.shell";
    public static final int USER_ID = 0;

    private static final Class<?> CLASS;

    private final Method getServiceMethod;

    private WindowManager windowManager;
    private DisplayManager displayManager;
    private InputManager inputManager;
    private PowerManager powerManager;
    private StatusBarManager statusBarManager;
    private ClipboardManager clipboardManager;
    private ActivityManager activityManager;

    static {
        try {
            CLASS = Class.forName("android.os.ServiceManager");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public ServiceManager() {
        try {
            getServiceMethod = CLASS.getDeclaredMethod("getService", String.class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private IInterface getService(String service, String type) {
        try {
            IBinder binder = (IBinder) getServiceMethod.invoke(null, service);
            Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
            return (IInterface) asInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static IBinder getService(String service) {
        try {
            return (IBinder) CLASS.getDeclaredMethod("getService", String.class).invoke(null, service);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static String[] listServices() {
        try {
            return (String[]) CLASS.getMethod("listServices").invoke(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static IBinder checkService(String name) {
        try {
            return (IBinder) CLASS.getMethod("checkService", String.class).invoke(null, name);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public WindowManager getWindowManager() {
        if (windowManager == null) {
            windowManager = new WindowManager(getService("window", "android.view.IWindowManager"));
        }
        return windowManager;
    }

    public DisplayManager getDisplayManager() {
        if (displayManager == null) {
            displayManager = new DisplayManager(getService("display", "android.hardware.display.IDisplayManager"));
        }
        return displayManager;
    }

    public InputManager getInputManager() {
        if (inputManager == null) {
            inputManager = new InputManager(getService("input", "android.hardware.input.IInputManager"));
        }
        return inputManager;
    }

    public PowerManager getPowerManager() {
        if (powerManager == null) {
            powerManager = new PowerManager(getService("power", "android.os.IPowerManager"));
        }
        return powerManager;
    }

    public StatusBarManager getStatusBarManager() {
        if (statusBarManager == null) {
            statusBarManager = new StatusBarManager(getService("statusbar", "com.android.internal.statusbar.IStatusBarService"));
        }
        return statusBarManager;
    }

    public ActivityManager getActivityManager() {
        if (activityManager == null) {
            try {
                // On old Android versions, the ActivityManager is not exposed via AIDL,
                // so use ActivityManagerNative.getDefault()
                Class<?> cls = Class.forName("android.app.ActivityManagerNative");
                Method getDefaultMethod = cls.getDeclaredMethod("getDefault");
                IInterface am = (IInterface) getDefaultMethod.invoke(null);
                activityManager = new ActivityManager(am);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        return activityManager;
    }
}
