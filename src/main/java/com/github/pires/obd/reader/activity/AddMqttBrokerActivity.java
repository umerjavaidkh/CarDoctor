package com.github.pires.obd.reader.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.mqtt_odbii.MQTT_Server;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

public class AddMqttBrokerActivity extends RoboActivity {


    @InjectView(R.id.brokerName)
    EditText brokerName;
    @InjectView(R.id.brokerURL)
    EditText brokerURL;
    @InjectView(R.id.serverPort)
    EditText serverPort;
    @InjectView(R.id.userName)
    EditText userName;
    @InjectView(R.id.password)
    EditText password;


    String message="Field should not be blank";


    private  Map<String,MQTT_Server> inputMap;


    static {
        RoboGuice.setUseAnnotationDatabases(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mqtt_broker);


        inputMap=loadMap(this);

        if(inputMap==null )
        {
            inputMap=new HashMap<>();
        }


    }





    @Override
    protected void onStop() {
        super.onStop();

        saveMap(inputMap);


    }

    public void OnNextClick(View view) {

        if(performValidation())
        {

            String  brokerNameTXT=brokerName.getText().toString();

            if(inputMap.containsKey(brokerNameTXT))
            {
                Toast.makeText(this, "Broker with name already exists", Toast.LENGTH_SHORT).show();
                return;
            }
            else
            {
                MQTT_Server  server=new MQTT_Server();

                server.setServerName(brokerNameTXT);
                server.setServerURL(brokerURL.getText().toString());
                server.setServerPort(Integer.parseInt(serverPort.getText().toString()));
                server.setUserName(userName.getText().toString());
                server.setPassword(password.getText().toString());

                inputMap.put(brokerNameTXT,server);
            }
        }

        finish();

    }



    public boolean performValidation()
    {

        if(brokerName.getText().toString().isEmpty() || brokerName.getText().toString().trim().length() ==0)
        {


            setErrorMessage(brokerName,message);

            return false;
        }

        else if(brokerURL.getText().toString().isEmpty() || brokerURL.getText().toString().trim().length() ==0)
        {


            setErrorMessage(brokerURL,message);

            return false;
        }


        else if(serverPort.getText().toString().isEmpty() || serverPort.getText().toString().trim().length() ==0)
        {


            setErrorMessage(serverPort,message);

            return false;
        }
        else
        {
            if(userName.getText().toString().isEmpty() || userName.getText().toString().trim().length() ==0)
            {

                if(!password.getText().toString().isEmpty() || password.getText().toString().trim().length() > 0) {
                    setErrorMessage(userName, "password field also required username");
                }
                else
                {
                    return  true;
                }

                return false;
            }
            else if(password.getText().toString().isEmpty() || password.getText().toString().trim().length() ==0)
            {

                if(!userName.getText().toString().isEmpty() || userName.getText().toString().trim().length() > 0) {
                    setErrorMessage(password, "username field also required password");
                }
                else
                {
                    return  true;
                }

                return false;
            }
            else
            {
                return  true;
            }



        }





    }


    /**
     * show error message on screen  EditText builtin method
     * @param et
     * @param msg
     */
    private void setErrorMessage(EditText et, String msg)
    {
        et.setError(msg);
        et.requestFocus();
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

    public static  Map<String,MQTT_Server> loadMap(Context context){
        Map<String,MQTT_Server> outputMap = new HashMap<String,MQTT_Server>();
        SharedPreferences pSharedPref = context.getApplicationContext().getSharedPreferences("MyVariables", Context.MODE_PRIVATE);
        try{
            if (pSharedPref != null){
                String jsonString = pSharedPref.getString("My_map", (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while(keysItr.hasNext()) {
                    String key = keysItr.next();
                    MQTT_Server value = MQTT_Server.jsonToObject(jsonObject.getString(key));
                    outputMap.put(key, value);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return outputMap;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity(new Intent(AddMqttBrokerActivity.this, MQTTConfigurationActivity.class));

        finish();

    }
}
