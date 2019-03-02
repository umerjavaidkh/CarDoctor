package com.github.pires.obd.reader.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.config.ObdConfig;
import com.google.inject.Inject;

import org.json.JSONObject;

import java.util.HashMap;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

import static com.github.pires.obd.reader.activity.ConfirmDialog.createDialog;
import static com.github.pires.obd.reader.activity.MainActivity.LookUpCommand;

/**
 * Some code taken from https://github.com/wdkapps/FillUp
 */

public class LastReceivedDataListActivity
        extends RoboActivity
        implements ConfirmDialog.Listener {



    public  static final    String    lastReceivedData_KEY_TAG="last_received_data";

    private    String    lastReceivedDataFromMQTT_Server="";


    @InjectView(R.id.data_table)
    private TableLayout tl;

    private PowerManager powerManager;
    @Inject
    private SharedPreferences prefs;


    private HashMap<String,String>   hashMap=new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trips_list);

        lastReceivedDataFromMQTT_Server=prefs.getString(lastReceivedData_KEY_TAG,"");


        //{"EQUIV_RATIO":"0.0%","AMBIENT_AIR_TEMP":"40C","DISTANCE_TRAVELED_MIL_ON":"4096km",
        // "SPEED":"160km\/h","Short Term Fuel Trim Bank 2":"40.6%","ENGINE_OIL_TEMP":"60C",
        // "ENGINE_LOAD":"47.1%","TROUBLE_CODES":"P057F\nP0126\nU1397\n","VIN":"1HGBH41JXMN109186",
        // "TIMING_ADVANCE":"51.8%","CONTROL_MODULE_VOLTAGE":"3.0V","ENGINE_RUNTIME":"01:08:36",
        // "Short Term Fuel Trim Bank 1":"40.6%","MAF":"257.00g\/s","FUEL_PRESSURE":"453kPa",
        // "ENGINE_RPM":"3237RPM","THROTTLE_POS":"58.8%","FUEL_TYPE":"Gasoline",
        // "Long Term Fuel Trim Bank 2":"40.6%","FUEL_CONSUMPTION_RATE":"6.0L\/h",
        // "DTC_NUMBER":"OK","INTAKE_MANIFOLD_PRESSURE":"153kPa","WIDEBAND_AIR_FUEL_RATIO":"6.03:1 AFR",
        // "ENGINE_COOLANT_TEMP":"121C","AIR_FUEL_RATIO":"11.60:1 AFR","Long Term Fuel Trim Bank 1":"40.6%",
        // "BAROMETRIC_PRESSURE":"150kPa","FUEL_LEVEL":"39.2%","FUEL_RAIL_PRESSURE":"1550kPa",
        // "AIR_INTAKE_TEMP":"120C","gpspositionlat":0,"gpspositionlan":0}



        LongOperation  longOperation=new LongOperation(lastReceivedDataFromMQTT_Server);

        longOperation.execute();



    }


    private class LongOperation extends AsyncTask<String, Void, String> {


        String jsonData;


        ProgressDialog  pd;

       public LongOperation(String jsonData)
        {
            this.jsonData=jsonData;
        }

        @Override
        protected String doInBackground(String... params) {

           try {

               JSONObject jsonObject = new JSONObject(jsonData);

               for (ObdCommand Command : ObdConfig.getCommands()) {


                   final String cmdName = Command.getName();

                   final String cmdID = LookUpCommand(cmdName);
                       String   value =  jsonObject.getString(cmdID);

                       hashMap.put(Command.getName(),value);

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


            for (String key : hashMap.keySet()) {

                String value=hashMap.get(key);

                addTableRow(key,key,value);


            }

        }


        @Override
        protected void onPreExecute() {


            pd=ProgressDialog.show(LastReceivedDataListActivity.this,"","");

        }



    }


    private void addTableRow(String id, String key, String val) {

        TableRow tr = new TableRow(this);
        MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(MainActivity.TABLE_ROW_MARGIN, MainActivity.TABLE_ROW_MARGIN, MainActivity.TABLE_ROW_MARGIN,
                MainActivity.TABLE_ROW_MARGIN);

        tr.setPadding(10,0,10,0);

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



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_trips_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        // create the menu
        getMenuInflater().inflate(R.menu.context_trip_list, menu);

        // get index of currently selected row
       /* AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedRow = (int) info.id;

        // get record that is currently selected
        TripRecord record = records.get(selectedRow);*/
    }



    public boolean onContextItemSelected(MenuItem item) {

        // get index of currently selected row
       /* AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        selectedRow = (int) info.id;

        switch (item.getItemId()) {
            case R.id.itemDelete:
                showDialog(ConfirmDialog.DIALOG_CONFIRM_DELETE_ID);
                return true;

            default:
                return super.onContextItemSelected(item);
        }*/

        return super.onContextItemSelected(item);
    }

    protected Dialog onCreateDialog(int id) {
        return createDialog(id, this, this);
    }

    /**
     * DESCRIPTION:
     * Called when the user has selected a gasoline record to delete
     * from the log and has confirmed deletion.
     */


    @Override
    public void onConfirmationDialogResponse(int id, boolean confirmed) {
        removeDialog(id);
        if (!confirmed) return;

        switch (id) {
            case ConfirmDialog.DIALOG_CONFIRM_DELETE_ID:

                break;

            default:
                //Utilities.toast(this,"Invalid dialog id.");
        }

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }
}
