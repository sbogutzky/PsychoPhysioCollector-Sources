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

/**
 * Created by Jan Schrader on 01.03.16.
 */
public class InternalSensorManager  implements SensorEventListener {

    private MainActivity activity;
    private static final String TAG = "InternalSensorManager";
    private String directoryName;
    private File root;

    private int maxValueCount;

    private SensorManager sensorManager;
    private android.hardware.Sensor accelerometer;
    private android.hardware.Sensor gyroscope;
    private Sensor linearAccelerationSensor;
    private int sensorDataDelay = 20000; // ca. 50 Hz

    private Double[][] buffer;
    private Double[][] buffer0;
    private Double[][] buffer1;

    private Double[][] accelerometerValues;
    private int accelerometerValueCount;
    private Double[][] gyroscopeValues;
    private int gyroscopeValueCount;
    private Double[][] linearAccelerationValues;
    private int linearAccelerationValueCount;

    private long gyroscopeEventStartTimestamp;
    private long gyroscopeStartTimestamp;
    private long accelerometerEventStartTimestamp;
    private long accelerometerStartTimestamp;
    private long linearAccelerationSensorEventStartTimestamp;
    private long linearAccelerationSensorStartTimestamp;

    public InternalSensorManager(String directoryName, File root, int maxValueCount, MainActivity mainActivity) {
        this.activity = mainActivity;
        this.directoryName = directoryName;
        this.root = root;

        this.maxValueCount = maxValueCount;

        this.gyroscopeEventStartTimestamp = 0L;
        this.accelerometerEventStartTimestamp = 0L;
        this.linearAccelerationSensorEventStartTimestamp = 0L;

        this.gyroscopeValues = new Double[maxValueCount][4];
        this.accelerometerValues = new Double[maxValueCount][4];
        this.linearAccelerationValues = new Double[maxValueCount][4];

        sensorManager = (SensorManager) this.activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        this.linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void startStreaming() {
        writeFileHeader();
        sensorManager.registerListener(this, accelerometer, sensorDataDelay);
        sensorManager.registerListener(this, gyroscope, sensorDataDelay);
        sensorManager.registerListener(this, linearAccelerationSensor, sensorDataDelay);
    }

    public void stopStreaming() {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {

            if (this.accelerometerEventStartTimestamp == 0L) {
                this.accelerometerEventStartTimestamp = event.timestamp; // Nanos
                this.accelerometerStartTimestamp = System.currentTimeMillis() - activity.getStartTimestamp(); // Millis
            }
            double relativeTimestamp = this.accelerometerStartTimestamp + (event.timestamp - this.accelerometerEventStartTimestamp) / 1000000.0;

            accelerometerValues[accelerometerValueCount][0] = relativeTimestamp;
            accelerometerValues[accelerometerValueCount][1] = Double.parseDouble(Float.toString(event.values[0]));
            accelerometerValues[accelerometerValueCount][2] = Double.parseDouble(Float.toString(event.values[1]));
            accelerometerValues[accelerometerValueCount][3] = Double.parseDouble(Float.toString(event.values[2]));

            accelerometerValueCount++;
            if(accelerometerValueCount > accelerometerValues.length - 2) {
                //save
                Log.v(TAG, "save acc");
                accelerometerValueCount = 0;
                this.accelerometerValues = new Double[this.maxValueCount][4];
            }
        }
        if (event.sensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE) {
            if (this.gyroscopeEventStartTimestamp == 0L) {
                this.gyroscopeEventStartTimestamp = event.timestamp; // Nanos
                this.gyroscopeStartTimestamp = System.currentTimeMillis() - activity.getStartTimestamp(); // Millis
            }
            double relativeTimestamp = this.gyroscopeStartTimestamp + (event.timestamp - this.gyroscopeEventStartTimestamp) / 1000000.0;

            gyroscopeValues[gyroscopeValueCount][0] = relativeTimestamp;
            gyroscopeValues[gyroscopeValueCount][1] = Double.parseDouble(Float.toString((float) (event.values[0] * 180.0 / Math.PI)));
            gyroscopeValues[gyroscopeValueCount][2] = Double.parseDouble(Float.toString((float) (event.values[1] * 180.0 / Math.PI)));
            gyroscopeValues[gyroscopeValueCount][3] = Double.parseDouble(Float.toString((float) (event.values[2] * 180.0 / Math.PI)));

            gyroscopeValueCount++;
            if(gyroscopeValueCount > gyroscopeValues.length - 2) {
                //save
                Log.v(TAG, "save gyro");
                gyroscopeValueCount = 0;
                this.gyroscopeValues = new Double[this.maxValueCount][4];
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            if (this.linearAccelerationSensorEventStartTimestamp == 0L) {
                this.linearAccelerationSensorEventStartTimestamp = event.timestamp; // Nanos
                this.linearAccelerationSensorStartTimestamp = System.currentTimeMillis() - activity.getStartTimestamp(); // Millis
            }
            double relativeTimestamp = this.linearAccelerationSensorStartTimestamp + (event.timestamp - this.linearAccelerationSensorEventStartTimestamp) / 1000000.0;

            linearAccelerationValues[linearAccelerationValueCount][0] = relativeTimestamp;
            linearAccelerationValues[linearAccelerationValueCount][1] = Double.parseDouble(Float.toString(event.values[0]));
            linearAccelerationValues[linearAccelerationValueCount][2] = Double.parseDouble(Float.toString(event.values[1]));
            linearAccelerationValues[linearAccelerationValueCount][3] = Double.parseDouble(Float.toString(event.values[2]));

            linearAccelerationValueCount++;
            if(linearAccelerationValueCount > linearAccelerationValues.length - 2) {
                //save
                Log.v(TAG, "save linear");
                linearAccelerationValueCount = 0;
                this.linearAccelerationValues = new Double[this.maxValueCount][4];
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
}
