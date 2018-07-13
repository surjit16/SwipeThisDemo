package com.example.surjitsingh.swipethisdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import java.util.ArrayList;

public class ListViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        ListView gridView = findViewById(R.id.list_view);
        MyAdapter myAdapter = new MyAdapter(this, R.layout.item_list_one, getPersonList());
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
