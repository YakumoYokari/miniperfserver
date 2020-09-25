package com.github.sandin.miniperfserver;

import android.content.Context;
import android.os.Looper;

import com.genymobile.scrcpy.wrappers.ActivityThread;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertNotNull;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ActivityThreadTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void getSystemContext() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread() {
            @Override
            public void run() {
                //Looper.prepareMainLooper();
                Looper.prepare();
                assertNotNull(ActivityThread.systemMain());
                assertNotNull(ActivityThread.systemMain().getSystemContext());
                countDownLatch.countDown();
            }
        }.start();
        countDownLatch.await();
    }
}