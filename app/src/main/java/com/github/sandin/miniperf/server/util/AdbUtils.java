package com.github.sandin.miniperf.server.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AdbUtils {

    public static List<String> executeCommand(String command) throws IOException {
        List<String> result = new ArrayList<>();
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader bw = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = bw.readLine()) != null) {
            result.add(line);
        }
        return result;
    }
}
