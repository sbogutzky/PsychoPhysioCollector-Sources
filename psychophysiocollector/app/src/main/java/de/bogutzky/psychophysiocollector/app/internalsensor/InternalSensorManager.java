package de.bogutzky.psychophysiocollector.app.internalsensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.bogutzky.psychophysiocollector.app.MainActivity;
import de.bogutzky.psychophysiocollector.app.R;
import de.bogutzky.psychophysiocollector.app.data.management.WriteDataTask;
import de.bogutzky.psychophysiocollector.app.data.management.WriteDataTaskParams;

public class InternalSensorManager implements SensorEventListener {

    private MainActivity activity;
    private static final String TAG = "InternalSensorManager";
    private static final int SENSOR_DATA_DELAY = 10000; // ca. 100 Hz

    public static final int ACCELEROMETER_STORE_ID = 0;
    public static final int GYROSCOPE_STORE_ID = 1;
    public static final int LINEAR_ACCELERATION_SENSOR_STORE_ID = 2;

    private File root;

    private int[] maxBatchCounts;
    private int[] batchRowCounts = {0, 0, 0, 0};

    private SensorManager sensorManager;
    private android.hardware.Sensor accelerometer;
    private android.hardware.Sensor gyroscope;
    private Sensor linearAccelerationSensor;

    private Double[][][] buffer;
    private Double[][][] buffer0;
    private Double[][][] buffer1;

    private long gyroscopeEventStartTimestamp;
    private long gyroscopeStartTimestamp;
    private long accelerometerEventStartTimestamp;
    private long accelerometerStartTimestamp;
    private long linearAccelerationSensorEventStartTimestamp;
    private long linearAccelerationSensorStartTimestamp;

