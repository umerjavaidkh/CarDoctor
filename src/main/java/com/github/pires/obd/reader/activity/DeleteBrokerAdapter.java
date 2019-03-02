package com.github.pires.obd.reader.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.github.pires.obd.reader.R;
import com.github.pires.obd.reader.mqtt_odbii.MQTT_Server;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Umer on 4/23/2018.
 */

public class DeleteBrokerAdapter extends BaseAdapter implements ListAdapter {
    private ArrayList<MQTT_Server> list = new ArrayList<MQTT_Server>();
    private Map<String,MQTT_Server> inputMap;
    private Context context;



    public DeleteBrokerAdapter(ArrayList<MQTT_Server> list, Context context, Map<String,MQTT_Server> inputMap) {
        this.list = list;
        this.context = context;
        this.inputMap=inputMap;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
        //just return 0 if your list items do not have an Id variable.
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.delete_broker_row, null);
        }

        //Handle TextView and display string from your list
        TextView listItemText = (TextView)view.findViewById(R.id.list_item_string);
        listItemText.setText(list.get(position).getServerName());

        //Handle buttons and add onClickListeners
        Button deleteBtn = (Button)view.findViewById(R.id.delete_btn);

        deleteButtonClick(deleteBtn,position);


        return view;
    }



    public  void  deleteButtonClick(Button deleteBtn,final int position)
    {

        deleteBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //do something

                inputMap.remove(list.get(position).getServerName());//or some other task
                list.remove(position);
                notifyDataSetChanged();
            }
        });

    }
}