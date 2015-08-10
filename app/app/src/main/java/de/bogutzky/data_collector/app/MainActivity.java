package de.bogutzky.data_collector.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import zephyr.android.BioHarnessBT.BTClient;

public class MainActivity extends ListActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final int MSG_BLUETOOTH_ADDRESS = 1;
    private static final int TIMER_UPDATE = 1;
    private static final int TIMER_END = 2;
    private static final int SCALE_ITEM_COUNT = 16;
    private final String bhHeartRateFilename = "bhHeartRate.csv";
    private final String bhRespirationRateFilename = "bhRespirationRate.csv";
    private final String bhPostureFilename = "bhPosture.csv";
    private final String bhSkinTemperatureFilename = "bhSkinTemperature.csv";
    private final String bhPeakAccelerationFilename = "bhPeakAcceleration.csv";
    private final String bhRRIntervalFilename = "bhRRInterval.csv";
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
    private String[][] linearAccelerationValues;
    private int linearAccelerationValueCount;
    private String[][] bhHeartRateValues;
    private int bhHeartRateValueCount;
    private String[][] bhRespirationtRateValues;
    private int bhRespirationRateValueCount;
    private String[][] bhSkinTemperatureValues;
    private int bhSkinTemperatureValueCount;
    private String[][] bhPostureValues;
    private int bhPostureValueCount;
    private String[][] bhPeakAccelerationValues;
    private int bhPeakAccelerationValueCount;
    private String[][] bhRRIntervalValues;
    private int bhRRIntervalValueCount;

    /* min api 9*/
    private Sensor linearAccelerationSensor;

    private Long firstGyroSensorTimestamp;
    private Long firstAccelerometerSensorTimestamp;
    private Long firstLinearAccelerationSensorTimestamp;
    private Long firstbhHeartRateTimestamp;
    private Long firstRespirationRateTimestamp;
    private Long firstSkinTemperatureTimestamp;
    private Long firstbhPostureTimestamp;
    private Long firstPeakAccelerationTimestamp;
    private Long firstRRIntervalTimestamp;

    private DecimalFormat decimalFormat;

    //bth adapter
    private BluetoothAdapter btAdapter = null;
    private BTClient _bt;
    private BioHarnessConnectedListener _NConnListener;
    private final int HEART_RATE = 0x100;
    private final int RESPIRATION_RATE = 0x101;
    private final int SKIN_TEMPERATURE = 0x102;
    private final int POSTURE = 0x103;
    private final int PEAK_ACCLERATION = 0x104;
    private final int RR_INTERVAL = 0x105;
    private String BhMacID;

    public final static int REQUEST_MAIN_COMMAND_SHIMMER=3;
    public final static int REQUEST_COMMANDS_SHIMMER=4;
    public static final int REQUEST_CONFIGURE_SHIMMER = 5;

    private MenuItem connectMenuItem;
    private MenuItem disconnectMenuItem;
    private MenuItem startStreamMenuItem;
    private MenuItem stopStreamMenuItem;


    ShimmerService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getBluetoothAddresses());
        setListAdapter(adapter);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        this.linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        timerCycleInMin = 15;

        this.firstGyroSensorTimestamp = 0L;
        this.firstAccelerometerSensorTimestamp = 0L;
        this.firstLinearAccelerationSensorTimestamp = 0L;
        this.firstbhHeartRateTimestamp = 0L;
        this.firstRespirationRateTimestamp = 0L;
        this.firstSkinTemperatureTimestamp = 0L;
        this.firstbhPostureTimestamp = 0L;
        this.firstPeakAccelerationTimestamp = 0L;
        this.firstRRIntervalTimestamp = 0L;

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#.###", otherSymbols);
        decimalFormat.setMinimumFractionDigits(5);

        textViewTimer = (TextView) findViewById(R.id.text_view_timer);
        textViewTimer.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        this.connectMenuItem = menu.getItem(1);
        this.disconnectMenuItem = menu.getItem(2);
        this.startStreamMenuItem = menu.getItem(3);
        this.stopStreamMenuItem = menu.getItem(4);
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
                createRootDirectory();
            }
            disconnectMenuItem.setEnabled(true);
            connectMenuItem.setEnabled(false);
            connectedAllShimmers();
            connectBioHarness();
        }

        if (id == R.id.action_disconnect) {
            disconnectedAllShimmers();
            if(_bt != null && _NConnListener != null) {
                _bt.removeConnectedEventListener(_NConnListener);
                _bt.Close();
            }
            this.directoryName = null;
            connectMenuItem.setEnabled(true);
            disconnectMenuItem.setEnabled(false);
        }

        if (id == R.id.action_start_streaming) {
            loggingEnabled = true;
            this.startStreamMenuItem.setEnabled(false);
            this.stopStreamMenuItem.setEnabled(true);
            if (this.directoryName == null) {
                createRootDirectory();
            }
            startAllStreaming();

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, "scale.csv"), true));


                String outputString = "\"System Timestamp 01\",\"System Timestamp 02\",";
                for (int i = 1; i <= SCALE_ITEM_COUNT; i++) {
                    if (i != SCALE_ITEM_COUNT) {
                        outputString += "\"Item " + String.format("%02d", i) + "\",";
                    } else {
                        outputString += "\"Item " + String.format("%02d", i) + "\"";
                    }
                }
                writer.write(outputString);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while writing in file", e);
            }

            startTimerThread();
            startStreamingInternalSensorData();
        }

        if (id == R.id.action_stop_streaming) {
            loggingEnabled = false;
            this.directoryName = null;
            stopAllStreaming();
            stopTimerThread();
            stopStreamingInternalSensorData();
            this.startStreamMenuItem.setEnabled(true);
            this.stopStreamMenuItem.setEnabled(false);
        }

        if (id == R.id.action_toggle_led) {

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Object o = l.getItemAtPosition(position);
        Log.d("Shimmer",o.toString());
        Intent mainCommandIntent=new Intent(MainActivity.this, MainCommandsActivity.class);
        mainCommandIntent.putExtra("LocalDeviceID", o.toString());
        mainCommandIntent.putExtra("CurrentSlot", position);
        mainCommandIntent.putExtra("requestCode", REQUEST_MAIN_COMMAND_SHIMMER);
        startActivityForResult(mainCommandIntent, REQUEST_MAIN_COMMAND_SHIMMER);
    }

    private void connectBioHarness() {

        bhPeakAccelerationValueCount = 0;
        bhRespirationRateValueCount = 0;
        bhHeartRateValueCount = 0;
        bhPostureValueCount = 0;
        bhSkinTemperatureValueCount = 0;
        bhRRIntervalValueCount = 0;
        bhHeartRateValues = new String[1000][2];
        bhPostureValues = new String[1000][2];
        bhPeakAccelerationValues = new String[1000][2];
        bhSkinTemperatureValues = new String[1000][2];
        bhRespirationtRateValues = new String[1000][2];
        bhRRIntervalValues = new String[1000][2];

        createBioHarnessFiles();

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().startsWith("BH")) {
                    if(bluetoothAddresses.contains(device.getAddress())) {
                        BluetoothDevice btDevice = device;
                        BhMacID = btDevice.getAddress();
                        break;
                    }
                }
            }
        }
        if(BhMacID != null) {
            BluetoothDevice Device = btAdapter.getRemoteDevice(BhMacID);
            String DeviceName = Device.getName();
            Log.v(TAG, "connected to: " + DeviceName);

            _bt = new BTClient(btAdapter, BhMacID);
            HarnessHandler harnessHandler = new HarnessHandler();
            _NConnListener = new BioHarnessConnectedListener(harnessHandler, harnessHandler);
            _bt.addConnectedEventListener(_NConnListener);

            if(_bt.IsConnected()) {
                _bt.start();
            }
        }
    }

    private void createBioHarnessFiles() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, bhHeartRateFilename), true));
            String outputString = "\"Timestamp\",\"HeartRate\"";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, bhRespirationRateFilename), true));
            String outputString = "\"Timestamp\",\"RespirationRate\"";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, bhPostureFilename), true));
            String outputString = "\"Timestamp\",\"Posture\"";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, bhSkinTemperatureFilename), true));
            String outputString = "\"Timestamp\",\"SkinTemperature\"";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, bhPeakAccelerationFilename), true));
            String outputString = "\"Timestamp\",\"PeakAcceleration\"";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }

    private void createRootDirectory() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH-mm-ss");
        String dateString = simpleDateFormat.format(new Date());
        String timeString = simpleTimeFormat.format(new Date());
        this.directoryName = "DataCollector/" + dateString + "_" + timeString;

        this.root = getStorageDir(this.directoryName);
    }

    @Override
    protected void onDestroy() {
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
                    //BhMacID = bluetoothAddress;

                    // Check if the bluetooth address has been previously selected
                    boolean isNewAddress = !getBluetoothAddresses().contains(bluetoothAddress);

                    if (isNewAddress) {
                        addBluetoothAddress(bluetoothAddress);
                        Log.v("Main", "bond: " + bluetoothAddress);
                    } else {
                        Toast.makeText(this, getString(R.string.device_is_already_in_list), Toast.LENGTH_LONG).show();
                    }
                    if(mService == null) {
                        Log.v(TAG, "service erstellen");
                        Intent intent=new Intent(this, ShimmerService.class);
                        startService(intent);
                        getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
                        registerReceiver(myReceiver, new IntentFilter("de.bogutzky.data_collector.app"));
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
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, MSG_BLUETOOTH_ADDRESS);
    }

    private void connectedAllShimmers() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        int count = 0;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains("RN42")) {
                    if(bluetoothAddresses.contains(device.getAddress())) {
                        BluetoothDevice btDevice = device;

                        String bluetoothAddress = btDevice.getAddress();
                        mService.connectShimmer(bluetoothAddress, Integer.toString(count),new ShimmerHandler("sensor_" + btDevice.getName() + ".csv", this.directoryName, 250));
                        count++;
                        break;
                    }
                }
            }
        }
    }

    private void disconnectedAllShimmers() {
        stopService(new Intent(MainActivity.this, ShimmerService.class));
        mService.disconnectAllDevices();
    }


    private void startAllStreaming() {
        mService.startStreamingAllDevicesGetSensorNames(this.root);
    }


    private void stopAllStreaming() {
       mService.stopStreamingAllDevices();
    }

    private void startTimerThread() {
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
        textViewTimer.setVisibility(View.INVISIBLE);
        timerThread = null;
    }

    private void showLikertScaleDialog() {
        final Dialog dialog = new Dialog(this);
        final long timestamp = System.currentTimeMillis();
        dialog.setContentView(R.layout.flow_short_scale);
        dialog.setTitle(getString(R.string.feedback));
        dialog.setCancelable(false);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);

        Button saveButton = (Button) dialog.findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                saveScaleItems(dialog, SCALE_ITEM_COUNT, timestamp);
                dialog.dismiss();
                if (loggingEnabled) {
                    resumeAllSensors();
                    startTimerThread();
                }
            }
        });
        dialog.show();
        pauseAllSensors();
    }

    private void saveScaleItems(final Dialog dialog, int items, long timestamp) {

        String outputString = Long.toString(timestamp) + "," + Long.toString(System.currentTimeMillis()) + ",";
        for (int i = 1; i <= items; i++) {
            int identifier = getResources().getIdentifier("item" + i, "id", getPackageName());
            if (identifier != 0) {
                RatingBar ratingBar = (RatingBar) dialog.findViewById(identifier);
                if (i != items) {
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
        linearAccelerationValues = new String[1000][5];
        linearAccelerationValueCount = 0;
        this.firstGyroSensorTimestamp = 0L;
        this.firstAccelerometerSensorTimestamp = 0L;
        this.firstLinearAccelerationSensorTimestamp = 0L;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, "accelerometer.csv"), true));
            String outputString = "\"Timestamp\",\"Accelerometer X\",\"Accelerometer Y\",\"Accelerometer Z\",\"System Timestamp\"";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, "gyroscope.csv"), true));
            String outputString = "\"Timestamp\",\"Gyroscope X\",\"Gyroscope Y\",\"Gyroscope Z\",\"System Timestamp\"";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, "linearAcceleration.csv"), true));
            String outputString = "\"Timestamp\",\"Accelerometer X\",\"Accelerometer Y\",\"Accelerometer Z\",\"System Timestamp\"";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_FASTEST);

        locationListener = new GPSListener("gps.csv", this.directoryName, 100);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void stopStreamingInternalSensorData() {
        sensorManager.unregisterListener(this);
        if(locationManager != null)
            locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (loggingEnabled) {
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                if (this.firstAccelerometerSensorTimestamp == 0L) {
                    this.firstAccelerometerSensorTimestamp = event.timestamp;
                }
                double time = (event.timestamp - this.firstAccelerometerSensorTimestamp) / 1000000000.0;

                accelerometerValues[accelerometerValueCount][0] = decimalFormat.format(time);
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
                            if (copy[0] != null) {
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
                if (this.firstGyroSensorTimestamp == 0L) {
                    this.firstGyroSensorTimestamp = event.timestamp;
                }
                double time = (event.timestamp - this.firstGyroSensorTimestamp) / 1000000000.0;
                gyroscopeValues[gyroscopeValueCount][0] = decimalFormat.format(time);
                gyroscopeValues[gyroscopeValueCount][1] = Float.toString((float) (event.values[0] * 180.0 / Math.PI));
                gyroscopeValues[gyroscopeValueCount][2] = Float.toString((float) (event.values[1] * 180.0 / Math.PI));
                gyroscopeValues[gyroscopeValueCount][3] = Float.toString((float) (event.values[2] * 180.0 / Math.PI));
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
                            if (copy[0] != null) {
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
            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                if (this.firstLinearAccelerationSensorTimestamp == 0L) {
                    this.firstLinearAccelerationSensorTimestamp = event.timestamp;
                }
                double time = (event.timestamp - this.firstLinearAccelerationSensorTimestamp) / 1000000000.0;
                linearAccelerationValues[linearAccelerationValueCount][0] = decimalFormat.format(time);
                linearAccelerationValues[linearAccelerationValueCount][1] = Float.toString(event.values[0]);
                linearAccelerationValues[linearAccelerationValueCount][2] = Float.toString(event.values[1]);
                linearAccelerationValues[linearAccelerationValueCount][3] = Float.toString(event.values[2]);
                linearAccelerationValues[linearAccelerationValueCount][4] = Long.toString(System.currentTimeMillis());

                linearAccelerationValueCount++;
                if (linearAccelerationValueCount > 999) {
                    Log.d(TAG, "Write linear accelerometer data");
                    linearAccelerationValueCount = 0;
                    String[][] linearAccelerationValuesCopies = new String[1000][5];
                    System.arraycopy(linearAccelerationValues, 0, linearAccelerationValuesCopies, 0, 999);
                    linearAccelerationValues = new String[1000][5];
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, "linearAcceleration.csv"), true));

                        for (String[] copy : linearAccelerationValuesCopies) {
                            if (copy[0] != null) {
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

    public File getStorageDir(String folderName) {
        Log.d(TAG, "create: " + folderName);
        try {
            File root = Environment.getExternalStorageDirectory();
            if (root.canWrite()) {
                File file = new File(root, folderName);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        Log.d(TAG, "could not create file");
                    }
                }
                return file;
            }
        } catch (Exception e) {
            Log.e("DEBUG", "Could not write file " + e.getMessage());
        }
        return null;
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

            this.root = getStorageDir(this.directoryName);

            this.maxValueCount = maxValueCount;
            this.values = new String[maxValueCount][4];

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));
                String outputString = "\"System Timestamp\",\"Latitude\",\"Longitude\",\"Altitude\"";
                writer.write(outputString);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while writing in file", e);
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            if (loggingEnabled) {
                values[i][0] = Long.toString(location.getTime());
                values[i][1] = Float.toString((float) location.getLatitude());
                values[i][2] = Float.toString((float) location.getLongitude());
                values[i][3] = Float.toString((float) location.getAltitude());

                i++;
                if (i > maxValueCount - 1) {
                    Log.d(TAG, "Write data in " + this.filename);
                    i = 0;
                    String[][] copies = new String[maxValueCount][5];
                    System.arraycopy(values, 0, copies, 0, maxValueCount - 1);
                    values = new String[maxValueCount][5];
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));

                        for (String[] copy : copies) {
                            if (copy[0] != null) {
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

    public class ShimmerHandler extends Handler {

        private static final String TAG = "ShimmerHandler";

        private String filename;
        private String directoryName;
        private File root;
        private int i = 0;
        private int maxValueCount;
        private String[][] values;
        private String[] fields;

        public void setRoot(File root) {
            this.root = root;
        }

        public void setFields(String[] fields) {
            this.fields = fields;
            this.values = new String[maxValueCount][fields.length];
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));
                String outputString = "";
                for (int k = 0; k < fields.length; k++) {
                    if (fields.length - 1 != k) {
                        outputString += "\"" + fields[k] + "\",";
                    } else {
                        outputString += "\"" + fields[k] + "\"";
                    }
                }
                writer.write(outputString);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while writing in file", e);
            }
        }

        ShimmerHandler(String filename, String directoryName, int maxValueCount) {
            this.filename = filename;
            this.directoryName = directoryName;

            this.root = getStorageDir(this.directoryName);

            this.maxValueCount = maxValueCount;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Shimmer.MESSAGE_READ:

                    if (msg.obj instanceof ObjectCluster) {
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;

                        for (int j = 0; j < fields.length; j++) {

                            Collection<FormatCluster> clusterCollection = objectCluster.mPropertyCluster.get(fields[j]);
                            if (j < fields.length - 1) {
                                if (!clusterCollection.isEmpty()) {
                                    FormatCluster formatCluster = ObjectCluster.returnFormatCluster(clusterCollection, "CAL");
                                    values[i][j] = Float.toString((float) formatCluster.mData);
                                }
                            } else {
                                values[i][j] = Long.toString(System.currentTimeMillis());
                            }
                        }

                        i++;
                        if (i > maxValueCount - 1) {
                            Log.d(TAG, "Write data in " + this.filename);
                            i = 0;
                            String[][] copies = new String[maxValueCount][fields.length];
                            System.arraycopy(values, 0, copies, 0, maxValueCount - 1);
                            values = new String[maxValueCount][fields.length];
                            try {
                                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));

                                for (String[] copy : copies) {
                                    if (copy[0] != null) {
                                        String outputString = "";
                                        for (int k = 0; k < fields.length; k++) {
                                            if (fields.length - 1 != k) {
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
                            break;
                        case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                            Log.d(TAG, "Fully initialized: " + bluetoothAddress);
                            String btRadioID = bluetoothAddress.replace(":", "").substring(8).toUpperCase();
                            Toast.makeText(MainActivity.this, btRadioID + " " + getString(R.string.is_ready), Toast.LENGTH_LONG).show();

                            break;
                        case Shimmer.MSG_STATE_STREAMING:
                            Log.d(TAG, "Streaming: " + bluetoothAddress);
                            break;
                        case Shimmer.MSG_STATE_STOP_STREAMING:
                            Log.d(TAG, "Stop streaming: " + bluetoothAddress);
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

    class HarnessHandler extends Handler {
        int maxVals = 5;
        long timestamp = 0;
        double time = 0;
        HarnessHandler() {}
        public void handleMessage(Message msg) {
            if(msg.what == 101) {
                notifyBHReady();
            }
            if(loggingEnabled) {
                switch (msg.what) {
                    case RR_INTERVAL:
                        String rrInterval = msg.getData().getString("rrinterval");
                        timestamp = msg.getData().getLong("Timestamp");
                        if (firstRRIntervalTimestamp == 0L) {
                            firstRRIntervalTimestamp = timestamp;
                        }
                        time = (timestamp - firstRRIntervalTimestamp) / 1000.0;
                        bhRRIntervalValues[bhRRIntervalValueCount][0] = String.valueOf(time);
                        bhRRIntervalValues[bhRRIntervalValueCount][1] = String.valueOf(rrInterval);
                        bhRRIntervalValueCount++;
                        if(bhRRIntervalValueCount >= maxVals) {
                            bhRRIntervalValueCount++;
                            writeData(bhRRIntervalValues, bhRRIntervalFilename);
                            bhRRIntervalValues = new String[1000][2];
                        }
                        break;

                    case HEART_RATE:
                        String HeartRatetext = msg.getData().getString("HeartRate");
                        timestamp = msg.getData().getLong("Timestamp");
                        if (firstbhHeartRateTimestamp == 0L) {
                            firstbhHeartRateTimestamp = timestamp;
                        }
                        time = (timestamp - firstbhHeartRateTimestamp) / 1000.0;
                        bhHeartRateValues[bhHeartRateValueCount][0] = String.valueOf(time);
                        bhHeartRateValues[bhHeartRateValueCount][1] = HeartRatetext;
                        bhHeartRateValueCount++;
                        System.out.println("Heart Rate Info is " + HeartRatetext);
                        if(bhHeartRateValueCount >= maxVals) {
                            bhHeartRateValueCount = 0;
                            writeData(bhHeartRateValues, bhHeartRateFilename);
                            bhHeartRateValues = new String[1000][2];
                        }
                        break;

                    case RESPIRATION_RATE:
                        String RespirationRatetext = msg.getData().getString("RespirationRate");
                        timestamp = msg.getData().getLong("Timestamp");
                        if (firstRespirationRateTimestamp == 0L) {
                            firstRespirationRateTimestamp = timestamp;
                        }
                        time = (timestamp - firstRespirationRateTimestamp) / 1000.0;
                        Log.v(TAG, "timestamp: " + timestamp + ", time: " + time);
                        bhRespirationtRateValues[bhRespirationRateValueCount][0] = String.valueOf(time);
                        bhRespirationtRateValues[bhRespirationRateValueCount][1] = RespirationRatetext;
                        bhRespirationRateValueCount++;
                        System.out.println("RespirationRate Info is " + RespirationRatetext);
                        if(bhRespirationRateValueCount >= maxVals) {
                            bhRespirationRateValueCount = 0;
                            writeData(bhRespirationtRateValues, bhRespirationRateFilename);
                            bhRespirationtRateValues = new String[1000][2];
                        }
                        break;

                    case SKIN_TEMPERATURE:
                        String SkinTemperaturetext = msg.getData().getString("SkinTemperature");
                        timestamp = msg.getData().getLong("Timestamp");
                        if (firstSkinTemperatureTimestamp == 0L) {
                            firstSkinTemperatureTimestamp = timestamp;
                        }
                        time = (timestamp - firstSkinTemperatureTimestamp) / 1000.0;
                        bhSkinTemperatureValues[bhSkinTemperatureValueCount][0] = String.valueOf(time);
                        bhSkinTemperatureValues[bhSkinTemperatureValueCount][1] = SkinTemperaturetext;
                        bhSkinTemperatureValueCount++;
                        System.out.println("SkinTemperature Info is " + SkinTemperaturetext);
                        if(bhSkinTemperatureValueCount >= maxVals) {
                            bhSkinTemperatureValueCount = 0;
                            writeData(bhSkinTemperatureValues, bhSkinTemperatureFilename);
                            bhSkinTemperatureValues = new String[1000][2];
                        }
                        break;

                    case POSTURE:
                        String PostureText = msg.getData().getString("Posture");
                        timestamp = msg.getData().getLong("Timestamp");
                        if (firstbhPostureTimestamp == 0L) {
                            firstbhPostureTimestamp = timestamp;
                        }
                        time = (timestamp - firstbhPostureTimestamp) / 1000.0;
                        bhPostureValues[bhPostureValueCount][0] = String.valueOf(time);
                        bhPostureValues[bhPostureValueCount][1] = PostureText;
                        bhPostureValueCount++;
                        System.out.println("Posture Info is " + PostureText);
                        if(bhPostureValueCount >= maxVals) {
                            bhPostureValueCount = 0;
                            writeData(bhPostureValues, bhPostureFilename);
                            bhPostureValues = new String[1000][2];
                        }
                        break;

                    case PEAK_ACCLERATION:
                        String PeakAccText = msg.getData().getString("PeakAcceleration");
                        timestamp = msg.getData().getLong("Timestamp");
                        if (firstPeakAccelerationTimestamp == 0L) {
                            firstPeakAccelerationTimestamp = timestamp;
                        }
                        time = (timestamp - firstPeakAccelerationTimestamp) / 1000.0;
                        bhPeakAccelerationValues[bhPeakAccelerationValueCount][0] = String.valueOf(time);
                        bhPeakAccelerationValues[bhPeakAccelerationValueCount][1] = PeakAccText;
                        bhPeakAccelerationValueCount++;
                        System.out.println("PeakAcceleration Info is " + PeakAccText);
                        if(bhPeakAccelerationValueCount >= maxVals) {
                            bhPeakAccelerationValueCount = 0;
                            writeData(bhPeakAccelerationValues, bhPeakAccelerationFilename);
                            bhPeakAccelerationValues = new String[1000][2];
                        }
                        break;
                }
            }
        }
    }
    void writeData (String[][] data, String filename) {
        Log.d(TAG, "Write data in " + filename);
        String[][] copies = new String[data.length][2];
        System.arraycopy(data, 0, copies, 0, data.length - 1);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, filename), true));

            for (String[] copy : copies) {
                if (copy[0] != null) {
                    writer.write(copy[0] + "," + copy[1]);
                    writer.newLine();
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }

    void pauseAllSensors() {
        //internal sensors
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(locationListener);
        //shimmer sensors
        stopAllStreaming();
        //bio harness
        if(_bt != null && _NConnListener != null)
            _bt.removeConnectedEventListener(_NConnListener);
    }
    void resumeAllSensors() {
        //internal
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        //shimmer
        startAllStreaming();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        //bioharness
        if(_bt != null && _NConnListener != null)
            _bt.addConnectedEventListener(_NConnListener);
    }

    private void notifyBHReady() {
        Toast.makeText(this, "BioHarness " + getString(R.string.is_ready), Toast.LENGTH_LONG).show();
    }

    private ServiceConnection mTestServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d(TAG, "service connected");
            ShimmerService.LocalBinder binder = (ShimmerService.LocalBinder) service;
            mService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "service connected");
        }
    };

    private BroadcastReceiver myReceiver= new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            if(arg1.getIntExtra("ShimmerState", -1)!=-1){
                Log.v(TAG, "receiver receive");
            }
        }
    };
}