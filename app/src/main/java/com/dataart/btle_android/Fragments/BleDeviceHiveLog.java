package com.dataart.btle_android.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.dataart.btle_android.MainActivity;
import com.dataart.btle_android.R;

import java.io.StringBufferInputStream;
import java.util.ArrayList;


public class BleDeviceHiveLog extends Fragment{
    public BleDeviceHiveLog() {
        this.strings = new ArrayList<>();
    }

    TextView textView;

    ArrayList<String> strings;

    public void setData(ArrayList<String> strings) {
        this.strings.addAll(strings);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_devicehive_log, container, false);
        textView = (TextView) rootView.findViewById(R.id.device_hive_log);

        StringBuffer buffer = new StringBuffer();

        for(String s : strings) {
            buffer.append(s);
            buffer.append("\n");
        }

        textView.setText(buffer.toString());

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.SECTION_DEVICE_HIVE_LOG);
    }
}