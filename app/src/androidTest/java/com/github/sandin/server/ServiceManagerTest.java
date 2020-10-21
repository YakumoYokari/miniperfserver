package com.github.sandin.server;

import android.content.Context;

import com.genymobile.scrcpy.wrappers.ServiceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ServiceManagerTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void listServices() {
        String[] services = ServiceManager.listServices();
        assertNotNull(services);
        assertTrue(services.length > 0);
        for (String service : services) {
            System.out.println("service: " + service);
        }
    }

    @Test
    public void checkService() {
        assertNotNull(ServiceManager.checkService("batteryproperties"));
        assertNotNull(ServiceManager.checkService("netstats"));
        assertNotNull(ServiceManager.checkService("SurfaceFlinger"));
        assertNotNull(ServiceManager.checkService("activity"));
        assertNotNull(ServiceManager.checkService("meminfo"));
    }

    @Test
    public void getService() {
        assertNotNull(ServiceManager.getService("batteryproperties"));
        assertNotNull(ServiceManager.getService("netstats"));
        assertNotNull(ServiceManager.getService("SurfaceFlinger"));
        assertNotNull(ServiceManager.getService("activity"));
        assertNotNull(ServiceManager.getService("meminfo"));
    }
}