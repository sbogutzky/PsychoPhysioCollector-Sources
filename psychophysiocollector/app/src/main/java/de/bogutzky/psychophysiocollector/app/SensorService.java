package de.bogutzky.psychophysiocollector.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import zephyr.android.BioHarnessBT.BTClient;

public class SensorService extends Service {
    private static final String TAG = "Service";
    private final IBinder mBinder = new LocalBinder();
    public HashMap<String, Object> mMultiShimmer = new HashMap<String, Object>(7);
    private boolean[][] mTriggerResting = new boolean[7][3];
    private boolean[][] mTriggerHitDetected = new boolean[7][3];
    private double[][] mMaxData = new double[7][3];
    private NotificationManager notificationManager;
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
        return mBinder;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "My Service Created", Toast.LENGTH_LONG).show();
        for (boolean[] row : mTriggerResting)
            Arrays.fill(row, true);
        for (boolean[] row : mTriggerHitDetected)
            Arrays.fill(row, true);
        for (double[] row : mMaxData)
            Arrays.fill(row, 0);
        Log.d(TAG, "onCreate");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    public void disconnectBioHarness() {
        if(_bt != null && bioHarnessConnectedListener != null) {
            _bt.removeConnectedEventListener(bioHarnessConnectedListener);
            _bt.Close();
        }
    }

    public class LocalBinder extends Binder {
        public SensorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SensorService.this;
        }
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }

    }

    public void disconnectAllDevices() {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }
        mMultiShimmer.clear();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LocalService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onStart");

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

    public void connectShimmer(String bluetoothAddress, String selectedDevice, MainActivity.ShimmerHandler handler) {
        Log.d("Shimmer", "net Connection");
        Shimmer shimmerDevice = new Shimmer(this, handler, selectedDevice, false);
        mMultiShimmer.remove(bluetoothAddress);
        if (mMultiShimmer.get(bluetoothAddress) == null) {
            mMultiShimmer.put(bluetoothAddress, shimmerDevice);
            ((Shimmer) mMultiShimmer.get(bluetoothAddress)).connect(bluetoothAddress, "default");
        }
    }

    public void stopStreamingAllDevices() {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();

            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.stopStreaming();
            }
        }
        if(_bt != null && bioHarnessConnectedListener != null)
            _bt.removeConnectedEventListener(bioHarnessConnectedListener);
    }

    public void startStreamingAllDevicesGetSensorNames(File root, String directoryName) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            ((MainActivity.ShimmerHandler)stemp.mHandler).setRoot(root);
            ((MainActivity.ShimmerHandler)stemp.mHandler).setDirectoryName(directoryName);
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.startStreaming();
                int mPosition = Integer.parseInt(stemp.getDeviceName());
                long mEnabledSensors = stemp.getEnabledSensors();
                ArrayList<String> fields = new ArrayList<String>();
                ArrayList<String> header = new ArrayList<String>();
                fields.add("Timestamp");
                header.add(getApplicationContext().getString(R.string.file_header_timestamp));
                if ((mEnabledSensors & Shimmer.SENSOR_ACCEL) != 0) {
                    fields.add("Accelerometer X");
                    fields.add("Accelerometer Y");
                    fields.add("Accelerometer Z");
                    header.add(getApplicationContext().getString(R.string.file_header_acceleration_x));
                    header.add(getApplicationContext().getString(R.string.file_header_acceleration_y));
                    header.add(getApplicationContext().getString(R.string.file_header_acceleration_z));
                }
                if ((mEnabledSensors & Shimmer.SENSOR_GYRO) != 0) {
                    fields.add("Gyroscope X");
                    fields.add("Gyroscope Y");
                    fields.add("Gyroscope Z");
                    header.add(getApplicationContext().getString(R.string.file_header_angular_velocity_x));
                    header.add(getApplicationContext().getString(R.string.file_header_angular_velocity_y));
                    header.add(getApplicationContext().getString(R.string.file_header_angular_velocity_z));
                }
                if ((mEnabledSensors & Shimmer.SENSOR_MAG) != 0) {
                    fields.add("Magnetometer X");
                    fields.add("Magnetometer Y");
                    fields.add("Magnetometer Z");
                }
                if ((mEnabledSensors & Shimmer.SENSOR_GSR) != 0) {
                    fields.add("GSR");
                }
                if ((mEnabledSensors & Shimmer.SENSOR_EMG) != 0) {
                    fields.add("EMG");
                }
                if ((mEnabledSensors & Shimmer.SENSOR_ECG) != 0) {
                    fields.add("ECG RA-LL");
                    fields.add("ECG LA-LL");
                    header.add(getString(R.string.file_header_ecg_ra_ll));
                    header.add(getString(R.string.file_header_ecg_la_ll));
                }
                if ((mEnabledSensors & Shimmer.SENSOR_HEART) != 0) {
                    fields.add("Heart Rate");
                }
                if ((mEnabledSensors & Shimmer.SENSOR_EXP_BOARD_A0) != 0) {
                    fields.add("ExpBoard A0");
                }
                if ((mEnabledSensors & Shimmer.SENSOR_EXP_BOARD_A7) != 0) {
                    fields.add("ExpBoard A7");
                }

                String[] handlerFields = fields.toArray(new String[fields.size()]);
                ((MainActivity.ShimmerHandler) stemp.mHandler).setFields(handlerFields);
                String[] handlerHeaders = header.toArray(new String[header.size()]);
                ((MainActivity.ShimmerHandler) stemp.mHandler).setHeader(handlerHeaders);
            }
        }
    }

    public void setEnabledSensors(int enabledSensors, String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeEnabledSensors(enabledSensors);
            }
        }
    }

    public void toggleLED(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.toggleLed();
            }
        }
    }

    public long getEnabledSensors(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        long enabledSensors = 0;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                enabledSensors = stemp.getEnabledSensors();
            }
        }
        return enabledSensors;
    }


    public void writeSamplingRate(String bluetoothAddress, double samplingRate) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeSamplingRate(samplingRate);
            }
        }
    }

    public void writeAccelRange(String bluetoothAddress, int accelRange) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeAccelRange(accelRange);
            }
        }
    }

    public void writeGSRRange(String bluetoothAddress, int gsrRange) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeGSRRange(gsrRange);
            }
        }
    }


    public double getSamplingRate(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        double SRate = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                SRate = stemp.getSamplingRate();
            }
        }
        return SRate;
    }

    public int getAccelRange(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int aRange = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                aRange = stemp.getAccelRange();
            }
        }
        return aRange;
    }

    public int getGSRRange(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int gRange = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                gRange = stemp.getGSRRange();
            }
        }
        return gRange;
    }

    public int getEnabledSensorForMac(String adress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int sensor = 0;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getBluetoothAddress().equals(adress)) {
                sensor = stemp.getEnabledSensors();
            }
        }
        return sensor;
    }

    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getString(R.string.service_running);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getString(R.string.service_name),
                text, contentIntent);

        // Send the notification.
        notificationManager.notify(1233, notification);
    }


}