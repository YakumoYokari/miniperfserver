package com.github.sandin.miniperf.server.util;

import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.genymobile.scrcpy.wrappers.ServiceManager;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class ReadSystemInfoUtils {

    private static final String TAG = "MiniPerfMonitor";

    /**
     * read system file info
     *
     * @param systemFilePaths
     * @return system file info
     */
    public static List<String> readInfoFromSystemFile(String[] systemFilePaths) {
        List<String> content = new LinkedList<>();
        for (String path : systemFilePaths) {
            Log.i(TAG, "now read file path is " + path);
            File systemFile = new File(path);
            Scanner scanner = null;
            Log.i(TAG, "is file exist : " + systemFile.exists());
            if (systemFile.exists()) {
                Log.i(TAG, "start read system file : " + path);
                try {
                    scanner = new Scanner(systemFile);
                    String line;
                    while ((line = scanner.nextLine()) != null) {
                        content.add(line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close(scanner);
                }
            }
            if (content.size() > 0)
                break;
        }
        return content;
    }

    /**
     * read system file info
     *
     * @param systemFilePath
     * @return system file info
     */
    public static List<String> readInfoFromSystemFile(String systemFilePath) {
        return readInfoFromSystemFile(new String[]{systemFilePath});
    }

    public static List<String> readInfoFromDumpsys(String serviceName, String[] args) {
        IBinder service = ServiceManager.getService(serviceName);
        List<String> content = new LinkedList<>();
        if (service != null) {
            ParcelFileDescriptor[] pipe = new ParcelFileDescriptor[0];
            BufferedReader reader = null;
            try {
                //first read, second write
                pipe = ParcelFileDescriptor.createPipe();
                service.dump(pipe[1].getFileDescriptor(), args);
                reader = new BufferedReader(new InputStreamReader(new ParcelFileDescriptor.AutoCloseInputStream(pipe[0])));
                while (reader.ready()) {
                    content.add(reader.readLine().trim());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close(reader, pipe[0], pipe[1]);
            }
        }
        return content;
    }

    private static void close(Closeable... needCloseObjects) {
        for (Closeable needCloseObject : needCloseObjects) {
            if (needCloseObject != null) {
                try {
                    needCloseObject.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
