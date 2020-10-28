package com.github.sandin.miniperf.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class MainActivity extends Activity {

    private volatile boolean running = false;

    private Thread mThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mThread == null) {
                    ((Button)findViewById(R.id.btn)).setText("Stop");
                    running = true;
                    mThread =  new Thread() {
                        @Override
                        public void run() {
                            //String[] args = new String[]{"--app"};
                            //MiniPerfServer.main(args);

                            while (running) {
                                for (int i = 0; i < 8; i++) {
                                    _read_current_freq2(i);
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    mThread.start();
                } else {
                    running = false;
                    try {
                        mThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ((Button)findViewById(R.id.btn)).setText("Start");
                    mThread = null;
                }
            }
        });
    }

    private boolean _read_current_freq2(int x) {
        File f = new File("/sys/devices/system/cpu/cpu" + x + "/cpufreq/scaling_cur_freq");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            System.out.println("current_freq: " + x + ": " + Integer.parseInt(reader.readLine()));
        } catch (IOException e) {
            System.out.println("current_freq: " + x + ": " + " offline");
            e.printStackTrace();
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {

                }
            }
        }
        return true;
    }

    private boolean _read_current_freq(int x) {
        File f = new File("/sys/devices/system/cpu/cpu" + x + "/cpufreq/scaling_cur_freq");
        Scanner scn = null;
        try {
            scn = new Scanner(f);
            System.out.println("current_freq: " + x + ": " + scn.nextLong());
        } catch (FileNotFoundException e) {
            System.out.println("current_freq: " + x + ": " + " offline");
            e.printStackTrace();
            return false;
        } finally {
            if (scn != null) {
                scn.close();
            }
        }
        return true;
    }
}