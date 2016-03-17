/**
 * The MIT License (MIT)
 Copyright (c) 2016 Simon Bogutzky, Jan Christoph Schrader

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included
 in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
            bioHarnessListener.bioHarnessHandler.setGraphView(graphView);
        }
    }

    public void stopDataVisualization() {
        if (bioHarnessConnected) {
            bioHarnessListener.bioHarnessHandler.setGraphView(null);
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

    public void startStreamingBioHarness(File root, String directoryName, long startTimestamp) { // File root, String directoryName,
        if (bioHarnessConnected) {
            bioHarnessListener.bioHarnessHandler.setRoot(root);
            bioHarnessListener.bioHarnessHandler.setDirectoryName(directoryName);
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