package com.qz.tracker.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.qz.tracker.BuildConfig;
import com.qz.tracker.R;
import com.qz.tracker.activity.GpsActivity;
import com.qz.tracker.service.AppConstants;
import com.qz.tracker.service.LocationServicev2;

public class MainFragmentv2 extends Fragment {

    private final String TAG = MainFragmentv2.class.getSimpleName();

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private MainViewModel mViewModel;
    private View containerView;
    private Button button;
    private Button gpsUpdate;
    private WebView webView;
    private boolean mRequestingLocationUpdates;
    private MyReceiver myReceiver;
    private JsInterface mJsInterface;

    public static MainFragmentv2 newInstance() {
        return new MainFragmentv2();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (containerView != null) {
            ViewParent parent = containerView.getParent();
            if (parent != null)
                ((ViewGroup) parent).removeView(containerView);
            return containerView;
        }
        containerView = inflater.inflate(R.layout.main_fragment, container, false);

        initComponent();

        initListener();

        return containerView;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initComponent() {

        button = containerView.findViewById(R.id.button);
        gpsUpdate = containerView.findViewById(R.id.gps_update);
        webView = containerView.findViewById(R.id.webView);

        myReceiver = new MyReceiver();
        mJsInterface = new JsInterface(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        webView.addJavascriptInterface(mJsInterface, "QZ");
        WebView.setWebContentsDebuggingEnabled(true);
        webView.loadUrl("file:///android_asset/map/index.html");
    }

    private void webViewLoadData(Object data){
        Gson gson = new Gson();
        String json = gson.toJson(data);
        webViewLoadData(json);
    }

    private void webViewLoadData(String json){
        webView.loadUrl("javascript:loadData(" + json + ")");
    }

    private void initListener() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mRequestingLocationUpdates) {

                    startLocationUpdates();

                } else {

                    stopLocationUpdates();

                }
            }
        });

        gpsUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainFragmentv2.this.getContext(), GpsActivity.class));
            }
        });

    }

    /**
     * Start record location.
     */
    private void startLocationUpdates() {

        if( !checkPermissions() ){
            requestPermissions();
            return;
        }

        // update state and ui.
        mRequestingLocationUpdates = true;
        button.setText(R.string.stop_record);

        Intent intent = new Intent(getActivity(), LocationServicev2.class);
        intent.setAction(AppConstants.ACTION_LOCATION_FETCH_START);
        getActivity().startService(intent);
    }

    /**
     * Stop record location.
     */
    private void stopLocationUpdates() {

        // update state and ui.
        mRequestingLocationUpdates = false;
        button.setText(R.string.start_record);

        Intent intent = new Intent(getActivity(), LocationServicev2.class);
        intent.setAction(AppConstants.ACTION_LOCATION_FETCH_STOP);
        getActivity().startService(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(myReceiver,
                new IntentFilter(AppConstants.ACTION_BROADCAST));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            } else {

                Intent intent = new Intent();
                intent.setAction(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package",
                        BuildConfig.APPLICATION_ID, null);
                intent.setData(uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(AppConstants.EXTRA_LOCATION);

            if (location != null) {

                Log.d(TAG, "MainFragment receive new location:" + location.toString());

                webViewLoadData("{type: 'currentLocation', lat: " + location.getLatitude() + ", lon: " + location.getLongitude() + "}");

            }
        }
    }

}
