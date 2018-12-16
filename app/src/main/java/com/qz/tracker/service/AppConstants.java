package com.qz.tracker.service;

public interface AppConstants {

    static final String PACKAGE_NAME = AppConstants.class.getName();

    public static final String ACTION_LOCATION_FETCH_START = "location.fetch.start";
    public static final String ACTION_LOCATION_FETCH_STOP = "location.fetch.stop";


    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

}
