package com.example.androidids;

import android.app.Application;
import android.content.Context;

import com.bun.miitmdid.core.JLibrary;

public class App extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        JLibrary.InitEntry(base);

    }
}
