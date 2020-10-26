package com.github.sandin.server;

import com.github.sandin.miniperf.server.data.DataSource;
import com.github.sandin.miniperf.server.util.ReadSystemInfoUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@RunWith(JUnit4.class)
public class UtilsTest {


    @Test
    public void readFileTest() throws Exception {
        List<String> traffics = ReadSystemInfoUtils.readInfoFromSystemFile("/proc/net/xt_qtaguid/stats");
        for (String line : traffics) {
            System.out.println("test!!!" + line);
        }
    }
}
