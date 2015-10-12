package de.bogutzky.data_collector.app;

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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import zephyr.android.BioHarnessBT.BTClient;

public class SensorService extends Service {
    private static final String TAG = "MyService";
    public Shimmer shimmerDevice1 = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private final IBinder mBinder = new LocalBinder();
    public HashMap<String, Object> mMultiShimmer = new HashMap<String, Object>(7);
    private String[][] mActivatedSensorNamesArray = new String[7][3];
    private boolean[][] mTriggerResting = new boolean[7][3];
    private boolean[][] mTriggerHitDetected = new boolean[7][3];
    private double[][] mMaxData = new double[7][3];
    DescriptiveStatistics mDataBuffer = new DescriptiveStatistics(5);
    private NotificationManager mNM;
    private BTClient _bt;
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
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    public void disconnectBioHarness() {
        if(_bt != null && bioHarnessConnectedListener != null)
            _bt.removeConnectedEventListener(bioHarnessConnectedListener);
    }

    public class LocalBinder extends Binder {
        public SensorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SensorService.this;
        }
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
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
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();

        Log.d(TAG, "onStart");

    }

    public void connectBioHarness(MainActivity.HarnessHandler harnessHandler, String bhMacID) {
        _bt = new BTClient(BluetoothAdapter.getDefaultAdapter(), bhMacID);
        bioHarnessConnectedListener = new BioHarnessConnectedListener(harnessHandler, harnessHandler);
        _bt.addConnectedEventListener(bioHarnessConnectedListener);
        if(_bt.IsConnected()) {
            _bt.start();
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

    public void onStop() {
        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }
    }

    public void toggleAllLEDS() {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.toggleLed();
            }
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

    public void startStreamingAllDevices() {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.startStreaming();
            }
        }
    }

    public void startStreamingAllDevicesGetSensorNames(File root) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            ((MainActivity.ShimmerHandler)stemp.mHandler).setRoot(root);
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.startStreaming();
                int mPosition = Integer.parseInt(stemp.getDeviceName());
                long mEnabledSensors = stemp.getEnabledSensors();
                ArrayList<String> fields = new ArrayList<String>();
                fields.add("Timestamp");
                if ((mEnabledSensors & Shimmer.SENSOR_ACCEL) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "Accelerometer X";
                    mActivatedSensorNamesArray[mPosition][1] = "Accelerometer Y";
                    mActivatedSensorNamesArray[mPosition][2] = "Accelerometer Z";
                    fields.add("Accelerometer X");
                    fields.add("Accelerometer Y");
                    fields.add("Accelerometer Z");
                } else if ((mEnabledSensors & Shimmer.SENSOR_GYRO) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "Gyroscope X";
                    mActivatedSensorNamesArray[mPosition][1] = "Gyroscope Y";
                    mActivatedSensorNamesArray[mPosition][2] = "Gyroscope Z";
                    fields.add("Gyroscope X");
                    fields.add("Gyroscope Y");
                    fields.add("Gyroscope Z");
                } else if ((mEnabledSensors & Shimmer.SENSOR_MAG) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "Magnetometer X";
                    mActivatedSensorNamesArray[mPosition][1] = "Magnetometer Y";
                    mActivatedSensorNamesArray[mPosition][2] = "Magnetometer Z";
                    fields.add("Magnetometer X");
                    fields.add("Magnetometer Y");
                    fields.add("Magnetometer Z");
                } else if ((mEnabledSensors & Shimmer.SENSOR_GSR) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "GSR";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                    fields.add("GSR");
                } else if ((mEnabledSensors & Shimmer.SENSOR_EMG) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "EMG";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                    fields.add("EMG");
                } else if ((mEnabledSensors & Shimmer.SENSOR_ECG) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "ECG RA-LL";
                    mActivatedSensorNamesArray[mPosition][1] = "ECG LA-LL";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                    fields.add("ECG RA-LL");
                    fields.add("ECG LA-LL");
                } else if ((mEnabledSensors & Shimmer.SENSOR_HEART) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "Heart Rate";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                    fields.add("Heart Rate");
                } else if ((mEnabledSensors & Shimmer.SENSOR_EXP_BOARD_A0) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "ExpBoard A0";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                    fields.add("ExpBoard A0");
                } else if ((mEnabledSensors & Shimmer.SENSOR_EXP_BOARD_A7) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "ExpBoard A7";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                    fields.add("ExpBoard A7");
                }

                fields.add("SystemTimestamp");
                String[] handlerFields = fields.toArray(new String[fields.size()]);
                ((MainActivity.ShimmerHandler) stemp.mHandler).setFields(handlerFields);
            }
        }
    }

    public void setAllSampingRate(double samplingRate) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeSamplingRate(samplingRate);
            }
        }
    }

    public void setAllAccelRange(int accelRange) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeAccelRange(accelRange);
            }
        }
    }

    public void setAllGSRRange(int gsrRange) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeGSRRange(gsrRange);
            }
        }
    }

    public void setAllEnabledSensors(int enabledSensors) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeEnabledSensors(enabledSensors);
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

    public int getShimmerState(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int status = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                status = stemp.getShimmerState();
                Log.d("ShimmerState", Integer.toString(status));
            }
        }
        return status;

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

    public void startStreaming(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.startStreaming();
                int mPosition = Integer.parseInt(stemp.getDeviceName());
                long mEnabledSensors = stemp.getEnabledSensors();
                if ((mEnabledSensors & Shimmer.SENSOR_ACCEL) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "Accelerometer X";
                    mActivatedSensorNamesArray[mPosition][1] = "Accelerometer Y";
                    mActivatedSensorNamesArray[mPosition][2] = "Accelerometer Z";
                } else if ((mEnabledSensors & Shimmer.SENSOR_GYRO) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "Gyroscope X";
                    mActivatedSensorNamesArray[mPosition][1] = "Gyroscope Y";
                    mActivatedSensorNamesArray[mPosition][2] = "Gyroscope Z";
                } else if ((mEnabledSensors & Shimmer.SENSOR_MAG) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "Magnetometer X";
                    mActivatedSensorNamesArray[mPosition][1] = "Magnetometer Y";
                    mActivatedSensorNamesArray[mPosition][2] = "Magnetometer Z";
                } else if ((mEnabledSensors & Shimmer.SENSOR_GSR) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "GSR";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                } else if ((mEnabledSensors & Shimmer.SENSOR_EMG) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "EMG";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                } else if ((mEnabledSensors & Shimmer.SENSOR_ECG) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "ECG RA-LL";
                    mActivatedSensorNamesArray[mPosition][1] = "ECG LA-LL";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                } else if ((mEnabledSensors & Shimmer.SENSOR_HEART) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "Heart Rate";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                } else if ((mEnabledSensors & Shimmer.SENSOR_EXP_BOARD_A0) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "ExpBoard A0";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                } else if ((mEnabledSensors & Shimmer.SENSOR_EXP_BOARD_A7) != 0) {
                    mActivatedSensorNamesArray[mPosition][0] = "ExpBoard A7";
                    mActivatedSensorNamesArray[mPosition][1] = "";
                    mActivatedSensorNamesArray[mPosition][2] = "";
                }


            }
        }
    }

    public void stopStreaming(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                    stemp.stopStreaming();
                }
            }
        }
    }

    public void disconnectShimmer(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.stop();

            }
        }

        mMultiShimmer.remove(bluetoothAddress);

    }

    public boolean DevicesConnected(String bluetoothAddress) {
        boolean deviceConnected = false;
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                deviceConnected = true;
            }
        }
        return deviceConnected;
    }

    public boolean DeviceIsStreaming(String bluetoothAddress) {
        boolean deviceStreaming = false;
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getStreamingStatus() == true && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                deviceStreaming = true;
            }
        }
        return deviceStreaming;
    }

    public boolean GetInstructionStatus(String bluetoothAddress) {
        boolean instructionStatus = false;
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                instructionStatus = stemp.getInstructionStatus();
            }
        }
        return instructionStatus;
    }

    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Service läuft";

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.shimmer_icon, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "Shimmer Service",
                text, contentIntent);

        // Send the notification.
        mNM.notify(1233, notification);
    }


}