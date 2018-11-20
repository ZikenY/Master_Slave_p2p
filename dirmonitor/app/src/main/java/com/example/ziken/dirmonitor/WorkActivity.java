package com.example.ziken.dirmonitor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;


public class WorkActivity extends Activity {
    private String _dirsvr_host = "";
    private int _dirsvr_port = -1;

    private TextView txt1 = null;
    public Button btn1 = null;
    public Button btn2 = null;
    public ListView lvFileManifests = null;
    ArrayAdapter<String> _lvadapter = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    public void list_file_manifests() {
        new Thread(new DirClient.FileManifestsRequester()).start();
        while (!DirClient.FileManifestsRequester.is_ok()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> filemani = new ArrayList<String>();
        String ss = DirClient.FileManifestsRequester.get_manifests();
        String[] sa = ss.split("\n");
        if (sa.length > 2) {
            for (int i = 0; i < sa.length-2; i++) {
                String item = "filename: " + sa[i] + "\n";
                int servercount = Integer.valueOf(sa[i + 1]);
                for (int j = 0; j < servercount; j++) {
                    item = item + "                   - " + sa[i + 2 + j] + "\n";
                }
                filemani.add(item);
                i += 1 + servercount;
            }
        }

        // data for listview
        _lvadapter = new ArrayAdapter<String>(WorkActivity.this, android.R.layout.simple_list_item_1, filemani);
        lvFileManifests.setAdapter(_lvadapter);
        lvFileManifests.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemName = (String)_lvadapter.getItem(position);
                String[] sa = itemName.split("\n");
                String fs_host = sa[1].split("_")[0].split("-")[1];
                fs_host = fs_host.substring(1, fs_host.length());
                int fs_port = Integer.parseInt(sa[1].split("_")[1]);
                String filename = sa[0].substring(10, sa[0].length());
                new Thread(new FileTransfer.GetFile(view.getContext(), filename, fs_host, fs_port)).start();
                while (!FileTransfer.GetFile.is_ok()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (FileTransfer.GetFile.get_success() == 0) {
                    report_dirsvr(fs_host, fs_port, "fileserver transfer succeeded");
                    while (!DirClient.ReportDirSvr.is_ok()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    String ext = filename.substring(filename.length()-3, filename.length()).toLowerCase();
                    if (ext.equals("jpg") || ext.equals("png") || ext.equals("bmp") || ext.equals("gif")) {
                        // display it on other active
                        // 生成intent对想
                        Intent intent = new Intent();
                        // 通过intent传递参数 putExtra键值对
                        intent.putExtra("filename", filename);
                        // 从一个active传递到另一个active, which is WorkActivity
                        intent.setClass(WorkActivity.this, ShowJpg.class);
                        // 启动另一个Activity
                        WorkActivity.this.startActivity(intent);
                    } else if (ext.equals("txt") || ext.equals("ini") || ext.equals("lst")
                            || ext.equals("cpp") || ext.equals("1st") || ext.equals("cfg")
                            || ext.equals("xml") || ext.equals("log")) {
                        Intent intent = new Intent();
                        intent.putExtra("filename", filename);
                        intent.setClass(WorkActivity.this, ShowTxt.class);
                        WorkActivity.this.startActivity(intent);
                    } else {
                        // ?
                    }
                    Toast.makeText(view.getContext(), FileTransfer.GetFile.get_filesize() + " bytes of \""
                            + filename + "\" has been saved", Toast.LENGTH_LONG).show();
                } else if (FileTransfer.GetFile.get_success() == 1) {
                    Toast.makeText(view.getContext(), "IO error: \"" + filename + "\"", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(view.getContext(), "fileserver transfer failed: \"" + filename + "\"", Toast.LENGTH_LONG).show();
                    report_dirsvr(fs_host, fs_port, "fileserver transfer failed");
                    while (!DirClient.ReportDirSvr.is_ok()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public void report_dirsvr(String fs_host, int fs_port, String content) {
        new Thread(new DirClient.ReportDirSvr(fs_host + "_" + fs_port, content)).start();
    }

    public void list_fileserver() {
        new Thread(new DirClient.FileserverStatusRequester()).start();
        while (!DirClient.FileserverStatusRequester.is_ok()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> filemani = new ArrayList<String>();
        String[] sa = DirClient.FileserverStatusRequester.get_status().split("\n");
        if (sa.length > 3) {
            for (int i = 0; i < sa.length-3; i++) {
                String item = "fileserver: " + sa[i] + "\n";
                item = item + "  -current connection count: " + sa[i + 1] + "\n";
                item = item + "  -successful transfer count: " + sa[i + 2] + "\n";
                item = item + "  -failed transfer count: " + sa[i + 3] + "\n";
                filemani.add(item);
                i += 1 + 3;
            }
        }

        // data for listview
        _lvadapter = new ArrayAdapter<String>(WorkActivity.this, android.R.layout.simple_list_item_1, filemani);
        lvFileManifests.setAdapter(_lvadapter);
        lvFileManifests.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemName = (String) _lvadapter.getItem(position);
                String fs_host = itemName.split("\n")[0].split(":")[1].split("_")[0];
                fs_host = fs_host.substring(1, fs_host.length());
                int fs_port = Integer.parseInt(itemName.split("\n")[0].split(":")[1].split("_")[1]);

                new Thread(new FileTransfer.GetFileList(fs_host, fs_port)).start();
                while (!FileTransfer.GetFileList.is_ok()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // display it on other active
                // 生成intent对想
                Intent intent = new Intent();
                // 通过intent传递参数 putExtra键值对
                intent.putExtra("filelist", FileTransfer.GetFileList.get_filelist());
                // 从一个active传递到另一个active, which is WorkActivity
                intent.setClass(WorkActivity.this, FileList.class);
                // 启动另一个Activity
                WorkActivity.this.startActivity(intent);

                Toast.makeText(view.getContext(), "file(s) shared on " + fs_host + ":" + fs_port, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 窗体布局
        setContentView(R.layout.content_work);

        // 取得传过来的Intent兑现个
        Intent intent = getIntent();
        _dirsvr_host = intent.getStringExtra("dirsvr_host");
        String gg = intent.getStringExtra("dirsvr_port");
//        _dirsvr_port = Integer.parseInt(intent.getStringExtra("dirsvr_port"));

        btn1 = (Button) findViewById(R.id.btnFileMani);
        btn2 = (Button) findViewById(R.id.btnSvrStatus);
        btn1.setOnClickListener(new Button1Listener());
        btn2.setOnClickListener(new Button2Listener());
        txt1 = (TextView) findViewById(R.id.showtext);
        lvFileManifests = (ListView) findViewById(R.id.lvFileManifests);

        btn1.setText("show file manifest");
        btn2.setText("show fileserver running status");

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    class Button1Listener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            list_file_manifests();
        }
    }

    class Button2Listener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            list_fileserver();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Work Page", // TODO: Define a title for the content shown.
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
                "Work Page", // TODO: Define a title for the content shown.
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
