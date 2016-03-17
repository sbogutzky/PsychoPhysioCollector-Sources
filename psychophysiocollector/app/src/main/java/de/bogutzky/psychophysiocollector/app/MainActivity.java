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
import android.location.GpsStatus;
import android.location.LocationListener;
import android.location.LocationManager;
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
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import de.bogutzky.psychophysiocollector.app.bioharness.BioHarnessHandler;
import de.bogutzky.psychophysiocollector.app.bioharness.BioHarnessHandlerInterface;
import de.bogutzky.psychophysiocollector.app.bioharness.BioHarnessMainConfigurationActivity;
import de.bogutzky.psychophysiocollector.app.bioharness.BioHarnessService;
import de.bogutzky.psychophysiocollector.app.sensors.GPSListener;
import de.bogutzky.psychophysiocollector.app.sensors.InternalSensorManager;
import de.bogutzky.psychophysiocollector.app.questionnaire.Questionnaire;
import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuHandler;
import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuHandlerInterface;
import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuMainConfigurationActivity;
import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuService;

public class MainActivity extends ListActivity implements ShimmerImuHandlerInterface, BioHarnessHandlerInterface {

    private static final String TAG = "MainActivity";
    private static final int MSG_BLUETOOTH_ADDRESS = 1;
    private static final int REQUEST_ENABLE_BT = 707;
    private static final int PERMISSIONS_REQUEST = 900;
    private static final int TIMER_UPDATE = 1;
    private static final int TIMER_END = 2;
    private static final int REQUEST_MAIN_COMMAND_SHIMMER = 3;
    private static final int REQUEST_MAIN_COMMAND_BIOHARNESS = 4;

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

    private LocationManager locationManager;
    private LocationListener locationListener;

    private String gpsStatusText;

    private Vibrator vibrator;
    private long[] vibratorPatternFeedback = {0, 500, 200, 100, 100, 100, 100, 100};

    private String activityName = "";
    private String participantFirstName = "";
    private String participantLastName = "";
    //private Spinner questionnaireSpinner;
    //private Spinner baselineQuestionnaireSpinner;
    private String questionnairePath = "questionnaires/flow-short-scale-running.json";
    //private String baselineQuestionnaireFileName = "questionnaires/flow-short-scale-running.json";

    private boolean intervalConfigured = false;
    private int selfReportInterval;
    private int selfReportVariance;

    private MenuItem addMenuItem;
    private MenuItem connectMenuItem;
    private MenuItem disconnectMenuItem;
    private MenuItem startStreamMenuItem;
    private MenuItem stopStreamMenuItem;

    ShimmerImuService shimmerImuService;
    BioHarnessService bioHarnessService;

    private boolean isSessionStarted = false;
    private boolean isFirstSelfReportRequest;



    private InternalSensorManager internalSensorManager;

    private TextView infoGpsConnectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBtEnabled();

