package de.bogutzky.data_collector.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;


public class MainCommandsActivity extends Activity {
    String mCurrentDevice = null;
    int mCurrentSlot = -1;
    private ShimmerService mService;
    private boolean mServiceBind = false;
    private String[] commands = new String[]{"Enable Sensors", "Sub Commands", "Show Graph"};
    private double mSamplingRate = -1;
    private int mAccelRange = -1;
    private int mGSRRange = -1;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_commands);

        Intent intent = new Intent(this, ShimmerService.class);
        getApplicationContext().bindService(intent, mTestServiceConnection, Context.BIND_AUTO_CREATE);
        Intent sender = getIntent();
        String extraData = sender.getExtras().getString("LocalDeviceID");
        mCurrentDevice = extraData;
        setTitle("CMD: " + mCurrentDevice);
        mCurrentSlot = sender.getExtras().getInt("CurrentSlot");
        Log.d("Shimmer", "Create MC:  " + extraData);


        final ListView listViewCommands = (ListView) findViewById(R.id.listView1);


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
                    Intent mainCommandIntent = new Intent(MainCommandsActivity.this, CommandsSub.class);
                    mainCommandIntent.putExtra("BluetoothAddress", "");
                    mainCommandIntent.putExtra("SamplingRate", mSamplingRate);
                    mainCommandIntent.putExtra("AccelerometerRange", mAccelRange);
                    mainCommandIntent.putExtra("GSRRange", mGSRRange);

                    startActivityForResult(mainCommandIntent, MainActivity.REQUEST_COMMANDS_SHIMMER);
                } else if (position == 0) {
                    Intent mainCommandIntent = new Intent(MainCommandsActivity.this, ConfigureActivity.class);
                    startActivityForResult(mainCommandIntent, MainActivity.REQUEST_CONFIGURE_SHIMMER);
                } else if (position == 2) {
                    Log.v("Commands Activity", "set result show graph");
                    Intent intent = new Intent();
                    intent.putExtra("mac", mCurrentDevice);
                    intent.putExtra("action", MainActivity.SHOW_GRAPH);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
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
                    mService.setEnabledSensors(data.getExtras().getInt(ConfigureActivity.mDone), mCurrentDevice);
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

        Intent intent = new Intent(MainCommandsActivity.this, MainActivity.class);
        Log.d("ShimmerH", "MCA on Resume");
    }


    private ServiceConnection mTestServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            ShimmerService.LocalBinder binder = (ShimmerService.LocalBinder) service;
            mService = binder.getService();
            mServiceBind = true;
            mSamplingRate = mService.getSamplingRate(mCurrentDevice);
            mAccelRange = mService.getAccelRange(mCurrentDevice);
            mGSRRange = mService.getGSRRange(mCurrentDevice);
        }

        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBind = false;
        }
    };
}
