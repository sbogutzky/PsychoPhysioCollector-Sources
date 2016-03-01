package de.bogutzky.psychophysiocollector.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import de.bogutzky.psychophysiocollector.app.bioharness.BioHarnessHandler;
import de.bogutzky.psychophysiocollector.app.bioharness.BioHarnessHandlerInterface;
import de.bogutzky.psychophysiocollector.app.bioharness.BioHarnessService;
import de.bogutzky.psychophysiocollector.app.questionnaire.Questionnaire;
import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuHandler;
import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuHandlerInterface;
import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuMainConfigurationActivity;
import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuService;

//import android.hardware.SensorManager;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import java.text.DecimalFormat;
//import java.text.DecimalFormatSymbols;

public class MainActivity extends ListActivity implements SensorEventListener, ShimmerImuHandlerInterface, BioHarnessHandlerInterface {

    private static final String TAG = "MainActivity";
    private static final int MSG_BLUETOOTH_ADDRESS = 1;
    private static final int REQUEST_ENABLE_BT = 707;
    private static final int PERMISSIONS_REQUEST = 900;
    private static final int TIMER_UPDATE = 1;
    private static final int TIMER_END = 2;
    private static final int REQUEST_MAIN_COMMAND_SHIMMER = 3;

    /*
    private static final int INTERNAL_SENSOR_CACHE_LENGTH = 1000;
    private static final int DATA_ARRAY_SIZE = 1000;
    */

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter arrayAdapter;
    private ArrayList<String> bluetoothAddresses;
    private ArrayList<String> deviceNames;

    private TextView textViewTimer;
    private Handler timerHandler;
    private Thread timerThread;
    private boolean timerThreadShouldContinue = false;

    private String directoryName;
    private File root;
    private long startTimestamp;
    private long stopTimestamp;

    /*
    private SensorManager sensorManager;
    private android.hardware.Sensor accelerometer;
    private android.hardware.Sensor gyroscope;
    private Sensor linearAccelerationSensor;
    private int sensorDataDelay = 20000; // ca. 50 Hz
    private LocationManager locationManager;
    private LocationListener locationListener;
    private float lastLocationAccuracy;
    */

    private String gpsStatusText;

    private Vibrator vibrator;
    private long[] vibratorPatternFeedback = {0, 500, 200, 100, 100, 100, 100, 100};

    private Spinner selfReportIntervalSpinner;
    private Spinner selfReportVarianceSpinner;
    private Spinner questionnaireSpinner;
    private Spinner initialQuestionnaireSpinner;
    private int selfReportInterval;
    private int selfReportVariance;
    private String questionnaireFileName = "questionnaires/flow-short-scale.json";
    private String initialQuestionnaireFileName = "questionnaires/flow-short-scale.json";

    /*
    private Double[][] accelerometerValues;
    private int accelerometerValueCount;
    private Double[][] gyroscopeValues;
    private int gyroscopeValueCount;
    private Double[][] linearAccelerationValues;
    private int linearAccelerationValueCount;
    */

    /*
    private long gyroscopeEventStartTimestamp;
    private long gyroscopeStartTimestamp;
    private long accelerometerEventStartTimestamp;
    private long accelerometerStartTimestamp;
    private long linearAccelerationSensorEventStartTimestamp;
    private long linearAccelerationSensorStartTimestamp;
    */

    private MenuItem addMenuItem;
    private MenuItem connectMenuItem;
    private MenuItem disconnectMenuItem;
    private MenuItem startStreamMenuItem;
    private MenuItem stopStreamMenuItem;

    ShimmerImuService shimmerImuService;
    BioHarnessService bioHarnessService;

    //private GraphView graphView;
    //private boolean graphShowing = false;
    //private String graphAdress = "";

    private boolean isSessionStarted = false;
    private boolean isFirstSelfReportRequest;

    //private boolean writingData = false;
    //private boolean secondWritingData = false;

    private String activityName = "";
    private String participantFirstName = "";
    private String participantLastName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBtEnabled();

        deviceNames = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        setListAdapter(arrayAdapter);

