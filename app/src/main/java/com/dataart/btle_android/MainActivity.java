package com.dataart.btle_android;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dataart.btle_android.btle_gateway.BluetoothLeService;
import com.dataart.btle_android.devicehive.BTLEDevicePreferences;
import com.dataart.btle_android.helpers.BleHelpersFactory;
import com.dataart.btle_android.helpers.ble.base.BleInitializer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.MapView;
import com.google.android.libraries.maps.OnMapReadyCallback;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.maps.model.Circle;
import com.google.android.libraries.maps.model.CircleOptions;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;
import java.util.UUID;

import timber.log.Timber;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private BleInitializer bleInitializer;

    private BluetoothManager mBluetoothManager;
    private TextInputEditText serverUrlEditText;
    private TextInputLayout serverUrlEditTextParent;
    private TextInputEditText gatewayIdEditText;
    private TextInputLayout gatewayIdEditTextParent;
    private TextInputEditText refreshTokenEditText;
    private TextInputLayout refreshTokenEditTextParent;
    private TextView hintText;
    private FloatingActionButton serviceButton;
    private FloatingActionButton restartServiceButton;
    private MapView mapView;
    private BTLEDevicePreferences prefs;
    private boolean isServiceStarted;
    private final View.OnClickListener restartClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            saveValues();
            BluetoothLeService.stop(MainActivity.this);
            BluetoothLeService.start(MainActivity.this);
            restartServiceButton.setVisibility(View.GONE);
            serviceButton.setVisibility(View.VISIBLE);
            onServiceRunning();
            hintText.setVisibility(View.GONE);
        }
    };

    private final TextView.OnEditorActionListener changeListener = (textView, actionId, keyEvent) -> {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
            onDataChanged();
        }
        return false;
    };

    private final TextWatcher changeWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            onDataChanged();
        }
    };
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private Location mLastLocation;
    private Circle mCurrLocationMarker;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
