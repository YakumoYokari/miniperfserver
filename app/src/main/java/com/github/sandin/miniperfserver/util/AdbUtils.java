package com.github.sandin.miniperfserver.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public final class AdbUtils {

    /**
     * execute command in android phone
     *
     * @param command
     * @return execute result
     * @throws IOException
     */
    public static List<String> executeCommand(String command) throws IOException {
        LinkedList<String> content = new LinkedList<>();
        Process process;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec(command);
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                content.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return content;
    }
}
