package com.dataart.btle_android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dataart.android.devicehive.Notification;
import com.dataart.btle_android.Fragments.BleDeviceHiveLog;
import com.dataart.btle_android.Fragments.BleDeviceHiveSettings;
import com.dataart.btle_android.Fragments.BleDevicesFragment;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;


public class MainActivity extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        BTLEDeviceHive.NotificationListener {

    public static final int SECTION_DEVICES = 0;
    public static final int SECTION_DEVICE_HIVE_LOG = 1;
    public static final int SECTION_DEVICE_HIVE_SETTINGS = 2;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private BTLEDeviceHive deviceHive;
    //private BTLEGateway gateway;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * check BLE is supported on the device
         * */

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.error_message_ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        final BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        /**
         * Checks if Bluetooth is supported on the device
         * */

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_message_ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == BleDevicesFragment.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Fragment fragment = null;
        switch (position) {
            case SECTION_DEVICES:
                fragment = BleDevicesFragment.newInstance();
                break;
            case SECTION_DEVICE_HIVE_LOG:
                fragment = BleDeviceHiveLog.newInstance();
                break;
            case SECTION_DEVICE_HIVE_SETTINGS:
                fragment = BleDeviceHiveSettings.newInstance(new BleDeviceHiveSettings.SettingsChangesListener() {
                    @Override
                    public void onApiEnpointUrlChanged(String apiEndPointUrl) {
                        //FIXME deviceHive.setApiEnpointUrl(apiEndPointUrl);
                    }
                });
                break;
            default:
                fragment = PlaceholderFragment.newInstance(position + 1);
                break;
        }
        final FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case SECTION_DEVICES:
                mTitle = getString(R.string.menu_devices);
                break;
            case SECTION_DEVICE_HIVE_LOG:
                mTitle = getString(R.string.menu_logs);
                break;
            case SECTION_DEVICE_HIVE_SETTINGS:
                mTitle = getString(R.string.menu_gateway);
                break;
        }
    }

    public void restoreActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
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

    /*@Override
    public void onDeviceReceivedCommand(Command command) {
        gateway.doCommand(this, deviceHive, command);
    }*/

    @Override
    public void onDeviceSentNotification(Notification notification) {

    }

    @Override
    public void onDeviceFailedToSendNotification(Notification notification) {

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            final PlaceholderFragment fragment = new PlaceholderFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