//        BleInitializer will start service on initialization success
        bleInitializer = BleHelpersFactory.getInitializer(this, bluetoothAdapter -> startService());
        init();
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void fatalDialog(int message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.unsupported)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .create().show();
    }

    private void init() {
        if (!isBluetoothLeSupported()) {
            fatalDialog(R.string.error_message_btle_not_supported);
            return;
        }
        if (!isBluetoothSupported()) {
            fatalDialog(R.string.error_message_bt_not_supported);
            return;
        }

        prefs = BTLEDevicePreferences.getInstance();

        serverUrlEditText = findViewById(R.id.server_url_edit);
        serverUrlEditTextParent = findViewById(R.id.server_url_parent);
        gatewayIdEditText = findViewById(R.id.settings_gateway_id);
        gatewayIdEditTextParent = findViewById(R.id.settings_gateway_id_parent);
        refreshTokenEditText = findViewById(R.id.refresh_token_edit);
        refreshTokenEditTextParent = findViewById(R.id.refresh_token_parent);
        hintText = findViewById(R.id.hintText);

        resetValues();


        serviceButton = findViewById(R.id.service_button);
        //noinspection ConstantConditions
        serviceButton.setOnClickListener(v -> {
            Timber.d(String.valueOf(validateValues()));
            if (validateValues()) {
                bleInitializer.start();
            }
        });

        restartServiceButton = findViewById(R.id.save_button);
        //noinspection ConstantConditions
        restartServiceButton.setOnClickListener(restartClickListener);

        serverUrlEditText.setOnEditorActionListener(changeListener);
        serverUrlEditText.addTextChangedListener(changeWatcher);

        gatewayIdEditText.setOnEditorActionListener(changeListener);
        gatewayIdEditText.addTextChangedListener(changeWatcher);

        refreshTokenEditText.setOnEditorActionListener(changeListener);
        refreshTokenEditText.addTextChangedListener(changeWatcher);

        if (isLeServiceRunning()) {
            onServiceRunning();
        }
    }

    private boolean isBluetoothLeSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private boolean isBluetoothSupported() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Timber.e(getString(R.string.bt_unable_init));
                return false;
            }
        }
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Timber.e(getString(R.string.bt_unable_get_btm));
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bleInitializer.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        bleInitializer.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        bleInitializer.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            if (mGoogleApiClient == null) {
                buildGoogleApiClient();
            }
            mGoogleMap.setMyLocationEnabled(true);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean isLeServiceRunning() {
        final ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (Objects.equals(BluetoothLeService.class.getName(), service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startService() {
        Timber.d("HERE %s", isServiceStarted);
        if (!isServiceStarted) {
            Timber.d("Started");
            saveValues();
            onServiceRunning();
            BluetoothLeService.start(MainActivity.this);
        } else {
            Timber.d("Not Started");
            onServiceStopped();
            BluetoothLeService.stop(MainActivity.this);
        }
    }

    private void onServiceRunning() {
        isServiceStarted = true;
        serviceButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_stop));
    }

    private void onServiceStopped() {
        isServiceStarted = false;
        serviceButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow));
    }

    private boolean isRestartRequired() {
        String newUrl = serverUrlEditText.getText().toString();
        String newGatewayId = gatewayIdEditText.getText().toString();
        String newRefreshToken = refreshTokenEditText.getText().toString();

        return !(Objects.equals(prefs.getServerUrl(), newUrl) &&
                Objects.equals(prefs.getGatewayId(), newGatewayId) &&
                Objects.equals(prefs.getRefreshToken(), newRefreshToken));
    }

    private void onDataChanged() {
        if (isServiceStarted && isRestartRequired()) {
            hintText.setVisibility(View.VISIBLE);
            restartServiceButton.setVisibility(View.VISIBLE);
            serviceButton.setVisibility(View.INVISIBLE);
        } else {
            hintText.setVisibility(View.GONE);
            restartServiceButton.setVisibility(View.GONE);
            serviceButton.setVisibility(View.VISIBLE);
        }
    }

    private void resetValues() {
        String serverUrl = prefs.getServerUrl();
        serverUrlEditText.setText(
                TextUtils.isEmpty(serverUrl)
                        ? getString(R.string.default_server_url)
                        : serverUrl
        );

        String gatewayId = prefs.getGatewayId();
        gatewayIdEditText.setText(
                TextUtils.isEmpty(gatewayId)
                        ? getString(R.string.default_gateway_id) + "-" +
                        UUID.randomUUID().toString().substring(0, 4)
                        : gatewayId
        );

        String refreshToken = prefs.getRefreshToken();
        refreshTokenEditText.setText(
                TextUtils.isEmpty(refreshToken)
                        ? ""
                        : refreshToken
        );
    }

    private void resetErrors() {
        serverUrlEditTextParent.setError(null);
        gatewayIdEditTextParent.setError(null);
        refreshTokenEditTextParent.setError(null);
    }

    private boolean validateValues() {
        resetErrors();

        final String serverUrl = serverUrlEditText.getText().toString();
        final String gatewayId = gatewayIdEditText.getText().toString();
        final String refreshToken = refreshTokenEditText.getText().toString();

        if (TextUtils.isEmpty(serverUrl)) {
            serverUrlEditTextParent.setError(getString(R.string.error_message_empty_server_url));
            serverUrlEditText.requestFocus();
        } else if (TextUtils.isEmpty(gatewayId)) {
            gatewayIdEditTextParent.setError(getString(R.string.error_message_empty_gateway_id));
            gatewayIdEditText.requestFocus();
        } else if (TextUtils.isEmpty(refreshToken)) {
            refreshTokenEditTextParent.setError(getString(R.string.error_message_empty_refresh_token));
            refreshTokenEditText.requestFocus();
        } else {
            return true;
        }

        return false;
    }

    private void saveValues() {
        String serverUrl = serverUrlEditText.getText().toString().trim();
        String gatewayId = gatewayIdEditText.getText().toString().trim();
        String refreshToken = refreshTokenEditText.getText().toString().trim();

        prefs.setRefreshTokenSync(refreshToken);
        prefs.setServerUrlSync(serverUrl);
        prefs.setGatewayIdSync(gatewayId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bleInitializer.onStart();
    }

    @Override
    protected void onStop() {
        bleInitializer.onStop();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(false);
            } else {
                //Request Location Permission
            }
        } else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(false);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        CircleOptions options = new CircleOptions();
        options.center(latLng);
        options.strokeColor(Color.argb(80, 102, 255, 102));
        options.fillColor(Color.argb(50, 102, 255, 102));
        options.strokeWidth(1f);
        options.radius(7000);
        mCurrLocationMarker = mGoogleMap.addCircle(options);
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

}
