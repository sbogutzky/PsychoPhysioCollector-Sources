package de.bogutzky.psychophysiocollector.app;

import android.annotation.SuppressLint;
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
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
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
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.Set;

public class MainActivity extends ListActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final int MSG_BLUETOOTH_ADDRESS = 1;
    private final static int REQUEST_ENABLE_BT = 707;
    private static final int TIMER_UPDATE = 1;
    private static final int TIMER_END = 2;
    private static final int INTERNAL_SENSOR_CACHE_LENGTH = 1000;
    private static final int DATA_ARRAY_SIZE = 1000;
    private boolean loggingEnabled = false;
    private ArrayAdapter adapter;
    private ArrayList<String> bluetoothAddresses;
    private ArrayList<String> deviceNames;
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
    private String gpsStatusText;
    private float lastLocationAccuracy;
    private Vibrator vibrator;
    private long[] vibratorPatternFeedback = {0, 500, 200, 100, 100, 100, 100, 100};
    private long[] vibratorPatternConnectionLost = {0, 100, 100, 100, 100, 100, 100, 100};
    private Spinner scale_timerSpinner;
    private Spinner scale_timerVarianceSpinner;
    private Spinner questionnaireSpinner;
    private int scaleTimerValue;
    private int scaleTimerVarianceValue;
    private String[][] accelerometerValues;
    private int accelerometerValueCount;
    private String[][] gyroscopeValues;
    private int gyroscopeValueCount;
    private String[][] linearAccelerationValues;
    private int linearAccelerationValueCount;
    private String[][] bhHeartRateValues;
    private int bhHeartRateValueCount;
    private String[][] bhRespirationRateValues;
    private int bhRespirationRateValueCount;
    private String[][] bhSkinTemperatureValues;
    private int bhSkinTemperatureValueCount;
    private String[][] bhPostureValues;
    private int bhPostureValueCount;
    private String[][] bhPeakAccelerationValues;
    private int bhPeakAccelerationValueCount;
    private String[][] bhRRIntervalValues;
    private int bhRRIntervalValueCount;
    private String[][] bhAxisAccelerationValues;
    private int bhAxisAccelerationValueCount;
    private String[][] bhBreathingValues;
    private int bhBreathingValueCount;
    private String[][] bhEcgValues;
    private int bhEcgValueCount;

    private String questionnaireFileName = "questionnaires/fks.json";
    private JSONObject questionnaire;

    /* min api 9*/
    private Sensor linearAccelerationSensor;

    private long startTimestamp;
    private long questionnaireTimestamp;
    private long gyroscopeEventStartTimestamp;
    private long gyroscopeStartTimestamp;
    private long accelerometerEventStartTimestamp;
    private long accelerometerStartTimestamp;
    private long linearAccelerationSensorEventStartTimestamp;
    private long linearAccelerationSensorStartTimestamp;
    private Long firstbhHeartRateTimestamp;
    private Long firstRespirationRateTimestamp;
    private Long firstSkinTemperatureTimestamp;
    private Long firstbhPostureTimestamp;
    private Long firstPeakAccelerationTimestamp;
    private Long firstRRIntervalTimestamp;
    private Long firstAxisAccelerationTimestamp;
    private Long firstBreathingTimestamp;
    private Long firstEcgTimestamp;
    private Long bhStartTimestamp;

    private DecimalFormat decimalFormat;

    //bth adapter
    private BluetoothAdapter btAdapter = null;
    private final int BREATHING_MSG_ID = 0x21;
    private final int ECG_MSG_ID = 0x22;
    private final int RtoR_MSG_ID = 0x24;
    private final int ACCEL_100mg_MSG_ID = 0x2A;

    private final int POSTURE = 0x103;
    private final int HEART_RATE = 0x100;
    private final int RESPIRATION_RATE = 0x101;
    private final int SKIN_TEMPERATURE = 0x102;
    private final int PEAK_ACCLERATION = 0x104;

    public final static int REQUEST_MAIN_COMMAND_SHIMMER=3;
    public final static int REQUEST_COMMANDS_SHIMMER=4;
    public static final int REQUEST_CONFIGURE_SHIMMER = 5;
    public static final int SHOW_GRAPH = 13;

    private MenuItem connectMenuItem;
    private MenuItem disconnectMenuItem;
    private MenuItem startStreamMenuItem;
    private MenuItem stopStreamMenuItem;

    private boolean connected = false;
    private boolean bioHarnessConnected = false;

    private ArrayList<String> scaleTypes;
    private ArrayList<Integer> scaleViewIds;

    private boolean wroteQuestionnaireHeader = false;

    SensorService mService;
    private GraphView graphView;
    private boolean graphShowing = false;
    private String graphAdress = "";

    private Date startLoggingDate = null;
    private Date stopLoggingDate = null;

    private int sensorDataDelay = 20000; // ca. 50 Hz
    private boolean startedStreaming = false;

    private boolean writingData = false;
    private boolean secondWritingData = false;

    private BioHarnessHandler bioHarnessHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBtEnabled();

        deviceNames = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNames);
        setListAdapter(adapter);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        this.linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        this.lastLocationAccuracy = 0;

        timerCycleInMin = 15;

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#.###", otherSymbols);
        decimalFormat.setMinimumFractionDigits(3);

        textViewTimer = (TextView) findViewById(R.id.text_view_timer);
        textViewTimer.setVisibility(View.INVISIBLE);

        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        scaleTimerValue = sharedPref.getInt("scaleTimerValue", 15);
        scaleTimerVarianceValue = sharedPref.getInt("scaleTimerVarianceValue", 30);
        questionnaireFileName = sharedPref.getString("questionnaireValue", "questionnaires/fks.json");
        timerCycleInMin = scaleTimerValue;

        questionnaire = readQuestionnaireFromJSON();

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
                        adapter.notifyDataSetChanged();
                        if (getBluetoothAddresses().size() == 0) {
                            connectMenuItem.setEnabled(false);
                        }
                        disconnectBioHarness();
                        disconnectedAllShimmers();
                    }
                });
                builder.create().show();
                return false;
            }
        });
    }

    private void checkBtEnabled() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_not_supported), Toast.LENGTH_LONG).show();
        } else if(!btAdapter.isEnabled()) {
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
        this.gyroscopeEventStartTimestamp = 0L;
        this.accelerometerEventStartTimestamp = 0L;
        this.linearAccelerationSensorEventStartTimestamp = 0L;
        this.firstbhHeartRateTimestamp = 0L;
        this.firstRespirationRateTimestamp = 0L;
        this.firstSkinTemperatureTimestamp = 0L;
        this.firstbhPostureTimestamp = 0L;
        this.firstPeakAccelerationTimestamp = 0L;
        this.firstRRIntervalTimestamp = 0L;
        this.firstAxisAccelerationTimestamp = 0L;
        this.firstBreathingTimestamp = 0L;
        this.firstEcgTimestamp = 0L;
        this.bhStartTimestamp = 0L;
    }

    private JSONObject readQuestionnaireFromJSON() {
        BufferedReader input = null;
        JSONObject jsonObject = null;
        try {
            input = new BufferedReader(new InputStreamReader(
                    getAssets().open(questionnaireFileName)));
            StringBuffer content = new StringBuffer();
            char[] buffer = new char[1024];
            int num;
            while ((num = input.read(buffer)) > 0) {
                content.append(buffer, 0, num);
            }
            jsonObject = new JSONObject(content.toString());

        }catch (IOException e) {

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        this.connectMenuItem = menu.getItem(4);
        this.disconnectMenuItem = menu.getItem(5);
        this.startStreamMenuItem = menu.getItem(0);
        this.stopStreamMenuItem = menu.getItem(1);
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
            disconnectMenuItem.setEnabled(true);
            connectMenuItem.setEnabled(false);
            connectedAllShimmers();
            connectBioHarness();
        }

        if (id == R.id.action_disconnect) {
            disconnectedAllShimmers();
            disconnectBioHarness();
            this.directoryName = null;
            connectMenuItem.setEnabled(true);
            disconnectMenuItem.setEnabled(false);
            connected = false;
        }

        if(id == R.id.action_settings) {
            showSettings();
        }

        if (id == R.id.action_start_streaming) {
            startLoggingDate = new Date();
            questionnaireTimestamp = System.currentTimeMillis();
            resetTime();
            wroteQuestionnaireHeader = false;
            loggingEnabled = true;
            startedStreaming = true;
            this.startStreamMenuItem.setEnabled(false);
            this.stopStreamMenuItem.setEnabled(true);
            if (this.directoryName == null) {
                createRootDirectory();
            }
            if(bioHarnessConnected) {
                resetBioharnessStorage();
            }
            startAllStreaming();
            startTimerThread();
            //startStreamingInternalSensorData();
        }

        if (id == R.id.action_stop_streaming) {
            stopLoggingDate = new Date();
            stopAllStreaming();
            stopTimerThread();
            //stopStreamingInternalSensorData();
            writeLeftOverData();
            startTimestamp = 0L;
            loggingEnabled = false;
            startedStreaming = false;
            this.directoryName = null;
            this.startStreamMenuItem.setEnabled(true);
            this.stopStreamMenuItem.setEnabled(false);
        }

        if (id == R.id.action_info) {
            Dialog dialog = new Dialog(this);
            dialog.setTitle(getString(R.string.action_info));
            dialog.setContentView(R.layout.info_popup);
            TextView statusTV = (TextView) dialog.findViewById(R.id.info_statusTextView);
            if(startedStreaming) {
                statusTV.setText(getString(R.string.info_streaming));
            } else {
                statusTV.setText(getString(R.string.info_not_streaming));
            }
            TextView bhTV = (TextView) dialog.findViewById(R.id.info_bh_connectedTextView);
            TextView shimmerTV = (TextView) dialog.findViewById(R.id.info_shimmer_connectedTextView);
            int shimmerCount = 0;
            int bhCount = 0;
            if(mService != null) {
                shimmerCount = mService.mMultiShimmer.values().size();
                if(mService.hasBioHarnessConnected())
                    bhCount = 1;
            }
            shimmerTV.setText(shimmerCount  + "" + getText(R.string.info_connected));
            bhTV.setText(bhCount + "" + getText(R.string.info_connected));
            TextView gpsStatusTV = (TextView) dialog.findViewById(R.id.info_gps_status);
            if(gpsStatusText == null)
                gpsStatusText = getString(R.string.gps_not_connected);

            gpsStatusTV.setText(gpsStatusText);

            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void writeLeftOverData() {
        /*writingData = false;
        secondWritingData = false;
        //internal sensor data
        writeAccelerometerValues(true,1);
        writeGyroscopeValues(true,1);
        writeLinearAccelerationValues(true,1);
        ((GPSListener)locationListener).writeGpsValues(true,1);

        //all shimmer data
        if(mService != null) {
            Collection<Object> colS = mService.mMultiShimmer.values();
            Iterator<Object> iterator = colS.iterator();
            while (iterator.hasNext()) {
                Shimmer stemp = (Shimmer) iterator.next();
                ((ShimmerHandler)stemp.mHandler).writeShimmerValues(true, 1);
            }

            if(bioHarnessConnected) {
                //bh data
                writeData(bhRRIntervalValues, getString(R.string.file_name_rr_interval), 2, true, getLoggingFooterString(), 1);
                writeData(bhRespirationRateValues, getString(R.string.file_name_respiration_rate), 2, true, getLoggingFooterString(), 1);
                writeData(bhPostureValues, getString(R.string.file_name_posture), 2, true, getLoggingFooterString(), 1);
                writeData(bhPeakAccelerationValues, getString(R.string.file_name_peak_acceleration), 2, true, getLoggingFooterString(), 1);
                writeData(bhAxisAccelerationValues, getString(R.string.file_name_axis_acceleration), 2, true, getLoggingFooterString(), 1);
                writeData(bhBreathingValues, getString(R.string.file_name_breathing), 2, true, getLoggingFooterString(), 1);
                writeData(bhEcgValues, getString(R.string.file_name_ecg), 2, true, getLoggingFooterString(), 1);
            }
        }*/
    }

    private void disconnectBioHarness() {
        if(mService != null && bioHarnessConnected)
            mService.disconnectBioHarness();
    }

    private String getLoggingHeaderString() {
        String outputString = "";
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        outputString += "# StartTime: " + dateFormat.format(startLoggingDate) + "\n";
        return outputString;
    }

    private String getLoggingFooterString() {
        String outputString = "";
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        outputString += "# StopTime: " + dateFormat.format(stopLoggingDate) + "\n";
        return outputString;
    }

    private void showSettings() {
        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int selectedTimePos = sharedPref.getInt("scaleTimerValuePos", 2);
        int selectedVariancePos = sharedPref.getInt("scaleTimerVarianceValuePos", 0);
        int questionnairePos = sharedPref.getInt("questionnairePos", 0);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.settings);
        dialog.setTitle(getString(R.string.action_settings));
        dialog.setCancelable(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
        scale_timerSpinner = (Spinner) dialog.findViewById(R.id.scala_timer_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.test_protocol_settings_interval_values, android.R.layout.simple_spinner_item);
        scale_timerSpinner.setAdapter(adapter);
        scale_timerSpinner.setSelection(selectedTimePos);
        scale_timerVarianceSpinner = (Spinner) dialog.findViewById(R.id.scala_variance_spinner);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.test_protocol_settings_interval_variance_values, android.R.layout.simple_spinner_item);
        scale_timerVarianceSpinner.setAdapter(adapter2);
        scale_timerVarianceSpinner.setSelection(selectedVariancePos);

        questionnaireSpinner = (Spinner) dialog.findViewById(R.id.questionnaireSpinner);
        AssetManager assetManager = getApplicationContext().getAssets();
        String[] questionnaires = new String[0];
        try {
            questionnaires = assetManager.list("questionnaires");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> qSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, questionnaires);
        questionnaireSpinner.setAdapter(qSpinnerAdapter);
        questionnaireSpinner.setSelection(questionnairePos);
        Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                scaleTimerValue = Integer.valueOf(scale_timerSpinner.getSelectedItem().toString());
                scaleTimerVarianceValue = Integer.valueOf(scale_timerVarianceSpinner.getSelectedItem().toString());
                timerCycleInMin = scaleTimerValue;

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("scaleTimerValuePos", scale_timerSpinner.getSelectedItemPosition());
                editor.putInt("scaleTimerVarianceValuePos", scale_timerVarianceSpinner.getSelectedItemPosition());
                editor.putInt("questionnairePos", questionnaireSpinner.getSelectedItemPosition());
                editor.putInt("scaleTimerValue", Integer.valueOf(scale_timerSpinner.getSelectedItem().toString()));
                editor.putInt("scaleTimerVarianceValue", Integer.valueOf(scale_timerVarianceSpinner.getSelectedItem().toString()));
                editor.putString("questionnaireValue", "questionnaires/" + questionnaireSpinner.getSelectedItem().toString());
                editor.commit();
                questionnaireFileName = "questionnaires/" + questionnaireSpinner.getSelectedItem().toString();
                questionnaire = readQuestionnaireFromJSON();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if(connected && deviceNames.get(position).contains("RN42")) {
            Object o = l.getItemAtPosition(position);
            Intent mainCommandIntent=new Intent(MainActivity.this, ShimmerMainConfigurationActivity.class);
            mainCommandIntent.putExtra("LocalDeviceID", o.toString());
            String mac = getBluetoothAddresses().get(position);
            mainCommandIntent.putExtra("mac", mac);
            mainCommandIntent.putExtra("CurrentSlot", position);
            mainCommandIntent.putExtra("requestCode", REQUEST_MAIN_COMMAND_SHIMMER);
            startActivityForResult(mainCommandIntent, REQUEST_MAIN_COMMAND_SHIMMER);
        }
    }

    private void connectBioHarness() {
        if (btAdapter != null) {
            String bioHarnessBtDeviceAdress = getBioHarnessBtDeviceAdress(btAdapter);

            if(bioHarnessBtDeviceAdress != null) {
                if(mService != null) {
                    bioHarnessHandler = new BioHarnessHandler();
                    mService.connectBioHarness(bioHarnessHandler, bioHarnessBtDeviceAdress);
                }
            }
        }

    }

    private void resetBioharnessStorage() {
        bioHarnessHandler.setFileStorageCreated(false);
        bhPeakAccelerationValueCount = 0;
        bhRespirationRateValueCount = 0;
        bhHeartRateValueCount = 0;
        bhPostureValueCount = 0;
        bhSkinTemperatureValueCount = 0;
        bhRRIntervalValueCount = 0;
        bhAxisAccelerationValueCount = 0;
        bhBreathingValueCount = 0;
        bhEcgValueCount = 0;
        bhHeartRateValues = new String[DATA_ARRAY_SIZE][2];
        bhPostureValues = new String[DATA_ARRAY_SIZE][2];
        bhPeakAccelerationValues = new String[DATA_ARRAY_SIZE][2];
        bhSkinTemperatureValues = new String[DATA_ARRAY_SIZE][2];
        bhRespirationRateValues = new String[DATA_ARRAY_SIZE][2];
        bhRRIntervalValues = new String[DATA_ARRAY_SIZE][2];
        bhAxisAccelerationValues = new String[DATA_ARRAY_SIZE][4];
        bhBreathingValues = new String[DATA_ARRAY_SIZE][2];
        bhEcgValues = new String[DATA_ARRAY_SIZE][2];
    }

    private void createBioHarnessFiles() {
        if(mService.getBioHarnessConnectedListener().isHeartRateEnabled()) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_heart_rate)), true));
                String outputString = getLoggingHeaderString();
                outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_heartrate) + "";
                writer.write(outputString);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while writing in file", e);
            }
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_respiration_rate)), true));
            String outputString = getLoggingHeaderString();
            outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_respirationrate) + "";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_posture)), true));
            String outputString = getLoggingHeaderString();
            outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_posture) + "";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_rr_interval)), true));
            String outputString = getLoggingHeaderString();
            outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_rr_interval) + "";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        if(mService.getBioHarnessConnectedListener().isSkinTemperatureEnabled()) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_skin_temperature)), true));
                String outputString = getLoggingHeaderString();
                outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_skintemperature) + "";
                writer.write(outputString);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while writing in file", e);
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_peak_acceleration)), true));
            String outputString = getLoggingHeaderString();
            outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_peakacceleration) + "";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
