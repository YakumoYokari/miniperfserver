package com.github.sandin.miniperf.app;

import android.app.Activity;
import android.os.Bundle;

import com.github.sandin.miniperf.server.MiniPerfServer;
import com.github.sandin.miniperf.server.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread() {
            @Override
            public void run() {
                String[] args = new String[]{"--app"};
                MiniPerfServer.main(args);
            }
        }.start();
    }
}