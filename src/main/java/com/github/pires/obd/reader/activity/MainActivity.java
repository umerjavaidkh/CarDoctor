package com.github.pires.obd.reader.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.ads.ShowAd;
import com.github.pires.obd.reader.config.BtDeviceListActivity;
import com.github.pires.obd.reader.config.ObdConfig;
import com.github.pires.obd.reader.io.AbstractGatewayService;
import com.github.pires.obd.reader.io.LogCSVWriter;
import com.github.pires.obd.reader.io.ObdCommandJob;
import com.github.pires.obd.reader.io.ObdGatewayService;
import com.github.pires.obd.reader.io.ObdProgressListener;
import com.github.pires.obd.reader.mqtt_odbii.AppConstants;
import com.github.pires.obd.reader.mqtt_odbii.AppMqttService;
import com.github.pires.obd.reader.net.ObdReading;
import com.github.pires.obd.reader.net.ObdService;
import com.github.pires.obd.reader.trips.TripLog;
import com.github.pires.obd.reader.trips.TripRecord;
import com.google.inject.Inject;
import com.sevenheaven.iosswitch.ShSwitchView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

import static com.github.pires.obd.reader.activity.ConfigActivity.getGpsDistanceUpdatePeriod;
import static com.github.pires.obd.reader.activity.ConfigActivity.getGpsUpdatePeriod;

// Some code taken from https://github.com/barbeau/gpstest

@ContentView(R.layout.main)
public class MainActivity extends RoboActivity implements ObdProgressListener, LocationListener, GpsStatus.Listener , AppMqttService.UpdateLoginDetails,AppMqttService.UpdateGUI{

    private static final String TAG = MainActivity.class.getName();
    private static final int NO_BLUETOOTH_ID = 0;
    public static final int CONNECT_OVER_NETWORK = -1;
    public static final int CONNECT_OVER_BLUETOOTH = -2;
    private static final int BLUETOOTH_DISABLED = 1;
    private static final int START_LIVE_DATA = 2;
    private static final int STOP_LIVE_DATA = 3;
    private static final int SETTINGS = 4;
    private static final int DATA_OVERVIEW = 6;
    private static final int GET_DTC = 5;
    private static final int MQTT_BROKER_DETAILS = 55;
    private static final int ODBII_DETAILS = 66;
    public static final int TABLE_ROW_MARGIN = 7;
    private static final int NO_ORIENTATION_SENSOR = 8;
    private static final int NO_GPS_SUPPORT = 9;
    private static final int TRIPS_LIST = 10;
    private static final int SAVE_TRIP_NOT_AVAILABLE = 11;
    private static final int REQUEST_ENABLE_BT = 1234;
    private static boolean bluetoothDefaultIsEnable = false;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;


    static {
        RoboGuice.setUseAnnotationDatabases(false);
    }

    public Map<String, String> commandResult = new HashMap<String, String>();
    boolean mGpsIsStarted = false;
    private LocationManager mLocService;
    private LocationProvider mLocProvider;
    private LogCSVWriter myCSVWriter;
    private Location mLastLocation;

    public  static  int  currentConnectionMode=CONNECT_OVER_BLUETOOTH;

    private BluetoothAdapter mBluetoothAdapter;

    /// the trip log
    private TripLog triplog;
    private TripRecord currentTrip;

    @InjectView(R.id.compass_text)
    private TextView compass;

    @InjectView(R.id.currentConnectionOver)
    private TextView currentConnectionOver;


    @InjectView(R.id.bluetoothDeviceStatus)
    TextView bluetoothDeviceStatus;
    @InjectView(R.id.networkDeviceStatus)
    TextView networkDeviceStatus;
    //@InjectView(R.id.mqttDeviceStatus)
    //TextView mqttDeviceStatus;



    @InjectView(R.id.sendCount)
    TextView sendCount;


    @InjectView(R.id.receivedCount)
    TextView receivedCount;


    @InjectView(R.id.topicPublishedTV)
    TextView topicPublishedTV;


    @InjectView(R.id.topicSubscribeTV)
    TextView topicSubscribeTV;


    @InjectView(R.id.bluetoothScanButton)
    ShSwitchView bluetoothScanButton;
    @InjectView(R.id.networkScanButton)
    ShSwitchView networkScanButton;
    //@InjectView(R.id.connectToMqttServer)
    //ShSwitchView connectToMqttServer;


    private Context mContext;
    //private AppMqttService mAppMqttService;
    //private boolean isMQTTConnected =false;
    private boolean isdisConnectedFromServer =true;
    private  boolean isServerConnected =false;