        /*
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        this.linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        this.lastLocationAccuracy = 0;
        */

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        textViewTimer = (TextView) findViewById(R.id.text_view_timer);
        textViewTimer.setVisibility(View.INVISIBLE);

        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        selfReportInterval = sharedPref.getInt("selfReportInterval", 15);
        selfReportVariance = sharedPref.getInt("selfReportVariance", 30);
        questionnaireFileName = sharedPref.getString("questionnaireValue", "questionnaires/flow-short-scale.json");
        initialQuestionnaireFileName = sharedPref.getString("initialQuestionnaireValue", "questionnaires/initial-questions.json");
        activityName = sharedPref.getString("activityName", "");
        participantFirstName = sharedPref.getString("participantFirstName", "");
        participantLastName = sharedPref.getString("participantLastName", "");

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final int index = position;

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle(getString(R.string.delete));
                builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        deviceNames.remove(index);
                        getBluetoothAddresses().remove(index);
                        arrayAdapter.notifyDataSetChanged();
                        if (getBluetoothAddresses().size() == 0) {
                            connectMenuItem.setEnabled(false);
                        }
                        disconnectBioHarness();
                        disconnectAllShimmerImus();
                    }
                });
                builder.create().show();
                return false;
            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADMIN},
                    PERMISSIONS_REQUEST);

        }
    }

    private void checkBtEnabled() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_not_supported), Toast.LENGTH_LONG).show();
        } else if(!bluetoothAdapter.isEnabled()) {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setCancelable(true);
            builder.setTitle(getString(R.string.activate_bluetooth));
            builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            });
            builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog = builder.create();
            dialog.show();
        }
    }

    private void resetTime() {
        this.startTimestamp = System.currentTimeMillis();
        /*
        this.gyroscopeEventStartTimestamp = 0L;
        this.accelerometerEventStartTimestamp = 0L;
        this.linearAccelerationSensorEventStartTimestamp = 0L;
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        this.connectMenuItem = menu.getItem(4);
        this.disconnectMenuItem = menu.getItem(5);
        this.startStreamMenuItem = menu.getItem(0);
        this.stopStreamMenuItem = menu.getItem(1);
        this.addMenuItem = menu.getItem(3);
        return true;
    }

    void disconnectDevices() {
        disconnectAllShimmerImus();
        disconnectBioHarness();

        connectMenuItem.setEnabled(true);
        disconnectMenuItem.setEnabled(false);
        addMenuItem.setEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_bluetooth_device) {
            addBluetoothDevice();
            return true;
        }

        if (id == R.id.action_connect) {
            disconnectMenuItem.setEnabled(true);
            connectMenuItem.setEnabled(false);
            addMenuItem.setEnabled(false);
            connectAllShimmerImus();
            connectBioHarness();
        }

        if (id == R.id.action_disconnect) {
            disconnectDevices();
        }

        if(id == R.id.action_settings) {
            showSettings();
        }

        if (id == R.id.action_start_streaming) {
            this.isFirstSelfReportRequest = true;
            showQuestionnaire(true);

            this.startStreamMenuItem.setEnabled(false);
            this.stopStreamMenuItem.setEnabled(true);
            this.disconnectMenuItem.setEnabled(false);

            if (this.directoryName == null) {
                createRootDirectory();
            }

            //startStreamingInternalSensorData();
        }

        if (id == R.id.action_stop_streaming) {
            this.stopTimestamp = System.currentTimeMillis();

            stopAllStreamingOfAllShimmerImus();
            stopStreamingBioHarness();
            this.isSessionStarted = false;
            stopTimerThread();
            //stopStreamingInternalSensorData();
            writeLeftOverData();

            this.directoryName = null;
            this.startStreamMenuItem.setEnabled(true);
            this.stopStreamMenuItem.setEnabled(false);
            this.disconnectMenuItem.setEnabled(true);
        }

        if (id == R.id.action_info) {
            Dialog dialog = new Dialog(this);
            dialog.setTitle(getString(R.string.action_info));
            dialog.setContentView(R.layout.info_popup);

            TextView infoSessionStatus = (TextView) dialog.findViewById(R.id.textViewInfoSessionStatus);
            if(isSessionStarted) {
                infoSessionStatus.setText(getString(R.string.info_started));
            }

            if(shimmerImuService != null) {
                int shimmerImuCount = shimmerImuService.shimmerImuMap.values().size();

                TextView infoShimmerImoConnectionStatus = (TextView) dialog.findViewById(R.id.textViewInfoShimmerImoConnectionStatus);
                infoShimmerImoConnectionStatus.setText(getString(R.string.info_connected, shimmerImuCount));
            }

            if(bioHarnessService != null) {
                if(bioHarnessService.isBioHarnessConnected()) {
                    TextView infoBioHarnessConnectionStatus = (TextView) dialog.findViewById(R.id.textViewInfoBioHarnessConnectionStatus);
                    infoBioHarnessConnectionStatus.setText(getString(R.string.info_connected, 1));
                }
            }

            TextView infoGpsConnectionStatus = (TextView) dialog.findViewById(R.id.textViewInfoGpsConnectionStatus);
            if(gpsStatusText == null) {
                gpsStatusText = getString(R.string.info_not_connected);
            }
            infoGpsConnectionStatus.setText(gpsStatusText);

            TextView infoVersionName = (TextView) dialog.findViewById(R.id.textViewInfoVersionName);
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                infoVersionName.setText(packageInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Could not read package info", e);
                infoVersionName.setText(R.string.info_could_not_read);
            }


            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void writeLeftOverData() {
        /*
        writingData = false;
        secondWritingData = false;
        //internal sensor data
        writeAccelerometerValues(true,1);
        writeGyroscopeValues(true,1);
        writeLinearAccelerationValues(true,1);
        ((GPSListener)locationListener).writeGpsValues(true,1);
         */
    }

    @Override
    public void connectionResetted() {
        disconnectDevices();
        Toast.makeText(this, getString(R.string.connection_to_shimmer_imu_resetted), Toast.LENGTH_LONG).show();
    }

    public String getHeaderComments() {
        return "# StartTime: " + Utils.getDateString(this.startTimestamp, "yyyy/MM/dd HH:mm:ss") + "\n";

    }

    public String getFooterComments() {
        return "# StopTime: " + Utils.getDateString(this.stopTimestamp, "yyyy/MM/dd HH:mm:ss");
    }

    private void showSettings() {
        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int selfReportIntervalSpinnerPosition = sharedPref.getInt("selfReportIntervalSpinnerPosition", 2);
        int selfReportVarianceSpinnerPosition = sharedPref.getInt("selfReportVarianceSpinnerPosition", 0);
        int questionnaireSpinnerPosition = sharedPref.getInt("questionnaireSpinnerPosition", 0);
        int initialQuestionnaireSpinnerPosition = sharedPref.getInt("initialQuestionnaireSpinnerPosition", 3);
        String activityName = sharedPref.getString("activityName", "");
        String participantFirstName = sharedPref.getString("participantFirstName", "");
        String participantLastName = sharedPref.getString("participantLastName", "");
        boolean configureInterval = sharedPref.getBoolean("configureInterval", false);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.settings);
        dialog.setTitle(getString(R.string.action_settings));
        dialog.setCancelable(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
        selfReportIntervalSpinner = (Spinner) dialog.findViewById(R.id.self_report_interval_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.study_protocol_settings_self_report_interval_values, android.R.layout.simple_spinner_item);
        selfReportIntervalSpinner.setAdapter(adapter);
        selfReportIntervalSpinner.setSelection(selfReportIntervalSpinnerPosition);
        selfReportVarianceSpinner = (Spinner) dialog.findViewById(R.id.self_report_variance_spinner);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.study_protocol_settings_self_report_variance_values, android.R.layout.simple_spinner_item);
        selfReportVarianceSpinner.setAdapter(adapter2);
        selfReportVarianceSpinner.setSelection(selfReportVarianceSpinnerPosition);

        questionnaireSpinner = (Spinner) dialog.findViewById(R.id.questionnaireSpinner);
        initialQuestionnaireSpinner = (Spinner) dialog.findViewById(R.id.initial_questionnaireSpinner);
        AssetManager assetManager = getApplicationContext().getAssets();
        String[] questionnaires = new String[0];
        try {
            questionnaires = assetManager.list("questionnaires");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> qSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, questionnaires);
        questionnaireSpinner.setAdapter(qSpinnerAdapter);
        questionnaireSpinner.setSelection(questionnaireSpinnerPosition);
        initialQuestionnaireSpinner.setAdapter(qSpinnerAdapter);
        initialQuestionnaireSpinner.setSelection(initialQuestionnaireSpinnerPosition);

        final EditText participantFirstNameEditText = (EditText) dialog.findViewById(R.id.participant_first_name_edit_text);
        final EditText participantLastNameEditText = (EditText) dialog.findViewById(R.id.participant_last_name_edit_text);
        final EditText activityNameEditText = (EditText) dialog.findViewById(R.id.activity_name_edit_text);
        participantFirstNameEditText.setText(participantFirstName);
        participantLastNameEditText.setText(participantLastName);
        activityNameEditText.setText(activityName);

        final Switch configureIntervalSwitch = (Switch) dialog.findViewById(R.id.configure_interval_switch);
        configureIntervalSwitch.setChecked(configureInterval);
        if(configureInterval) {
            dialog.findViewById(R.id.configure_interval_layout).setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.configure_variance_layout).setVisibility(View.VISIBLE);
        } else {
            dialog.findViewById(R.id.configure_interval_layout).setVisibility(View.GONE);
            dialog.findViewById(R.id.configure_variance_layout).setVisibility(View.GONE);
        }

        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                selfReportInterval = Integer.valueOf(selfReportIntervalSpinner.getSelectedItem().toString());
                selfReportVariance = Integer.valueOf(selfReportVarianceSpinner.getSelectedItem().toString());
                questionnaireFileName = "questionnaires/" + questionnaireSpinner.getSelectedItem().toString();
                initialQuestionnaireFileName = "questionnaires/" + initialQuestionnaireSpinner.getSelectedItem().toString();
                MainActivity.this.participantFirstName = participantFirstNameEditText.getText().toString();
                MainActivity.this.participantLastName = participantLastNameEditText.getText().toString();
                MainActivity.this.activityName = activityNameEditText.getText().toString();

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("selfReportIntervalSpinnerPosition", selfReportIntervalSpinner.getSelectedItemPosition());
                editor.putInt("selfReportVarianceSpinnerPosition", selfReportVarianceSpinner.getSelectedItemPosition());
                editor.putInt("questionnaireSpinnerPosition", questionnaireSpinner.getSelectedItemPosition());
                editor.putInt("initialQuestionnaireSpinnerPosition", initialQuestionnaireSpinner.getSelectedItemPosition());
                editor.putInt("selfReportInterval", Integer.valueOf(selfReportIntervalSpinner.getSelectedItem().toString()));
                editor.putInt("selfReportVariance", Integer.valueOf(selfReportVarianceSpinner.getSelectedItem().toString()));
                editor.putString("questionnaireValue", "questionnaires/" + questionnaireSpinner.getSelectedItem().toString());
                editor.putString("initialQuestionnaireValue", "questionnaires/" + initialQuestionnaireSpinner.getSelectedItem().toString());
                editor.putString("participantFirstName", participantFirstNameEditText.getText().toString());
                editor.putString("participantLastName", participantLastNameEditText.getText().toString());
                editor.putString("activityName", activityNameEditText.getText().toString());
                editor.putBoolean("configureInterval", configureIntervalSwitch.isChecked());
                editor.apply();

                dialog.dismiss();
            }
        });

        configureIntervalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    dialog.findViewById(R.id.configure_interval_layout).setVisibility(View.VISIBLE);
                    dialog.findViewById(R.id.configure_variance_layout).setVisibility(View.VISIBLE);
                } else {
                    dialog.findViewById(R.id.configure_interval_layout).setVisibility(View.GONE);
                    dialog.findViewById(R.id.configure_variance_layout).setVisibility(View.GONE);
                }
            }
        });
        dialog.show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if(shimmerImuService != null) {
            int shimmerImuCount = shimmerImuService.shimmerImuMap.values().size();
            if(shimmerImuCount > 0 && deviceNames.get(position).contains("RN42")) {
                Object o = l.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, ShimmerImuMainConfigurationActivity.class);
                intent.putExtra("DeviceName", o.toString());
                intent.putExtra("BluetoothDeviceAddress", getBluetoothAddresses().get(position));
                startActivityForResult(intent, REQUEST_MAIN_COMMAND_SHIMMER);
            }
        }
    }

    private void createRootDirectory() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH-mm-ss", Locale.GERMAN);
        String dateString = simpleDateFormat.format(new Date());
        String timeString = simpleTimeFormat.format(new Date());
        if(activityName.equals("")) activityName = getString(R.string.settings_undefined);
        if(participantLastName.equals("")) participantLastName = getString(R.string.settings_undefined);
        if(participantFirstName.equals("")) participantFirstName = getString(R.string.settings_undefined);
        this.directoryName = "PsychoPhysioCollector/" + activityName.toLowerCase() + "/" + participantLastName.toLowerCase() + "-" + participantFirstName.toLowerCase() + "/" + dateString + "--" + timeString;
        this.root = getStorageDirectory(this.directoryName);
    }

    @Override
    protected void onDestroy() {
        if(shimmerImuService != null) {
            stopService(new Intent(MainActivity.this, ShimmerImuService.class));
            shimmerImuService = null;
        }
        if(bioHarnessService != null) {
            stopService(new Intent(MainActivity.this, BioHarnessService.class));
            bioHarnessService = null;
        }

        if (timerThread != null) {
            stopTimerThread();
        }

        super.onDestroy();
    }

    private void addBluetoothDevice() {
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, MSG_BLUETOOTH_ADDRESS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK){
                    Toast.makeText(MainActivity.this, getString(R.string.bluetooth_activated), Toast.LENGTH_LONG).show();
                }
                break;

            case PERMISSIONS_REQUEST:
                if(resultCode == RESULT_OK){
                    Toast.makeText(MainActivity.this, getString(R.string.permission_granted), Toast.LENGTH_LONG).show();
                }
                break;

            case MSG_BLUETOOTH_ADDRESS:
                if (resultCode == Activity.RESULT_OK) {
                    String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Log.d(TAG, "Bluetooth address: " + bluetoothAddress);

                    // Check, if device is list view
                    if (!getBluetoothAddresses().contains(bluetoothAddress)) {
                        Log.d(TAG, "Bluetooth address of a new device");
                        addBluetoothAddress(bluetoothAddress);

                        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(bluetoothAddress);

                        String deviceName = device.getName();
                        if(deviceName == null) {
                            Log.d(TAG, "Device has no device name");
                            deviceName = bluetoothAddress + "";
                        }

                        // Change list view
                        deviceNames.add(deviceName);
                        arrayAdapter.notifyDataSetChanged();

                        // Check, if device is paired
                        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                        boolean paired = false;
                        for(BluetoothDevice pairedDevice:pairedDevices) {
                            if(pairedDevice.getAddress().equals(bluetoothAddress)) {
                                paired = true;
                                Log.d(TAG, "Device already paired");
                            }
                        }
                        if(!paired) {
                            pairBluetoothDevice(device);
                        }

                        if(device.getName().startsWith("RN42") & shimmerImuService == null) {
                            Intent intent = new Intent(this, ShimmerImuService.class);
                            startService(intent);
                            getApplicationContext().bindService(intent, shimmerImuServiceConnection, Context.BIND_AUTO_CREATE);
                            registerReceiver(shimmerImuReceiver, new IntentFilter("de.bogutzky.data_collector.app"));
                        }

                        if(device.getName().startsWith("BH") & bioHarnessService == null) {
                            Intent intent = new Intent(this, BioHarnessService.class);
                            startService(intent);
                            getApplicationContext().bindService(intent, bioHarnessServiceConnection, Context.BIND_AUTO_CREATE);
                            registerReceiver(bioHarnessReceiver, new IntentFilter("de.bogutzky.data_collector.app"));
                        }

                    } else {
                        Toast.makeText(this, getString(R.string.device_is_already_in_list), Toast.LENGTH_LONG).show();
                    }
                }
                break;
            /*
            case REQUEST_MAIN_COMMAND_SHIMMER:
                if(resultCode == Activity.RESULT_OK) {
                    int action = data.getIntExtra("action", 0);
                    graphAdress = data.getStringExtra("mac");
                    int which = data.getIntExtra("datastart", 0);
                    if(action == MainActivity.SHOW_GRAPH) {
                        showGraph(which);
                    }
                }

                break;
            */
        }
    }

    private ArrayList<String> getBluetoothAddresses() {
        if (bluetoothAddresses == null) {
            bluetoothAddresses = new ArrayList<>();
        }
        return bluetoothAddresses;
    }

    private void addBluetoothAddress(String bluetoothAddress) {
        getBluetoothAddresses().add(bluetoothAddress);
        if (getBluetoothAddresses().size() > 0) {
            connectMenuItem.setEnabled(true);
        }
    }

    private void connectAllShimmerImus() {
        if(bluetoothAdapter == null)
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        int count = 0;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains("RN42")) {
                    if(bluetoothAddresses.contains(device.getAddress())) {
                        if(shimmerImuService != null) {
                            shimmerImuService.connectShimmerImu(device.getAddress(), Integer.toString(count), new ShimmerImuHandler(this, "imu-" + device.getName().toLowerCase() + ".csv", 2500));
                            count++;
                        }
                    }
                }
            }
        }
    }

    private void disconnectAllShimmerImus() {
        if(shimmerImuService != null)
            shimmerImuService.disconnectAllShimmerImus();
    }

    private void startStreamingOfAllShimmerImus() {
        if(shimmerImuService != null)
            shimmerImuService.startStreamingAllShimmerImus(this.root, this.directoryName, this.startTimestamp);
    }

    private void stopAllStreamingOfAllShimmerImus() {
        if(shimmerImuService != null)
            shimmerImuService.stopStreamingAllShimmerImus();
    }

    private void connectBioHarness() {
        if (bluetoothAdapter == null)
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().startsWith("BH")) {
                    if (bluetoothAddresses.contains(device.getAddress())) {
                        if (bioHarnessService != null) {
                            bioHarnessService.connectBioHarness(device.getAddress(), new BioHarnessHandler(this, new int[]{100, 1000}));
                        }
                    }
                }
            }
        }
    }

    private void disconnectBioHarness() {
        if(bioHarnessService != null && bioHarnessService.isBioHarnessConnected())
            bioHarnessService.disconnectBioHarness();
    }

    private void startStreamingBioHarness() {
        if(bioHarnessService != null)
            bioHarnessService.startStreamingBioHarness(this.root, this.directoryName, this.startTimestamp);
    }

    private void stopStreamingBioHarness() {
        if(bioHarnessService != null)
            bioHarnessService.stopStreamingBioHarness();
    }

    class TimerHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message message) {

            switch (message.what) {
                case TIMER_UPDATE:
                    if (textViewTimer.getVisibility() == View.INVISIBLE) {
                        textViewTimer.setVisibility(View.VISIBLE);
                        textViewTimer.requestLayout();
                    }
                    int minutes = message.arg1 / 1000 / 60;
                    int seconds = message.arg1 / 1000 % 60;
                    String time = String.format("%02d:%02d", minutes, seconds);
                    textViewTimer.setText(time);
                    break;

                case TIMER_END:
                    feedbackNotification();
                    textViewTimer.setVisibility(View.INVISIBLE);
                    showQuestionnaire(false);
                    break;
            }

            return true;
        }
    }

    void showQuestionnaire(boolean initialQuestionnaire) {
        final Questionnaire questionnaire;
        if(initialQuestionnaire) {
            questionnaire = new Questionnaire(this, initialQuestionnaireFileName);
        } else {
            questionnaire = new Questionnaire(this, questionnaireFileName);
        }
        Button saveButton = questionnaire.getSaveButton();
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (isFirstSelfReportRequest) {
                    resetTime();
                    questionnaire.saveQuestionnaireItems(root, isFirstSelfReportRequest, getHeaderComments(), null, startTimestamp);
                    startTimerThread();
                    startStreamingOfAllShimmerImus();
                    startStreamingBioHarness();
                    isSessionStarted = true;
                    isFirstSelfReportRequest = false;
                } else if (isSessionStarted) {
                    questionnaire.saveQuestionnaireItems(root, false, getHeaderComments(), null, startTimestamp);
                    startTimerThread();
                } else {
                    questionnaire.saveQuestionnaireItems(root, false, null, getFooterComments(), startTimestamp);
                }
                questionnaire.getQuestionnaireDialog().dismiss();
            }
        });
    }

    private void startTimerThread() {
        timerThreadShouldContinue = true;
        timerHandler = new Handler(new TimerHandlerCallback());

        Random r = new Random();
        int variance = 0;
        if(selfReportVariance != 0)
            variance = r.nextInt(selfReportVariance *2) - selfReportVariance;
        long timerInterval = (long) (1000 * 60 * selfReportInterval) - (1000 * variance);
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

   /* private void startStreamingInternalSensorData() {

        this.accelerometerValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
        this.accelerometerValueCount = 0;
        this.gyroscopeValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
        this.gyroscopeValueCount = 0;
        this.linearAccelerationValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
        this.linearAccelerationValueCount = 0;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_acceleration)), true));
            String outputString = getHeaderComments();
            outputString += getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_acceleration_x) + "," + getString(R.string.file_header_acceleration_y) + "," + getString(R.string.file_header_acceleration_z);
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_angular_velocity)), true));
            String outputString = getHeaderComments();
            outputString += getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_angular_velocity_x) + "," + getString(R.string.file_header_angular_velocity_y) + "," + getString(R.string.file_header_angular_velocity_z);
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_linear_acceleration)), true));
            String outputString = getHeaderComments();
            outputString += getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_acceleration_x) + "," + getString(R.string.file_header_acceleration_y) + "," + getString(R.string.file_header_acceleration_z);
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }

        sensorManager.registerListener(this, accelerometer, sensorDataDelay);
        sensorManager.registerListener(this, gyroscope, sensorDataDelay);
        sensorManager.registerListener(this, linearAccelerationSensor, sensorDataDelay);

        locationListener = new GPSListener(getString(R.string.file_name_gps_position), this.directoryName, 100);
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.addGpsStatusListener(new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                    switch (event) {
                        case GpsStatus.GPS_EVENT_STARTED:
                            gpsStatusText = "GPS " + getString(R.string.info_connected);
                            break;
                        case GpsStatus.GPS_EVENT_STOPPED:
                            gpsStatusText = "GPS " + getString(R.string.info_not_connected);
                            break;
                        case GpsStatus.GPS_EVENT_FIRST_FIX:
                            gpsStatusText = "GPS " + getString(R.string.info_connected_fix_received);
                            break;
                    }
                }
            });
        }
    } */
/*
    private void stopStreamingInternalSensorData() {
        sensorManager.unregisterListener(this);
        if(locationManager != null)
            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(locationListener);
            }
    }*/

    @Override
    public void onSensorChanged(SensorEvent event) {
    }

    /*
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (loggingEnabled) {
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {

                if (this.accelerometerEventStartTimestamp == 0L) {
                    this.accelerometerEventStartTimestamp = event.timestamp; // Nanos
                    this.accelerometerStartTimestamp = System.currentTimeMillis() - this.startTimestamp; // Millis
                }
                double relativeTimestamp = this.accelerometerStartTimestamp + (event.timestamp - this.accelerometerEventStartTimestamp) / 1000000.0;

                accelerometerValues[accelerometerValueCount][0] = Double.parseDouble(decimalFormat.format(relativeTimestamp));
                accelerometerValues[accelerometerValueCount][1] = Double.parseDouble(Float.toString(event.values[0]));
                accelerometerValues[accelerometerValueCount][2] = Double.parseDouble(Float.toString(event.values[1]));
                accelerometerValues[accelerometerValueCount][3] = Double.parseDouble(Float.toString(event.values[2]));

                accelerometerValueCount++;
                if(accelerometerValueCount > accelerometerValues.length - 2) {
                    if(!writingData) {
                        accelerometerValueCount = 0;
                        setWritingData(true);
                        writeAccelerometerValues(false, 1);
                        linearAccelerationValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else if(!secondWritingData) {
                        accelerometerValueCount = 0;
                        setSecondWritingData(true);
                        writeAccelerometerValues(false, 2);
                        linearAccelerationValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else {
                        if(accelerometerValueCount > 5000) {
                            writeAccelerometerValues(false, 2);
                        } else {
                            accelerometerValues = resizeArray(accelerometerValues);
                        }
                    }
                }
            }
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE) {
                if (this.gyroscopeEventStartTimestamp == 0L) {
                    this.gyroscopeEventStartTimestamp = event.timestamp; // Nanos
                    this.gyroscopeStartTimestamp = System.currentTimeMillis() - this.startTimestamp; // Millis
                }
                double relativeTimestamp = this.gyroscopeStartTimestamp + (event.timestamp - this.gyroscopeEventStartTimestamp) / 1000000.0;

                gyroscopeValues[gyroscopeValueCount][0] = Double.parseDouble(decimalFormat.format(relativeTimestamp));
                gyroscopeValues[gyroscopeValueCount][1] = Double.parseDouble(Float.toString((float) (event.values[0] * 180.0 / Math.PI)));
                gyroscopeValues[gyroscopeValueCount][2] = Double.parseDouble(Float.toString((float) (event.values[1] * 180.0 / Math.PI)));
                gyroscopeValues[gyroscopeValueCount][3] = Double.parseDouble(Float.toString((float) (event.values[2] * 180.0 / Math.PI)));

                gyroscopeValueCount++;
                if(gyroscopeValueCount > gyroscopeValues.length - 2) {
                    if(!writingData) {
                        gyroscopeValueCount = 0;
                        setWritingData(true);
                        writeGyroscopeValues(false, 1);
                        gyroscopeValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else if(!secondWritingData) {
                        gyroscopeValueCount = 0;
                        setSecondWritingData(true);
                        writeGyroscopeValues(false, 2);
                        gyroscopeValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else {
                        if(gyroscopeValueCount > 5000) {
                            writeGyroscopeValues(false, 2);
                        } else{
                            gyroscopeValues = resizeArray(gyroscopeValues);
                        }
                    }
                }
            }
            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

                if (this.linearAccelerationSensorEventStartTimestamp == 0L) {
                    this.linearAccelerationSensorEventStartTimestamp = event.timestamp; // Nanos
                    this.linearAccelerationSensorStartTimestamp = System.currentTimeMillis() - this.startTimestamp; // Millis
                }
                double relativeTimestamp = this.linearAccelerationSensorStartTimestamp + (event.timestamp - this.linearAccelerationSensorEventStartTimestamp) / 1000000.0;

                linearAccelerationValues[linearAccelerationValueCount][0] = Double.parseDouble(decimalFormat.format(relativeTimestamp));
                linearAccelerationValues[linearAccelerationValueCount][1] = Double.parseDouble(Float.toString(event.values[0]));
                linearAccelerationValues[linearAccelerationValueCount][2] = Double.parseDouble(Float.toString(event.values[1]));
                linearAccelerationValues[linearAccelerationValueCount][3] = Double.parseDouble(Float.toString(event.values[2]));

                linearAccelerationValueCount++;
                if(linearAccelerationValueCount > linearAccelerationValues.length - 2) {
                    if(!writingData) {
                        linearAccelerationValueCount = 0;
                        setWritingData(true);
                        writeLinearAccelerationValues(false, 1);
                        accelerometerValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else if(!secondWritingData) {
                        linearAccelerationValueCount = 0;
                        setSecondWritingData(true);
                        writeLinearAccelerationValues(false, 2);
                        accelerometerValues = new Double[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else {
                        if(linearAccelerationValueCount > 5000) {
                            writeLinearAccelerationValues(false, 2);
                        } else {
                            linearAccelerationValues = resizeArray(linearAccelerationValues);
                        }
                    }
                }
            }
        }
    }

    private void writeLinearAccelerationValues(boolean footer, int slot) {
        if(footer)
            writeData(linearAccelerationValues, getString(R.string.file_name_linear_acceleration), 4, linearAccelerationValues.length, getFooterComments());
        else
            writeData(linearAccelerationValues, getString(R.string.file_name_linear_acceleration), 4, linearAccelerationValues.length, "");
    }

    private void writeGyroscopeValues(boolean footer, int slot) {
        if(footer)
            writeData(gyroscopeValues, getString(R.string.file_name_angular_velocity), 4, gyroscopeValues.length, getFooterComments());
        else
            writeData(gyroscopeValues, getString(R.string.file_name_angular_velocity), 4, gyroscopeValues.length, "");
    }

    private void writeAccelerometerValues(boolean footer, int slot) {
        if(footer)
            writeData(accelerometerValues, getString(R.string.file_name_acceleration), 4, accelerometerValues.length, getFooterComments());
        else
            writeData(accelerometerValues, getString(R.string.file_name_acceleration), 4, accelerometerValues.length, "");
    }

    private void writeFooter (String data, String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, filename), true));
            writer.write(data);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    } */

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

    public File getStorageDirectory(String directoryName) {
        try {
            File root = Environment.getExternalStorageDirectory();
            if (root.canWrite()) {
                File file = new File(root, directoryName);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        Log.d(TAG, "Could not write file: " + directoryName);
                    } else {
                        Log.d(TAG, "Created: " + directoryName);
                    }
                }
                return file;
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not write file: " + directoryName + " " + e.getMessage());
        }
        return null;
    }

    /*
    public class GPSListener implements LocationListener {
        private static final String TAG = "GPSListener";
        private String filename;
        private String directoryName;
        private File root;
        private int i = 0;
        private int maxValueCount;
        private Double[][] values;

        public GPSListener(String filename, String directoryName, int maxValueCount) {
            this.filename = filename;
            this.directoryName = directoryName;

            this.root = getStorageDirectory(this.directoryName);

            this.maxValueCount = maxValueCount;
            this.values = new Double[maxValueCount][4];

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));
                String outputString = getHeaderComments();
                outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_gps_latitude) + "," + getString(R.string.file_header_gps_longitude) + "," + getString(R.string.file_header_gps_altitude);
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
                double time = location.getTime() - startTimestamp;
                values[i][0] = time;
                values[i][1] = location.getLatitude();
                values[i][2] = location.getLongitude();
                values[i][3] = location.getAltitude();

                i++;
                if(i > maxValueCount - 1) {
                    if(!writingData) {
                        i = 0;
                        setWritingData(true);
                        writeGpsValues(false, 1);
                        values = new Double[maxValueCount][4];
                    } else if(!secondWritingData) {
                        i = 0;
                        setSecondWritingData(true);
                        writeGpsValues(false, 2);
                        values = new Double[maxValueCount][4];
                    } else {
                        values = resizeArray(values);
                    }
                }
                if(lastLocationAccuracy - location.getAccuracy() > 5.0) {
                    gpsStatusText = "GPS " + getText(R.string.info_connected_fix_received) + getString(R.string.accuracy) + location.getAccuracy();
                    lastLocationAccuracy = location.getAccuracy();
                }
            }
        }

        public void writeGpsValues(boolean footer, int slot) {
            if(footer)
                writeData(values,this.filename,4, values.length, getFooterComments());
            else
                writeData(values,this.filename,4, values.length, "");
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
    } */

    private void pairBluetoothDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, "Error during the pairing", e);
        }
    }

    private ServiceConnection shimmerImuServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            ShimmerImuService.LocalBinder binder = (ShimmerImuService.LocalBinder) service;
            shimmerImuService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private ServiceConnection bioHarnessServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            BioHarnessService.LocalBinder binder = (BioHarnessService.LocalBinder) service;
            bioHarnessService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private BroadcastReceiver shimmerImuReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action = arg1.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state  = arg1.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = arg1.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(getApplicationContext(), R.string.shimmer_imu_paired, Toast.LENGTH_SHORT).show();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(getApplicationContext(), R.string.shimmer_imu_removed, Toast.LENGTH_SHORT).show();
                }

            }
        }
    };

    private BroadcastReceiver bioHarnessReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action = arg1.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = arg1.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = arg1.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(getApplicationContext(), R.string.bio_harness_paired, Toast.LENGTH_SHORT).show();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(getApplicationContext(), R.string.bio_harness_removed, Toast.LENGTH_SHORT).show();
                }

            }
        }
    };
/*
    private Double[][] resizeArray(Double[][] original) {
        Double[][] copy = new Double[original.length + INTERNAL_SENSOR_CACHE_LENGTH][original[0].length];
        System.arraycopy(original,0,copy,0,original.length);
        Log.v(TAG, "original length: " + original.length + ", copy length: " + copy.length);
        return copy;
    }

    public void setSecondWritingData(boolean d) {
        this.secondWritingData = d;
        //Log.v(TAG, "secondWritingData: " + secondWritingData);
    }

    public void setWritingData(boolean d) {
        this.writingData = d;
        //Log.v(TAG, "writingdata: " + writingData);
    }
    */

    /*
    private void showGraph(int which) {
        graphView = new GraphView(this, which);
        graphShowing = true;
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(graphView);
        dialog.setTitle(getString(R.string.graph));
        dialog.setCancelable(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                graphShowing = false;
            }
        });

        dialog.show();
    }
    */
}