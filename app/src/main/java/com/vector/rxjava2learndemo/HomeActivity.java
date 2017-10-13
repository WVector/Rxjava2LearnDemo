package com.vector.rxjava2learndemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    public void simple(View view) {
        jumpTo(SimpleActivity.class);
    }


    public void netSimulate(View view) {
        jumpTo(NetSimulateActivity.class);
    }

    private void jumpTo(Class clazz) {
        startActivity(new Intent(HomeActivity.this, clazz));
    }

    public void connectableObservable(View view) {
        jumpTo(ConnectableObservableActivity.class);
    }

    public void time(View view) {
        jumpTo(TimeActivity.class);
    }
}
