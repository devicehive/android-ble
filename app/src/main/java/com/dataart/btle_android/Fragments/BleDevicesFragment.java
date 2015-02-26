package com.dataart.btle_android.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dataart.btle_android.MainActivity;
import com.dataart.btle_android.R;

import java.util.ArrayList;


public class BleDevicesFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static final int ARG_SECTION_NUMBER = 0;


    LeDeviceListAdapter deviceListAdapter;

    public BleDevicesFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ble_devices, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listView);

        deviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(deviceListAdapter);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.SECTION_DEVICES);

    }

    public static Fragment newInstance() {
        Fragment fragment = new BleDevicesFragment();
        return  fragment;
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<DeviceInfo> mLeDevices;
        private LayoutInflater mInflator;


        class DeviceInfo {
            String name;
            String address;

            public DeviceInfo(String name, String address) {
                this.name = name;
                this.address = address;
            }

            public String getName() {
                return name;
            }

            public String getAddress() {
                return address;
            }
        }

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<DeviceInfo>();
            mInflator = BleDevicesFragment.this.getActivity().getLayoutInflater();
            mLeDevices.add(new DeviceInfo("test", "testInfo"));
        }

        public void addDevice(DeviceInfo device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public DeviceInfo getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            DeviceInfo device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }


    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}
