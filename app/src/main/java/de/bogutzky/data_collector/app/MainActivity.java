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
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import de.bogutzky.data_collector.app.tools.Logger;

public class MainActivity extends ListActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final int MSG_BLUETOOTH_ADDRESS = 1;
    private static final int TIMER_UPDATE = 1;
    private static final int TIMER_END = 2;
    private static final String SCALE = "Flow Short Scale";
    private static final int SCALE_ITEM_COUNT = 16;
    private static final String ACCELEROMETER = "Internal Accelerometer";
    private static final String GYROSCOPE = "Internal Gyroscope";
    private static final String GPS = "GPS";
    private HashMap<String, Shimmer> shimmers;
    private HashMap<String, Shimmer> connectedShimmers;
    private HashMap<String, Shimmer> streamingShimmers;
    private HashMap<String, Logger> loggers;
    private boolean loggingEnabled = false;
    private ArrayAdapter adapter;
    private ArrayList<String> bluetoothAddresses;
    private TextView textViewTimer;
    private Handler timerHandler;
    private Thread timerThread;
    private boolean timerThreadShouldContinue = false;
    private double timerCycleInMin;
    private Thread logThread;
    private boolean logThreadShouldContinue = false;
    private SensorManager sensorManager;
    private android.hardware.Sensor accelerometer;
    private android.hardware.Sensor gyroscope;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Vibrator vibrator;
    private long[] vibratorPatternFeedback = {0, 500, 200, 100, 100, 100, 100, 100};
    private long[] vibratorPatternConnectionLost = {0, 100, 100, 100, 100, 100, 100, 100};
    private Handler shimmerHandler = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Shimmer.MESSAGE_READ:
                    if (loggingEnabled) {
                        if (msg.obj instanceof ObjectCluster) {
                            ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                            objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
                            Logger logger = getLoggers().get(objectCluster.mBluetoothAddress);
                            logger.addObjectCluster(objectCluster);
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getBluetoothAddresses());
        setListAdapter(adapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "Connect to: " + getBluetoothAddresses().get(i));
                String btRadioID = getBluetoothAddresses().get(i).replace(":", "").substring(8).toUpperCase();
                connectShimmer(getBluetoothAddresses().get(i), btRadioID);
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //startStreamingInternalSensorData();

        startLogThread();
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

        if (id == R.id.action_start_logging) {
            initializeLoggers();
            loggingEnabled = true;
        }

        if (id == R.id.action_stop_logging) {
            loggingEnabled = false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        //stopStreamingInternalSensorData();
        loggingEnabled = false;

        if (timerThread != null) {
            stopTimerThread();
        }

        if (logThread != null) {
            stopLogThread();
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

    public ArrayList<String> getBluetoothAddresses() {
        if (bluetoothAddresses == null) {
            bluetoothAddresses = new ArrayList<String>();
        }
        return bluetoothAddresses;
    }

    public void addBluetoothAddress(String bluetoothAddress) {
        getBluetoothAddresses().add(bluetoothAddress);
        adapter.notifyDataSetChanged();
    }

    public void findBluetoothAddress() {
        if (!shimmersAreStreaming()) {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, MSG_BLUETOOTH_ADDRESS);
        } else {
            Toast.makeText(this, getString(R.string.ensure_no_device_is_streaming), Toast.LENGTH_LONG).show();
        }
    }

    public HashMap<String, Shimmer> getShimmers() {
        if (shimmers == null) {
            shimmers = new HashMap<String, Shimmer>(4);
        }
        return shimmers;
    }

    public HashMap<String, Shimmer> getConnectedShimmers() {
        if (connectedShimmers == null) {
            connectedShimmers = new HashMap<String, Shimmer>(4);
        }
        return connectedShimmers;
    }

    public HashMap<String, Shimmer> getStreamingShimmers() {
        if (streamingShimmers == null) {
            streamingShimmers = new HashMap<String, Shimmer>(4);
        }
        return streamingShimmers;
    }

    public void connectShimmer(String bluetoothAddress, String deviceName) {
        Shimmer shimmer;
        if (!getShimmers().containsKey(bluetoothAddress)) {
            shimmer = new Shimmer(this, shimmerHandler, deviceName, false);
            getShimmers().put(bluetoothAddress, shimmer);
        } else {
            Log.d(TAG, "Already added");
            if (!getConnectedShimmers().containsKey(bluetoothAddress)) {
                shimmer = getShimmers().get(bluetoothAddress);
                shimmer.connect(bluetoothAddress, "default");
            } else {
                Log.d(TAG, "Already connected");
                //shimmer = (Shimmer) getConnectedShimmers().get(bluetoothAddress);
                //shimmer.toggleLed();
                //disconnectShimmer(bluetoothAddress);
                startStreaming(bluetoothAddress);
            }
        }
    }

    public void disconnectShimmer(String bluetoothAddress) {
        if (getConnectedShimmers().containsKey(bluetoothAddress)) {
            Shimmer shimmer = getConnectedShimmers().get(bluetoothAddress);
            if (shimmer.getShimmerState() == Shimmer.STATE_CONNECTED) {
                shimmer.stop();
            }
        }
    }

    public void startStreaming(String bluetoothAddress) {
        if (getConnectedShimmers().containsKey(bluetoothAddress)) {
            Shimmer shimmer = getConnectedShimmers().get(bluetoothAddress);
            if (shimmer.getShimmerState() == Shimmer.STATE_CONNECTED && !shimmer.getStreamingStatus()) {
                shimmer.startStreaming();
            }
        }
    }

    public void stopStreaming(String bluetoothAddress) {
        if (getConnectedShimmers().containsKey(bluetoothAddress)) {
            Shimmer shimmer = getStreamingShimmers().get(bluetoothAddress);
            if (shimmer.getBluetoothAddress().equals(bluetoothAddress) && shimmer.getShimmerState() == Shimmer.STATE_CONNECTED && shimmer.getStreamingStatus()) {
                shimmer.stopStreaming();
            }
        }
    }

    public boolean shimmersAreStreaming() {
        return !getStreamingShimmers().isEmpty();
    }

    public HashMap<String, Logger> getLoggers() {
        if (loggers == null) {
            loggers = new HashMap<String, Logger>(8);
        }
        return loggers;
    }

    private void initializeLoggers() {
        getLoggers().clear();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH-mm-ss");
        String dateString = simpleDateFormat.format(new Date());
        String timeString = simpleTimeFormat.format(new Date());
        String directoryName = "DataCollector/" + dateString + "_" + timeString;
        for (String bluetoothAddress : getStreamingShimmers().keySet()) {
            String btRadioID = bluetoothAddress.replace(":", "").substring(8).toUpperCase();
            Logger logger = new Logger("sensor_" + btRadioID, ",", directoryName);
            getLoggers().put(bluetoothAddress, logger);
        }
        Logger scaleLogger = new Logger("scale", ",", directoryName);
        getLoggers().put(SCALE, scaleLogger);
        Logger accelerometerLogger = new Logger("accelerometer", ",", directoryName);
        getLoggers().put(ACCELEROMETER, accelerometerLogger);
        Logger gyroscopeLogger = new Logger("gyroscope", ",", directoryName);
        getLoggers().put(GYROSCOPE, gyroscopeLogger);
        Logger gpsLogger = new Logger("gps", ",", directoryName);
        getLoggers().put(GPS, gpsLogger);
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
            timerThread.interrupt();
        }
        timerThread = null;
    }

    private void startLogThread() {
        logThreadShouldContinue = true;

        logThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (logThreadShouldContinue) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (Logger logger : getLoggers().values()) {
                        logger.writeObjectClusters("CAL", false);
                    }
                }
            }
        });
        logThread.start();
    }

    private void stopLogThread() {
        logThreadShouldContinue = false;
        if (logThread.isAlive()) {
            logThread.interrupt();
        }
        logThread = null;
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
                saveScaleItems(dialog, SCALE, SCALE_ITEM_COUNT, timestamp);
                dialog.dismiss();
                if (loggingEnabled) {
                    startTimerTread();
                }
            }
        });
        dialog.show();
    }

    private void saveScaleItems(final Dialog dialog, String scale, int items, long timestamp) {

        ObjectCluster objectCluster = new ObjectCluster(scale, scale);
        objectCluster.mPropertyCluster.put("System Timestamp 01", new FormatCluster("CAL", "mSecs", timestamp));
        objectCluster.mPropertyCluster.put("System Timestamp 02", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
        for (int i = 1; i <= items; i++) {
            int identifier = getResources().getIdentifier("item" + i, "id", getPackageName());
            if (identifier != 0) {
                RatingBar ratingBar = (RatingBar) dialog.findViewById(identifier);
                objectCluster.mPropertyCluster.put("Item " + String.format("%02d", i), new FormatCluster("CAL", "n. u.", (int) ratingBar.getRating()));
            }
        }
        Logger logger = loggers.get(scale);
        logger.addObjectCluster(objectCluster);
    }

    private void startStreamingInternalSensorData() {

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        locationListener = new GPSListener();
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
                ObjectCluster objectCluster = new ObjectCluster(ACCELEROMETER, ACCELEROMETER);
                objectCluster.mPropertyCluster.put("Timestamp", new FormatCluster("CAL", "nSecs", event.timestamp));
                objectCluster.mPropertyCluster.put("Accelerometer X", new FormatCluster("CAL", "m/s^2", event.values[0]));
                objectCluster.mPropertyCluster.put("Accelerometer Y", new FormatCluster("CAL", "m/s^2", event.values[1]));
                objectCluster.mPropertyCluster.put("Accelerometer Z", new FormatCluster("CAL", "m/s^2", event.values[2]));
                objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
                Logger logger = loggers.get(ACCELEROMETER);
                logger.addObjectCluster(objectCluster);
            }
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE) {
                ObjectCluster objectCluster = new ObjectCluster(GYROSCOPE, GYROSCOPE);
                objectCluster.mPropertyCluster.put("Timestamp", new FormatCluster("CAL", "nSecs", event.timestamp));
                objectCluster.mPropertyCluster.put("Gyroscope X", new FormatCluster("CAL", "deg/s", event.values[0] * 180 / Math.PI));
                objectCluster.mPropertyCluster.put("Gyroscope Y", new FormatCluster("CAL", "deg/s", event.values[1] * 180 / Math.PI));
                objectCluster.mPropertyCluster.put("Gyroscope Z", new FormatCluster("CAL", "deg/s", event.values[2] * 180 / Math.PI));
                objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
                Logger logger = loggers.get(GYROSCOPE);
                logger.addObjectCluster(objectCluster);
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
        @Override
        public void onLocationChanged(Location location) {
            if (loggingEnabled) {
                ObjectCluster objectCluster = new ObjectCluster(GPS, GPS);
                objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", location.getTime()));
                objectCluster.mPropertyCluster.put("Latitude", new FormatCluster("CAL", "mSecs", location.getLatitude()));
                objectCluster.mPropertyCluster.put("Longitude", new FormatCluster("CAL", "mSecs", location.getLongitude()));
                objectCluster.mPropertyCluster.put("Altitude", new FormatCluster("CAL", "mSecs", location.getAltitude()));
                Logger logger = loggers.get(GPS);
                logger.addObjectCluster(objectCluster);
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
}