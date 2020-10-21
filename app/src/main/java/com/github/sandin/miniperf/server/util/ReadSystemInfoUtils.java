package com.github.sandin.miniperf.server.util;

import android.os.IBinder;
import android.os.ParcelFileDescriptor;

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

    /**
     * read system file info
     * @param systemFilePaths
     * @return system file info
     */
    public static List<String> readInfoFromSystemFile(String[] systemFilePaths) {
        List<String> content = new LinkedList<>();
        for (String path : systemFilePaths) {
            File systemFile = new File(path);
            Scanner scanner = null;
            if (systemFile.exists()) {
                try {
                    scanner = new Scanner(systemFile);
                    if (scanner.hasNext()) {
                        content.add(scanner.nextLine());
                    }
                    if (content.size() > 0)
                        break;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    close(scanner);
                }
            }
        }
        return content;
    }

    /**
     * read system file info
     * @param systemFilePath
     * @return system file info
     */
    public static List<String> readInfoFromSystemFile(String systemFilePath) {
        return readInfoFromSystemFile(new String[]{systemFilePath});
    }

    public static List<String> readInfoFromDumpsys(String serviceName) {
        IBinder service = ServiceManager.getService(serviceName);
        List<String> content = new LinkedList<>();
        if (service != null) {
            ParcelFileDescriptor[] pipe = new ParcelFileDescriptor[0];
            BufferedReader reader = null;
            try {
                //first read, second write
                pipe = ParcelFileDescriptor.createPipe();
                service.dump(pipe[1].getFileDescriptor(), new String[0]);
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