    private  long   messageSendToBroker=0;
    private  long   messageReceivedFromBroker=0;

    Timer timer;


    public  static final    String    lastReceivedData_KEY_TAG="last_received_data";

    private    String    lastReceivedDataFromMQTT_Server="";



    private final SensorEventListener orientListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            String dir = "";
            if (x >= 337.5 || x < 22.5) {
                dir = "N";
            } else if (x >= 22.5 && x < 67.5) {
                dir = "NE";
            } else if (x >= 67.5 && x < 112.5) {
                dir = "E";
            } else if (x >= 112.5 && x < 157.5) {
                dir = "SE";
            } else if (x >= 157.5 && x < 202.5) {
                dir = "S";
            } else if (x >= 202.5 && x < 247.5) {
                dir = "SW";
            } else if (x >= 247.5 && x < 292.5) {
                dir = "W";
            } else if (x >= 292.5 && x < 337.5) {
                dir = "NW";
            }
            updateTextView(compass, dir);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };
    @InjectView(R.id.BT_STATUS)
    private TextView btStatusTextView;
    @InjectView(R.id.OBD_STATUS)
    private TextView obdStatusTextView;
    @InjectView(R.id.GPS_POS)
    private TextView gpsStatusTextView;
    @InjectView(R.id.vehicle_view)
    private LinearLayout vv;
    @InjectView(R.id.data_table)
    private TableLayout tl;

   // @InjectView(R.id.json_data_table)
    //private TableLayout json_tl;


    @Inject
    private SensorManager sensorManager;
    @Inject
    private PowerManager powerManager;
    @Inject
    private SharedPreferences prefs;



    private boolean isServiceBound;
    private AbstractGatewayService service;


    public TimerTask setupNewTask()
    {
          TimerTask mQueueCommands = new TimerTask() {
            public void run() {
                if (service != null && service.isRunning() && service.queueEmpty()) {
                 //if (service != null && service.isRunning() ) {
                    queueCommands();

                    double lat = 0;
                    double lon = 0;
                    double alt = 0;
                    final int posLen = 7;

                    if (mGpsIsStarted && mLastLocation != null) {
                        lat = mLastLocation.getLatitude();
                        lon = mLastLocation.getLongitude();
                        alt = mLastLocation.getAltitude();


                    }

                    if (prefs.getBoolean(ConfigActivity.UPLOAD_DATA_KEY, false)) {


                        // Upload the current reading by http
                        final String vin = prefs.getString(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                        Map<String, String> temp = new HashMap<String, String>();
                        temp.putAll   (commandResult);
                        ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp);


                        /*String data = reading.toJSON();

                        if(mAppMqttService!=null && isMQTTConnected)
                        {
                            String  publishTopicValue=AppConstants.App_Topic_Value;
                            publishTopicValue=MQTTConfigurationActivity.getServerPublishedTopic(prefs);
                            mAppMqttService.publishTopic(publishTopicValue,data);
                        }*/

                        //new UploadAsyncTask().execute(reading);

                    } else if (prefs.getBoolean(ConfigActivity.ENABLE_FULL_LOGGING_KEY, false)) {
                        // Write the current reading to CSV
                       /* final String vin = prefs.getString(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                        Map<String, String> temp = new HashMap<String, String>();
                        temp.putAll(commandResult);
                        ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp);
                        if(reading != null) myCSVWriter.writeLineCSV(reading);*/


                        final String vin = prefs.getString(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                        Map<String, String> temp = new HashMap<String, String>();
                        temp.putAll   (commandResult);
                        ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp);


                        String data = reading.toJSON();

                       /* if(mAppMqttService!=null && !isdisConnectedFromServer)
                        {
                            String  publishTopicValue=AppConstants.App_Topic_Value;
                            publishTopicValue=MQTTConfigurationActivity.getServerPublishedTopic(prefs);
                            mAppMqttService.publishTopic(publishTopicValue,data);
                            messageSendToBroker++;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    sendCount.setText("Message Send : "+messageSendToBroker);

                                }
                            });


                        }
                        else
                        {
                           // if(reading != null) myCSVWriter.writeLineCSV(reading);
                        }
*/

                    }
                    commandResult.clear();
                }
                // run again in period defined in preferences
                // new Handler().postDelayed(mQueueCommands, ConfigActivity.getObdUpdatePeriod(prefs));
            }
        };


          return mQueueCommands;
    }

    private Sensor orientSensor = null;
    private PowerManager.WakeLock wakeLock = null;
    private boolean preRequisites = true;
    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, className.toString() + " service is bound");
            isServiceBound = true;
            service = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            service.setContext(MainActivity.this);
            Log.d(TAG, "Starting live data");
            try {

                if(currentConnectionMode==CONNECT_OVER_BLUETOOTH)
                {
                    service.startService();
                }
                else
                {
                    service.startServiceNetwork();
                }

                if (preRequisites)
                    btStatusTextView.setText(getString(R.string.status_bluetooth_connected));
            } catch (IOException ioe) {
                Log.e(TAG, "Failure Starting live data");
                btStatusTextView.setText(getString(R.string.status_bluetooth_error_connecting));
                doUnbindService();
            }
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, className.toString() + " service is unbound");
            isServiceBound = false;
        }
    };

    public static String LookUpCommand(String txt) {
        for (AvailableCommandNames item : AvailableCommandNames.values()) {
            if (item.getValue().equals(txt)) return item.name();
        }
        return txt;
    }

    public void updateTextView(final TextView view, final String txt) {
        new Handler().post(new Runnable() {
            public void run() {
                view.setText(txt);
            }
        });
    }

    public void stateUpdate(final ObdCommandJob job) {
        final String cmdName = job.getCommand().getName();
        String cmdResult = "";
        final String cmdID = LookUpCommand(cmdName);

        if (job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
            cmdResult = job.getCommand().getResult();
            if (cmdResult != null && isServiceBound) {
                obdStatusTextView.setText(cmdResult.toLowerCase());
            }
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.BROKEN_PIPE)) {
            if (isServiceBound)
                stopLiveData();
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)) {
            cmdResult = getString(R.string.status_obd_no_support);
        } else {
            cmdResult = job.getCommand().getFormattedResult();
            if(isServiceBound) {
                obdStatusTextView.setText(getString(R.string.status_obd_data));
                changeStatusConnect();
            }

        }

        if (vv.findViewWithTag(cmdID) != null) {
            TextView existingTV = (TextView) vv.findViewWithTag(cmdID);
            existingTV.setText(cmdResult);
        } else addTableRow(cmdID, cmdName, cmdResult);
        commandResult.put(cmdID, cmdResult);
       // updateTripStatistic(job, cmdID);
    }

    private boolean gpsInit() {
        mLocService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mLocService != null) {
            mLocProvider = mLocService.getProvider(LocationManager.GPS_PROVIDER);
            if (mLocProvider != null) {
                mLocService.addGpsStatusListener(this);
                if (mLocService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    gpsStatusTextView.setText(getString(R.string.status_gps_ready));
                    return true;
                }
            }
        }
        gpsStatusTextView.setText(getString(R.string.status_gps_no_support));
        showDialog(NO_GPS_SUPPORT);
        Log.e(TAG, "Unable to get GPS PROVIDER");
        // todo disable gps controls into Preferences
        return false;
    }

    private void updateTripStatistic(final ObdCommandJob job, final String cmdID) {

        if (currentTrip != null) {
            if (cmdID.equals(AvailableCommandNames.SPEED.toString())) {
                SpeedCommand command = (SpeedCommand) job.getCommand();
                currentTrip.setSpeedMax(command.getMetricSpeed());
            } else if (cmdID.equals(AvailableCommandNames.ENGINE_RPM.toString())) {
                RPMCommand command = (RPMCommand) job.getCommand();
                currentTrip.setEngineRpmMax(command.getRPM());
            } else if (cmdID.endsWith(AvailableCommandNames.ENGINE_RUNTIME.toString())) {
                RuntimeCommand command = (RuntimeCommand) job.getCommand();
                currentTrip.setEngineRuntime(command.getFormattedResult());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext=this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            // Storage Permissions
            final int REQUEST_EXTERNAL_STORAGE = 1;
            final String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            // Workaround for FileUriExposedException in Android >= M
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }


        triplog = TripLog.getInstance(this.getApplicationContext());
        
        obdStatusTextView.setText(getString(R.string.status_obd_disconnected));



        bluetoothScanButton.setOnSwitchStateChangeListener(new ShSwitchView.OnSwitchStateChangeListener() {
            @Override
            public void onSwitchStateChange(boolean isOn) {


                   if (isOn && currentConnectionMode == CONNECT_OVER_NETWORK && service != null && service.isRunning()) {
                       stopLiveData();
                    }


                currentConnectionMode=CONNECT_OVER_BLUETOOTH;

                currentConnectionOver.setText("Bluetooth");

                if(bluetoothScanButton.isOn())
                {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if ( mBluetoothAdapter != null)
                    {

                        ShowAd.showAd();

                        preRequisites=bluetoothDefaultIsEnable = mBluetoothAdapter.isEnabled();

                        if (!bluetoothDefaultIsEnable)
                        {
                            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                        }
                        else
                        {

                            Intent serverIntent = new Intent(MainActivity.this, BtDeviceListActivity.class);
                            startActivityForResult(serverIntent, false
                                    ? REQUEST_CONNECT_DEVICE_SECURE
                                    : REQUEST_CONNECT_DEVICE_INSECURE );

                        }
                    }

                }
                else
                {
                    ShowAd.createAd();
                     stopLiveData();
                }

            }
        });


        networkScanButton.setOnSwitchStateChangeListener(new ShSwitchView.OnSwitchStateChangeListener() {
            @Override
            public void onSwitchStateChange(boolean isOn) {


                    if (isOn &&currentConnectionMode == CONNECT_OVER_BLUETOOTH && service != null && service.isRunning()) {

                        stopLiveData();
                    }

                currentConnectionMode=CONNECT_OVER_NETWORK;

                currentConnectionOver.setText("Network");

                if(networkScanButton.isOn())
                {
                    if(wifiState())
                    {
                        ShowAd.showAd();
                        startLiveData();


                    }
                    else
                    {
                        networkScanButton.setOn(false);
                        networkDeviceStatus.setText("Disconnected");
                        Toast.makeText(MainActivity.this, "Turn on WIFI", Toast.LENGTH_SHORT).show();
                    }
                   // connectNetworkDevice(" 192.168.100.2",35000);
                }
                else
                {
                    ShowAd.createAd();
                   stopLiveData();
                }
            }
        });




       /* connectToMqttServer.setOnSwitchStateChangeListener(new ShSwitchView.OnSwitchStateChangeListener() {
            @Override
            public void onSwitchStateChange(boolean isOn) {

                if(connectToMqttServer.isOn())
                {
                    if(wifiState())
                    {
                        if(isMQTTConnected && mAppMqttService!=null)
                        {
                            *//*if(prefs!=null && prefs.getBoolean(MQTTConfigurationActivity.ENABLE_CREDENTIALS,false))
                            {
                                showLoginDialog();
                            }
                            else*//*





                            mAppMqttService.login("","");
                        }
                    }
                    else
                    {
                        connectToMqttServer.setOn(false);
                        mqttDeviceStatus.setText("Disconnected");
                        Toast.makeText(MainActivity.this, "Turn on WIFI", Toast.LENGTH_SHORT).show();
                    }

                }
                else
                {
                    if(isMQTTConnected && mAppMqttService!=null) {
                        mAppMqttService.disconnect();
                        isdisConnectedFromServer = true;
                    }
                }

            }
        });
*/


        ShowAd.createAd();

        registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));





        lastReceivedDataFromMQTT_Server=prefs.getString(lastReceivedData_KEY_TAG,"");

        if(!lastReceivedDataFromMQTT_Server.isEmpty())
        {
          /*  LongOperation longOperation=new LongOperation(lastReceivedDataFromMQTT_Server);

            longOperation.execute();*/


            try {

                JSONObject  jsonObj=new JSONObject(lastReceivedDataFromMQTT_Server);


                String status=jsonObj.getString("status");
                String  value=  jsonObj.getString("probability");

                String time=jsonObj.getString("time");

                addJsonTableRow(time,time,""+status+" : "+ value+" % ");


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private class LongOperation extends AsyncTask<String, Void, String> {


        String jsonData;


        ProgressDialog  pd;


        HashMap<String,String> hashMap=new HashMap();

        public LongOperation(String jsonData)
        {
            this.jsonData=jsonData;
        }

        @Override
        protected String doInBackground(String... params) {

            try {

                try {

                    JSONObject  jsonObj=new JSONObject(lastReceivedDataFromMQTT_Server);


                    String status=jsonObj.getString("status");
                    String  value=  jsonObj.getString("probability");

                    String time=getCurrentTime();


                    hashMap.put("status",status);

                    hashMap.put("probability",value);

                    hashMap.put("time",time);

                } catch (JSONException e) {
                    e.printStackTrace();
                }



            }catch (Exception ex)
            {
                ex.toString();
            }


            return "Executed";
        }


        @Override
        protected void onPostExecute(String result) {


            pd.dismiss();



            addJsonTableRow("received_data","Last Received data from MQTT","");


            try {

                JSONObject  jsonObj=new JSONObject(jsonData);


                String status=jsonObj.getString("Status");
                String  value=  jsonObj.getString("probability");

                String time=getCurrentTime();

                addJsonTableRow(time,time," "+status+" : "+ value);


            } catch (JSONException e) {
                e.printStackTrace();
            }

        }


        @Override
        protected void onPreExecute() {


            pd= ProgressDialog.show(MainActivity.this,"","");

        }

    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==1)
        {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                       gpsInit();
                    } else {

                    }
                }
            }
        }

    }

    private   boolean wifiState()
    {
        WifiManager mng = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return mng.isWifiEnabled();
    }


    @Override
    protected void onStart() {
        super.onStart();
        /*Log.d(TAG, "Entered onStart...");

        boolean isrunning= AppConstants.isMyServiceRunning(AppMqttService.class,MainActivity.this);

        if(!isrunning) {

            try {
                Intent startServiceIntent = new Intent(mContext, AppMqttService.class);
                bindService(startServiceIntent, mServiceConnection, BIND_NOT_FOREGROUND);
                isServerConnected = false;
                startService(startServiceIntent);
            }catch (Exception ex)
            {
                ex.toString();
            }
        }
        else
        {


            Intent startServiceIntent = new Intent(mContext, AppMqttService.class);
            bindService(startServiceIntent, mServiceConnection, BIND_NOT_FOREGROUND);
            isServerConnected =true;
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();

       /* if(isMQTTConnected)
        {
            unbindService(mServiceConnection);
            isMQTTConnected=false;


        }*/
    }


    private void saveJsonData(String lastReceivedDataFromMQTT_Server)
    {
        SharedPreferences pref = prefs;
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(lastReceivedData_KEY_TAG, lastReceivedDataFromMQTT_Server);

        editor.commit(); // commit changes
    }


    private ServiceConnection mServiceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

           /* mAppMqttService=((AppMqttService.NewsBinder)service).getService();
            isMQTTConnected =true;

            mAppMqttService.setPreference(prefs);
            mAppMqttService.setUpdateGUILoginListener(MainActivity.this);*/

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

           /* isMQTTConnected =false;

            mAppMqttService.setUpdateGUILoginListener(null);
            mAppMqttService=null;*/

        }
    };



    @Override
    public void onUpdateLoginDetails(boolean isLoginSuccessful, String message) {


       /* if(isMQTTConnected && mAppMqttService!=null)
        {

            String  publishTopicValue=AppConstants.App_Topic_Value;
            publishTopicValue=MQTTConfigurationActivity.getServerPublishedTopic(prefs);

            String  subscribeTopicValue=AppConstants.App_Subscribed_Value;
            subscribeTopicValue=MQTTConfigurationActivity.getServerSubscribedTopic(prefs);

            mAppMqttService.publishTopic(publishTopicValue,"");
            mAppMqttService.subscribeToTopic(subscribeTopicValue);

            mqttDeviceStatus.setText("Connected");

            isdisConnectedFromServer=false;


            mAppMqttService.setUpdateGUIDataListener(MainActivity.this);



        }
        else
        {
            mqttDeviceStatus.setText("Disconnected");

            isdisConnectedFromServer=true;


        }*/


    }

    @Override
    public void connectionStatus(boolean status) {

        if(status)
        {
            //mqttDeviceStatus.setText("Connected");

            isdisConnectedFromServer=false;


        }
        else
        {
           // mqttDeviceStatus.setText("Disconnected");

            isdisConnectedFromServer=true;
        }

    }

    @Override
    public void onUpdateData(final String newsData, final String itemId) {


        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {





                messageReceivedFromBroker++;
                lastReceivedDataFromMQTT_Server=newsData;
                receivedCount.setText("Message Received : "+messageReceivedFromBroker);


                try {

                    JSONObject  jsonObj=new JSONObject(lastReceivedDataFromMQTT_Server);


                    String status=jsonObj.getString("Status");
                    String  value=  jsonObj.getString("probability");



                    String time=getCurrentTime();


                    jsonObj.put("time",time);

                    addJsonTableRow(time,time," "+status+" : "+ value);


                    lastReceivedDataFromMQTT_Server=jsonObj.toString();

                } catch (JSONException e) {
                    e.printStackTrace();

                    String time=getCurrentTime();



                    addJsonTableRow(time,time,lastReceivedDataFromMQTT_Server);

                }





            }
        });
