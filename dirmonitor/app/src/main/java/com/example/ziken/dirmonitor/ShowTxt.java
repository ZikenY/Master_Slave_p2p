package com.example.ziken.dirmonitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ShowTxt extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 窗体布局
        setContentView(R.layout.content_showtxt);
        // 取得传过来的Intent兑现个
        Intent intent = getIntent();
        String filename = intent.getStringExtra("filename");
        filename = getFilesDir() + "/" + filename;

        File dataFile = new File(filename);
        if (!dataFile.exists()) {
            return;
        }

        try {
            DataInputStream fileInputStream  = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
            byte[] data_buff = new byte[196608];
            int len = fileInputStream.read(data_buff);
            fileInputStream.close();
            String text = new String(data_buff, 0, len);

            TextView tv = (TextView)findViewById(R.id.tv_showtxt);
            tv.setMovementMethod(ScrollingMovementMethod.getInstance());
            tv.setText(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}