package com.github.pires.obd.reader.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.mqtt_odbii.MQTT_Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MQTTConfigurationActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {



    public static final String MQTT_BROKER_NAME = "mqtt_broker_name";
    public static final String MQTT_BROKER_URL =  "mqtt_broker_url";
    public static final String MQTT_BROKER_PORT = "mqtt_broker_port";
    public static final String MQTT_PUBLISH_TOPIC = "mqtt_broker_published_topic";
    public static final String MQTT_SUBSCRIBE_TOPIC = "mqtt_broker_subscribe_topic";
    public static final String ENABLE_SSL = "enable_ssl_preference";
    public static final String ENABLE_CREDENTIALS = "enable_credentials_preference";
    public static final String MQTT_BROKER_LIST = "brokers_list_preference";
    public static final String ADD_MQTT_BROKER = "add_mqtt_broker_screen";
    public static final String Remove_MQTT_BROKER = "remove_mqtt_broker_screen";





    private Map<String,MQTT_Server> mqttBrokers;


    private Context  mContext;



    private   ListPreference listPreference;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext=this;

        addPreferencesFromResource(R.xml.mqtt_configuration_preferences);


        mqttBrokers=AddMqttBrokerActivity.loadMap(mContext);

        if(mqttBrokers==null )
        {
            mqttBrokers=new HashMap<>();
        }


        PreferenceScreen addBrokerpreferenceScreen = (PreferenceScreen) getPreferenceScreen()
                .findPreference(ADD_MQTT_BROKER);


        addBrokerpreferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                startActivity(new Intent(MQTTConfigurationActivity.this, AddMqttBrokerActivity.class));
                finish();
                return false;
            }
        });


        PreferenceScreen removeBrokerpreferenceScreen = (PreferenceScreen) getPreferenceScreen()
                .findPreference(Remove_MQTT_BROKER);


        removeBrokerpreferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                startActivity(new Intent(MQTTConfigurationActivity.this, RemoveMqttBrokerActivity.class));

                finish();

                return false;
            }
        });



      listPreference = (ListPreference) getPreferenceScreen() .findPreference(MQTT_BROKER_LIST);






        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                int index = listPreference.findIndexOfValue(newValue.toString());
                if (index != -1) {

                    setCurrentPreference(listPreference.getEntries()[index].toString());

                }
                return true;
            }
        });






    }


    @Override
    protected void onResume() {
        super.onResume();

        Set<Map.Entry<String, MQTT_Server>> mittBrokersEntries = mqttBrokers.entrySet();

        ArrayList<CharSequence> mqttBrokersStrings = new ArrayList<>();
        ArrayList<CharSequence> mqttBrokersStringsURLs = new ArrayList<>();

        mqttBrokersStrings.add("Default");
        mqttBrokersStringsURLs.add("Default Broker");

        if (mittBrokersEntries.size() > 0) {
            for (Map.Entry<String, MQTT_Server> device : mittBrokersEntries) {

                MQTT_Server  obj=device.getValue();
                mqttBrokersStrings.add(obj.getServerName());
                mqttBrokersStringsURLs.add(obj.getServerURL()+" "+obj.getServerPort());
            }
        }

        listPreference.setEntries(mqttBrokersStrings.toArray(new CharSequence[0]));
        listPreference.setEntryValues(mqttBrokersStrings.toArray(new CharSequence[0]));


        CharSequence entry=  listPreference.getEntry();

        if(entry!=null) {
            setCurrentPreference(entry.toString());
        }

    }

    private   void   setCurrentPreference(String serverName)
    {

        if(mqttBrokers!=null && !serverName.toLowerCase().equals("default"))
        {
            MQTT_Server  object  =  mqttBrokers.get(serverName);

            try {

                EditTextPreference etPreferenceName = (EditTextPreference) getPreferenceScreen().findPreference(MQTT_BROKER_NAME);
                EditTextPreference etPreferenceURL = (EditTextPreference) getPreferenceScreen().findPreference(MQTT_BROKER_URL);
                EditTextPreference etPreferencePORT = (EditTextPreference) getPreferenceScreen().findPreference(MQTT_BROKER_PORT);

                etPreferenceName.setText("Broker Name: " + object.getServerName());
                etPreferenceName.setTitle("Broker Name: " + object.getServerName());
                etPreferenceURL.setText(object.getServerURL());
                etPreferencePORT.setText(object.getServerPort() + "");
            }catch (Exception ex)
            {
                ex.toString();
            }
        }
        else
        {
            try {

                EditTextPreference etPreferenceName = (EditTextPreference) getPreferenceScreen().findPreference(MQTT_BROKER_NAME);
                EditTextPreference etPreferenceURL = (EditTextPreference) getPreferenceScreen().findPreference(MQTT_BROKER_URL);
                EditTextPreference etPreferencePORT = (EditTextPreference) getPreferenceScreen().findPreference(MQTT_BROKER_PORT);

                etPreferenceName.setText("Broker Name: " + "Default");
                etPreferenceName.setTitle("Broker Name: " + "Default");
                etPreferenceURL.setText("broker.hivemq.com");
                etPreferencePORT.setText(1883 + "");
            }catch (Exception ex)
            {
                ex.toString();
            }

        }

    }


    public static String getServerUrl(SharedPreferences prefs) {
        String url = prefs
                .getString(MQTTConfigurationActivity.MQTT_BROKER_URL, "broker.hivemq.com"); // 1 as in seconds

        return url;
    }


    public static String getServerPORT(SharedPreferences prefs) {
        String port = prefs
                .getString(MQTTConfigurationActivity.MQTT_BROKER_PORT, "1883"); // 1 as in seconds

        return port;
    }





    public static String getServerPublishedTopic(SharedPreferences prefs) {
        String topic_P = prefs
                .getString(MQTTConfigurationActivity.MQTT_PUBLISH_TOPIC, "/thomasj/cardata"); // 1 as in seconds

        return topic_P;
    }

    public static String getServerSubscribedTopic(SharedPreferences prefs) {
        String topic_S = prefs
                .getString(MQTTConfigurationActivity.MQTT_SUBSCRIBE_TOPIC, "/thomasj/statusdata"); // 1 as in seconds

        return topic_S;
    }

    public static boolean isSSL_Required(SharedPreferences prefs) {
        boolean ssl = prefs
                .getBoolean(MQTTConfigurationActivity.ENABLE_SSL, false); // 1 as in seconds

        return ssl;
    }


    public static boolean isUserCredentials_Required(SharedPreferences prefs) {
        boolean CREDENTIALS = prefs
                .getBoolean(MQTTConfigurationActivity.ENABLE_CREDENTIALS, false); // 1 as in seconds

        return CREDENTIALS;
    }


    public static MQTT_Server getBroker(SharedPreferences prefs,Context mContext) {

        MQTT_Server  tempBroker=null;

        try {
            Map<String, MQTT_Server> tempBrokers = AddMqttBrokerActivity.loadMap(mContext);


            String brokerName = prefs.getString(MQTT_BROKER_LIST, null);

            tempBroker = tempBrokers.get(brokerName);

            if(tempBroker==null)
            {
                tempBroker=tempBrokers.get("Default");

                if(tempBroker==null)
                {
                    tempBroker=new MQTT_Server();

                    tempBroker.setServerPort(1883);

                    tempBroker.setUserName("Default");

                    tempBroker.setServerURL("broker.hivemq.com");
                }
            }

        }catch (Exception ex)
        {
            ex.toString();
        }

        return  tempBroker;

    }





    /*public static MQTT_Server getSelectedBroker(SharedPreferences prefs,Context mContext) {

        MQTT_Server  tempBroker=null;

        try {
            Map<String, MQTT_Server> tempBrokers = AddMqttBrokerActivity.loadMap(mContext);


            String brokerName = prefs.getString(MQTT_BROKER_LIST, null);

            tempBroker = tempBrokers.get(brokerName);
        }catch (Exception ex)
        {
            ex.toString();
        }

        return  tempBroker;

    }*/


    public static MQTT_Server getSelectedBroker(SharedPreferences prefs,Context mContext) {

        MQTT_Server  tempBroker=null;

        try {
            Map<String, MQTT_Server> tempBrokers = AddMqttBrokerActivity.loadMap(mContext);


            String brokerName = prefs.getString(MQTT_BROKER_LIST, null);

            tempBroker = tempBrokers.get(brokerName);
        }catch (Exception ex)
        {
            ex.toString();
        }

        return  tempBroker;

    }



    public static  ArrayList<MQTT_Server> getSelectedBrokers(SharedPreferences prefs,Context mContext) {

        ArrayList<MQTT_Server>  tempBrokerList=new ArrayList<>();

        try {
           Map<String, MQTT_Server> tempBrokers = AddMqttBrokerActivity.loadMap(mContext);


            for (Map.Entry<String, MQTT_Server> entry : tempBrokers.entrySet())
            {
                tempBrokerList.add(entry.getValue());
            }


        }catch (Exception ex)
        {
            ex.toString();
        }

        return  tempBrokerList;

    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {


        return false;
    }
}
