package com.example.surjitsingh.swipethisdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void listViewOne(View view) {
        startActivity(new Intent(this, ListViewActivity.class));
    }

    public void listViewTwo(View view) {
    }

    public void gridViewLeft(View view) {
        startActivity(new Intent(this, GridViewActivity.class));
    }

    public void gridViewRight(View view) {
    }
}
