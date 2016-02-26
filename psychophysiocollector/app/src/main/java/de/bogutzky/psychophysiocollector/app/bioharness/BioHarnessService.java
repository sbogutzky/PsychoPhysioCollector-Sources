package de.bogutzky.psychophysiocollector.app.bioharness;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import de.bogutzky.psychophysiocollector.app.R;
import zephyr.android.BioHarnessBT.BTClient;

public class BioHarnessService extends Service {
    //private static final String TAG = "BioHarnessService";
    private final IBinder binder = new LocalBinder();

    private BTClient btClient;

    public boolean isBioHarnessConnected() {
        return bioHarnessConnected;
    }

    private boolean bioHarnessConnected = false;

    private BioHarnessListener bioHarnessListener;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, R.string.bio_harness_service_started, Toast.LENGTH_LONG).show();
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

    public void connectBioHarness(String bluetoothAddress, BioHarnessHandler bioHarnessHandler) {
        btClient = new BTClient(BluetoothAdapter.getDefaultAdapter(), bluetoothAddress);
        bioHarnessListener = new BioHarnessListener(bioHarnessHandler, bioHarnessHandler);
        btClient.addConnectedEventListener(bioHarnessListener);
        if(btClient.IsConnected()) {
            btClient.start();
            bioHarnessConnected = true;
        }
    }

    public void disconnectBioHarness() {
        if(btClient != null && bioHarnessListener != null) {
            btClient.removeConnectedEventListener(bioHarnessListener);
            btClient.Close();
            bioHarnessConnected = false;
        }
    }

    public void startStreamingBioHarness(long startTimestamp) { // File root, String directoryName,
        if (bioHarnessConnected) {
            //bioHarnessListener.bioHarnessHandler.setRoot(root);
            //bioHarnessListener.bioHarnessHandler.setDirectoryName(directoryName);
            bioHarnessListener.bioHarnessHandler.setStartTimestamp(startTimestamp);
            bioHarnessListener.bioHarnessHandler.startStreaming();
        }
    }

    public void stopStreamingBioHarness() {
        if(btClient != null && bioHarnessListener != null) {
            bioHarnessListener.bioHarnessHandler.stopStreaming();
            btClient.removeConnectedEventListener(bioHarnessListener);
        }
    }
}