package com.example.ziken.dirmonitor;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class FileList extends ListActivity {
    String[] _items = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String fl = intent.getStringExtra("filelist");
        _items = fl.split("\n");
        setListAdapter(new ArrayAdapter<String>(this , android.R.layout.simple_list_item_1, _items));
    }

    public void onListItemClick (ListView parent,View view, int position, long id){
        Toast.makeText(view.getContext(), _items[position], Toast.LENGTH_LONG).show();
    }
}