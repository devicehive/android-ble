package com.dataart.btle_android;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;

import com.dataart.android.devicehive.Command;
import com.dataart.android.devicehive.Notification;
import com.dataart.btle_android.Fragments.BleDevicesFragment;
import com.dataart.btle_android.btle_gateway.BTLEGateway;
import com.dataart.btle_android.btle_gateway.BluetoothServer;
import com.dataart.btle_android.devicehive.BTLEDeviceHive;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        BTLEDeviceHive.CommandListener,
        BTLEDeviceHive.NotificationListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    BluetoothServer bluetoothServerGateway;
    BTLEDeviceHive deviceHive;
    BTLEGateway gateway;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        bluetoothServerGateway = new BluetoothServer();
        //bluetoothServerGateway.scanStart(this);

        gateway = new BTLEGateway(bluetoothServerGateway);

        BTLEApplication app = (BTLEApplication) getApplication();
        deviceHive = app.getDevice();

        //deviceHive.addDeviceListener(this);
        deviceHive.addCommandListener(this);
        //deviceHive.addNotificationListener(this);
        //deviceInfoFragment.setDeviceData(deviceHive.getDeviceData());
        if (!deviceHive.isRegistered()) {
            deviceHive.registerDevice();
            deviceHive.startProcessingCommands();
        } else {
            deviceHive.startProcessingCommands();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        deviceHive.removeCommandListener(this);
        deviceHive.stopProcessingCommands();

    }

    public static final int SECTION_DEVICES = 0;
    public static final int SECTION_DEVICE_HIVE_LOG = 1;

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments

        Fragment fragment = null;



        switch(position) {
            case SECTION_DEVICES:
                fragment = BleDevicesFragment.newInstance();
                break;
            case SECTION_DEVICE_HIVE_LOG:
                break;

            default:
                fragment = PlaceholderFragment.newInstance(position + 1);
            break;
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case SECTION_DEVICES:
                mTitle = getString(R.string.title_section1);
                break;
            case SECTION_DEVICE_HIVE_LOG:
                mTitle = getString(R.string.title_section2);
                break;
            default:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
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
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }



    @Override
    public void onDeviceReceivedCommand(Command command) {

        gateway.doCommand(this, deviceHive,command);
    }



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
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
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
