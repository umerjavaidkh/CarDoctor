package com.github.pires.obd.reader.mqtt_odbii;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;


public class AppConstants {


     public static final  int   ON_FOREGROUND=0;
     public static final  int   ON_PAUSED=1;
     public static final  int   ON_STOP=2;
     public static final  int   ON_DESTROYED=3;


     public  static    int   PORT=35000;

    public  static  final  String   DeviceIp="192.168.0.10";  //"192.168.100.2";

     public  static  final String App_Topic_Value="/thomasj/cardata";
     public  static  final String App_Subscribed_Value="/thomasj/statusdata";


    public static void  hideKeyboard(Context context)
    {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(((Activity)context).getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }


    public static void showKeyBoard(EditText editText, Context context)
    {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    }


    public static  boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }




}
