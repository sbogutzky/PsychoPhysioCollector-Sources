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

import de.bogutzky.psychophysiocollector.app.shimmer.imu.ShimmerImuService;


public class ShimmerMainConfigurationActivity extends Activity {
    private static final String TAG = "ShimmerMConfigActivity";

    private String mCurrentBluetoothDeviceAddress;

    private ShimmerImuService mService;

    private double mSamplingRate = -1;
    private int mAccelerometerRange = -1;
    private int mGyroscopeRange = -1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_commands);

        Intent intent = new Intent(this, ShimmerImuService.class);
        getApplicationContext().bindService(intent, mSensorServiceConnection, Context.BIND_AUTO_CREATE);

        Intent sender = getIntent();
        String currentDeviceName = sender.getExtras().getString("DeviceName");
        mCurrentBluetoothDeviceAddress = sender.getExtras().getString("BluetoothDeviceAddress");
        setTitle(getTitle() + ": " + currentDeviceName);

        final ListView listViewCommands = (ListView) findViewById(R.id.listViewSamplingRates);

        String[] commands = new String[]{getString(R.string.imu_config_enable_sensors), getString(R.string.imu_config_configurate_sensors), getString(R.string.imu_config_show_sensor_data)};

        ArrayList<String> commandsList = new ArrayList<>();
        commandsList.addAll(Arrays.asList(commands));
        ArrayAdapter<String> sR = new ArrayAdapter<>(this, R.layout.commands_name, commandsList);
        listViewCommands.setAdapter(sR);

        listViewCommands.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                switch (position) {
                    case 0:
                        Intent intent0 = new Intent(ShimmerMainConfigurationActivity.this, ShimmerSensorActivationActivity.class);
                        intent0.putExtra("enabledSensors", mService.getEnabledSensors(mCurrentBluetoothDeviceAddress));
                        startActivityForResult(intent0, MainActivity.REQUEST_CONFIGURE_SHIMMER);
                        break;

                    case 1:
                        mSamplingRate = mService.getSamplingRate(mCurrentBluetoothDeviceAddress);
                        mAccelerometerRange = mService.getAccelerometerRange(mCurrentBluetoothDeviceAddress);
                        mGyroscopeRange = mService.getGyroscopeRange(mCurrentBluetoothDeviceAddress);

                        Intent intent1 = new Intent(ShimmerMainConfigurationActivity.this, ShimmerSensorConfigurationActivity.class);
                        intent1.putExtra("BluetoothAddress", mCurrentBluetoothDeviceAddress);
                        intent1.putExtra("SamplingRate", mSamplingRate);
                        intent1.putExtra("AccelerometerRange", mAccelerometerRange);
                        intent1.putExtra("GyroscopeRange", mGyroscopeRange);

                        startActivityForResult(intent1, MainActivity.REQUEST_COMMANDS_SHIMMER);
                        break;

                    case 2:
                        ArrayList<String> spinnerArray = new ArrayList<>();
                        Collection<Object> shimmerImus = mService.shimmerImuMap.values();
                        for (Object shimmerImu1 : shimmerImus) {
                            Shimmer shimmerImu = (Shimmer) shimmerImu1;
                            int enabledSensors = shimmerImu.getEnabledSensors();
                            String sensorName;
                            if ((enabledSensors & Shimmer.SENSOR_ACCEL) != 0) {
                                sensorName = getString(R.string.accel_name);
                                if (!spinnerArray.contains(sensorName)) {
                                    spinnerArray.add(sensorName);
                                }
                            }
                            if ((enabledSensors & Shimmer.SENSOR_GYRO) != 0) {
                                sensorName = getString(R.string.gyro_name);
                                if (!spinnerArray.contains(sensorName)) {
                                    spinnerArray.add(sensorName);
                                }
                            }
                            if ((enabledSensors & Shimmer.SENSOR_ECG) != 0) {
                                sensorName = getString(R.string.ecg_name);
                                if (!spinnerArray.contains(sensorName)) {
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
                                        intent.putExtra("mac", mCurrentBluetoothDeviceAddress);
                                        intent.putExtra("action", MainActivity.SHOW_GRAPH);
                                        intent.putExtra("datastart", which);
                                        setResult(Activity.RESULT_OK, intent);
                                        finish();
                                    }
                                });
                        builder.show();
                        break;
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.REQUEST_COMMANDS_SHIMMER:
                Log.d(TAG, "Commands Received");
                if (resultCode == Activity.RESULT_OK) {
                    if (data.getExtras().getBoolean("ToggleLED", false)) {
                        mService.toggleLED(mCurrentBluetoothDeviceAddress);
                    }

                    mSamplingRate = data.getExtras().getDouble("SamplingRate", -1);
                    if (mSamplingRate != -1) {
                        mService.writeSamplingRate(mCurrentBluetoothDeviceAddress, mSamplingRate);
                    }

                    mAccelerometerRange = data.getExtras().getInt("AccelerometerRange", -1);
                    if (mAccelerometerRange != -1) {
                        mService.writeAccelerometerRange(mCurrentBluetoothDeviceAddress, mAccelerometerRange);
                    }

                    mGyroscopeRange = data.getExtras().getInt("GyroscopeRange", -1);
                    if (mGyroscopeRange != -1) {
                        mService.writeGyroscopeRange(mCurrentBluetoothDeviceAddress, mGyroscopeRange);
                    }
                }
                break;
            case MainActivity.REQUEST_CONFIGURE_SHIMMER:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Current device set sensors: " + mCurrentBluetoothDeviceAddress + ", " + data.getExtras().getInt(ShimmerSensorActivationActivity.mDone));
                    mService.setEnabledSensors(data.getExtras().getInt(ShimmerSensorActivationActivity.mDone), mCurrentBluetoothDeviceAddress);
                }
                break;
        }
    }

    private ServiceConnection mSensorServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            ShimmerImuService.LocalBinder binder = (ShimmerImuService.LocalBinder) service;
            mService = binder.getService();
            Log.d(TAG, "Service connected");
        }

        public void onServiceDisconnected(ComponentName arg0) {}
    };
}