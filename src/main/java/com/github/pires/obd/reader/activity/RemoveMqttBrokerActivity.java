package com.github.pires.obd.reader.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;

import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.mqtt_odbii.MQTT_Server;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;

import static com.github.pires.obd.reader.activity.AddMqttBrokerActivity.loadMap;

public class RemoveMqttBrokerActivity extends RoboActivity {




    String message="Field should not be blank";


    private  Map<String,MQTT_Server> inputMap;



    private  DeleteBrokerAdapter mDeleteBrokerAdapter;


    static {
        RoboGuice.setUseAnnotationDatabases(false);
    }


    ListView brokersList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_mqtt_broker);



        brokersList= (ListView) findViewById(R.id.brokersList);



        inputMap=loadMap(this);

        if(inputMap==null )
        {
            inputMap=new HashMap<>();


        }

        ArrayList<MQTT_Server>  tempBrokerList=new ArrayList<>();
        try {



            for (Map.Entry<String, MQTT_Server> entry : inputMap.entrySet())
            {
                tempBrokerList.add(entry.getValue());
            }


        }catch (Exception ex)
        {
            ex.toString();
        }


        mDeleteBrokerAdapter=new DeleteBrokerAdapter(tempBrokerList,RemoveMqttBrokerActivity.this,inputMap);


        brokersList.setAdapter(mDeleteBrokerAdapter);

    }


    @Override
    protected void onStop() {
        super.onStop();

        saveMap(inputMap);


    }

    private void saveMap(Map<String,MQTT_Server> inputMap){


        Map<String,String> temp=new HashMap<>();

        Iterator it = inputMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            temp.put((String) pair.getKey(),((MQTT_Server)pair.getValue()).toJSON());
        }

        SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences("MyVariables", Context.MODE_PRIVATE);
        if (pSharedPref != null){

            JSONObject jsonObject = new JSONObject(temp);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("My_map").commit();
            editor.putString("My_map", jsonString);
            editor.commit();
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity(new Intent(RemoveMqttBrokerActivity.this, MQTTConfigurationActivity.class));

        finish();

    }

}
