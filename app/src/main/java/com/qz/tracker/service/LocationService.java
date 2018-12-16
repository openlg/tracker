package com.qz.tracker.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.qz.tracker.R;
import com.qz.tracker.activity.MainActivity;

public class LocationService extends Service {

    private static final long NO_FALLBACK = 0;
    private final String TAG = LocationService.class.getSimpleName();
    private final int NOTIFICATION_ID = 101;

    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mCurrentLocation;

    private boolean mRequestingLocationUpdates;
    private NotificationManager mNotificationManager;
    private NotificationChannel mNotificationChannel;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Location Service created.");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationChannel = new NotificationChannel("com.qz.tracker.channel.id", getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
        mNotificationManager.createNotificationChannel(mNotificationChannel);

        createLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);

        Log.i(TAG,"locationService start command "+ intent.getAction());

        // start fetch location
        if( AppConstants.ACTION_LOCATION_FETCH_START.equals(intent.getAction()) ){

            LocationRequest mLocationRequest = intent.getParcelableExtra("location.request");

            requestLocationUpdates(mLocationRequest);

            return super.onStartCommand(intent, flags, startId);

        }

        // stop fetch location
        else if( AppConstants.ACTION_LOCATION_FETCH_STOP.equals(intent.getAction()) )
            stopLocationUpdates();

        return START_NOT_STICKY;
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates(LocationRequest mLocationRequest) {

        if (mLocationRequest == null)
            throw new IllegalStateException("Location request can't be null");

        if( mRequestingLocationUpdates ){
            Log.d(TAG, "requestLocationUpdates: location is updates.");
            return;
        }

        mRequestingLocationUpdates = true;
        Task<Void> task = mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());

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

        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mRequestingLocationUpdates = false;
        stopForeground(true);
        stopSelf();
    }

    private void onNewLocation(Location location){
        Log.i(TAG, "New location: " + location);

        mCurrentLocation = location;

        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(AppConstants.ACTION_BROADCAST);
        intent.putExtra(AppConstants.EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service.
        //if (serviceIsRunningInForeground(this)) {
            //mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        ///}
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                onNewLocation(locationResult.getLastLocation());
            }
        };
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



    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }
}
