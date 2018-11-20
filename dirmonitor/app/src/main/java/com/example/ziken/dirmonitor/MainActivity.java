package com.example.ziken.dirmonitor;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private GoogleApiClient client;

    // save to file
    public void string2file(String string, String filename) throws IOException {
        try {
            FileOutputStream fout = openFileOutput(filename, MODE_PRIVATE);
            byte [] bytes = string.getBytes();
            fout.write(bytes);
            fout.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    // load from file
    public String file2string(String filename) throws IOException{
        String string = "";
        try {
            FileInputStream fin = openFileInput(filename);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);
//            res = EncodingUtils.getString(buffer, "UTF-8");
            string = new String(buffer, 0, length);
            fin.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return string;
    }

    // controls
    public EditText editHost = null;
    public EditText editPort = null;
    public EditText editTimeout = null;
    public Button btn1 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 布局文件
        setContentView(R.layout.activity_main);

        // retrieve controls
        btn1 = (Button)findViewById(R.id.button1);
        btn1.setText(R.string.label_connect);
        editHost = (EditText)findViewById(R.id.inputHost);
        editPort = (EditText)findViewById(R.id.inputPort);
        editTimeout = (EditText)findViewById(R.id.inputTimeout);
        editHost.setText("192.168.0.104");
        editPort.setText("8911");
        editTimeout.setText("2000");

        // load history
        try {
            String connecthistory = file2string("connecthistory.list");
            String[] stringarray = connecthistory.split("\n");
            if (stringarray.length >= 3) {
                editHost.setText(stringarray[0]);
                editPort.setText(stringarray[1]);
                editTimeout.setText(stringarray[2]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 设置button.click()事件句柄对想
        btn1.setOnClickListener(new Button1Listener());


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // 监听器listener，listen on buttonclick
    class Button1Listener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            // save connect history
            String connecthistory = editHost.getText().toString() +"\n";
            connecthistory += editPort.getText().toString() +"\n";
            connecthistory += editTimeout.getText().toString() +"\n";
            try {
                MainActivity.this.string2file(connecthistory, "connecthistory.list");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("unable to save connecting history to file");
            }

            new Thread(new DirClient.Starter(editHost.getText().toString(),
                    Integer.parseInt(editPort.getText().toString()),
                    Integer.parseInt(editTimeout.getText().toString()))).start();

            while (!DirClient.Starter.is_ok()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!DirClient._dirclient.isconnected()) {
                // 弹出非阻塞线程对话框
                new AlertDialog.Builder(v.getContext())
                        .setTitle("time to go home now")
                        .setMessage("connected to directory server failed")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // just fuck
                                    }
                                }
                        )
                        .show();
                return;
            }

            // 生成intent对想
            Intent intent = new Intent();

            // 通过intent传递参数 putExtra键值对
            intent.putExtra("dirsvr_host", editHost.getText());
            intent.putExtra("dirsvr_port", editPort.getText());

            // 从一个active传递到另一个active, which is WorkActivity
            intent.setClass(MainActivity.this, WorkActivity.class);

            // 启动另一个Activity
            MainActivity.this.startActivity(intent);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.ziken.dirmonitor/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.ziken.dirmonitor/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
