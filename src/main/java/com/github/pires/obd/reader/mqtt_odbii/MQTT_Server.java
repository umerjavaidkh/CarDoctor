package com.github.pires.obd.reader.mqtt_odbii;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Umer on 2/23/2018.
 */

public class MQTT_Server {


    public  static final String  SERVER_NAME_TAG="server_name";
    public  static final String  SERVER_URL_TAG="server_url";
    public  static final String  SERVER_PORT_TAG="server_port";
    public  static final String  USER_NAME_TAG="user_name";
    public  static final String  PASSWORD_TAG="password";


    private   String  serverName="";
    private   String  serverURL="";
    private   int     serverPort=1883;
    private   String  userName="";
    private   String  password="";

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public String toJSON() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(SERVER_NAME_TAG, getServerName());
            jsonObject.put(SERVER_URL_TAG, getServerURL());
            jsonObject.put(SERVER_PORT_TAG, getServerPort());
            jsonObject.put(USER_NAME_TAG, getUserName());
            jsonObject.put(PASSWORD_TAG, getPassword());

            return jsonObject.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }

    public  static  MQTT_Server  jsonToObject(String json) {

        MQTT_Server mqtt_Server=new MQTT_Server();

        try {

            JSONObject newsDataJson = new JSONObject(json);

            String serverName = newsDataJson.getString(SERVER_NAME_TAG);
            String serverURL = newsDataJson.getString(SERVER_URL_TAG);
            String serverPort= newsDataJson.getString(SERVER_PORT_TAG);
            String userName = newsDataJson.getString(USER_NAME_TAG);
            String userPassword = newsDataJson.getString(PASSWORD_TAG);

            mqtt_Server.setServerName(serverName);
            mqtt_Server.setServerURL(serverURL);
            mqtt_Server.setServerPort(Integer.parseInt(serverPort));
            mqtt_Server.setUserName(userName);
            mqtt_Server.setPassword(userPassword);


        } catch (JSONException e) {
            e.printStackTrace();
        }


        return  mqtt_Server;

    }
}
