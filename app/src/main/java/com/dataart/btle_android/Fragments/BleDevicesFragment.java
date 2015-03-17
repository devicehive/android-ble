package com.dataart.btle_android.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dataart.btle_android.MainActivity;
import com.dataart.btle_android.R;
import com.dataart.btle_android.btle_gateway.BluetoothServer;

import java.util.ArrayList;


public class BleDevicesFragment extends Fragment {

    public static final int REQUEST_ENABLE_BT = 1;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServer bluetoothServerGateway;

    private boolean mScanning;
    private ListView listView;

    public BleDevicesFragment() {
    }

    public static Fragment newInstance() {
        final BleDevicesFragment fragment = new BleDevicesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_ble_devices, container, false);
        listView = (ListView) rootView.findViewById(R.id.listView);
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                final DeviceInfo device = mLeDeviceListAdapter.getDevice(i);
                if (device == null) {
                    return;
                }
                if (mScanning) {
                    //mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
        /*final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        startActivity(intent);*/
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        bluetoothServerGateway = new BluetoothServer();
        bluetoothServerGateway.setDiscoveredDeviceListener(discoveredDeviceListener);

        return rootView;
    }

    private BluetoothServer.DiscoveredDeviceListener discoveredDeviceListener = new BluetoothServer.DiscoveredDeviceListener() {
        @Override
        public void onDiscoveredDevice(final BluetoothDevice device) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final DeviceInfo deviceInfo = new DeviceInfo(device.getName(), device.getAddress());
                    mLeDeviceListAdapter.addDevice(deviceInfo);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(MainActivity.SECTION_DEVICES);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);
        //bluetoothServerGateway.scanStart(getActivity());
        scanLeDevice(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    //
                    bluetoothServerGateway.scanStop();
                    //mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    getActivity().invalidateOptionsMenu();
                }
            }, BluetoothServer.COMMAND_SCAN_DEALY);

            mScanning = true;
            bluetoothServerGateway.scanStart(getActivity());
            //mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            bluetoothServerGateway.scanStop();
            //mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        getActivity().invalidateOptionsMenu();
    }

    /*private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    getActivity().invalidateOptionsMenu();
                }
            }, BluetoothServer.COMMAND_SCAN_DEALY);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        getActivity().invalidateOptionsMenu();
    }*/

    /*private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final DeviceInfo deviceInfo = new DeviceInfo(device.getName(), device.getAddress());
                    mLeDeviceListAdapter.addDevice(deviceInfo);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };*/

    private class LeDeviceListAdapter extends BaseAdapter {

        private ArrayList<DeviceInfo> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<DeviceInfo>();
            mInflator = BleDevicesFragment.this.getActivity().getLayoutInflater();
        }

        public void addDevice(DeviceInfo device) {
            if (!mLeDevices.contains(device)) {
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

            final DeviceInfo device = mLeDevices.get(i);
            final String deviceName = device.getName();
            viewHolder.deviceName.setText(!TextUtils.isEmpty(deviceName) ? deviceName
                    : getString(R.string.unknown_device));
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    private static class ViewHolder {

        /*package*/ TextView deviceName;

        /*package*/ TextView deviceAddress;
    }

    /*package*/ public class DeviceInfo {

        private String name;
        private String address;

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

}
