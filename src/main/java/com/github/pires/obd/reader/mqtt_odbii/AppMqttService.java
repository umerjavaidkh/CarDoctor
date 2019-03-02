package com.github.pires.obd.reader.mqtt_odbii;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.github.pires.obd.reader.activity.MQTTConfigurationActivity;
import com.google.inject.Inject;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class AppMqttService extends Service {

    private Context mContext;
    private String mServerURL = "tcp://broker.hivemq.com:1883";  //tcp://iot.eclipse.org:1883//192.168.100.2
    private String mClientId = "android_client_1";

    private MqttAndroidClient mqttAndroidClient;

    @Inject
    SharedPreferences prefs;

    private IBinder mIbinder = new NewsBinder();

    MQTT_Server broker;


    private UpdateGUI mUpdateGUIListener;
    private UpdateLoginDetails mUpdateLoginDetails;
    private UpdateRegistrationDetails mUpdateRegistrationDetails;



    public  void setPreference(SharedPreferences prefs)
    {
        this.prefs=prefs;

        try {

             broker = MQTTConfigurationActivity.getBroker(prefs, AppMqttService.this);



        }catch (Exception ex)
        {
            ex.toString();
        }


    }

    public AppMqttService() {




    }


    // runnable class called by handler after every 10 seconds of subscription
    private  class   RunnableClass implements Runnable
    {

        @Override
        public void run() {


        }
    }



    //  used to bind service with any activity
    @Override
    public IBinder onBind(Intent intent) {
        return mIbinder;
    }


    public class NewsBinder extends Binder {


        public AppMqttService getService() {
            return AppMqttService.this;
        }

    }


    /**
     *  Part of Service Lifecycle When Service is created
     */
    @Override
    public void onCreate() {


        super.onCreate();

        mContext=AppMqttService.this;



       // login("","");


    }


    /**
     *  called when service started and START_STICKY_COMPATIBILITY runs it when app closed
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        return START_STICKY_COMPATIBILITY;
    }


    private void init_MQTT() {


       String serverURL=mServerURL;

        if(broker!=null)
        {
            serverURL="tcp://"+broker.getServerURL()+":"+broker.getServerPort();

        }
        /*else
        {
            broker=new MQTT_Server();

            broker.setServerURL(MQTTConfigurationActivity.getServerUrl(prefs));

            broker.setServerPort(Integer.parseInt(MQTTConfigurationActivity.getServerPORT(prefs)));

            broker.setPassword(MQTTConfigurationActivity.getServerPORT());

            serverURL="tcp://"+broker.getServerURL()+":"+broker.getServerPort();
        }*/



        mqttAndroidClient = new MqttAndroidClient(mContext, serverURL, mClientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                String  publishTopicValue=AppConstants.App_Topic_Value;
                publishTopicValue=MQTTConfigurationActivity.getServerPublishedTopic(prefs);

                String  subscribeTopicValue=AppConstants.App_Subscribed_Value;
                subscribeTopicValue=MQTTConfigurationActivity.getServerPublishedTopic(prefs);

                if (reconnect) {

                    publishTopic(publishTopicValue,"");
                    subscribeToTopic(subscribeTopicValue);

                } else {

                }

            }

            @Override
            public void connectionLost(Throwable cause) {


                if(mUpdateLoginDetails!=null)
                {
                    mUpdateLoginDetails.connectionStatus(false);
                }


            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {



                    if (mUpdateGUIListener != null) {

                        mUpdateGUIListener.onUpdateData(new String(message.getPayload()), message.getId() + "");
                    } else {


                    }


            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {


              /* String jsonValue=" { \"Status\": \"Crash\", \"Prohability\":\"0.9\" }";

                if (mUpdateGUIListener != null) {

                    mUpdateGUIListener.onUpdateData(new String(jsonValue),  "12345");
                } else {


                }*/


            }
        });


    }



    /**
     *  login method  to connect to server with username and password
     *
     */
    public void login(String userName, String userPass) {

        init_MQTT();

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(false);

        if(broker!=null && prefs!=null && prefs.getBoolean(MQTTConfigurationActivity.ENABLE_CREDENTIALS,false))

        {
            if(broker.getUserName().length() > 0) {
                mqttConnectOptions.setUserName(broker.getUserName());
                mqttConnectOptions.setPassword(broker.getPassword().toCharArray());
            }
        }

        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(1);

        try {

            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(200);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);




                    if(mUpdateLoginDetails!=null)
                    {
                        mUpdateLoginDetails.onUpdateLoginDetails(true, asyncActionToken.getMessageId()+"");
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {


                   if(mUpdateLoginDetails!=null)
                   {
                     mUpdateLoginDetails.connectionStatus(false);
                   }

                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }



    public  void   disconnect()
    {
        try {

            if(mqttAndroidClient!=null) {
                mqttAndroidClient.disconnect();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     *  register method  to connect to server with all user info
     */
    public void register(final String userName, final String topic, final String userInfo) {


        mClientId=userName;

        init_MQTT();

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(10);
        mqttConnectOptions.setPassword("registration".toCharArray());
        mqttConnectOptions.setUserName("registration");


        try {

            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(200);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);


                    publishClientInfo(topic,userInfo);

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {


                   if(mUpdateRegistrationDetails!=null)
                   {

                       mUpdateRegistrationDetails.onUpdateRegistrationDetails(false,exception.getMessage());
                   }

                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }





    /**
     *  subscribeTo method  to subscribe Topic  called from NewsDetailActivity
     */
    public   void subscribeToTopic(String topic)
    {
        if(!mqttAndroidClient.isConnected())
        {
            return;
        }

        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken)
                {

                    asyncActionToken.toString();


                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    asyncActionToken.toString();
                }
            });



        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }


    }

    /**
     *  unSubscribe method  to unsubscribe Topic OR Whole Tab  called from Tabed Activity and in private context
     */
    public   void unSubscribe(final String topic)
    {
        if(!mqttAndroidClient.isConnected())
        {
            return;
        }


        try {
            mqttAndroidClient.unsubscribe(topic,  null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken)
                {

                    asyncActionToken.toString();



                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    asyncActionToken.toString();

                }
            });



        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }


    }

    /**
     *  publishTopic method  to publishTopic Topic
     */

    public void publishTopic(String topic, String messageString){



        try {
             if( mqttAndroidClient==null || (!mqttAndroidClient.isConnected()))
            {

                return;
            }



            mqttAndroidClient.publish(topic,messageString.getBytes(),0,true,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {


                    //Toast.makeText(mContext, "yes", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {


                    //Toast.makeText(mContext, "no", Toast.LENGTH_SHORT).show();
                }
            });


        } catch (MqttException e) {

            e.printStackTrace();

        }
    }

    /**
     *  publishClientInfo method  used from registration
     */

    public void publishClientInfo(String topic, String messageString){

        try {
            if(!mqttAndroidClient.isConnected())
            {
               return;
            }

            mqttAndroidClient.publish(topic,messageString.getBytes(),0,true,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {


                    if(mUpdateRegistrationDetails!=null)
                    {

                        mUpdateRegistrationDetails.onUpdateRegistrationDetails(true,asyncActionToken.getMessageId()+"");
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {


                    if(mUpdateRegistrationDetails!=null)
                    {

                        mUpdateRegistrationDetails.onUpdateRegistrationDetails(false,exception.getMessage());
                    }

                }
            });


        } catch (MqttException e) {

            e.printStackTrace();

        }
    }

    /**
     *  A generic listener to send received data to activities
     */

    public interface UpdateGUI {

        public void onUpdateData(String newsData, String itemId);

    }


    /**
     *  A  listener to send received data to login activity
     */
    public interface UpdateLoginDetails {


        public void onUpdateLoginDetails(boolean isLoginSuccessful, String message);

        public void connectionStatus(boolean status);

    }



    public void setUpdateGUILoginListener(UpdateLoginDetails listener)
    {
        mUpdateLoginDetails=listener;
    }

    public void setUpdateGUIRegisterListener(UpdateRegistrationDetails listener)
    {
        mUpdateRegistrationDetails=listener;
    }


    public void setUpdateGUIDataListener(UpdateGUI listener)
    {
        mUpdateGUIListener=listener;
    }

    public interface UpdateRegistrationDetails {
         public void onUpdateRegistrationDetails(boolean isLoginSuccessful, String message);
 }




    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(getApplicationContext(), "Destroyed", Toast.LENGTH_SHORT).show();
    }
}
