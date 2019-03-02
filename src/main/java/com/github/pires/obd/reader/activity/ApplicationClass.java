package com.github.pires.obd.reader.activity;

import android.app.Application;
import android.content.Context;


/**
 * Created by Umer on 12/3/2017.
 */

public class ApplicationClass extends Application{


    public static Context   appContext;

    @Override
    public void onCreate() {
        super.onCreate();

        appContext=getApplicationContext();
    }



}
