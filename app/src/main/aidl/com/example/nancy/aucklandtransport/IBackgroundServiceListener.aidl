// IBackgroundServiceListener.aidl
package com.example.nancy.aucklandtransport;

// Declare any non-default types here with import statements

interface IBackgroundServiceListener {

    void handleGPSUpdate(double lat, double lon, float angle);

    void locationDiscovered(double lat, double lon);
    void addressDiscovered(String address);
}