        deviceNames = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        setListAdapter(arrayAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        textViewTimer = (TextView) findViewById(R.id.text_view_timer);
        textViewTimer.setVisibility(View.INVISIBLE);

        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        participantFirstName = sharedPref.getString("participantFirstName", "");
        participantLastName = sharedPref.getString("participantLastName", "");

        String[] questionnaireFileNames = getResources().getStringArray(R.array.study_protocol_settings_questionnaire_file_names);
        String[] activities = getResources().getStringArray(R.array.study_protocol_settings_activities);
        activityName = activities[sharedPref.getInt("activityNamePosition", 0)];
        questionnairePath = "questionnaires/" + questionnaireFileNames[sharedPref.getInt("activityNamePosition", 0)];
        //questionnairePath = sharedPref.getString("questionnairePath", "questionnaires/flow-short-scale-running.json");
        //baselineQuestionnaireFileName = sharedPref.getString("baselineQuestionnairePath", "questionnaires/flow-short-scale-running.json");

        intervalConfigured = sharedPref.getBoolean("configureInterval", false);
        selfReportInterval = sharedPref.getInt("selfReportInterval", 15);
        selfReportVariance = sharedPref.getInt("selfReportVariance", 0);

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
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION},
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
            startSession();
            //showQuestionnaire(true);
        }

        if (id == R.id.action_stop_streaming) {
            this.isSessionStarted = false;
            this.stopTimestamp = System.currentTimeMillis();
            stopAllStreamingOfAllShimmerImus();
            stopStreamingBioHarness();
            stopStreamingInternalSensorData();

            if(intervalConfigured) {
                stopTimerThread();
            } else {
                feedbackNotification();
                showQuestionnaire();
            }

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
                infoShimmerImoConnectionStatus.setText(getString(R.string.info_number_connected, shimmerImuCount));
            }

            if(bioHarnessService != null) {
                if(bioHarnessService.isBioHarnessConnected()) {
                    TextView infoBioHarnessConnectionStatus = (TextView) dialog.findViewById(R.id.textViewInfoBioHarnessConnectionStatus);
                    infoBioHarnessConnectionStatus.setText(getString(R.string.info_number_connected, 1));
                }
            }

            infoGpsConnectionStatus = (TextView) dialog.findViewById(R.id.textViewInfoGpsConnectionStatus);
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

    @Override
    public void connectionResetted() {
        disconnectDevices();
        Toast.makeText(this, getString(R.string.connection_to_shimmer_imu_resetted), Toast.LENGTH_LONG).show();
    }

    public void startSession() {
        this.startStreamMenuItem.setEnabled(false);
        this.stopStreamMenuItem.setEnabled(true);
        this.disconnectMenuItem.setEnabled(false);

        this.startTimestamp = System.currentTimeMillis();
        createRootDirectory();

        if (intervalConfigured) {
            startTimerThread();
        }
        startStreamingOfAllShimmerImus(true);
        startStreamingBioHarness();
        startStreamingInternalSensorData();
        this.isSessionStarted = true;
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

        String participantFirstName = sharedPref.getString("participantFirstName", "");
        String participantLastName = sharedPref.getString("participantLastName", "");
        int activityPosition = sharedPref.getInt("activityNamePosition", 0);

        //int questionnaireSpinnerPosition = sharedPref.getInt("questionnaireSpinnerPosition", 0);
        //int baselineQuestionnaireSpinnerPosition = sharedPref.getInt("baselineQuestionnaireSpinnerPosition", 0);

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

        final EditText participantFirstNameEditText = (EditText) dialog.findViewById(R.id.participant_first_name_edit_text);
        final EditText participantLastNameEditText = (EditText) dialog.findViewById(R.id.participant_last_name_edit_text);
        participantFirstNameEditText.setText(participantFirstName);
        participantLastNameEditText.setText(participantLastName);

        final Spinner activitySpinner = (Spinner) dialog.findViewById(R.id.activity_spinner);
        ArrayAdapter<CharSequence> activityArrayAdapter = ArrayAdapter.createFromResource(this,
                R.array.study_protocol_settings_activities, android.R.layout.simple_spinner_item);
        activitySpinner.setAdapter(activityArrayAdapter);
        activitySpinner.setSelection(activityPosition);

        /*
        questionnaireSpinner = (Spinner) dialog.findViewById(R.id.questionnaireSpinner);
        baselineQuestionnaireSpinner = (Spinner) dialog.findViewById(R.id.baseline_questionnaireSpinner);
        AssetManager assetManager = getApplicationContext().getAssets();
        String[] questionnaires = new String[0];
        try {
            questionnaires = assetManager.list("questionnaires");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> questionnaireArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, questionnaires);
        questionnaireSpinner.setAdapter(questionnaireArrayAdapter);
        questionnaireSpinner.setSelection(questionnaireSpinnerPosition);
        baselineQuestionnaireSpinner.setAdapter(questionnaireArrayAdapter);
        baselineQuestionnaireSpinner.setSelection(baselineQuestionnaireSpinnerPosition);
        */

        final Spinner selfReportIntervalSpinner = (Spinner) dialog.findViewById(R.id.self_report_interval_spinner);
        ArrayAdapter<CharSequence> intervalArrayAdapter = ArrayAdapter.createFromResource(this,
                R.array.study_protocol_settings_self_report_interval_values, android.R.layout.simple_spinner_item);
        selfReportIntervalSpinner.setAdapter(intervalArrayAdapter);
        selfReportIntervalSpinner.setSelection(selfReportIntervalSpinnerPosition);

        final Spinner selfReportVarianceSpinner = (Spinner) dialog.findViewById(R.id.self_report_variance_spinner);
        ArrayAdapter<CharSequence> varianceArrayAdapter = ArrayAdapter.createFromResource(this,
                R.array.study_protocol_settings_self_report_variance_values, android.R.layout.simple_spinner_item);
        selfReportVarianceSpinner.setAdapter(varianceArrayAdapter);
        selfReportVarianceSpinner.setSelection(selfReportVarianceSpinnerPosition);

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
                MainActivity.this.participantFirstName = participantFirstNameEditText.getText().toString().trim();
                MainActivity.this.participantLastName = participantLastNameEditText.getText().toString().trim();

                MainActivity.this.activityName = activitySpinner.getSelectedItem().toString();
                String[] questionnaireFileNames = getResources().getStringArray(R.array.study_protocol_settings_questionnaire_file_names);
                questionnairePath = "questionnaires/" + questionnaireFileNames[activitySpinner.getSelectedItemPosition()];

                // questionnairePath = "questionnaires/" + questionnaireSpinner.getSelectedItem().toString();
                // baselineQuestionnaireFileName = "questionnaires/" + baselineQuestionnaireSpinner.getSelectedItem().toString();

                MainActivity.this.intervalConfigured = configureIntervalSwitch.isChecked();
                selfReportInterval = Integer.valueOf(selfReportIntervalSpinner.getSelectedItem().toString());
                selfReportVariance = Integer.valueOf(selfReportVarianceSpinner.getSelectedItem().toString());

                SharedPreferences.Editor editor = sharedPref.edit();

                editor.putString("participantFirstName", participantFirstNameEditText.getText().toString().trim());
                editor.putString("participantLastName", participantLastNameEditText.getText().toString().trim());
                editor.putInt("activityNamePosition", activitySpinner.getSelectedItemPosition());

                //editor.putInt("questionnaireSpinnerPosition", questionnaireSpinner.getSelectedItemPosition());
                //editor.putInt("baselineQuestionnaireSpinnerPosition", baselineQuestionnaireSpinner.getSelectedItemPosition());
                //editor.putString("questionnairePath", "questionnaires/" + questionnaireSpinner.getSelectedItem().toString());
                //editor.putString("baselineQuestionnairePath", "questionnaires/" + baselineQuestionnaireSpinner.getSelectedItem().toString());

                editor.putString("questionnairePath", questionnairePath);

                editor.putBoolean("configureInterval", configureIntervalSwitch.isChecked());
                editor.putInt("selfReportIntervalSpinnerPosition", selfReportIntervalSpinner.getSelectedItemPosition());
                editor.putInt("selfReportVarianceSpinnerPosition", selfReportVarianceSpinner.getSelectedItemPosition());
                editor.putInt("selfReportInterval", Integer.valueOf(selfReportIntervalSpinner.getSelectedItem().toString()));
                editor.putInt("selfReportVariance", Integer.valueOf(selfReportVarianceSpinner.getSelectedItem().toString()));

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
        if(bioHarnessService != null) {
            if(bioHarnessService.isBioHarnessConnected()) {
                if(deviceNames.get(position).contains("BH")) {
                    Object o = l.getItemAtPosition(position);
                    Intent intent = new Intent(MainActivity.this, BioHarnessMainConfigurationActivity.class);
                    intent.putExtra("DeviceName", o.toString());
                    intent.putExtra("BluetoothDeviceAddress", getBluetoothAddresses().get(position));
                    startActivityForResult(intent, REQUEST_MAIN_COMMAND_BIOHARNESS);
                }
            }
        }
    }

    private void createRootDirectory() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH-mm-ss", Locale.GERMAN);
        String dateString = simpleDateFormat.format(this.startTimestamp);
        String timeString = simpleTimeFormat.format(this.startTimestamp);
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
            case REQUEST_MAIN_COMMAND_SHIMMER:
                if(resultCode == Activity.RESULT_OK) {
                    int action = data.getIntExtra("action", 0);
                    String bluetoothDeviceAddress = data.getStringExtra("bluetoothDeviceAddress");
                    int which = data.getIntExtra("which", 0);
                    if(action == 13) {
                        showGraph(bluetoothDeviceAddress, REQUEST_MAIN_COMMAND_SHIMMER, which);
                    }
                }
                break;
            case REQUEST_MAIN_COMMAND_BIOHARNESS:
                if(resultCode == Activity.RESULT_OK) {
                    int action = data.getIntExtra("action", 0);
                    String bluetoothDeviceAddress = data.getStringExtra("bluetoothDeviceAddress");
                    if(action == 13) {
                        showGraph(bluetoothDeviceAddress, REQUEST_MAIN_COMMAND_BIOHARNESS, -1);
                    }
                }
                break;
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

    private void startStreamingOfAllShimmerImus(boolean saveData) {
        if(shimmerImuService != null)
            shimmerImuService.startStreamingAllShimmerImus(this.root, this.directoryName, this.startTimestamp, saveData);
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
                    showQuestionnaire();
                    break;
            }

            return true;
        }
    }

    void showQuestionnaire() { // boolean baselineQuestionnaire
        final Questionnaire questionnaire;
        //if(baselineQuestionnaire) {
        //    questionnaire = new Questionnaire(this, baselineQuestionnaireFileName);
        //} else {
            questionnaire = new Questionnaire(this, questionnairePath);
        //}
        Button saveButton = questionnaire.getSaveButton();
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String headerComments = "";
                if (isFirstSelfReportRequest)
                    headerComments = getHeaderComments();

                if (isSessionStarted) {
                    questionnaire.saveQuestionnaireItems(root, isFirstSelfReportRequest, headerComments, null, startTimestamp);
                    isFirstSelfReportRequest = false;
                    if (intervalConfigured) {
                        startTimerThread();
                    }
                } else {
                    questionnaire.saveQuestionnaireItems(root, isFirstSelfReportRequest, headerComments, getFooterComments(), startTimestamp);
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

   private void startStreamingInternalSensorData() {
       internalSensorManager = new InternalSensorManager(root, new int[]{1000, 1000, 1000}, this);
       internalSensorManager.startStreaming();

       locationListener = new GPSListener(getString(R.string.file_name_gps_position), this.directoryName, 25, this);
       if (ContextCompat.checkSelfPermission(this,
               Manifest.permission.ACCESS_FINE_LOCATION)
               != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
               Manifest.permission.ACCESS_COARSE_LOCATION)
               != PackageManager.PERMISSION_GRANTED) {
           ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION  },
                   PERMISSIONS_REQUEST);

       } else {
           locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
           locationManager.addGpsStatusListener(new GpsStatus.Listener() {
               @Override
               public void onGpsStatusChanged(int event) {
                   switch (event) {
                       case GpsStatus.GPS_EVENT_STARTED:
                           gpsStatusText = getString(R.string.info_connected);
                           break;
                       case GpsStatus.GPS_EVENT_STOPPED:
                           gpsStatusText = getString(R.string.info_not_connected);
                           break;
                       case GpsStatus.GPS_EVENT_FIRST_FIX:
                           gpsStatusText = getString(R.string.info_connected_fix_received);
                           break;
                   }
               }
           });
       }
   }

    private void  stopStreamingInternalSensorData() {
        internalSensorManager.stopStreaming();
        ((GPSListener)locationListener).stopStreaming();
        if(locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                Log.e(TAG, e.toString());
            }
        }
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

    private void showGraph(String bluetoothDeviceAddress, int requestCode, int which) {
        int beginAtField = 0;
        GraphView graphView = null;
        Dialog dialog = null;
        switch (requestCode) {
            case REQUEST_MAIN_COMMAND_SHIMMER:
                beginAtField = 1;
                switch (which) {
                    case 1:
                        beginAtField = 4;
                        break;
                }

                graphView = new GraphView(this, beginAtField);
                if(this.shimmerImuService != null) {
                    if(!isSessionStarted)
                        this.startStreamingOfAllShimmerImus(false);
                    this.shimmerImuService.startDataVisualization(bluetoothDeviceAddress, graphView);
                }
                dialog = new Dialog(this);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(shimmerImuService != null) {
                            shimmerImuService.stopDataVisualization();
                        }
                        if (!isSessionStarted) {
                            stopAllStreamingOfAllShimmerImus();
                        }
                    }
                });
                break;
            case REQUEST_MAIN_COMMAND_BIOHARNESS:
                graphView = new GraphView(this, beginAtField);
                if(this.bioHarnessService != null) {
                    this.bioHarnessService.startDataVisualization(graphView);
                }
                dialog = new Dialog(this);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(bioHarnessService != null) {
                            bioHarnessService.stopDataVisualization();
                        }
                    }
                });
                break;
        }

        assert dialog != null;
        dialog.setContentView(graphView);
        dialog.setTitle(getString(R.string.graph));
        dialog.setCancelable(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);

        dialog.show();
    }

    public void setGpsStatusText(String gpsStatusText) {
        this.gpsStatusText = gpsStatusText;
        if(infoGpsConnectionStatus != null) {
            infoGpsConnectionStatus.setText(gpsStatusText);
        }
    }

    public long getStartTimestamp() {
        return this.startTimestamp;
    }
}