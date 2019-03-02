package com.github.pires.obd.reader;

import android.app.Application;
import android.content.Context;
import android.content.Intent;


/**
 * Created by Umer on 12/3/2017.
 */

public class ODBII_Reader extends Application {


    public static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();

        appContext=getApplicationContext();

        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException (Thread thread, Throwable e)
            {
                handleUncaughtException (thread, e);
            }
        });
    }


    public void handleUncaughtException (Thread thread, Throwable e)
    {
        e.printStackTrace(); // not all Android versions will print the stack trace automatically

        Intent intent = new Intent ();
        intent.setAction ("com.mydomain.SEND_LOG"); // see step 5.
        intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        startActivity (intent);

        System.exit(1); // kill off the crashed app
    }


}
