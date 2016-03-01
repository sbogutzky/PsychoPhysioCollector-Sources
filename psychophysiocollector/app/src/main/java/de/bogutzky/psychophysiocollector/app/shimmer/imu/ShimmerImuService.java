package de.bogutzky.psychophysiocollector.app.shimmer.imu;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import de.bogutzky.psychophysiocollector.app.GraphView;
import de.bogutzky.psychophysiocollector.app.R;

public class ShimmerImuService extends Service {
    //private static final String TAG = "ShimmerService";
    private final IBinder binder = new LocalBinder();
    public HashMap<String, Object> shimmerImuMap = new HashMap<>(7);

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, R.string.shimmer_imu_service_started, Toast.LENGTH_LONG).show();
    }

    public class LocalBinder extends Binder {
        public ShimmerImuService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ShimmerImuService.this;
        }
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, R.string.shimmer_imu_service_stopped, Toast.LENGTH_LONG).show();
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            shimmerImu.stop();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void connectShimmerImu(String bluetoothAddress, String selectedDevice, ShimmerImuHandler handler) {
        Shimmer shimmerDevice = new Shimmer(this, handler, selectedDevice, false);
        shimmerImuMap.remove(bluetoothAddress);
        if (shimmerImuMap.get(bluetoothAddress) == null) {
            shimmerImuMap.put(bluetoothAddress, shimmerDevice);
            ((Shimmer) shimmerImuMap.get(bluetoothAddress)).connect(bluetoothAddress, "default");
        }
    }

    public void disconnectAllShimmerImus() {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            shimmerImu.stop();
        }
        shimmerImuMap.clear();
    }

    public void stopStreamingAllShimmerImus() {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;

            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED) {
                shimmerImu.stopStreaming();
            }
        }
    }

    public void startStreamingAllShimmerImus(File root, String directoryName, long startTimestamp) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            ((ShimmerImuHandler) shimmerImu.mHandler).setRoot(root);
            ((ShimmerImuHandler) shimmerImu.mHandler).setDirectoryName(directoryName);
            ((ShimmerImuHandler) shimmerImu.mHandler).setStartTimestamp(startTimestamp);
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED) {
                shimmerImu.startStreaming();

                long mEnabledSensors = shimmerImu.getEnabledSensors();
                ArrayList<String> fields = new ArrayList<>();
                ArrayList<String> header = new ArrayList<>();
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
                /*
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
                */
                if ((mEnabledSensors & Shimmer.SENSOR_ECG) != 0) {
                    fields.add("ECG RA-LL");
                    fields.add("ECG LA-LL");
                    header.add(getString(R.string.file_header_ecg_ra_ll));
                    header.add(getString(R.string.file_header_ecg_la_ll));
                }
                /*
                if ((mEnabledSensors & Shimmer.SENSOR_HEART) != 0) {
                    fields.add("Heart Rate");
                }
                if ((mEnabledSensors & Shimmer.SENSOR_EXP_BOARD_A0) != 0) {
                    fields.add("ExpBoard A0");
                }
                if ((mEnabledSensors & Shimmer.SENSOR_EXP_BOARD_A7) != 0) {
                    fields.add("ExpBoard A7");
                }
                */

                String[] handlerFields = fields.toArray(new String[fields.size()]);
                ((ShimmerImuHandler) shimmerImu.mHandler).setFields(handlerFields);
                String[] handlerHeaders = header.toArray(new String[header.size()]);
                ((ShimmerImuHandler) shimmerImu.mHandler).writeHeader(handlerHeaders);
            }
        }
    }

    public void setEnabledSensors(int enabledSensors, String bluetoothAddress) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                shimmerImu.writeEnabledSensors(enabledSensors);
            }
        }
    }

    public void visualizeData(String bluetoothAddress, GraphView graphView) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                ((ShimmerImuHandler) shimmerImu.mHandler).setGraphView(graphView);
            }
        }
    }

    public void toggleLED(String bluetoothAddress) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                shimmerImu.toggleLed();
            }
        }
    }

    public long getEnabledSensors(String bluetoothAddress) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        Iterator<Object> iterator = shimmerImus.iterator();
        long enabledSensors = 0;
        while (iterator.hasNext()) {
            Shimmer shimmerImu = (Shimmer) iterator.next();
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                enabledSensors = shimmerImu.getEnabledSensors();
            }
        }
        return enabledSensors;
    }

    public void writeSamplingRate(String bluetoothAddress, double samplingRate) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                shimmerImu.writeSamplingRate(samplingRate);
            }
        }
    }

    public void writeAccelerometerRange(String bluetoothAddress, int accelerometerRange) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                shimmerImu.writeAccelRange(accelerometerRange);
            }
        }
    }

    public void writeGyroscopeRange(String bluetoothAddress, int gyroscopeRange) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        for (Object shimmerImu1 : shimmerImus) {
            Shimmer shimmerImu = (Shimmer) shimmerImu1;
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                shimmerImu.writeGyroRange(gyroscopeRange);
            }
        }
    }

    /*
    public void writeGSRRange(String bluetoothAddress, int gsrRange) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        Iterator<Object> iterator = shimmerImus.iterator();
        while (iterator.hasNext()) {
            Shimmer shimmerImu = (Shimmer) iterator.next();
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                shimmerImu.writeGSRRange(gsrRange);
            }
        }
    }
    */

    public double getSamplingRate(String bluetoothAddress) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        Iterator<Object> iterator = shimmerImus.iterator();
        double samplingRate = -1;
        while (iterator.hasNext()) {
            Shimmer shimmerImu = (Shimmer) iterator.next();
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                samplingRate = shimmerImu.getSamplingRate();
            }
        }
        return samplingRate;
    }

    public int getAccelerometerRange(String bluetoothAddress) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        Iterator<Object> iterator = shimmerImus.iterator();
        int range = -1;
        while (iterator.hasNext()) {
            Shimmer shimmerImu = (Shimmer) iterator.next();
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                range = shimmerImu.getAccelRange();
            }
        }
        return range;
    }

    public int getGyroscopeRange(String bluetoothAddress) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        Iterator<Object> iterator = shimmerImus.iterator();
        int range = -1;
        while (iterator.hasNext()) {
            Shimmer shimmerImu = (Shimmer) iterator.next();
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                range = shimmerImu.getGyroRange();
            }
        }
        return range;
    }

    /*
    public int getGSRRange(String bluetoothAddress) {
        Collection<Object> shimmerImus = shimmerImuMap.values();
        Iterator<Object> iterator = shimmerImus.iterator();
        int range = -1;
        while (iterator.hasNext()) {
            Shimmer shimmerImu = (Shimmer) iterator.next();
            if (shimmerImu.getShimmerState() == Shimmer.STATE_CONNECTED && shimmerImu.getBluetoothAddress().equals(bluetoothAddress)) {
                range = shimmerImu.getGSRRange();
            }
        }
        return range;
    }
    */
}