*/

    }



    public String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss");
        String strDate = " " + mdformat.format(calendar.getTime());
       return  strDate;
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent startServiceIntent = new Intent(this, AppMqttService.class);
        stopService(startServiceIntent);

        if (mLocService != null) {
            mLocService.removeGpsStatusListener(this);
            mLocService.removeUpdates(this);
        }

        releaseWakeLockIfHeld();
        if (isServiceBound) {
            doUnbindService();
        }

        endTrip();

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null && btAdapter.isEnabled() && !bluetoothDefaultIsEnable)
            btAdapter.disable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Pausing..");
        releaseWakeLockIfHeld();


        if(!lastReceivedDataFromMQTT_Server.isEmpty())
        {
            saveJsonData(lastReceivedDataFromMQTT_Server);
        }
    }

    /**
     * If lock is held, release. Lock will be held when the service is running.
     */
    private void releaseWakeLockIfHeld() {
        if (wakeLock.isHeld())
            wakeLock.release();
    }

    protected void onResume() {
        super.onResume();


        Log.d(TAG, "Resuming..");
        sensorManager.registerListener(orientListener, orientSensor,
                SensorManager.SENSOR_DELAY_UI);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                "ObdReader");


        String  publishTopicValue=AppConstants.App_Topic_Value;
        publishTopicValue=MQTTConfigurationActivity.getServerPublishedTopic(prefs);

        String  subscribeTopicValue=AppConstants.App_Subscribed_Value;
        subscribeTopicValue=MQTTConfigurationActivity.getServerSubscribedTopic(prefs);

        topicPublishedTV.setText(publishTopicValue);
        topicSubscribeTV.setText(subscribeTopicValue);

       // testDelayValue.setText(""+ObdGatewayService.OBD_INIT_TIMEOUT_VAL);

    }

    private void updateConfig() {
        startActivity(new Intent(this, ConfigActivity.class));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
       // menu.add(0, START_LIVE_DATA, 0, getString(R.string.menu_start_live_data));
       // menu.add(0, STOP_LIVE_DATA, 0, getString(R.string.menu_stop_live_data));
       // menu.add(0, MQTT_BROKER_DETAILS, 0, "Configure Mqtt Broker");
        //menu.add(0, TRIPS_LIST, 0, getString(R.string.menu_trip_list));
        menu.add(0, SETTINGS, 0, "ODBII Device Setting");

       // menu.add(0, DATA_OVERVIEW, 0, "Overview Received Data");

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case START_LIVE_DATA:
                startLiveData();
                return true;
            case STOP_LIVE_DATA:
                stopLiveData();
                return true;
            case SETTINGS:
                updateConfig();
                return true;
                case DATA_OVERVIEW:
                    startActivity(new Intent(this, LastReceivedDataListActivity.class));
                return true;
            case GET_DTC:
                getTroubleCodes();
                return true;
                case MQTT_BROKER_DETAILS:
                    startActivity(new Intent(this, MQTTConfigurationActivity.class));
                return true;
            case TRIPS_LIST:
                startActivity(new Intent(this, LastReceivedDataListActivity.class));
                return true;
        }
        return false;
    }

    private void getTroubleCodes() {
        startActivity(new Intent(this, TroubleCodesActivity.class));
    }

    private void startLiveData() {
        Log.d(TAG, "Starting live data..");

        try {


           // ObdGatewayService.OBD_INIT_TIMEOUT_VAL = Integer.parseInt(testDelayValue.getText().toString());

            tl.removeAllViews(); //start fresh

            doBindService();

            currentTrip = triplog.startTrip();

            //if (currentTrip == null)
               // showDialog(SAVE_TRIP_NOT_AVAILABLE);


            if(timer!=null)
            {
                timer.cancel();
                timer.purge();
                timer=null;
            }

            timer = new Timer();
            timer.scheduleAtFixedRate(setupNewTask(), 0, ConfigActivity.getObdUpdatePeriod(prefs));

            if (prefs.getBoolean(ConfigActivity.ENABLE_GPS_KEY, false))
                gpsStart();
            else
                gpsStatusTextView.setText(getString(R.string.status_gps_not_used));



            wakeLock.acquire();

            if (prefs.getBoolean(ConfigActivity.ENABLE_FULL_LOGGING_KEY, false)) {


                long mils = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("_dd_MM_yyyy_HH_mm_ss");

                try {
                    myCSVWriter = new LogCSVWriter("Log" + sdf.format(new Date(mils)).toString() + ".csv",
                            prefs.getString(ConfigActivity.DIRECTORY_FULL_LOGGING_KEY,
                                    getString(R.string.default_dirname_full_logging))
                    );
                } catch (FileNotFoundException | RuntimeException e) {
                    Log.e(TAG, "Can't enable logging to file.", e);
                }
            }


        }catch (Exception ex)
        {
            ex.toString();
        }
    }

    private void stopLiveData( ) {

        Log.d(TAG, "Stopping live data..");

        try

        {

            if (timer != null) {
                timer.cancel();
                timer.purge();
                timer=null;
            }

            gpsStop();

            doUnbindService();
            endTrip();

            releaseWakeLockIfHeld();

            final String devemail = prefs.getString(ConfigActivity.DEV_EMAIL_KEY, null);
           /* if (devemail != null && !devemail.isEmpty()) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                ObdGatewayService.saveLogcatToFile(getApplicationContext(), devemail);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Where there issues?\nThen please send us the logs.\nSend Logs?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }*/

            if (myCSVWriter != null) {
                myCSVWriter.closeLogCSVWriter();
            }


        }catch (Exception ex)
        {
            ex.toString();
        }
    }

    protected void endTrip() {
        if (currentTrip != null) {
            currentTrip.setEndDate(new Date());
            triplog.updateRecord(currentTrip);
        }
    }

    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        switch (id) {
            case NO_BLUETOOTH_ID:
                build.setMessage(getString(R.string.text_no_bluetooth_id));
                return build.create();
            case BLUETOOTH_DISABLED:
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return build.create();
            case NO_ORIENTATION_SENSOR:
                build.setMessage(getString(R.string.text_no_orientation_sensor));
                return build.create();
            case NO_GPS_SUPPORT:
                build.setMessage(getString(R.string.text_no_gps_support));
                return build.create();
            case SAVE_TRIP_NOT_AVAILABLE:
                build.setMessage(getString(R.string.text_save_trip_not_available));
                return build.create();
        }
        return null;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        //MenuItem startItem = menu.findItem(START_LIVE_DATA);
        //MenuItem stopItem = menu.findItem(STOP_LIVE_DATA);
        MenuItem settingsItem = menu.findItem(SETTINGS);
       // MenuItem getMQTTtem = menu.findItem(MQTT_BROKER_DETAILS);

        if ((service != null && service.isRunning()) || (!isdisConnectedFromServer)) {
           // getMQTTtem.setEnabled(false);
            //startItem.setEnabled(false);
            //stopItem.setEnabled(true);
            settingsItem.setEnabled(false);
        } else {
         //   getMQTTtem.setEnabled(true);
            //stopItem.setEnabled(false);
            //startItem.setEnabled(true);
            settingsItem.setEnabled(true);
        }

        return true;
    }

    private void addTableRow(String id, String key, String val) {

        TableRow tr = new TableRow(this);
        MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN,
                TABLE_ROW_MARGIN);
        tr.setLayoutParams(params);

        TextView name = new TextView(this);
        name.setGravity(Gravity.RIGHT);
        name.setText(key + ": ");
        TextView value = new TextView(this);
        value.setGravity(Gravity.LEFT);
        value.setText(val);
        value.setTag(id);
        tr.addView(name);
        tr.addView(value);
        tl.addView(tr, params);
    }

    private void addJsonTableRow(String id, String key, String val) {

        TableRow tr = new TableRow(this);
        MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN,
                TABLE_ROW_MARGIN);
        tr.setLayoutParams(params);

        TextView name = new TextView(this);
        name.setGravity(Gravity.RIGHT);
        name.setText(key + ": ");
        TextView value = new TextView(this);
        value.setGravity(Gravity.LEFT);
        value.setText(val);
        value.setSingleLine(false);
        value.setMaxLines(50);
        value.setTag(id);
        tr.addView(name);
        tr.addView(value);
       // json_tl.addView(tr, params);
    }


    /**
     *
     */
    private void queueCommands() {
        if (isServiceBound) {
            for (ObdCommand Command : ObdConfig.getCommands()) {

                /*if(Command.getName().equals(AvailableCommandNames.AMBIENT_AIR_TEMP.getValue()))
                {
                    int x=0;
                    Log.i(TAG, "queueCommands: "+x);
                }*/
                if (prefs.getBoolean(Command.getName(), true))
                    service.queueJob(new ObdCommandJob(Command));
            }
        }
    }

    private void doBindService() {
        if (!isServiceBound) {


            if(currentConnectionMode==CONNECT_OVER_BLUETOOTH) {
                Log.d(TAG, "Binding OBD service..");
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {

                    btStatusTextView.setText(getString(R.string.status_bluetooth_connecting));
                    Intent serviceIntent = new Intent(this, ObdGatewayService.class);
                    bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
                } else {
               /* btStatusTextView.setText(getString(R.string.status_bluetooth_disabled));
                Intent serviceIntent = new Intent(this, MockObdGatewayService.class);
                bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);*/

                    Toast.makeText(mContext, "Turn On Bluetooth", Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                if (wifiState()) {

                    btStatusTextView.setText(getString(R.string.status_bluetooth_connecting));
                    Intent serviceIntent = new Intent(this, ObdGatewayService.class);
                    bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
                }
                else
                {
                    //connectToMqttServer.setOn(false);
                    //mqttDeviceStatus.setText("Disconnected");
                    Toast.makeText(MainActivity.this, "Turn on WIFI", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void doUnbindService() {
        if (isServiceBound) {
            if (service.isRunning()) {

                if(currentConnectionMode==CONNECT_OVER_BLUETOOTH)
                {
                    service.stopService();
                }
                else
                {
                    service.stopServiceNetwork();
                }


                if (preRequisites)
                    btStatusTextView.setText(getString(R.string.status_bluetooth_ok));
            }
            Log.d(TAG, "Unbinding OBD service..");
            unbindService(serviceConn);
            isServiceBound = false;
            obdStatusTextView.setText(getString(R.string.status_obd_disconnected));

            changeStatusDisconnect();

        }
    }


    private  void changeStatusDisconnect()
    {
        if(currentConnectionMode==CONNECT_OVER_BLUETOOTH)
        {
            bluetoothDeviceStatus.setText("Disconnected");
            bluetoothScanButton.setOn(false);
        }
        else
        {
            networkDeviceStatus.setText("Disconnected");
            networkScanButton.setOn(false);
        }

    }


    private  void changeStatusConnect()
    {
        if(currentConnectionMode==CONNECT_OVER_BLUETOOTH)
        {
            bluetoothDeviceStatus.setText("Connected");

            //gpsStart();
        }
        else
        {
            networkDeviceStatus.setText("Connected");

            //gpsStop();

        }

    }


    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    public void onGpsStatusChanged(int event) {

        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                gpsStatusTextView.setText(getString(R.string.status_gps_started));
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                gpsStatusTextView.setText(getString(R.string.status_gps_stopped));
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                gpsStatusTextView.setText(getString(R.string.status_gps_fix));
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                break;
        }
    }


    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {

                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                //Toast.makeText(context, "GPS Enabled: " + isGpsEnabled , Toast.LENGTH_LONG).show();

                if(isGpsEnabled) {
                    gpsInit();
                }

            }
        }
    };







    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


       // Toast.makeText(mContext, "Hello", Toast.LENGTH_SHORT).show();

            if (resultCode == Activity.RESULT_OK)
            {


                if(requestCode==1234)
                {

                    Intent serverIntent = new Intent(MainActivity.this, BtDeviceListActivity.class);
                    startActivityForResult(serverIntent, false
                            ? REQUEST_CONNECT_DEVICE_SECURE
                            : REQUEST_CONNECT_DEVICE_INSECURE );

                }

                else if(requestCode==REQUEST_CONNECT_DEVICE_INSECURE)
                {


                    String address = data.getExtras().getString(
                            BtDeviceListActivity.EXTRA_DEVICE_ADDRESS);

                    SharedPreferences.Editor editor = prefs.edit();

                    editor.putString(ConfigActivity.BLUETOOTH_DEVICE_KEY, address);

                    editor.commit();


                   startLiveData();

                }

                super.onActivityResult(requestCode, resultCode, data);
        }

    }



    private synchronized void gpsStart() {
        if (!mGpsIsStarted && mLocProvider != null && mLocService != null && mLocService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocService.requestLocationUpdates(mLocProvider.getName(), getGpsUpdatePeriod(prefs), getGpsDistanceUpdatePeriod(prefs), this);
            mGpsIsStarted = true;
        } else {
            gpsStatusTextView.setText(getString(R.string.status_gps_no_support));
        }
    }

    private synchronized void gpsStop() {
        if (mGpsIsStarted) {
            mLocService.removeUpdates(this);
            mGpsIsStarted = false;
            gpsStatusTextView.setText(getString(R.string.status_gps_stopped));
        }
    }

    /**
     * Uploading asynchronous task
     */
    private class UploadAsyncTask extends AsyncTask<ObdReading, Void, Void> {

        @Override
        protected Void doInBackground(ObdReading... readings) {
            Log.d(TAG, "Uploading " + readings.length + " readings..");
            // instantiate reading service client
            final String endpoint = prefs.getString(ConfigActivity.UPLOAD_URL_KEY, "");
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(endpoint)
                    .build();
            ObdService service = restAdapter.create(ObdService.class);
            // upload readings
            for (ObdReading reading : readings) {
                try {
                    Response response = service.uploadReading(reading);
                    assert response.getStatus() == 200;
                } catch (RetrofitError re) {
                    Log.e(TAG, re.toString());
                }

            }
            Log.d(TAG, "Done");
            return null;
        }

    }
}
