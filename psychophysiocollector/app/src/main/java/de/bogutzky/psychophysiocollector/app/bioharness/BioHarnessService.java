package de.bogutzky.psychophysiocollector.app.bioharness;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;

import de.bogutzky.psychophysiocollector.app.GraphView;
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

    public void startDataVisualization(GraphView graphView) {
        if (bioHarnessConnected) {
            bioHarnessListener.bioHarnessHandler1.setGraphView(graphView);
        }
    }

    public void stopDataVisualization() {
        if (bioHarnessConnected) {
            bioHarnessListener.bioHarnessHandler1.setGraphView(null);
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

    public void connectBioHarness(String bluetoothAddress, BioHarnessHandler1 bioHarnessHandler1) {
        btClient = new BTClient(BluetoothAdapter.getDefaultAdapter(), bluetoothAddress);
        bioHarnessListener = new BioHarnessListener(bioHarnessHandler1, bioHarnessHandler1);
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

    public void startStreamingBioHarness(File root, String directoryName, long startTimestamp) { // File root, String directoryName,
        if (bioHarnessConnected) {
            bioHarnessListener.bioHarnessHandler1.setRoot(root);
            bioHarnessListener.bioHarnessHandler1.setDirectoryName(directoryName);
            bioHarnessListener.bioHarnessHandler1.setStartTimestamp(startTimestamp);
            bioHarnessListener.bioHarnessHandler1.startStreaming();
        }
    }

    public void stopStreamingBioHarness() {
        if(btClient != null && bioHarnessListener != null) {
            bioHarnessListener.bioHarnessHandler1.stopStreaming();
            btClient.removeConnectedEventListener(bioHarnessListener);
        }
    }
}