package com.github.sandin.miniperf.app;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import androidx.annotation.NonNull;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private volatile boolean running = false;

    private Thread mThread = null;

    private SurfaceView mSurfaceView;
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView.getHolder().addCallback(this);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TestActivity.class));
                /*
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
                 */
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

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setLooping(true);

            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            mMediaPlayer.setDataSource("http://10.11.130.47/video/test.mp4");
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
            //FIXME: mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }

    }
}