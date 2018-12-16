package com.qz.tracker.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.qz.tracker.R;
import com.qz.tracker.activity.MainActivity;

/**
 * @author lg
 *
 */
public class LocationServicev2 extends Service {

    private static final long NO_FALLBACK = 0;
    private final String TAG = LocationServicev2.class.getSimpleName();
    private final int NOTIFICATION_ID = 101;

    private Location mCurrentLocation;

    // The minimum distance to change Updates in meters
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 5 ; // 5 second

    // Flag for GPS status
    boolean isGPSEnabled = false;

    // Flag for network status
    boolean isNetworkEnabled = false;

    private boolean mRequestingLocationUpdates;
    private NotificationManager mNotificationManager;
    private NotificationChannel mNotificationChannel;
    private LocationManager locationManager;
    private LocationListener mLocationCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Location Service created.");

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationChannel = new NotificationChannel("com.qz.tracker.channel.id", getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
        mNotificationManager.createNotificationChannel(mNotificationChannel);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        createLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);

        Log.i(TAG,"locationService start command "+ intent.getAction());

        // start fetch location
        if( AppConstants.ACTION_LOCATION_FETCH_START.equals(intent.getAction()) ){

            requestLocationUpdates();

            return super.onStartCommand(intent, flags, startId);
        }

        // stop fetch location
        else if( AppConstants.ACTION_LOCATION_FETCH_STOP.equals(intent.getAction()) )
            stopLocationUpdates();

        return START_NOT_STICKY;
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates() {

        if( mRequestingLocationUpdates ){
            Log.d(TAG, "requestLocationUpdates: location is updates.");
            return;
        }

        mRequestingLocationUpdates = true;
        try {

            // Getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // No network provider is enabled
                Log.e(TAG, "No location provider is enabled");
            } else {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                // If GPS enabled, get latitude/longitude using GPS Services
                if (isGPSEnabled) {
                    if (mCurrentLocation == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationCallback);
                        Log.d(TAG, "Use GPS location provider");
                        if (locationManager != null) {
                            mCurrentLocation = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            mLocationCallback.onLocationChanged(mCurrentLocation);
                        }
                    }
                }

                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationCallback);
                    Log.d("GPSTracker", "Use network location provider");
                    if (locationManager != null) {
                        mCurrentLocation = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        mLocationCallback.onLocationChanged(mCurrentLocation);
                    }
                }

            }
        }
        catch (Exception e) {
            Log.e("GPSTracker", "Tracker location fail.", e);
        }

        startForeground(NOTIFICATION_ID, getNotification());
    }

    private Notification getNotification(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, mNotificationChannel.getId());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText("Recording");
        if( mCurrentLocation == null )
            builder.setContentInfo("You're current location is unknow.");
        else
            builder.setContentInfo("You're current location is " + mCurrentLocation.getLongitude() + "," + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getAltitude());
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(false);
        builder.setPriority(NotificationManager.IMPORTANCE_MAX);
        builder.setCategory(Notification.CATEGORY_STATUS);
        builder.setOngoing(true);
        builder.setSound(null);

        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        return builder.build();
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // TODO: remove location update lintener
        mRequestingLocationUpdates = false;
        stopForeground(true);
        stopSelf();
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                onNewLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    private void onNewLocation(Location location){
        Log.i(TAG, "New location: " + location);

        mCurrentLocation = location;

        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(AppConstants.ACTION_BROADCAST);
        intent.putExtra(AppConstants.EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Location Service destroy.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
