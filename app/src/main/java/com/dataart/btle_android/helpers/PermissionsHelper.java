package com.dataart.btle_android.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.dataart.btle_android.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import timber.log.Timber;

/**
 * Created by Constantine Mars on 12/6/15.
 * Android M with Google Play Services requires Location enabled before starting BLE devices discovery
 * This helper implements turning on Location services programmatically
 */
class PermissionsHelper implements ResultCallback<LocationSettingsResult>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final int REQUEST_CHECK_SETTINGS = 100;
    private LocationSettingsRequest mLocationSettingsRequest;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationEnabledListener listener;
    private Activity activity;
    private boolean waitForResume = false;

    PermissionsHelper(LocationEnabledListener listener, Activity activity) {
        this.listener = listener;
        this.activity = activity;
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    public void start() {
        mGoogleApiClient.connect();
    }

    protected void stop() {
        mGoogleApiClient.disconnect();
    }

    /**
     * Build request to enable Location
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Location request is necessary for defining which exactly permissions must be enabled
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                log(R.string.location_satisfied);
                listener.onLocationEnabled();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                log(R.string.location_unsatisfied);

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                    status.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    log(R.string.intent_not_started);
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                log(R.string.location_cant_be_fixed);
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        log(R.string.location_changes_applied);
                        listener.onLocationEnabled();
                        break;
                    case Activity.RESULT_CANCELED:
                        log(R.string.loccation_changes_not_applied);
                        break;
                }
                break;
        }
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        return gps_enabled || network_enabled;
    }

    void checkLocationEnabled() {
        if (isLocationEnabled()) {
            listener.onLocationEnabled();
            return;
        }

//        If location isn't enabled - check whether we can call Google Play Services or user to manually
//        switch Location

        final int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity.getApplicationContext());
        if (status == ConnectionResult.SUCCESS) {
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(
                            mGoogleApiClient,
                            mLocationSettingsRequest
                    );
            result.setResultCallback(this);
        } else {
//            If no services available - the only thing we can do is to
//            ask user to switch Location manually
            activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            waitForResume = true;
        }
    }

    void resume() {
        if (waitForResume) {
            checkLocationEnabled();
        }
    }

    private String getString(int id) {
        return activity.getString(id);
    }

    private void log(int id) {
        Timber.i(getString(id));
    }

    interface LocationEnabledListener {
        void onLocationEnabled();
    }
}

