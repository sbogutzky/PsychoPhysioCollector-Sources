package de.bogutzky.psychophysiocollector.app.bioharness;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import de.bogutzky.psychophysiocollector.app.BioHarnessConnectedListener;
import de.bogutzky.psychophysiocollector.app.R;
import zephyr.android.BioHarnessBT.BTClient;

public class BioHarnessService extends Service {
    //private static final String TAG = "BioHarnessService";
    private final IBinder binder = new LocalBinder();

    private BTClient btClient;

    public boolean hasBioHarnessConnected() {
        return bioHarnessConnected;
    }

    private boolean bioHarnessConnected = false;

    private BioHarnessConnectedListener bioHarnessConnectedListener;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, R.string.bio_harness_service_started, Toast.LENGTH_LONG).show();
    }

    public void disconnectBioHarness() {
        if(btClient != null && bioHarnessConnectedListener != null) {
            btClient.removeConnectedEventListener(bioHarnessConnectedListener);
            btClient.Close();
        }
    }

    public class LocalBinder extends Binder {
        public BioHarnessService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BioHarnessService.this;
        }
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, R.string.bio_harness_service_stopped, Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void connectBioHarness(BioHarnessHandler bioHarnessHandler, String bhMacID) {
        btClient = new BTClient(BluetoothAdapter.getDefaultAdapter(), bhMacID);
        bioHarnessConnectedListener = new BioHarnessConnectedListener(bioHarnessHandler, bioHarnessHandler);
        btClient.addConnectedEventListener(bioHarnessConnectedListener);
        if(btClient.IsConnected()) {
            btClient.start();
            bioHarnessConnected = true;
        }
    }

    /*
    public void stopStreamingAllDevices() {

        if(btClient != null && bioHarnessConnectedListener != null)
            btClient.removeConnectedEventListener(bioHarnessConnectedListener);
    }*/
}