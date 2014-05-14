package de.bogutzky.data_collector.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends ListActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final int MSG_BLUETOOTH_ADDRESS = 1;
    private static final int TIMER_UPDATE = 1;
    private static final int TIMER_END = 2;
    private static final int SCALE_ITEM_COUNT = 16;
    private static final String ECG_SENSOR_ADDRESS = "00:06:66:46:BD:38";
    private static final int MOTION_SAMPLE_RATE = 56;
    private static final int ECG_SAMPLE_RATE = 512;
    private static final String[] MOTION_FIELDS = {"Timestamp", "Accelerometer X", "Accelerometer Y", "Accelerometer Z", "Gyroscope X", "Gyroscope Y", "Gyroscope Z", "System Timestamp"};
    private static final String[] ECG_FIELDS = {"Timestamp", "Accelerometer X", "Accelerometer Y", "Accelerometer Z", "System Timestamp"};
    //private static final String[] ECG_FIELDS = {"Timestamp", "ECG RA-LL", "ECG LA-LL", "System Timestamp"};
    private HashMap<String, Shimmer> shimmers;
    private HashMap<String, Shimmer> connectedShimmers;
    private HashMap<String, Shimmer> streamingShimmers;
    private boolean loggingEnabled = false;
    private ArrayAdapter adapter;
    private ArrayList<String> bluetoothAddresses;
    private TextView textViewTimer;
    private Handler timerHandler;
    private Thread timerThread;
    private boolean timerThreadShouldContinue = false;
    private double timerCycleInMin;
    private String directoryName;
    private File root;
    private SensorManager sensorManager;
    private android.hardware.Sensor accelerometer;
    private android.hardware.Sensor gyroscope;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Vibrator vibrator;
    private long[] vibratorPatternFeedback = {0, 500, 200, 100, 100, 100, 100, 100};
    private long[] vibratorPatternConnectionLost = {0, 100, 100, 100, 100, 100, 100, 100};
    private String[][] accelerometerValues;
    private int accelerometerValueCount;
    private String[][] gyroscopeValues;
    private int gyroscopeValueCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getBluetoothAddresses());
        setListAdapter(adapter);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        timerCycleInMin = 1;

        textViewTimer = (TextView)findViewById(R.id.text_view_timer);
        textViewTimer.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_sensor) {
            findBluetoothAddress();
            return true;
        }

        if (id == R.id.action_connect) {
            if (this.directoryName == null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH-mm-ss");
                String dateString = simpleDateFormat.format(new Date());
                String timeString = simpleTimeFormat.format(new Date());
                this.directoryName = "DataCollector/" + dateString + "_" + timeString;

                this.root = new File(Environment.getExternalStorageDirectory() + "/" + this.directoryName);
                if (this.root.exists()) {
                    if (this.root.mkdir()) {
                        Log.d(TAG, "Directory " + this.directoryName + " created");
                    }
                }
            }

            connectedAllShimmers();
        }

        if (id == R.id.action_disconnect) {
            disconnectedAllShimmers();
            this.directoryName = null;
        }

        if (id == R.id.action_start_streaming) {
            loggingEnabled = true;
            startAllStreaming();
            startTimerTread();
            startStreamingInternalSensorData();
        }

        if (id == R.id.action_stop_streaming) {
            loggingEnabled = false;
            stopAllStreaming();
            stopTimerThread();
            stopStreamingInternalSensorData();
        }

        if (id == R.id.action_toggle_led) {
            toggleLEDs();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopStreamingInternalSensorData();
        loggingEnabled = false;

        if (timerThread != null) {
            stopTimerThread();
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case MSG_BLUETOOTH_ADDRESS:

                // When DeviceListActivity returns with a device address to connect
                if (resultCode == Activity.RESULT_OK) {
                    String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Log.d(TAG, "Bluetooth Address: " + bluetoothAddress);

                    // Check if the bluetooth address has been previously selected
                    boolean isNewAddress = !getBluetoothAddresses().contains(bluetoothAddress);

                    if (isNewAddress) {
                        addBluetoothAddress(bluetoothAddress);
                    } else {
                        Toast.makeText(this, getString(R.string.device_is_already_in_list), Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    private ArrayList<String> getBluetoothAddresses() {
        if (bluetoothAddresses == null) {
            bluetoothAddresses = new ArrayList<String>();
        }
        return bluetoothAddresses;
    }

    private void addBluetoothAddress(String bluetoothAddress) {
        getBluetoothAddresses().add(bluetoothAddress);
        adapter.notifyDataSetChanged();
    }

    private void findBluetoothAddress() {
        if (!shimmersAreStreaming()) {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, MSG_BLUETOOTH_ADDRESS);
        } else {
            Toast.makeText(this, getString(R.string.ensure_no_device_is_streaming), Toast.LENGTH_LONG).show();
        }
    }

    private HashMap<String, Shimmer> getShimmers() {
        if (shimmers == null) {
            shimmers = new HashMap<String, Shimmer>(4);
        }
        return shimmers;
    }

    private HashMap<String, Shimmer> getConnectedShimmers() {
        if (connectedShimmers == null) {
            connectedShimmers = new HashMap<String, Shimmer>(4);
        }
        return connectedShimmers;
    }

    private HashMap<String, Shimmer> getStreamingShimmers() {
        if (streamingShimmers == null) {
            streamingShimmers = new HashMap<String, Shimmer>(4);
        }
        return streamingShimmers;
    }

    private void connectedAllShimmers() {
        for (String bluetoothAddress : getBluetoothAddresses()) {
            Log.d(TAG, "Connect to: " + bluetoothAddress);
            String btRadioID = bluetoothAddress.replace(":", "").substring(8).toUpperCase();
            connectShimmer(bluetoothAddress, btRadioID);
        }
    }

    private void connectShimmer(String bluetoothAddress, String deviceName) {
        Shimmer shimmer;
        if (!getShimmers().containsKey(bluetoothAddress)) {
            int deviceType = Shimmer.SENSOR_ACCEL | Shimmer.SENSOR_GYRO;
            int sampleRate = MOTION_SAMPLE_RATE;
            int maxValue = 250;
            String[] fields = MOTION_FIELDS;
            if (bluetoothAddress.equals(ECG_SENSOR_ADDRESS)) {
                deviceType = Shimmer.SENSOR_ACCEL; //Shimmer.SENSOR_ECG;
                sampleRate = ECG_SAMPLE_RATE;
                maxValue = 5000;
                fields = ECG_FIELDS;
            }
            shimmer = new Shimmer(this, new ShimmerHandler("sensor_" + deviceName + ".csv", this.directoryName, maxValue, fields), deviceName, sampleRate, 0, 0, deviceType, false);
            getShimmers().put(bluetoothAddress, shimmer);
        } else {
            Log.d(TAG, "Already added");
        }
        if (!getConnectedShimmers().containsKey(bluetoothAddress)) {
            shimmer = getShimmers().get(bluetoothAddress);
            shimmer.connect(bluetoothAddress, "default");
        } else {
            Log.d(TAG, "Already connected");
        }
    }

    private void disconnectedAllShimmers() {
        for (Shimmer shimmer : getConnectedShimmers().values()) {
            Log.d(TAG, "Disconnect: " + shimmer.getBluetoothAddress());
            disconnectShimmer(shimmer.getBluetoothAddress());
        }
    }

    private void disconnectShimmer(String bluetoothAddress) {
        if (getConnectedShimmers().containsKey(bluetoothAddress)) {
            Shimmer shimmer = getConnectedShimmers().get(bluetoothAddress);
            if (shimmer.getShimmerState() == Shimmer.STATE_CONNECTED) {
                shimmer.stop();
            } else {
                getConnectedShimmers().remove(bluetoothAddress);
            }
        }
    }

    private void startAllStreaming() {
        for (Shimmer shimmer : getConnectedShimmers().values()) {
            Log.d(TAG, "Start streaming from: " + shimmer.getBluetoothAddress());
            startStreaming(shimmer.getBluetoothAddress());
        }
    }

    private void startStreaming(String bluetoothAddress) {
        if (getConnectedShimmers().containsKey(bluetoothAddress)) {
            Shimmer shimmer = getConnectedShimmers().get(bluetoothAddress);
            if (shimmer.getShimmerState() == Shimmer.STATE_CONNECTED && !shimmer.getStreamingStatus()) {
                shimmer.startStreaming();
            } else {
                getConnectedShimmers().remove(bluetoothAddress);
            }
        }
    }

    private void stopAllStreaming() {
        for (Shimmer shimmer : getStreamingShimmers().values()) {
            Log.d(TAG, "Stop streaming from: " + shimmer.getBluetoothAddress());
            stopStreaming(shimmer.getBluetoothAddress());
        }
    }

    private void stopStreaming(String bluetoothAddress) {
        if (getStreamingShimmers().containsKey(bluetoothAddress)) {
            Shimmer shimmer = getStreamingShimmers().get(bluetoothAddress);
            if (shimmer.getBluetoothAddress().equals(bluetoothAddress) && shimmer.getShimmerState() == Shimmer.STATE_CONNECTED && shimmer.getStreamingStatus()) {
                shimmer.stopStreaming();
            } else {
                getStreamingShimmers().remove(bluetoothAddress);
            }
        }
    }

    private void toggleLEDs() {
        for (Shimmer shimmer : getConnectedShimmers().values()) {
            shimmer.toggleLed();
        }
    }

    private boolean shimmersAreStreaming() {
        return !getStreamingShimmers().isEmpty();
    }

    private void startTimerTread() {
        timerThreadShouldContinue = true;

        timerHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case TIMER_UPDATE:
                        if (textViewTimer.getVisibility() == View.INVISIBLE) {
                            textViewTimer.setVisibility(View.VISIBLE);
                            textViewTimer.requestLayout();
                        }
                        int minutes = msg.arg1 / 1000 / 60;
                        int seconds = msg.arg1 / 1000 % 60;
                        String time = String.format("%02d:%02d", minutes, seconds);
                        textViewTimer.setText(time);
                        break;

                    case TIMER_END:
                        feedbackNotification();
                        textViewTimer.setVisibility(View.INVISIBLE);
                        showLikertScaleDialog();
                        break;
                }
            }
        };

        long timerInterval = (long) (1000 * 60 * timerCycleInMin);
        final long endTime = System.currentTimeMillis() + timerInterval;

        timerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                while (now < endTime && timerThreadShouldContinue) {
                    Message message = new Message();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    message.what = TIMER_UPDATE;
                    message.arg1 = (int) (endTime - now);
                    timerHandler.sendMessage(message);

                    // Update time
                    now = System.currentTimeMillis();
                }
                Message message = new Message();
                message.what = TIMER_END;
                timerHandler.sendMessage(message);
            }
        });
        timerThread.start();
    }

    private void stopTimerThread() {
        timerThreadShouldContinue = false;
        if (timerThread.isAlive()) {
            textViewTimer.setVisibility(View.INVISIBLE);
            //timerThread.interrupt();
        }
        timerThread = null;
    }

    private void showLikertScaleDialog() {
        final Dialog dialog = new Dialog(this);
        final long timestamp = System.currentTimeMillis();
        dialog.setContentView(R.layout.flow_short_scale);
        dialog.setTitle(getString(R.string.feedback));
        dialog.setCancelable(false);

        Button saveButton = (Button) dialog.findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                saveScaleItems(dialog, SCALE_ITEM_COUNT, timestamp);
                dialog.dismiss();
                if (loggingEnabled) {
                    startTimerTread();
                }
            }
        });
        dialog.show();
    }

    private void saveScaleItems(final Dialog dialog, int items, long timestamp) {

        String outputString = Long.toString(timestamp) + "," + Long.toString(timestamp) + ",";
        for (int i = 1; i <= items; i++) {
            int identifier = getResources().getIdentifier("item" + i, "id", getPackageName());
            if (identifier != 0) {
                RatingBar ratingBar = (RatingBar) dialog.findViewById(identifier);
                if(i != items) {
                    outputString += Float.toString(ratingBar.getRating()) + ",";
                } else {
                    outputString += Float.toString(ratingBar.getRating());
                }
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(root, "scale.csv"), true));
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }

    private void startStreamingInternalSensorData() {

        accelerometerValues = new String[1000][5];
        accelerometerValueCount = 0;
        gyroscopeValues = new String[1000][5];
        gyroscopeValueCount = 0;

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        locationListener = new GPSListener("gps.csv", this.directoryName, 100);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void stopStreamingInternalSensorData() {
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (loggingEnabled) {
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues[accelerometerValueCount][0] = Long.toString(event.timestamp);
                accelerometerValues[accelerometerValueCount][1] = Float.toString(event.values[0]);
                accelerometerValues[accelerometerValueCount][2] = Float.toString(event.values[1]);
                accelerometerValues[accelerometerValueCount][3] = Float.toString(event.values[2]);
                accelerometerValues[accelerometerValueCount][4] = Long.toString(System.currentTimeMillis());

                accelerometerValueCount++;
                if (accelerometerValueCount > 999) {
                    Log.d(TAG, "Write accelerometer data");
                    accelerometerValueCount = 0;
                    String[][] accelerometerValueCopies = new String[1000][5];
                    System.arraycopy(accelerometerValues, 0, accelerometerValueCopies, 0, 999);
                    accelerometerValues = new String[1000][5];
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, "accelerometer.csv"), true));

                        for (String[] copy : accelerometerValueCopies) {
                            if (copy != null) {
                                writer.write(copy[0] + "," + copy[1] + "," + copy[2] + "," + copy[3] + "," + copy[4]);
                                writer.newLine();
                            }
                        }
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error while writing in file", e);
                    }
                }
            }
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE) {
                gyroscopeValues[gyroscopeValueCount][0] = Long.toString(event.timestamp);
                gyroscopeValues[gyroscopeValueCount][1] = Double.toString(event.values[0] * 180.0 / Math.PI);
                gyroscopeValues[gyroscopeValueCount][2] = Double.toString(event.values[1] * 180.0 / Math.PI);
                gyroscopeValues[gyroscopeValueCount][3] = Double.toString(event.values[2] * 180.0 / Math.PI);
                gyroscopeValues[gyroscopeValueCount][4] = Long.toString(System.currentTimeMillis());

                gyroscopeValueCount++;
                if (gyroscopeValueCount > 999) {
                    Log.d(TAG, "Write gyroscope data");
                    gyroscopeValueCount = 0;
                    String[][] gyroscopeValueCopies = new String[1000][5];
                    System.arraycopy(gyroscopeValues, 0, gyroscopeValueCopies, 0, 999);
                    gyroscopeValues = new String[1000][5];
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, "gyroscope.csv"), true));

                        for (String[] copy : gyroscopeValueCopies) {
                            if (copy != null) {
                                writer.write(copy[0] + "," + copy[1] + "," + copy[2] + "," + copy[3] + "," + copy[4]);
                                writer.newLine();
                            }
                        }
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error while writing in file", e);
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void feedbackNotification() {
        vibrator.vibrate(vibratorPatternFeedback, -1);
        playSound(R.raw.notifcation);
    }

    private void playSound(int soundID) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, soundID);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        mediaPlayer.start();
    }

    public class GPSListener implements LocationListener {
        private static final String TAG = "GPSListener";
        private String filename;
        private String directoryName;
        private File root;
        private int i = 0;
        private int maxValueCount;
        private String[][] values;

        public GPSListener(String filename, String directoryName, int maxValueCount) {
            this.filename = filename;
            this.directoryName = directoryName;

            this.root = new File(Environment.getExternalStorageDirectory() + "/" + this.directoryName);
            if (!this.root.exists()) {
                if (this.root.mkdir()) {
                    Log.d(TAG, "Directory " + this.directoryName + " created");
                }
            }

            this.maxValueCount = maxValueCount;
            this.values = new String[maxValueCount][4];
        }

        @Override
        public void onLocationChanged(Location location) {
            if (loggingEnabled) {
                values[i][0] = Long.toString(location.getTime());
                values[i][1] = Double.toString(location.getLatitude());
                values[i][2] = Double.toString(location.getLongitude());
                values[i][3] = Double.toString(location.getAltitude());

                i++;
                if (i > maxValueCount -1) {
                    Log.d(TAG, "Write data in " + this.filename);
                    i = 0;
                    String[][] copies = new String[maxValueCount][5];
                    System.arraycopy(values, 0, copies, 0, maxValueCount -1);
                    values = new String[maxValueCount][5];
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));

                        for (String[] copy : copies) {
                            if (copy != null) {
                                writer.write(copy[0] + "," + copy[1] + "," + copy[2] + "," + copy[3]);
                                writer.newLine();
                            }
                        }
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error while writing in file", e);
                    }
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, getString(R.string.gps_not_available), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

    class ShimmerHandler extends Handler {

        private static final String TAG = "ShimmerHandler";
        private String filename;
        private String directoryName;
        private File root;
        private int i = 0;
        private int maxValueCount;
        private String[][] values;
        private String[] fields;

        ShimmerHandler(String filename, String directoryName, int maxValueCount, String[] fields) {
            this.filename = filename;
            this.directoryName = directoryName;

            this.root = new File(Environment.getExternalStorageDirectory() + "/" + this.directoryName);
            if (!this.root.exists()) {
                if (this.root.mkdir()) {
                    Log.d(TAG, "Directory " + this.directoryName + " created");
                }
            }

            this.maxValueCount = maxValueCount;
            this.fields = fields;
            this.values = new String[maxValueCount][fields.length];
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Shimmer.MESSAGE_READ:

                        if (msg.obj instanceof ObjectCluster) {
                            ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                            objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));

                            for (int j = 0; j < fields.length; j++) {

                                Collection<FormatCluster> clusterCollection = objectCluster.mPropertyCluster.get(fields[j]);
                                if (!clusterCollection.isEmpty()) {
                                    FormatCluster formatCluster = ObjectCluster.returnFormatCluster(clusterCollection, "CAL");
                                    values[i][j] = Double.toString(formatCluster.mData);
                                }
                            }

                            i++;
                            if (i > maxValueCount -1) {
                                Log.d(TAG, "Write data in " + this.filename);
                                i = 0;
                                String[][] copies = new String[maxValueCount][fields.length];
                                System.arraycopy(values, 0, copies, 0, maxValueCount -1);
                                values = new String[maxValueCount][fields.length];
                                try {
                                    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));

                                    for (String[] copy : copies) {
                                        if (copy != null) {
                                            String outputString = "";
                                            for (int k = 0; k < fields.length; k++) {
                                                if (fields.length != k) {
                                                    outputString += copy[k] + ",";
                                                } else {
                                                    outputString += copy[k];
                                                }
                                            }
                                            writer.write(outputString);
                                            writer.newLine();
                                        }
                                    }
                                    writer.flush();
                                    writer.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Error while writing in file", e);
                                }
                            }
                        }

                    break;
                case Shimmer.MESSAGE_TOAST:
                    Log.d(TAG, msg.getData().getString(Shimmer.TOAST));
                    if ("Device connection was lost".equals(msg.getData().getString(Shimmer.TOAST))) {
                        vibrator.vibrate(vibratorPatternConnectionLost, -1);
                    }
                    break;
                case Shimmer.MESSAGE_STATE_CHANGE:
                    String bluetoothAddress = "None";
                    if ((msg.obj instanceof ObjectCluster)) {
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        bluetoothAddress = objectCluster.mBluetoothAddress;
                    }
                    switch (msg.arg1) {
                        case Shimmer.STATE_CONNECTED:
                            Log.d(TAG, "Connected: " + bluetoothAddress);
                            break;
                        case Shimmer.STATE_CONNECTING:
                            Log.d(TAG, "Connecting: " + bluetoothAddress);
                            break;
                        case Shimmer.STATE_NONE:
                            Log.d(TAG, "None State: " + bluetoothAddress);
                            getConnectedShimmers().remove(bluetoothAddress);
                            break;
                        case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                            Log.d(TAG, "Fully initialized: " + bluetoothAddress);
                            getConnectedShimmers().put(bluetoothAddress, getShimmers().get(bluetoothAddress));
                            break;
                        case Shimmer.MSG_STATE_STREAMING:
                            Log.d(TAG, "Streaming: " + bluetoothAddress);
                            getStreamingShimmers().put(bluetoothAddress, getConnectedShimmers().get(bluetoothAddress));
                            break;
                        case Shimmer.MSG_STATE_STOP_STREAMING:
                            Log.d(TAG, "Stop streaming: " + bluetoothAddress);
                            getStreamingShimmers().remove(bluetoothAddress);
                            break;
                        case Shimmer.MESSAGE_STOP_STREAMING_COMPLETE:
                            Log.d(TAG, "Stop streaming complete:" + bluetoothAddress);
                            break;
                    }
                    break;
                case Shimmer.MESSAGE_PACKET_LOSS_DETECTED:
                    Log.d(TAG, "Packet loss detected");
                    break;
            }
        }
    }
}