package com.example.surjitsingh.swipethisdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;

import java.util.ArrayList;

public class GridViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_view);

        GridView gridView = findViewById(R.id.grid_view);
        MyAdapter myAdapter = new MyAdapter(this, R.layout.item_grid_left, getPersonList());
        gridView.setAdapter(myAdapter);
    }

    public ArrayList<String> getPersonList() {
        ArrayList<String> mealList = new ArrayList<>();
        mealList.add("Surjit Singh");
        mealList.add("SSPJ");
        mealList.add("Birbal Singh");
        mealList.add("Varun Kohal");
        mealList.add("Vikash Kumar");
        mealList.add("Navdeep Kumar");
        mealList.add("Abhishek");
        mealList.add("Jatinder");
        mealList.add("Sagar");
        return mealList;
    }
}