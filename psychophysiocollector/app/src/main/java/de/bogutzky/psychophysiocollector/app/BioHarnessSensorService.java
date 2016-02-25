package de.bogutzky.psychophysiocollector.app;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuHandler;
import zephyr.android.BioHarnessBT.BTClient;

public class BioHarnessSensorService extends Service {
    private static final String TAG = "BioHarnessService";
    private final IBinder binder = new LocalBinder();

    private BTClient _bt;

    public boolean hasBioHarnessConnected() {
        return bioHarnessConnected;
    }

    private boolean bioHarnessConnected = false;

    public BioHarnessConnectedListener getBioHarnessConnectedListener() {
        return bioHarnessConnectedListener;
    }

    private BioHarnessConnectedListener bioHarnessConnectedListener;


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "BioHarness Service Created", Toast.LENGTH_LONG).show();
    }

    public void disconnectBioHarness() {
        if(_bt != null && bioHarnessConnectedListener != null) {
            _bt.removeConnectedEventListener(bioHarnessConnectedListener);
            _bt.Close();
        }
    }

    public class LocalBinder extends Binder {
        public BioHarnessSensorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BioHarnessSensorService.this;
        }
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Shimmer Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LocalService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    public void connectBioHarness(MainActivity.BioHarnessHandler bioHarnessHandler, String bhMacID) {
        _bt = new BTClient(BluetoothAdapter.getDefaultAdapter(), bhMacID);
        bioHarnessConnectedListener = new BioHarnessConnectedListener(bioHarnessHandler, bioHarnessHandler);
        _bt.addConnectedEventListener(bioHarnessConnectedListener);
        if(_bt.IsConnected()) {
            _bt.start();
            bioHarnessConnected = true;
        }
    }

    public void stopStreamingAllDevices() {

        if(_bt != null && bioHarnessConnectedListener != null)
            _bt.removeConnectedEventListener(bioHarnessConnectedListener);
    }

}