////////////////
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_axis_acceleration)), true)); //TODO: RENAME
            String outputString = getLoggingHeaderString();
            outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_acceleration_x) + "," + getString(R.string.file_header_acceleration_y) + "," + getString(R.string.file_header_acceleration_z) + "";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_breathing)), true));
            String outputString = getLoggingHeaderString();
            outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_interval) + "";
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_ecg)), true));
            String outputString = getLoggingHeaderString();
            outputString += "" + getString(R.string.file_header_timestamp) + "," + getString(R.string.file_header_voltage) + "";
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
        this.directoryName = "PsychoPhysioCollector/" + dateString + "_" + timeString;

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
            case REQUEST_ENABLE_BT:
                if(resultCode==RESULT_OK){
                    Toast.makeText(MainActivity.this, getString(R.string.bluetooth_activated), Toast.LENGTH_LONG).show();
                }
                break;
            case MSG_BLUETOOTH_ADDRESS:

                // When DeviceListActivity returns with a device address to connect
                if (resultCode == Activity.RESULT_OK) {
                    String bluetoothAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Log.d(TAG, "Bluetooth Address: " + bluetoothAddress);

                    // Check if the bluetooth address has been previously selected
                    boolean isNewAddress = !getBluetoothAddresses().contains(bluetoothAddress);

                    if (isNewAddress) {
                        addBluetoothAddress(bluetoothAddress);
                        btAdapter = BluetoothAdapter.getDefaultAdapter();
                        BluetoothDevice device = btAdapter.getRemoteDevice(bluetoothAddress);
                        String name = device.getName();
                        if(name==null) {
                            name = bluetoothAddress + "";
                        }
                        deviceNames.add(name);
                        adapter.notifyDataSetChanged();
                        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
                        boolean paired = false;
                        for(BluetoothDevice d:pairedDevices) {
                            if(d.getAddress().equals(bluetoothAddress)) {
                                paired = true;
                            }
                        }
                        if(!paired)
                            pairDevice(device);
                        Log.v("Main", "bond: " + bluetoothAddress);
                    } else {
                        Toast.makeText(this, getString(R.string.device_is_already_in_list), Toast.LENGTH_LONG).show();
                    }
                    if(mService == null) {
                        Log.v(TAG, "service erstellen");
                        Intent intent=new Intent(this, SensorService.class);
                        startService(intent);
                        getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
                        registerReceiver(myReceiver, new IntentFilter("de.bogutzky.data_collector.app"));
                    }
                }
                break;
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
        }
    }

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

    private ArrayList<String> getBluetoothAddresses() {
        if (bluetoothAddresses == null) {
            bluetoothAddresses = new ArrayList<String>();
        }
        return bluetoothAddresses;
    }

    private void addBluetoothAddress(String bluetoothAddress) {
        getBluetoothAddresses().add(bluetoothAddress);
        if (getBluetoothAddresses().size() > 0) {
            connectMenuItem.setEnabled(true);
        }
    }

    private void findBluetoothAddress() {
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, MSG_BLUETOOTH_ADDRESS);
    }

    private void connectedAllShimmers() {
        if(btAdapter == null)
            btAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        int count = 0;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains("RN42")) {
                    if(bluetoothAddresses.contains(device.getAddress())) {
                        BluetoothDevice btDevice = device;

                        String bluetoothAddress = btDevice.getAddress();
                        mService.connectShimmer(bluetoothAddress, Integer.toString(count), new ShimmerHandler("imu-" + btDevice.getName().toLowerCase() + ".csv", 250));
                        count++;
                    }
                }
            }
        }
    }

    private void disconnectedAllShimmers() {
        stopService(new Intent(MainActivity.this, SensorService.class));
        if(mService != null)
            mService.disconnectAllDevices();
    }

    private void startAllStreaming() {
        if(mService != null)
            mService.startStreamingAllDevicesGetSensorNames(this.root, this.directoryName);
    }

    private void stopAllStreaming() {
        if(mService != null)
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

        Random r = new Random();
        int variance = 0;
        if(scaleTimerVarianceValue != 0)
            variance = r.nextInt(scaleTimerVarianceValue*2) - scaleTimerVarianceValue;
        long timerInterval = (long) (1000 * 60 * timerCycleInMin) - (1000 * variance);
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
        final long showTimestamp = System.currentTimeMillis();
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.questionnaire);
        dialog.setTitle(getString(R.string.feedback));
        dialog.setCancelable(false);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
        Button startQuestionnaireButton = (Button)dialog.findViewById(R.id.startQuestionnaireButton);
        startQuestionnaireButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createQuestionnaire(dialog, showTimestamp);
            }
        });
        dialog.show();
        //pauseAllSensors();
    }

    private void createQuestionnaire(final Dialog dialog, final long showTimestamp) {
        final long startTimestamp = System.currentTimeMillis();

        scaleTypes = new ArrayList<>();
        scaleViewIds = new ArrayList<>();

        ScrollView scrollView = new ScrollView(this);
        RelativeLayout relativeLayout = new RelativeLayout(this);
        Button saveButton = new Button(this);
        RelativeLayout.LayoutParams slp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams saveParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        Random rnd = new Random();
        try {
            JSONArray questions = questionnaire.getJSONObject("questionnaire").getJSONArray("questions");
            int tmpid = 0;
            int tmpid2 = 0;
            int tmpid3 = 0;
            int oldtmp = 0;
            for (int i = 0; i < questions.length(); i++) {
                JSONObject q = questions.getJSONObject(i);
                if (q.getString("type").equals("rating")) {
                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    RelativeLayout.LayoutParams params4 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    tmpid = rnd.nextInt(Integer.MAX_VALUE);
                    tmpid2 = rnd.nextInt(Integer.MAX_VALUE);
                    tmpid3 = rnd.nextInt(Integer.MAX_VALUE);
                    TextView textView = new TextView(this);
                    textView.setId(tmpid);
                    Drawable bottom = getResources().getDrawable(R.drawable.section_header);
                    textView.setCompoundDrawables(null,null,null,bottom);
                    textView.setCompoundDrawablePadding(4);
                    textView.setPadding(4, 0, 0, 0);
                    textView.setTextColor(Color.WHITE);
                    textView.setTextSize(16);
                    params1.setMargins(0, 10, 0, 0);
                    textView.setText(q.getString("question"));
                    if(i > 0) {
                        params1.addRule(RelativeLayout.BELOW, oldtmp);
                    }
                    textView.setLayoutParams(params1);

                    TextView textView1 = new TextView(this);
                    textView1.setText(q.getJSONArray("ratings").getString(0));
                    params2.setMargins(0, 8, 0, 0);
                    params2.addRule(RelativeLayout.BELOW, tmpid);
                    textView1.setLayoutParams(params2);

                    TextView textView2 = new TextView(this);
                    textView2.setId(tmpid2);
                    textView2.setText(q.getJSONArray("ratings").getString(1));
                    params3.setMargins(0, 8, 0, 0);
                    params3.addRule(RelativeLayout.BELOW, tmpid);
                    params3.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    textView2.setLayoutParams(params3);

                    RatingBar ratingBar = new RatingBar(new ContextThemeWrapper(this, R.style.RatingBar), null, 0);
                    ratingBar.setNumStars(q.getInt("stars"));
                    ratingBar.setStepSize(1.0f);
                    ratingBar.setId(tmpid3);
                    params4.addRule(RelativeLayout.BELOW, tmpid2);
                    params4.setMargins(0, 8, 0, 20);
                    params4.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    oldtmp = tmpid3;
                    ratingBar.setLayoutParams(params4);

                    relativeLayout.addView(textView);
                    relativeLayout.addView(textView1);
                    relativeLayout.addView(textView2);
                    relativeLayout.addView(ratingBar);

                    scaleTypes.add(q.getString("type"));
                    scaleViewIds.add(tmpid3);

                } else if (q.getString("type").equals("text")) {
                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

                    tmpid = rnd.nextInt(Integer.MAX_VALUE);
                    tmpid2 = rnd.nextInt(Integer.MAX_VALUE);

                    TextView textView = new TextView(this);
                    textView.setId(tmpid);
                    textView.setText(q.getString("question"));
                    if(i > 0) {
                        params1.addRule(RelativeLayout.BELOW, oldtmp);
                    }
                    textView.setLayoutParams(params1);

                    EditText editText = new EditText(this);
                    editText.setId(tmpid2);
                    params2.addRule(RelativeLayout.BELOW, tmpid);
                    editText.setLayoutParams(params2);
                    oldtmp = tmpid2;
                    relativeLayout.addView(textView);
                    relativeLayout.addView(editText);

                    scaleTypes.add(q.getString("type"));
                    scaleViewIds.add(tmpid2);
                } else if (q.getString("type").equals("truefalse")) {
                    RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

                    tmpid = rnd.nextInt(Integer.MAX_VALUE);
                    tmpid2 = rnd.nextInt(Integer.MAX_VALUE);

                    TextView textView = new TextView(this);
                    textView.setId(tmpid);
                    textView.setText(q.getString("question"));
                    if(i > 0) {
                        params1.addRule(RelativeLayout.BELOW, oldtmp);
                    }
                    textView.setLayoutParams(params1);

                    Switch yesNoSwitch = new Switch(this);
                    yesNoSwitch.setText("");
                    yesNoSwitch.setTextOff(getResources().getString(R.string.no));
                    yesNoSwitch.setTextOn(getResources().getString(R.string.yes));
                    yesNoSwitch.setId(tmpid2);

                    params2.addRule(RelativeLayout.BELOW, tmpid);
                    yesNoSwitch.setLayoutParams(params2);
                    oldtmp = tmpid2;
                    relativeLayout.addView(textView);
                    relativeLayout.addView(yesNoSwitch);

                    scaleTypes.add(q.getString("type"));
                    scaleViewIds.add(tmpid2);
                }
            }
            saveParams.addRule(RelativeLayout.BELOW, oldtmp);
            saveButton.setLayoutParams(saveParams);
            saveButton.setText(getText(R.string.save));
            relativeLayout.addView(saveButton);
            relativeLayout.setLayoutParams(rlp);
            scrollView.addView(relativeLayout);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //dialog.setContentView(R.layout.flow_short_scale);
        dialog.setContentView(scrollView, slp);

        //Button saveButton = (Button) dialog.findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                saveScaleItems(dialog, showTimestamp, startTimestamp);
                dialog.dismiss();
                if (loggingEnabled) {
                    //resumeAllSensors();
                    startTimerThread();
                }
            }
        });
    }

    private void saveScaleItems(final Dialog dialog, long showTimestamp, long startTimestamp) {

        String outputString = "";
        if(!wroteQuestionnaireHeader) {
            wroteQuestionnaireHeader = true;
            outputString = getLoggingHeaderString();
            outputString += "" + getString(R.string.file_header_timestamp_show) + "," + getString(R.string.file_header_timestamp_start) + "," + getString(R.string.file_header_timestamp_stop) + ",";
            for (int i = 0; i < scaleTypes.size(); i++) {
                if (i != scaleTypes.size() - 1) {
                    outputString += "item." + String.format("%02d", (i+1)) + ",";
                } else {
                    outputString += "item." + String.format("%02d", (i+1)) + "\n";
                }
            }
        }
        outputString += Long.toString((showTimestamp-questionnaireTimestamp)) + "," + Long.toString((startTimestamp-questionnaireTimestamp)) + "," + Long.toString((System.currentTimeMillis()-questionnaireTimestamp)) + ",";
        for (int i = 0; i < scaleTypes.size(); i++) {
            String value = "";
            if(scaleTypes.get(i).equals("rating")) {
                RatingBar r = (RatingBar) dialog.findViewById(scaleViewIds.get(i));
                value = Float.toString(r.getRating());
            } else if(scaleTypes.get(i).equals("text")) {
                EditText e = (EditText) dialog.findViewById(scaleViewIds.get(i));
                value = e.getText().toString();
            } else if(scaleTypes.get(i).equals("truefalse")) {
                Switch s = (Switch) dialog.findViewById(scaleViewIds.get(i));
                if(s.isChecked())
                    value = "1";
                else
                    value = "0";
            }
            if(i != scaleTypes.size()-1) {
                outputString += value + ",";
            } else {
                outputString += value;
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(root, getString(R.string.file_name_self_report)), true));
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
        if(!loggingEnabled) {
            String footer = getLoggingFooterString();
            writeFoooter(footer, getString(R.string.file_name_self_report));
        }
    }

    private void startStreamingInternalSensorData() {

        this.accelerometerValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
        this.accelerometerValueCount = 0;
        this.gyroscopeValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
        this.gyroscopeValueCount = 0;
        this.linearAccelerationValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
        this.linearAccelerationValueCount = 0;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, getString(R.string.file_name_acceleration)), true));
            String outputString = getLoggingHeaderString();
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
            String outputString = getLoggingHeaderString();
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
            String outputString = getLoggingHeaderString();
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.addGpsStatusListener(new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                switch(event) {
                    case GpsStatus.GPS_EVENT_STARTED:
                        gpsStatusText = getString(R.string.gps_connected);
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        gpsStatusText = getString(R.string.gps_not_connected);
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        gpsStatusText = getString(R.string.gps_connected_fix_received);
                        break;
                }
            }
        });
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

                if (this.accelerometerEventStartTimestamp == 0L) {
                    this.accelerometerEventStartTimestamp = event.timestamp; // Nanos
                    this.accelerometerStartTimestamp = System.currentTimeMillis() - this.startTimestamp; // Millis
                }
                double relativeTimestamp = this.accelerometerStartTimestamp + (event.timestamp - this.accelerometerEventStartTimestamp) / 1000000.0;

                accelerometerValues[accelerometerValueCount][0] = decimalFormat.format(relativeTimestamp);
                accelerometerValues[accelerometerValueCount][1] = Float.toString(event.values[0]);
                accelerometerValues[accelerometerValueCount][2] = Float.toString(event.values[1]);
                accelerometerValues[accelerometerValueCount][3] = Float.toString(event.values[2]);

                accelerometerValueCount++;
                if(accelerometerValueCount > accelerometerValues.length - 2) {
                    if(!writingData) {
                        accelerometerValueCount = 0;
                        setWritingData(true);
                        writeAccelerometerValues(false, 1);
                        linearAccelerationValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else if(!secondWritingData) {
                        accelerometerValueCount = 0;
                        setSecondWritingData(true);
                        writeAccelerometerValues(false, 2);
                        linearAccelerationValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
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

                gyroscopeValues[gyroscopeValueCount][0] = decimalFormat.format(relativeTimestamp);
                gyroscopeValues[gyroscopeValueCount][1] = Float.toString((float) (event.values[0] * 180.0 / Math.PI));
                gyroscopeValues[gyroscopeValueCount][2] = Float.toString((float) (event.values[1] * 180.0 / Math.PI));
                gyroscopeValues[gyroscopeValueCount][3] = Float.toString((float) (event.values[2] * 180.0 / Math.PI));

                gyroscopeValueCount++;
                if(gyroscopeValueCount > gyroscopeValues.length - 2) {
                    if(!writingData) {
                        gyroscopeValueCount = 0;
                        setWritingData(true);
                        writeGyroscopeValues(false, 1);
                        gyroscopeValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else if(!secondWritingData) {
                        gyroscopeValueCount = 0;
                        setSecondWritingData(true);
                        writeGyroscopeValues(false, 2);
                        gyroscopeValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
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

                linearAccelerationValues[linearAccelerationValueCount][0] = decimalFormat.format(relativeTimestamp);
                linearAccelerationValues[linearAccelerationValueCount][1] = Float.toString(event.values[0]);
                linearAccelerationValues[linearAccelerationValueCount][2] = Float.toString(event.values[1]);
                linearAccelerationValues[linearAccelerationValueCount][3] = Float.toString(event.values[2]);

                linearAccelerationValueCount++;
                if(linearAccelerationValueCount > linearAccelerationValues.length - 2) {
                    if(!writingData) {
                        linearAccelerationValueCount = 0;
                        setWritingData(true);
                        writeLinearAccelerationValues(false, 1);
                        accelerometerValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
                    } else if(!secondWritingData) {
                        linearAccelerationValueCount = 0;
                        setSecondWritingData(true);
                        writeLinearAccelerationValues(false, 2);
                        accelerometerValues = new String[INTERNAL_SENSOR_CACHE_LENGTH][4];
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
            writeData(linearAccelerationValues, getString(R.string.file_name_linear_acceleration), 4, true, getLoggingFooterString(), slot);
        else
            writeData(linearAccelerationValues, getString(R.string.file_name_linear_acceleration), 4, false, "", slot);
    }

    private void writeGyroscopeValues(boolean footer, int slot) {
        if(footer)
            writeData(gyroscopeValues, getString(R.string.file_name_angular_velocity), 4, true, getLoggingFooterString(), slot);
        else
            writeData(gyroscopeValues, getString(R.string.file_name_angular_velocity), 4, false, "", slot);
    }

    private void writeAccelerometerValues(boolean footer, int slot) {
        if(footer)
            writeData(accelerometerValues, getString(R.string.file_name_acceleration), 4, true, getLoggingFooterString(), slot);
        else
            writeData(accelerometerValues, getString(R.string.file_name_acceleration), 4, false, "", slot);
    }

    private void writeFoooter (String data, String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, filename), true));
            writer.write(data);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
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
                String outputString = getLoggingHeaderString();
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
                values[i][0] = Double.toString(time);
                values[i][1] = Float.toString((float) location.getLatitude());
                values[i][2] = Float.toString((float) location.getLongitude());
                values[i][3] = Float.toString((float) location.getAltitude());

                i++;
                if(i > maxValueCount - 1) {
                    if(!writingData) {
                        i = 0;
                        setWritingData(true);
                        writeGpsValues(false, 1);
                        values = new String[maxValueCount][4];
                    } else if(!secondWritingData) {
                        i = 0;
                        setSecondWritingData(true);
                        writeGpsValues(false, 2);
                        values = new String[maxValueCount][4];
                    } else {
                        values = resizeArray(values);
                    }
                }
                if(lastLocationAccuracy - location.getAccuracy() > 5.0) {
                    gpsStatusText = getText(R.string.gps_connected_fix_received) + getString(R.string.accuracy) + location.getAccuracy();
                    lastLocationAccuracy = location.getAccuracy();
                }
            }
        }

        public void writeGpsValues(boolean footer, int slot) {
            if(footer)
                writeData(values,this.filename,4, true, getLoggingFooterString(), slot);
            else
                writeData(values,this.filename,4, false, "", slot);
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

    @SuppressLint("HandlerLeak")
    public class ShimmerHandler extends Handler {

        private static final String TAG = "ShimmerHandler";

        private String filename;
        private String directoryName;
        private File root;
        private int i = 0;
        private int maxValueCount;
        private Double[][] values;
        private Double[][] values0;
        private Double[][] values1;
        private String[] fields;
        //float[] dataArray;
        int enabledSensor;

        public void setRoot(File root) {
            this.root = root;
        }

        public void setHeader(String[] header) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));
                String outputString = getLoggingHeaderString();
                outputString += "";
                for (int k = 0; k < header.length; k++) {
                    if (header.length - 1 != k) {
                        outputString += header[k] + ",";
                    } else {
                        outputString += header[k] + "";
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

        public void setFields(String[] fields) {
            //dataArray = new float[fields.length - 1];
            enabledSensor = mService.getEnabledSensorForMac(graphAdress);
            this.fields = fields;
            this.values0 = new Double[maxValueCount][fields.length];
            this.values1 = new Double[maxValueCount][fields.length];
            this.values = values0;
        }

        public void setDirectoryName(String directoryName) {
            this.directoryName = directoryName;
            this.root = getStorageDir(this.directoryName);
        }

        ShimmerHandler(String filename, int maxValueCount) {
            this.filename = filename;
            this.maxValueCount = maxValueCount;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Shimmer.MESSAGE_READ:

                    if (msg.obj instanceof ObjectCluster) {
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;

                        //int graphDataCounter = 0;
                        for (int j = 0; j < fields.length; j++) {
                            Collection<FormatCluster> clusterCollection = objectCluster.mPropertyCluster.get(fields[j]);
                            if (j < fields.length) {
                                if (!clusterCollection.isEmpty()) {
                                    FormatCluster formatCluster = ObjectCluster.returnFormatCluster(clusterCollection, "CAL");
                                    this.values[i][j] = formatCluster.mData;

                                    //if(graphShowing && graphAdress.equals(this.bluetoothAdress)) {
                                        //if(j != 0 && j != fields.length) {
                                            //dataArray[graphDataCounter] = Float.valueOf(values[i][j]);
                                            //graphDataCounter++;
                                        //}
                                    //}
                                }
                            }
                        }

                        //values[i][0] = decimalFormat.format(Double.valueOf(values[i][0]));
                        //if(graphShowing && graphAdress.equals(this.bluetoothAdress)) {
                            //graphView.setDataWithAdjustment(dataArray, graphAdress, "i8");
                        //}
                        i++;
                        if(i == maxValueCount) {
                            i = 0;
                            writeValues(this.values);
                            if(this.values == this.values0) {
                                this.values = this.values1;
                            } else  {
                                this.values = this.values0;
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
                            connected = true;

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

        public void writeValues(Double[][] values) {
            WriteDataTask task = new WriteDataTask();
            task.execute(new WriteDataTaskParams(values, this.filename, this.root, this.fields.length, false, ""));


            //if(footer)
                //writeData(values,this.filename, fields.length, true, getLoggingFooterString(), slot);
            //else
                //writeData(values,this.filename, fields.length, false, "", slot);
        }
    }

    public String getBioHarnessBtDeviceAdress(BluetoothAdapter bluetoothAdapter) {
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                if (bluetoothDevice.getName().startsWith("BH")) {
                    if(getBluetoothAddresses().contains(bluetoothDevice.getAddress())) {
                        return bluetoothDevice.getAddress();
                    }
                }
            }
        }
        return null;
    }

    class BioHarnessHandler extends Handler {
        private double rrTime = 0;

        private boolean fileStorageCreated = false;

        public void setFileStorageCreated(boolean fileStorageCreated) {
            this.fileStorageCreated = fileStorageCreated;
        }

        BioHarnessHandler() {}
        public void handleMessage(Message msg) {
            if(!fileStorageCreated && loggingEnabled) {
                createBioHarnessFiles();
                fileStorageCreated = true;
            }
            if(msg.what == 101) {
                notifyBHReady();
            }
            if(loggingEnabled) {
                double time = 0;
                long timestamp = 0;
                switch (msg.what) {
                    case RtoR_MSG_ID:
                        int rrInterval = msg.getData().getInt("rrInterval");
                        timestamp = msg.getData().getLong("Timestamp");
                        if(firstRRIntervalTimestamp == 0L) {
                            firstRRIntervalTimestamp = timestamp;
                            rrTime = System.currentTimeMillis() - startTimestamp;
                        }
                        rrTime += rrInterval;

                        bhRRIntervalValues[bhRRIntervalValueCount][0] = String.valueOf(rrTime);
                        bhRRIntervalValues[bhRRIntervalValueCount][1] = String.valueOf(rrInterval);
                        bhRRIntervalValueCount++;
                        if(bhRRIntervalValueCount >= bhRRIntervalValues.length) {
                            if(!writingData) {
                                bhRRIntervalValueCount = 0;
                                setWritingData(true);
                                writeData(bhRRIntervalValues, getString(R.string.file_name_rr_interval), 2, false, "", 1);
                                bhRRIntervalValues = new String[DATA_ARRAY_SIZE][2];
                            } else if(!secondWritingData) {
                                bhRRIntervalValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhRRIntervalValues, getString(R.string.file_name_rr_interval), 2, false, "", 2);
                                bhRRIntervalValues = new String[DATA_ARRAY_SIZE][2];
                            } else {
                                bhRRIntervalValues = resizeArray(bhRRIntervalValues);
                            }
                        }
                        Log.v(TAG, "Logge RR interval mit Timestamp: " + time);
                        break;

                    case HEART_RATE:
                        String HeartRatetext = msg.getData().getString("HeartRate");
                        timestamp = msg.getData().getLong("Timestamp");
                        if(firstbhHeartRateTimestamp == 0L) {
                            firstbhHeartRateTimestamp = timestamp;
                            bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                        }
                        time = bhStartTimestamp + (timestamp - firstbhHeartRateTimestamp) / 1000.0;
                        bhHeartRateValues[bhHeartRateValueCount][0] = String.valueOf(time);
                        bhHeartRateValues[bhHeartRateValueCount][1] = HeartRatetext;
                        bhHeartRateValueCount++;
                        System.out.println("Heart Rate Info is " + HeartRatetext);
                        if(bhHeartRateValueCount >= bhHeartRateValues.length) {
                            if(!writingData) {
                                bhHeartRateValueCount = 0;
                                setWritingData(true);
                                writeData(bhHeartRateValues, getString(R.string.file_name_heart_rate), 2, false, "", 1);
                                bhHeartRateValues = new String[DATA_ARRAY_SIZE][2];
                            } else if(!secondWritingData) {
                                bhHeartRateValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhHeartRateValues, getString(R.string.file_name_heart_rate), 2, false, "", 2);
                                bhHeartRateValues = new String[DATA_ARRAY_SIZE][2];
                            } else {
                                bhHeartRateValues = resizeArray(bhHeartRateValues);
                            }
                        }
                        break;

                    case RESPIRATION_RATE:
                        String RespirationRatetext = msg.getData().getString("RespirationRate");
                        if(RespirationRatetext != null) {
                            timestamp = msg.getData().getLong("Timestamp");
                            if(firstRespirationRateTimestamp == 0L) {
                                firstRespirationRateTimestamp = timestamp;
                                bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                            }
                            time = bhStartTimestamp + (timestamp - firstRespirationRateTimestamp);
                            bhRespirationRateValues[bhRespirationRateValueCount][0] = String.valueOf(time);
                            bhRespirationRateValues[bhRespirationRateValueCount][1] = RespirationRatetext;
                            bhRespirationRateValueCount++;
                            System.out.println("RespirationRate Info is " + RespirationRatetext);
                            if(bhRespirationRateValueCount >= bhRespirationRateValues.length) {
                                if(!writingData) {
                                    bhRespirationRateValueCount = 0;
                                    setWritingData(true);
                                    writeData(bhRespirationRateValues, getString(R.string.file_name_respiration_rate), 2, false, "", 1);
                                    bhRespirationRateValues = new String[DATA_ARRAY_SIZE][2];
                                } else if(!secondWritingData) {
                                    bhRespirationRateValueCount = 0;
                                    setSecondWritingData(true);
                                    writeData(bhRespirationRateValues, getString(R.string.file_name_respiration_rate), 2, false, "", 2);
                                    bhRespirationRateValues = new String[DATA_ARRAY_SIZE][2];
                                } else {
                                    bhRespirationRateValues = resizeArray(bhRespirationRateValues);
                                }
                            }
                        }
                        break;

                    case SKIN_TEMPERATURE:
                        String SkinTemperaturetext = msg.getData().getString("SkinTemperature");
                        timestamp = msg.getData().getLong("Timestamp");
                        if(firstSkinTemperatureTimestamp == 0L) {
                            firstSkinTemperatureTimestamp = timestamp;
                            bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                        }
                        time = bhStartTimestamp + (timestamp - firstSkinTemperatureTimestamp);
                        bhSkinTemperatureValues[bhSkinTemperatureValueCount][0] = String.valueOf(time);
                        bhSkinTemperatureValues[bhSkinTemperatureValueCount][1] = SkinTemperaturetext;
                        bhSkinTemperatureValueCount++;
                        System.out.println("SkinTemperature Info is " + SkinTemperaturetext);
                        if(bhSkinTemperatureValueCount >= bhSkinTemperatureValues.length) {
                            if(!writingData) {
                                bhSkinTemperatureValueCount = 0;
                                setWritingData(true);
                                writeData(bhSkinTemperatureValues, getString(R.string.file_name_skin_temperature), 2, false, "", 1);
                                bhSkinTemperatureValues = new String[DATA_ARRAY_SIZE][2];
                            } else if(!secondWritingData) {
                                bhSkinTemperatureValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhSkinTemperatureValues, getString(R.string.file_name_skin_temperature), 2, false, "", 2);
                                bhSkinTemperatureValues = new String[DATA_ARRAY_SIZE][2];
                            } else {
                                bhSkinTemperatureValues = resizeArray(bhSkinTemperatureValues);
                            }
                        }
                        break;

                    case POSTURE:
                        String PostureText = msg.getData().getString("Posture");
                        timestamp = msg.getData().getLong("Timestamp");
                        if(firstbhPostureTimestamp == 0L) {
                            firstbhPostureTimestamp = timestamp;
                            bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                        }
                        time = bhStartTimestamp + (timestamp - firstbhPostureTimestamp);
                        bhPostureValues[bhPostureValueCount][0] = String.valueOf(time);
                        bhPostureValues[bhPostureValueCount][1] = PostureText;
                        bhPostureValueCount++;
                        System.out.println("Posture Info is " + PostureText);
                        if(bhPostureValueCount >= bhPostureValues.length) {
                            if(!writingData) {
                                bhPostureValueCount = 0;
                                setWritingData(true);
                                writeData(bhPostureValues, getString(R.string.file_name_posture), 2, false, "", 1);
                                bhPostureValues = new String[DATA_ARRAY_SIZE][2];
                            } else if(!secondWritingData) {
                                bhPostureValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhPostureValues, getString(R.string.file_name_posture), 2, false, "", 2);
                                bhPostureValues = new String[DATA_ARRAY_SIZE][2];
                            } else {
                                bhPostureValues = resizeArray(bhPostureValues);
                            }
                        }
                        break;

                    case PEAK_ACCLERATION:
                        String PeakAccText = msg.getData().getString("PeakAcceleration");
                        timestamp = msg.getData().getLong("Timestamp");

                        if(firstPeakAccelerationTimestamp == 0L) {
                            firstPeakAccelerationTimestamp = timestamp;
                            bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                        }
                        time = bhStartTimestamp + (timestamp - firstPeakAccelerationTimestamp);

                        bhPeakAccelerationValues[bhPeakAccelerationValueCount][0] = String.valueOf(time);
                        bhPeakAccelerationValues[bhPeakAccelerationValueCount][1] = PeakAccText;
                        bhPeakAccelerationValueCount++;
                        System.out.println("PeakAcceleration Info is " + PeakAccText);

                        if(bhPeakAccelerationValueCount >= bhPeakAccelerationValues.length) {
                            if(!writingData) {
                                bhPeakAccelerationValueCount = 0;
                                setWritingData(true);
                                writeData(bhPeakAccelerationValues, getString(R.string.file_name_peak_acceleration), 2, false, "", 1);
                                bhPeakAccelerationValues = new String[DATA_ARRAY_SIZE][2];
                            } else if(!secondWritingData) {
                                bhPeakAccelerationValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhPeakAccelerationValues, getString(R.string.file_name_peak_acceleration), 2, false, "", 2);
                                bhPeakAccelerationValues = new String[DATA_ARRAY_SIZE][2];
                            } else {
                                bhPeakAccelerationValues = resizeArray(bhPeakAccelerationValues);
                            }
                        }
                        break;
                    ////////////////////////////////
                    case BREATHING_MSG_ID:
                        timestamp = msg.getData().getLong("Timestamp");
                        short interval = msg.getData().getShort("Interval");
                        if(firstBreathingTimestamp == 0L) {
                            firstBreathingTimestamp = timestamp;
                            bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                        }
                        time = bhStartTimestamp + (timestamp - firstBreathingTimestamp);
                        bhBreathingValues[bhBreathingValueCount][0] = String.valueOf(time);
                        bhBreathingValues[bhBreathingValueCount][1] = String.valueOf(interval);
                        if(bhBreathingValueCount >= bhBreathingValues.length) {
                            if(!writingData) {
                                bhBreathingValueCount = 0;
                                setWritingData(true);
                                writeData(bhBreathingValues, getString(R.string.file_name_breathing), 2, false, "", 1);
                                bhBreathingValues = new String[DATA_ARRAY_SIZE][2];
                            } else if(!secondWritingData) {
                                bhBreathingValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhBreathingValues, getString(R.string.file_name_breathing), 2, false, "", 2);
                                bhBreathingValues = new String[DATA_ARRAY_SIZE][2];
                            } else {
                                bhBreathingValues = resizeArray(bhBreathingValues);
                            }
                        }
                        break;
                    case ECG_MSG_ID:
                        timestamp = msg.getData().getLong("Timestamp");
                        short voltage = msg.getData().getShort("Voltage");
                        if(firstEcgTimestamp == 0L) {
                            firstEcgTimestamp = timestamp;
                            bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                        }
                        time = bhStartTimestamp + (timestamp - firstEcgTimestamp);
                        bhEcgValues[bhEcgValueCount][0] = String.valueOf(time);
                        bhEcgValues[bhEcgValueCount][1] = String.valueOf(voltage);
                        if(bhEcgValueCount >= bhEcgValues.length) {
                            if(!writingData) {
                                bhEcgValueCount = 0;
                                setWritingData(true);
                                writeData(bhEcgValues, getString(R.string.file_name_ecg), 2, false, "", 1);
                                bhEcgValues = new String[DATA_ARRAY_SIZE][2];
                            } else if(!secondWritingData) {
                                bhEcgValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhEcgValues, getString(R.string.file_name_ecg), 2, false, "", 2);
                                bhEcgValues = new String[DATA_ARRAY_SIZE][2];
                            } else {
                                bhEcgValues = resizeArray(bhEcgValues);
                            }
                        }
                        break;
                    case ACCEL_100mg_MSG_ID:
                        timestamp = msg.getData().getLong("Timestamp");
                        double acc_x = msg.getData().getDouble("AccelerationX");
                        double acc_y = msg.getData().getDouble("AccelerationY");
                        double acc_z = msg.getData().getDouble("AccelerationZ");
                        if(firstAxisAccelerationTimestamp == 0L) {
                            firstAxisAccelerationTimestamp = timestamp;
                            bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                        }
                        time = bhStartTimestamp + (timestamp - firstAxisAccelerationTimestamp);
                        bhAxisAccelerationValues[bhAxisAccelerationValueCount][0] = String.valueOf(time);
                        bhAxisAccelerationValues[bhAxisAccelerationValueCount][0] = String.valueOf(acc_x);
                        bhAxisAccelerationValues[bhAxisAccelerationValueCount][0] = String.valueOf(acc_y);
                        bhAxisAccelerationValues[bhAxisAccelerationValueCount][0] = String.valueOf(acc_z);
                        if(bhAxisAccelerationValueCount >= bhAxisAccelerationValues.length) {
                            if(!writingData) {
                                bhAxisAccelerationValueCount = 0;
                                setWritingData(true);
                                writeData(bhAxisAccelerationValues, getString(R.string.file_name_axis_acceleration), 2, false, "", 1);
                                bhAxisAccelerationValues = new String[DATA_ARRAY_SIZE][4];
                            } else if(!secondWritingData) {
                                bhAxisAccelerationValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhAxisAccelerationValues, getString(R.string.file_name_axis_acceleration), 2, false, "", 2);
                                bhAxisAccelerationValues = new String[DATA_ARRAY_SIZE][4];
                            } else {
                                bhAxisAccelerationValues = resizeArray(bhAxisAccelerationValues);
                            }
                        }
                        break;
                    ////////////////////////////////
                }
            }
        }
    }

    void writeData (String[][] data, String filename, int fields, boolean footer, String footerString, int slot) {
        //WriteDataTask task = new WriteDataTask();
        //task.execute(new WriteDataTaskParams(data, filename, this.root, fields, footer, footerString, slot, this));
        //task.execute(new WriteDataTaskParams(data, filename, this.root, fields, footer, footerString, slot));
    }

    private void notifyBHReady() {
        Toast.makeText(this, "BioHarness " + getString(R.string.is_ready), Toast.LENGTH_LONG).show();
        bioHarnessConnected = true;
    }

    private ServiceConnection mTestServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d(TAG, "service connected");
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            mService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "service connected");
        }
    };

    private BroadcastReceiver myReceiver= new BroadcastReceiver(){

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if(arg1.getIntExtra("ShimmerState", -1)!=-1){
                Log.v(TAG, "receiver receive");
            }
            String action = arg1.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = arg1.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = arg1.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(getApplicationContext(), "Paired", Toast.LENGTH_SHORT);
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(getApplicationContext(), "UnPaired", Toast.LENGTH_SHORT);
                }

            }
        }
    };

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[][] resizeArray(String[][] original) {
        String[][] copy = new String[original.length + INTERNAL_SENSOR_CACHE_LENGTH][original[0].length];
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

}