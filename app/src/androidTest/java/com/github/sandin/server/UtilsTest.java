package com.github.sandin.server;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@RunWith(JUnit4.class)
public class UtilsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }


    @Test
    public void readFileTest() throws Exception {
        List<String> traffics = ReadSystemInfoUtils.readInfoFromSystemFile("/proc/net/xt_qtaguid/stats");
        for (String line : traffics) {
            System.out.println("test!!!" + line);
        }
    }
}
