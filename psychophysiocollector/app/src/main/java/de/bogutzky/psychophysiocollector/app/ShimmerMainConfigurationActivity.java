package de.bogutzky.psychophysiocollector.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.shimmerresearch.android.Shimmer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;


public class ShimmerMainConfigurationActivity extends Activity {
    String mCurrentDevice = null;
    String currentDeviceName;
    int mCurrentSlot = -1;
    private SensorService mService;
    private String[] commands;
    private double mSamplingRate = -1;
    private int mAccelRange = -1;
    private int mGSRRange = -1;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_commands);

        Intent intent = new Intent(this, SensorService.class);
        getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
        Intent sender = getIntent();
        String extraData = sender.getExtras().getString("LocalDeviceID");
        String mac = sender.getExtras().getString("mac");
        Log.v("Tag", "mac: " + mac);
        currentDeviceName = extraData;
        mCurrentDevice = mac;
        setTitle(getTitle() + ": " + currentDeviceName);
        mCurrentSlot = sender.getExtras().getInt("CurrentSlot");
        Log.d("Shimmer", "Create MC:  " + extraData);


        final ListView listViewCommands = (ListView) findViewById(R.id.listViewSamplingRates);

        commands = new String[]{getString(R.string.imu_config_enable_sensors), getString(R.string.imu_config_configurate_sensors), getString(R.string.imu_config_show_sensor_data)};

        ArrayList<String> commandsList = new ArrayList<String>();
        commandsList.addAll(Arrays.asList(commands));
        ArrayAdapter<String> sR = new ArrayAdapter<String>(this, R.layout.commands_name, commandsList);
        listViewCommands.setAdapter(sR);


        listViewCommands.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (position == 1) {
                    mSamplingRate = mService.getSamplingRate(mCurrentDevice);
                    mAccelRange = mService.getAccelRange(mCurrentDevice);
                    mGSRRange = mService.getGSRRange(mCurrentDevice);
                    Intent mainCommandIntent = new Intent(ShimmerMainConfigurationActivity.this, ShimmerSensorConfigurationActivity.class);
                    mainCommandIntent.putExtra("BluetoothAddress", mCurrentDevice);
                    mainCommandIntent.putExtra("SamplingRate", mSamplingRate);
                    mainCommandIntent.putExtra("AccelerometerRange", mAccelRange);
                    mainCommandIntent.putExtra("GSRRange", mGSRRange);

                    startActivityForResult(mainCommandIntent, MainActivity.REQUEST_COMMANDS_SHIMMER);
                } else if (position == 0) {
                    Intent mainCommandIntent = new Intent(ShimmerMainConfigurationActivity.this, ShimmerSensorActivationActivity.class);
                    Long enabledSensors = mService.getEnabledSensors(mCurrentDevice);
                    mainCommandIntent.putExtra("enabledSensors", enabledSensors);
                    startActivityForResult(mainCommandIntent, MainActivity.REQUEST_CONFIGURE_SHIMMER);
                } else if (position == 2) {
                    ArrayList<String> spinnerArray = new ArrayList<String>();
                    Collection<Object> colS = mService.shimmerImuMap.values();
                    Iterator<Object> iterator = colS.iterator();
                    while (iterator.hasNext()) {
                        Shimmer stemp = (Shimmer) iterator.next();
                        int enabledSensors = stemp.getEnabledSensors();
                        String sensorName = "";
                        if ((enabledSensors & Shimmer.SENSOR_ACCEL) != 0) {
                            sensorName = getString(R.string.accel_name);
                            if(!spinnerArray.contains(sensorName)) {
                                spinnerArray.add(sensorName);
                            }
                        }
                        if ((enabledSensors & Shimmer.SENSOR_GYRO) != 0) {
                            sensorName = getString(R.string.gyro_name);
                            if(!spinnerArray.contains(sensorName)) {
                                spinnerArray.add(sensorName);
                            }
                        }
                        if ((enabledSensors & Shimmer.SENSOR_ECG) != 0) {
                            sensorName = getString(R.string.ecg_name);
                            if(!spinnerArray.contains(sensorName)) {
                                spinnerArray.add(sensorName);
                            }
                        }
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(ShimmerMainConfigurationActivity.this);
                    CharSequence[] cs = spinnerArray.toArray(new CharSequence[spinnerArray.size()]);
                    builder.setTitle(getString(R.string.select_graph_gata))
                            .setItems(cs, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.v("Commands Activity", "set result show graph");
                                    Intent intent = new Intent();
                                    intent.putExtra("mac", mCurrentDevice);
                                    intent.putExtra("action", MainActivity.SHOW_GRAPH);
                                    intent.putExtra("datastart", which);
                                    setResult(Activity.RESULT_OK, intent);
                                    finish();
                                }
                            });
                    builder.show();
                }
            }
        });


    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.REQUEST_COMMANDS_SHIMMER:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("Shimmer", "COmmands Received");
                    Log.d("Shimmer", "iam");
                    if (resultCode == Activity.RESULT_OK) {
                        if (data.getExtras().getBoolean("ToggleLED", false) == true) {
                            mService.toggleLED(mCurrentDevice);
                        }

                        if (data.getExtras().getDouble("SamplingRate", -1) != -1) {
                            mService.writeSamplingRate(mCurrentDevice, (data.getExtras().getDouble("SamplingRate", -1)));
                        }

                        if (data.getExtras().getInt("AccelRange", -1) != -1) {
                            mService.writeAccelRange(mCurrentDevice, data.getExtras().getInt("AccelRange", -1));
                        }

                        if (data.getExtras().getInt("GSRRange", -1) != -1) {
                            mService.writeGSRRange(mCurrentDevice, data.getExtras().getInt("GSRRange", -1));
                        }

                    }
                }
                break;
            case MainActivity.REQUEST_CONFIGURE_SHIMMER:
                if (resultCode == Activity.RESULT_OK) {
                    Log.v("TAG", "current device set sensors: " + mCurrentDevice + ", " + data.getExtras().getInt(ShimmerSensorActivationActivity.mDone));
                    mService.setEnabledSensors(data.getExtras().getInt(ShimmerSensorActivationActivity.mDone), mCurrentDevice);
                }
                break;
        }
    }

    public void onPause() {
        super.onPause();

        Log.d("ShimmerH", "MCA on Pause");
    }

    public void onResume() {
        super.onResume();

        Intent intent = new Intent(ShimmerMainConfigurationActivity.this, MainActivity.class);
        Log.d("ShimmerH", "MCA on Resume");
    }


    private ServiceConnection mTestServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            Log.v("TAG", "service bekommen");
            mService = binder.getService();
            mSamplingRate = mService.getSamplingRate(mCurrentDevice);
            mAccelRange = mService.getAccelRange(mCurrentDevice);
            mGSRRange = mService.getGSRRange(mCurrentDevice);
        }

        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}
