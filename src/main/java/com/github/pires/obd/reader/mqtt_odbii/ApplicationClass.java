package com.github.pires.obd.reader.mqtt_odbii;

import android.app.Application;
import android.content.Intent;

/**
 * Created by Umer on 2/22/2018.
 */

public class ApplicationClass extends Application {


    private  Thread.UncaughtExceptionHandler defaultHandler=null;
    private Thread thread;
    private Throwable ex;


    @Override
    public void onCreate() {
        super.onCreate();

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