    public InternalSensorManager(File root, int[] maxBatchCounts, MainActivity mainActivity) {
        this.activity = mainActivity;
        this.root = root;

        this.maxBatchCounts = maxBatchCounts;

        this.gyroscopeEventStartTimestamp = 0L;
        this.accelerometerEventStartTimestamp = 0L;
        this.linearAccelerationSensorEventStartTimestamp = 0L;

        sensorManager = (SensorManager) this.activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        this.linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void startStreaming() {
        this.buffer0 = new Double[3][][];
        this.buffer1 = new Double[3][][];
        this.buffer = new Double[3][][];

        this.buffer0[ACCELEROMETER_STORE_ID] = new Double[this.maxBatchCounts[ACCELEROMETER_STORE_ID]][4];
        this.buffer1[ACCELEROMETER_STORE_ID] = new Double[this.maxBatchCounts[ACCELEROMETER_STORE_ID]][4];
        this.buffer[ACCELEROMETER_STORE_ID] = buffer0[ACCELEROMETER_STORE_ID];

        this.buffer0[GYROSCOPE_STORE_ID] = new Double[this.maxBatchCounts[GYROSCOPE_STORE_ID]][4];
        this.buffer1[GYROSCOPE_STORE_ID] = new Double[this.maxBatchCounts[GYROSCOPE_STORE_ID]][4];
        this.buffer[GYROSCOPE_STORE_ID] = buffer0[GYROSCOPE_STORE_ID];

        this.buffer0[LINEAR_ACCELERATION_SENSOR_STORE_ID] = new Double[this.maxBatchCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID]][4];
        this.buffer1[LINEAR_ACCELERATION_SENSOR_STORE_ID] = new Double[this.maxBatchCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID]][4];
        this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID] = buffer0[LINEAR_ACCELERATION_SENSOR_STORE_ID];

        writeFileHeader();
        sensorManager.registerListener(this, accelerometer, SENSOR_DATA_DELAY);
        sensorManager.registerListener(this, gyroscope, SENSOR_DATA_DELAY);
        sensorManager.registerListener(this, linearAccelerationSensor, SENSOR_DATA_DELAY);
    }

    public void stopStreaming() {
        sensorManager.unregisterListener(this);
        String footerComments = activity.getFooterComments();
        writeValues(activity.getString(R.string.file_name_acceleration), this.buffer[ACCELEROMETER_STORE_ID], 4, batchRowCounts[ACCELEROMETER_STORE_ID], footerComments);
        writeValues(activity.getString(R.string.file_name_angular_velocity), this.buffer[GYROSCOPE_STORE_ID], 4, batchRowCounts[GYROSCOPE_STORE_ID], footerComments);
        writeValues(activity.getString(R.string.file_name_linear_acceleration), this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID], 4, batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID], footerComments);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {

            if (this.accelerometerEventStartTimestamp == 0L) {
                this.accelerometerEventStartTimestamp = event.timestamp; // Nanos
                this.accelerometerStartTimestamp = System.currentTimeMillis() - activity.getStartTimestamp(); // Millis
            }
            double relativeTimestamp = this.accelerometerStartTimestamp + (event.timestamp - this.accelerometerEventStartTimestamp) / 1000000.0;
            this.buffer[ACCELEROMETER_STORE_ID][batchRowCounts[ACCELEROMETER_STORE_ID]][0] = relativeTimestamp;
            this.buffer[ACCELEROMETER_STORE_ID][batchRowCounts[ACCELEROMETER_STORE_ID]][1] = Double.parseDouble(Float.toString(event.values[0]));
            this.buffer[ACCELEROMETER_STORE_ID][batchRowCounts[ACCELEROMETER_STORE_ID]][2] = Double.parseDouble(Float.toString(event.values[1]));
            this.buffer[ACCELEROMETER_STORE_ID][batchRowCounts[ACCELEROMETER_STORE_ID]][3] = Double.parseDouble(Float.toString(event.values[2]));

            batchRowCounts[ACCELEROMETER_STORE_ID]++;

            if (batchRowCounts[ACCELEROMETER_STORE_ID] == maxBatchCounts[ACCELEROMETER_STORE_ID]) {
                writeValues(activity.getString(R.string.file_name_acceleration), this.buffer[ACCELEROMETER_STORE_ID], 4, batchRowCounts[ACCELEROMETER_STORE_ID], null); // "# BatchRowCount: " + batchRowCounts
                batchRowCounts[ACCELEROMETER_STORE_ID] = 0;
                if (this.buffer[ACCELEROMETER_STORE_ID] == this.buffer0[ACCELEROMETER_STORE_ID]) {
                    this.buffer[ACCELEROMETER_STORE_ID] = this.buffer1[ACCELEROMETER_STORE_ID];
                } else {
                    this.buffer[ACCELEROMETER_STORE_ID] = this.buffer0[ACCELEROMETER_STORE_ID];
                }
            }

        }
        if (event.sensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE) {
            if (this.gyroscopeEventStartTimestamp == 0L) {
                this.gyroscopeEventStartTimestamp = event.timestamp; // Nanos
                this.gyroscopeStartTimestamp = System.currentTimeMillis() - activity.getStartTimestamp(); // Millis
            }
            double relativeTimestamp = this.gyroscopeStartTimestamp + (event.timestamp - this.gyroscopeEventStartTimestamp) / 1000000.0;

            this.buffer[GYROSCOPE_STORE_ID][batchRowCounts[GYROSCOPE_STORE_ID]][0] = relativeTimestamp;
            this.buffer[GYROSCOPE_STORE_ID][batchRowCounts[GYROSCOPE_STORE_ID]][1] = Double.parseDouble(Float.toString((float) (event.values[0] * 180.0 / Math.PI)));
            this.buffer[GYROSCOPE_STORE_ID][batchRowCounts[GYROSCOPE_STORE_ID]][2] = Double.parseDouble(Float.toString((float) (event.values[1] * 180.0 / Math.PI)));
            this.buffer[GYROSCOPE_STORE_ID][batchRowCounts[GYROSCOPE_STORE_ID]][3] = Double.parseDouble(Float.toString((float) (event.values[2] * 180.0 / Math.PI)));

            batchRowCounts[GYROSCOPE_STORE_ID]++;

            if (batchRowCounts[GYROSCOPE_STORE_ID] == maxBatchCounts[GYROSCOPE_STORE_ID]) {
                writeValues(activity.getString(R.string.file_name_angular_velocity), this.buffer[GYROSCOPE_STORE_ID], 4, batchRowCounts[GYROSCOPE_STORE_ID], null); // "# BatchRowCount: " + batchRowCounts
                batchRowCounts[GYROSCOPE_STORE_ID] = 0;
                if (this.buffer[GYROSCOPE_STORE_ID] == this.buffer0[GYROSCOPE_STORE_ID]) {
                    this.buffer[GYROSCOPE_STORE_ID] = this.buffer1[GYROSCOPE_STORE_ID];
                } else {
                    this.buffer[GYROSCOPE_STORE_ID] = this.buffer0[GYROSCOPE_STORE_ID];
                }
            }

        }
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            if (this.linearAccelerationSensorEventStartTimestamp == 0L) {
                this.linearAccelerationSensorEventStartTimestamp = event.timestamp; // Nanos
                this.linearAccelerationSensorStartTimestamp = System.currentTimeMillis() - activity.getStartTimestamp(); // Millis
            }
            double relativeTimestamp = this.linearAccelerationSensorStartTimestamp + (event.timestamp - this.linearAccelerationSensorEventStartTimestamp) / 1000000.0;

            this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID][batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID]][0] = relativeTimestamp;
            this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID][batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID]][1] = Double.parseDouble(Float.toString(event.values[0]));
            this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID][batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID]][2] = Double.parseDouble(Float.toString(event.values[1]));
            this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID][batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID]][3] = Double.parseDouble(Float.toString(event.values[2]));

            batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID]++;

            if (batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID] == maxBatchCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID]) {
                writeValues(activity.getString(R.string.file_name_linear_acceleration), this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID], 4, batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID], null); // "# BatchRowCount: " + batchRowCounts
                batchRowCounts[LINEAR_ACCELERATION_SENSOR_STORE_ID] = 0;
                if (this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID] == this.buffer0[LINEAR_ACCELERATION_SENSOR_STORE_ID]) {
                    this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID] = this.buffer1[LINEAR_ACCELERATION_SENSOR_STORE_ID];
                } else {
                    this.buffer[LINEAR_ACCELERATION_SENSOR_STORE_ID] = this.buffer0[LINEAR_ACCELERATION_SENSOR_STORE_ID];
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void writeFileHeader() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, activity.getString(R.string.file_name_acceleration)), true));
            String outputString = activity.getHeaderComments();
            outputString += activity.getString(R.string.file_header_timestamp) + "," + activity.getString(R.string.file_header_acceleration_x) + "," + activity.getString(R.string.file_header_acceleration_y) + "," + activity.getString(R.string.file_header_acceleration_z);
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, activity.getString(R.string.file_name_angular_velocity)), true));
            String outputString = activity.getHeaderComments();
            outputString += activity.getString(R.string.file_header_timestamp) + "," + activity.getString(R.string.file_header_angular_velocity_x) + "," + activity.getString(R.string.file_header_angular_velocity_y) + "," + activity.getString(R.string.file_header_angular_velocity_z);
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, activity.getString(R.string.file_name_linear_acceleration)), true));
            String outputString = activity.getHeaderComments();
            outputString += activity.getString(R.string.file_header_timestamp) + "," + activity.getString(R.string.file_header_acceleration_x) + "," + activity.getString(R.string.file_header_acceleration_y) + "," + activity.getString(R.string.file_header_acceleration_z);
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

    }

    private void writeValues(String filename, Double[][] buffer, int fields, int batchRowCount, String batchComments) {
        new WriteDataTask().execute(new WriteDataTaskParams(this.root, filename, buffer, fields, batchRowCount, batchComments));
    }
}
