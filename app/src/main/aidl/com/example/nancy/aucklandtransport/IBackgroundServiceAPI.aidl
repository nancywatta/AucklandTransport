// IBackgroundServiceAPI.aidl
package com.example.nancy.aucklandtransport;

import com.example.nancy.aucklandtransport.IBackgroundServiceListener;

interface IBackgroundServiceAPI {

    int requestLastKnownAddress(int getAddress);

    boolean isGPSOn();

    void setRoute(String route);
    void cancelRoute(int notify);

    void addListener(IBackgroundServiceListener l);
    void removeListener(IBackgroundServiceListener l);
}
