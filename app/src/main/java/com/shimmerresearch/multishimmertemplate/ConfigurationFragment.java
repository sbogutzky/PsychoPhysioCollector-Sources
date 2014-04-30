package com.shimmerresearch.multishimmertemplate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.BiMap;
import com.shimmerresearch.adapters.ExpandableListViewAdapter;
import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.database.ShimmerConfiguration;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.service.MultiShimmerTemplateService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.bogutzky.datacollector.app.R;
/*
 *  Shimmer Research, important to note that the activity which holds this fragment should hold mShimmerConfigurationList . Reason is so this is extensible to multi fragment in single activity approach. 
 */


/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link ItemListActivity} in two-pane mode (on tablets) or a
 * {@link ItemDetailActivity} on handsets.
 */
public class ConfigurationFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    static ExpandableListViewAdapter mAdapter;
    static int countDisplayPRR = 0;
    private static Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int pos = 0;
            int position = 0;
            ImageView img;
            switch (msg.what) {
                case MultiShimmerTemplateService.MESSAGE_CONFIGURATION_CHANGE:
                    MultiShimmerTemplateService service = mServiceWRef.get();
                    service.mDataBase.getShimmerConfigurations("Temp");
                    updateExpandableListFromHandler(service.mDataBase.getShimmerConfigurations("Temp"));
                    break;
                case Shimmer.MESSAGE_PACKET_LOSS_DETECTED:
                    if (countDisplayPRR % 10000 == 0) { //this is to prevent the UI from having to overwork when the PRR is very low
                        countDisplayPRR++;
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                case Shimmer.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Shimmer.STATE_CONNECTED:
                            //check to see if there are other Shimmer Devices which need to be connected
                            //sendBroadcast(intent);

                            break;
                        case Shimmer.STATE_CONNECTING:
                            mAdapter.notifyDataSetChanged();
                            break;
                        case Shimmer.STATE_NONE:
                            Log.d("Shimmer", "NO_State" + ((ObjectCluster) msg.obj).mBluetoothAddress);
                               /*for (ShimmerConfiguration sc:mShimmerConfigurationList){
				           		if (sc.getBluetoothAddress().equals( ((ObjectCluster)msg.obj).mBluetoothAddress)){
				           			position=pos+1;
				           		}
				           		pos++;
				           	}
				        	img = (ImageView) viewArray[position].findViewById(R.id.usericon);
				        	img.setImageResource(R.drawable.locker_default);*/
                            mAdapter.notifyDataSetChanged();
                            //sendBroadcast(intent);
                            break;
                        case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                            Log.d("ShimmerCA", "Fully Initialized");
				           	/*pos=0;
				           	position=0;
				           	for (ShimmerConfiguration sc:mShimmerConfigurationList){
				           		if (sc.getBluetoothAddress().equals( ((ObjectCluster)msg.obj).mBluetoothAddress)){
				           			position=pos+1;
				           		}
				           		pos++;
				           	}
					        img = (ImageView) viewArray[position].findViewById(R.id.usericon);
					        img.setImageResource(R.drawable.locker_selected);*/
                            //mAdapter.notifyDataSetChanged();
                            break;

                    }

                    break;
            }
        }
    };
    private static WeakReference<MultiShimmerTemplateService> mServiceWRef;
    private static int mMaxNumberofChildsinList = 12;
    public final int MSG_BLUETOOTH_ADDRESS = 1;
    public final int MSG_CONFIGURE_SHIMMER = 2;
    public final int MSG_CONFIGURE_SENSORS_SHIMMER = 3;
    public String fragmentName = "ConfigureFrag";
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */

    boolean mServiceBind = false;
    public ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            // TODO Auto-generated method stub
            Log.d("Shimmer", "SERRRVVVIIICE");


            //mService.connectShimmer("00:06:66:46:B7:BE","1");


        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
            mServiceBind = false;
        }
    };
    ListView listViewGestures;
    List<String> listValues = new ArrayList<String>();
    ExpandableListView listViewShimmers;
    DatabaseHandler db;
    MultiShimmerTemplateService mService;
    String[] deviceNames;
    String[] shimmerVersions;
    String[] deviceBluetoothAddresses;
    String[][] mShimmerConfigs;
    int numberofChilds[];
    ImageButton mButtonAddDevice;
    int tempPosition;
    Dialog dialog;
    boolean firstTime = true;
    View rootView = null;
    Dialog mDialog;
    int dialogEnabledSensors = 0;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ConfigurationFragment() {
    }

    public static void updateExpandableListFromHandler(List<ShimmerConfiguration> shimmerConfigurationList) {
        //save configuration settings
        int pos = 0;
        String[] deviceNames;
        String[] shimmerVersions;
        String[] deviceBluetoothAddresses;
        String[][] shimmerConfigs;
        deviceNames = new String[shimmerConfigurationList.size() + pos]; //+1 to include All Devices
        shimmerVersions = new String[shimmerConfigurationList.size() + pos]; //+1 to include All Devices
        deviceBluetoothAddresses = new String[shimmerConfigurationList.size() + pos]; //+1 to include All Devices
        shimmerConfigs = new String[shimmerConfigurationList.size() + pos][mMaxNumberofChildsinList];
        int[] numberofChilds;
        numberofChilds = new int[shimmerConfigurationList.size() + pos];
        Arrays.fill(numberofChilds, 7);

        for (ShimmerConfiguration sc : shimmerConfigurationList) {
            if (sc.getShimmerVersion() == Shimmer.SHIMMER_3) {
                deviceNames[pos] = sc.getDeviceName();
                shimmerVersions[pos] = Integer.toString(sc.getShimmerVersion());
                deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
                shimmerConfigs[pos][0] = Integer.toString(sc.getEnabledSensors());
                shimmerConfigs[pos][1] = Double.toString(sc.getSamplingRate());
                shimmerConfigs[pos][2] = Integer.toString(sc.getAccelRange());
                shimmerConfigs[pos][3] = Integer.toString(sc.getGyroRange());
                shimmerConfigs[pos][4] = Integer.toString(sc.getMagRange());
                shimmerConfigs[pos][5] = Integer.toString(sc.getPressureResolution());
                shimmerConfigs[pos][6] = Integer.toString(sc.getGSRRange());
                shimmerConfigs[pos][7] = Integer.toString(sc.getIntExpPower());
                shimmerConfigs[pos][8] = "Set EXG Setting";
                shimmerConfigs[pos][9] = "Set Device Name";
                shimmerConfigs[pos][10] = "Set Bluetooth Address";
                shimmerConfigs[pos][11] = "Delete";
                numberofChilds[pos] = 12;
                pos++;
            } else {
                deviceNames[pos] = sc.getDeviceName();
                shimmerVersions[pos] = Integer.toString(sc.getShimmerVersion());
                deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
                shimmerConfigs[pos][0] = Integer.toString(sc.getEnabledSensors());
                shimmerConfigs[pos][1] = Double.toString(sc.getSamplingRate());
                shimmerConfigs[pos][2] = Integer.toString(sc.getAccelRange());
                shimmerConfigs[pos][3] = Integer.toString(sc.getMagRange());
                shimmerConfigs[pos][4] = Integer.toString(sc.getGSRRange());
                shimmerConfigs[pos][5] = "Set Device Name";
                shimmerConfigs[pos][6] = "Set Bluetooth Address";
                shimmerConfigs[pos][7] = "Delete";
                numberofChilds[pos] = 8;
                pos++;
            }
        }

        mAdapter.updatedata(deviceNames, shimmerVersions, shimmerConfigs, numberofChilds, deviceBluetoothAddresses, shimmerConfigurationList);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.

        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mDialog = new Dialog(getActivity());
        rootView = inflater.inflate(R.layout.configure_main, container, false);
        mButtonAddDevice = (ImageButton) rootView.findViewById(R.id.floating_button);
        mButtonAddDevice.setBackgroundColor(Color.TRANSPARENT);

        mButtonAddDevice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                mService.mShimmerConfigurationList.add(new ShimmerConfiguration("Device", "", mService.mShimmerConfigurationList.size(), Shimmer.SENSOR_ACCEL, 51.2, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1));
                updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                mService.removeExapandableStates("ControlActivity");
                mService.removeExapandableStates("PlotActivity");
            }

        });


        // Show the dummy content as text in a TextView.
		/*if (mItem != null) {
			((TextView) rootView.findViewById(R.id.item_detail))
					.setText(mItem.content);
		}*/

        if (getActivity().getClass().getSimpleName().equals("ItemListActivity")) {
            this.mService = ((ItemListActivity) getActivity()).mService;

        } else {
            this.mService = ((ItemDetailActivity) getActivity()).mService;

        }

        if (mService != null) {
            setup();
        }

        return rootView;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d("Activity Name", activity.getClass().getSimpleName());

        if (!isMyServiceRunning()) {
            Intent intent = new Intent(getActivity(), MultiShimmerTemplateService.class);
            getActivity().startService(intent);
        }
    }

    protected boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.shimmerresearch.service.MultiShimmerTemplateService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void onPause() {
        super.onPause();
        if (mService != null) {
            db.saveShimmerConfigurations("Temp", mService.mShimmerConfigurationList);

            //store the states of the listview
            mService.storeExapandableStates("ConfigureActivity", mAdapter.getExpandableStates());
        }

    }

    public void setDeviceName(final int position, final String currentDeviceName) {
        if (mService.noDevicesStreaming()) {
            // custom dialog
            final Dialog dialog = new Dialog(getActivity());
            dialog.setContentView(R.layout.dialogentry);
            dialog.setTitle("Set Device Name");

            // set the custom dialog components - text, image and button
            TextView text = (TextView) dialog.findViewById(R.id.text);
            text.setText("Enter Device Name");
            Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonProceed);
            final EditText textEntry = (EditText) dialog.findViewById(R.id.editTextEntry);
            textEntry.setText(currentDeviceName);
            // if button is clicked, close the custom dialog
            dialogButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                    sc.setDeviceName(textEntry.getText().toString());
                    mService.mShimmerConfigurationList.set(position, sc);


                    updateShimmerConfigurationList(mService.mShimmerConfigurationList);

                    dialog.dismiss();
                }
            });

            Button dialogButtonCancel = (Button) dialog.findViewById(R.id.dialogButtonCancel);
            // if button is clicked, close the custom dialog
            dialogButtonCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else {
            Toast.makeText(getActivity(), "Please ensure no device is streaming.", Toast.LENGTH_LONG).show();
        }
    }

    public void updateShimmerConfigurationList(List<ShimmerConfiguration> shimmerConfigurationList) {
        //save configuration settings
        db.saveShimmerConfigurations("Temp", mService.mShimmerConfigurationList);
        int pos = 0;
        deviceNames = new String[shimmerConfigurationList.size() + pos]; //+1 to include All Devices
        shimmerVersions = new String[shimmerConfigurationList.size() + pos]; //+1 to include All Devices
        deviceBluetoothAddresses = new String[shimmerConfigurationList.size() + pos]; //+1 to include All Devices
        mShimmerConfigs = new String[shimmerConfigurationList.size() + pos][mMaxNumberofChildsinList];
        numberofChilds = new int[shimmerConfigurationList.size() + pos];
        Arrays.fill(numberofChilds, 7);
        //initialize all Devices
	/*  		deviceNames[0]="All Devices";
	  		numberofChilds[0]=4;
	  		deviceBluetoothAddresses[0]="";
	  		mShimmerConfigs[0][0]="Enable Sensors";
	  		mShimmerConfigs[0][1]="Set Sampling Rate";
	  		mShimmerConfigs[0][2]="Set Accel Range";
	  		mShimmerConfigs[0][3]="Set GSR Range";*/

        //fill in the rest of the devices

        for (ShimmerConfiguration sc : shimmerConfigurationList) {
            if (sc.getShimmerVersion() == Shimmer.SHIMMER_3) {
                deviceNames[pos] = sc.getDeviceName();
                shimmerVersions[pos] = Integer.toString(sc.getShimmerVersion());
                deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
                mShimmerConfigs[pos][0] = Integer.toString(sc.getEnabledSensors());
                mShimmerConfigs[pos][1] = Double.toString(sc.getSamplingRate());
                mShimmerConfigs[pos][2] = Integer.toString(sc.getAccelRange());
                mShimmerConfigs[pos][3] = Integer.toString(sc.getGyroRange());
                mShimmerConfigs[pos][4] = Integer.toString(sc.getMagRange());
                mShimmerConfigs[pos][5] = Integer.toString(sc.getPressureResolution());
                mShimmerConfigs[pos][6] = Integer.toString(sc.getGSRRange());
                mShimmerConfigs[pos][7] = Integer.toString(sc.getIntExpPower());
                mShimmerConfigs[pos][8] = "Set EXG Setting";
                mShimmerConfigs[pos][9] = "Set Device Name";
                mShimmerConfigs[pos][10] = "Set Bluetooth Address";
                mShimmerConfigs[pos][11] = "Delete";
                numberofChilds[pos] = 12;
                pos++;
            } else {
                deviceNames[pos] = sc.getDeviceName();
                shimmerVersions[pos] = Integer.toString(sc.getShimmerVersion());
                deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
                mShimmerConfigs[pos][0] = Integer.toString(sc.getEnabledSensors());
                mShimmerConfigs[pos][1] = Double.toString(sc.getSamplingRate());
                mShimmerConfigs[pos][2] = Integer.toString(sc.getAccelRange());
                mShimmerConfigs[pos][3] = Integer.toString(sc.getMagRange());
                mShimmerConfigs[pos][4] = Integer.toString(sc.getGSRRange());
                mShimmerConfigs[pos][5] = "Set Device Name";
                mShimmerConfigs[pos][6] = "Set Bluetooth Address";
                mShimmerConfigs[pos][7] = "Delete";
                numberofChilds[pos] = 8;
                pos++;
            }
        }

        mAdapter.updatedata(deviceNames, shimmerVersions, mShimmerConfigs, numberofChilds, deviceBluetoothAddresses);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                //update the children array in mAdapter first
                mAdapter.notifyDataSetChanged();
            }
        });

    }

    public void deleteShimmerFromList(final int position) {
        // custom dialog
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialogverify);
        dialog.setTitle("Verification");

        // set the custom dialog components - text, image and button
        TextView text = (TextView) dialog.findViewById(R.id.text);
        text.setText("Delete Shimmer From List?");
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonProceed);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //first check to see if there is a connection to the device
                if (!mService.isShimmerConnected(mService.mShimmerConfigurationList.get(position).getBluetoothAddress())) {
                    mService.mShimmerConfigurationList.remove(position);
                    updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    mService.removeExapandableStates("ControlActivity");
                    mService.removeExapandableStates("PlotActivity");
                } else {
                    Toast.makeText(getActivity(), "Please disconnect from device first.", Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
            }
        });

        Button dialogButtonCancel = (Button) dialog.findViewById(R.id.dialogButtonCancel);
        // if button is clicked, close the custom dialog
        dialogButtonCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    public void setSensors(int position) {
        if (mService.noDevicesStreaming()) {
            if (position == -1) {
                Toast.makeText(getActivity(), "For the moment due to Shimmer 3 and Shimmer 2 having a set of different sensors this is disabled.", Toast.LENGTH_LONG).show();
            } else {
                if (mService.mShimmerConfigurationList.get(position).getShimmerVersion() == -1) {
                    Toast.makeText(getActivity(), "Please connect to the Shimmer device first. This only needs to be done the first time so the application can determine the version of the Shimmer device.", Toast.LENGTH_LONG).show();
                } else {
                    String bAddress = mService.mShimmerConfigurationList.get(position).getBluetoothAddress();
                    ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                    if (mService.mShimmerConfigurationList.get(position).getShimmerVersion() == Shimmer.SHIMMER_3) {
                        showEnableSensors(Shimmer.getListofSupportedSensors(sc.getShimmerVersion()), mService.mShimmerConfigurationList.get(position).getEnabledSensors(), bAddress, position);
                    } else {
                        showEnableSensors(Shimmer.getListofSupportedSensors(sc.getShimmerVersion()), mService.mShimmerConfigurationList.get(position).getEnabledSensors(), bAddress, position);
                    }
                }
            }
        } else {
            Toast.makeText(getActivity(), "Please ensure no device is streaming.", Toast.LENGTH_LONG).show();
        }
    }

    public void setBluetoothAddress(int position) {
        if (mService.noDevicesStreaming()) {
            Intent mainCommandIntent = new Intent(getActivity(), DeviceListActivity.class);
            mainCommandIntent.putExtra("Position", position);
            startActivityForResult(mainCommandIntent, MSG_BLUETOOTH_ADDRESS);
        } else {
            Toast.makeText(getActivity(), "Please ensure no device is streaming.", Toast.LENGTH_LONG).show();
        }

    }

    // position indicates the location of the device within the list
    public void configureShimmer(String attribute, int position) {
        if (mService.noDevicesStreaming()) {
            if (mService.mShimmerConfigurationList.get(position).getShimmerVersion() == -1) {
                Toast.makeText(getActivity(), "Please connect to the Shimmer device first. This only needs to be done the first time so the application can determine the version of the Shimmer device.", Toast.LENGTH_LONG).show();
            } else {
                Intent mainCommandIntent = new Intent(getActivity(), CommandsActivity.class);
                if (position == -1) { //means all devices, skip because
                    if (attribute.equals("SamplingRate")) {
                        mainCommandIntent.putExtra("AttributeValue", "");
                        mainCommandIntent.putExtra("Attribute", attribute);
                        mainCommandIntent.putExtra("Position", position);
                        startActivityForResult(mainCommandIntent, MSG_CONFIGURE_SHIMMER);
                    }
                } else {
                    if (attribute.equals("SamplingRate")) {
                        mainCommandIntent.putExtra("AttributeValue", mService.mShimmerConfigurationList.get(position).getSamplingRate());
                        mainCommandIntent.putExtra("Attribute", attribute);
                        mainCommandIntent.putExtra("Position", position);
                        startActivityForResult(mainCommandIntent, MSG_CONFIGURE_SHIMMER);
                    } else if (attribute.equals("Accelerometer")) {
                        int accelRange = mService.mShimmerConfigurationList.get(position).getAccelRange();
                        mainCommandIntent.putExtra("AttributeValue", (double) accelRange);
                        mainCommandIntent.putExtra("Attribute", attribute);
                        mainCommandIntent.putExtra("AccelLowPower", mService.mShimmerConfigurationList.get(position).getLowPowerAccelEnabled());
                        mainCommandIntent.putExtra("ShimmerVersion", mService.mShimmerConfigurationList.get(position).getShimmerVersion());
                        mainCommandIntent.putExtra("Position", position);
                        startActivityForResult(mainCommandIntent, MSG_CONFIGURE_SHIMMER);
                    } else {
                        mainCommandIntent.putExtra("Attribute", attribute);
                        mainCommandIntent.putExtra("Position", position);
                        mainCommandIntent.putExtra("AccelLowPower", mService.mShimmerConfigurationList.get(position).getLowPowerAccelEnabled());
                        mainCommandIntent.putExtra("GyroLowPower", mService.mShimmerConfigurationList.get(position).getLowPowerGyroEnabled());
                        mainCommandIntent.putExtra("MagLowPower", mService.mShimmerConfigurationList.get(position).getLowPowerMagEnabled());
                        mainCommandIntent.putExtra("ShimmerVersion", mService.mShimmerConfigurationList.get(position).getShimmerVersion());
                        startActivityForResult(mainCommandIntent, MSG_CONFIGURE_SHIMMER);
                    }
                }
            }
        } else {
            Toast.makeText(getActivity(), "Please ensure no device is streaming.", Toast.LENGTH_LONG).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MSG_BLUETOOTH_ADDRESS:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Log.d("SHimmerps", address);
                    boolean newAddress = true;

                    //first check if the Bluetooth Address has been previously selected
                    for (ShimmerConfiguration sc : mService.mShimmerConfigurationList) {
                        if (sc.getBluetoothAddress().equals(address)) {
                            newAddress = false;
                        }
                    }

                    if (newAddress) {
                        int position = data.getExtras().getInt("Position");
                        ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                        sc.setBluetoothAddress(address);
                        sc.setShimmerVersion(-1);
                        mService.mShimmerConfigurationList.set(position, sc);
                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    } else {
                        Toast.makeText(getActivity(), "Bluetooth Address already selected in list", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case MSG_CONFIGURE_SHIMMER:
                if (resultCode == Activity.RESULT_OK) {
                    int position = data.getExtras().getInt("Position");
                    if (data.getExtras().getDouble("SamplingRate", -1) != -1) {

                        for (int i = 0; i < mService.mShimmerConfigurationList.size(); i++) {
                            ShimmerConfiguration sctemp = mService.mShimmerConfigurationList.get(i);
                            sctemp.setSamplingRate(data.getExtras().getDouble("SamplingRate", -1));
                            double SamplingRateValue = data.getExtras().getDouble("SamplingRate", -1);
                            mShimmerConfigs[i][ExpandableListViewAdapter.SAMPLING_RATE_POSITION] = Double.toString(data.getExtras().getDouble("SamplingRate", -1));
                            mService.mShimmerConfigurationList.set(i, sctemp);
                        }


                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                        mService.writeSamplingRateAllDevices(data.getExtras().getDouble("SamplingRate", -1));
                        //updateShimmerConfigurationList(mService.mShimmerConfigurationList);

                    }
                    if (data.getExtras().getInt("LowPower", -1) != -1) {
                        if (data.getExtras().getString("Attribute").equals("Accelerometer")) {
                            ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                            sc.setLowPowerAccelEnabled(data.getExtras().getInt("LowPower", -1));
                            mService.mShimmerConfigurationList.set(position, sc);
                            mService.setAccelLowPower(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("LowPower", -1));
                            updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                        }
                        if (data.getExtras().getString("Attribute").equals("Gyroscope")) {
                            ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                            sc.setLowPowerGyroEnabled(data.getExtras().getInt("LowPower", -1));
                            mService.mShimmerConfigurationList.set(position, sc);
                            mService.setGyroLowPower(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("LowPower", -1));
                            updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                        }
                        if (data.getExtras().getString("Attribute").equals("Magnetometer")) {
                            ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                            sc.setLowPowerMagEnabled(data.getExtras().getInt("LowPower", -1));
                            mService.mShimmerConfigurationList.set(position, sc);
                            mService.setMagLowPower(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("LowPower", -1));
                            updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                        }
                    }
                    if (data.getExtras().getInt("AccelRange", -1) != -1) {
                        if (position == -1) {
                            for (int i = 0; i < mService.mShimmerConfigurationList.size(); i++) {
                                ShimmerConfiguration sctemp = mService.mShimmerConfigurationList.get(i);
                                sctemp.setAccelRange(data.getExtras().getInt("AccelRange", -1));
                                mShimmerConfigs[i][ExpandableListViewAdapter.ACCEL_RANGE_POSITION] = Integer.toString(data.getExtras().getInt("AccelRange", -1));
                                mService.mShimmerConfigurationList.set(i, sctemp);
                                mService.writeAccelRange(mService.mShimmerConfigurationList.get(i).getBluetoothAddress(), data.getExtras().getInt("AccelRange", -1));
                            }
                        } else {
                            ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                            sc.setAccelRange(data.getExtras().getInt("AccelRange", -1));
                            //TextView textView = (TextView) tempViewChild.findViewById(R.id.grp_child);
                            //textView.setText(sc.getAccelRangeText());
                            mService.mShimmerConfigurationList.set(position, sc);
                            mService.writeAccelRange(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("AccelRange", -1));
                        }
                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    }

                    if (data.getExtras().getInt("GyroRange", -1) != -1) {

                        ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                        sc.setGyroRange(data.getExtras().getInt("GyroRange", -1));
                        //TextView textView = (TextView) tempViewChild.findViewById(R.id.grp_child);
                        //textView.setText(sc.getAccelRangeText());
                        mService.mShimmerConfigurationList.set(position, sc);
                        mService.writeGyroRange(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("GyroRange", -1));
                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    }
                    if (data.getExtras().getInt("PressureResolution", -1) != -1) {

                        ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                        sc.setPressureResolution(data.getExtras().getInt("PressureResolution", -1));
                        //TextView textView = (TextView) tempViewChild.findViewById(R.id.grp_child);
                        //textView.setText(sc.getAccelRangeText());
                        mService.mShimmerConfigurationList.set(position, sc);
                        mService.writePressureResolution(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("PressureResolution", -1));
                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    }
                    if (data.getExtras().getInt("IntExpPower", -1) != -1) {

                        ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                        sc.setIntExpPower(data.getExtras().getInt("IntExpPower", -1));
                        //TextView textView = (TextView) tempViewChild.findViewById(R.id.grp_child);
                        //textView.setText(sc.getAccelRangeText());
                        mService.mShimmerConfigurationList.set(position, sc);
                        mService.writeIntExpPower(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("IntExpPower", -1));
                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    }
                    if (data.getExtras().getString("EXG") != null) {
                        String v = data.getExtras().getString("EXG");
                        if (v.equals("ECG")) {
                            mService.writeEXGSetting(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), 0);
                        }
                        if (v.equals("EMG")) {
                            mService.writeEXGSetting(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), 1);
                        }
                        if (v.equals("Test Signal")) {
                            mService.writeEXGSetting(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), 2);
                        }
                        Toast.makeText(getActivity(), "Please ensure you are connected to the device when executing this command", Toast.LENGTH_LONG).show();
                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    }
                    if (data.getExtras().getInt("MagRange", -1) != -1) {

                        ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                        sc.setMagRange(data.getExtras().getInt("MagRange", -1));
                        //TextView textView = (TextView) tempViewChild.findViewById(R.id.grp_child);
                        //textView.setText(sc.getAccelRangeText());
                        mService.mShimmerConfigurationList.set(position, sc);
                        mService.writeMagRange(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("MagRange", -1));
                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    }

                    if (data.getExtras().getInt("GSRRange", -1) != -1) {

                        if (position == -1) {
                            for (int i = 0; i < mService.mShimmerConfigurationList.size(); i++) {
                                ShimmerConfiguration sctemp = mService.mShimmerConfigurationList.get(i);
                                sctemp.setGSRRange(data.getExtras().getInt("GSRRange", -1));
                                mShimmerConfigs[i][ExpandableListViewAdapter.GSR_RANGE_POSITION] = Integer.toString(data.getExtras().getInt("GSRRange", -1));
                                mService.mShimmerConfigurationList.set(i, sctemp);
                                mService.writeGSRRange(mService.mShimmerConfigurationList.get(i).getBluetoothAddress(), data.getExtras().getInt("GSRRange", -1));
                            }
                        } else {
                            ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                            sc.setGSRRange(data.getExtras().getInt("GSRRange", -1));
                            //TextView textView = (TextView) tempViewChild.findViewById(R.id.grp_child);
                            //textView.setText(sc.getGSRRangeText());
                            mService.mShimmerConfigurationList.set(position, sc);
                            mService.writeGSRRange(mService.mShimmerConfigurationList.get(position).getBluetoothAddress(), data.getExtras().getInt("GSRRange", -1));
                        }
                        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                    }
                    db.saveShimmerConfigurations("Temp", mService.mShimmerConfigurationList); // save changes to the db
                }
                break;
            case MSG_CONFIGURE_SENSORS_SHIMMER:
                if (resultCode == Activity.RESULT_OK) {
                    int position = data.getExtras().getInt("Position");
                    if (position == -1) {
                        for (int i = 0; i < mService.mShimmerConfigurationList.size(); i++) {
                            ShimmerConfiguration sctemp = mService.mShimmerConfigurationList.get(i);
                            sctemp.setEnabledSensors(data.getExtras().getInt(ConfigureSensorsActivity.mDone));
                            mShimmerConfigs[i][ExpandableListViewAdapter.ENABLED_SENSORS_POSITION] = Integer.toString(data.getExtras().getInt(ConfigureSensorsActivity.mDone));
                            mService.mShimmerConfigurationList.set(i, sctemp);
                            mService.setEnabledSensors(data.getExtras().getInt(ConfigureSensorsActivity.mDone), mService.mShimmerConfigurationList.get(i).getBluetoothAddress());
                        }
                    } else {
                        ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                        sc.setEnabledSensors(data.getExtras().getInt(ConfigureSensorsActivity.mDone));
                        db.saveShimmerConfigurations("Temp", mService.mShimmerConfigurationList); // save changes to the db
                        mService.setEnabledSensors(sc.getEnabledSensors(), sc.getBluetoothAddress());
                    }
                    updateShimmerConfigurationList(mService.mShimmerConfigurationList);
                }
                break;
        }
    }

    public boolean isShimmerConnected(String bluetoothAddress) {
        // TODO Auto-generated method stub
        return mService.isShimmerConnected(bluetoothAddress);
    }

    public void showEnableSensors(final String[] sensorNames, int enabledSensors, final String bluetoothAddress, final int position) {
        dialogEnabledSensors = enabledSensors;
        mDialog.setContentView(R.layout.dialog_enable_sensor_view);
        TextView title = (TextView) mDialog.findViewById(android.R.id.title);
        title.setText("Select Signal");
        final ListView listView = (ListView) mDialog.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        ArrayAdapter<String> adapterSensorNames = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, android.R.id.text1, sensorNames);
        listView.setAdapter(adapterSensorNames);
        ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
        final BiMap<String, String> sensorBitmaptoName = Shimmer.generateBiMapSensorIDtoSensorName(sc.getShimmerVersion());
        for (int i = 0; i < sensorNames.length; i++) {
            int iDBMValue = Integer.parseInt(sensorBitmaptoName.inverse().get(sensorNames[i]));
            if ((iDBMValue & enabledSensors) > 0) {
                listView.setItemChecked(i, true);
            }
        }

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int clickIndex,
                                    long arg3) {
                int sensorIdentifier = Integer.parseInt(sensorBitmaptoName.inverse().get(sensorNames[clickIndex]));
                //check and remove any old daughter boards (sensors) which will cause a conflict with sensorIdentifier
                dialogEnabledSensors = mService.sensorConflictCheckandCorrection(dialogEnabledSensors, sensorIdentifier, mService.mShimmerConfigurationList.get(position).getShimmerVersion());
                //update the checkbox accordingly
                for (int i = 0; i < sensorNames.length; i++) {
                    int iDBMValue = Integer.parseInt(sensorBitmaptoName.inverse().get(sensorNames[i]));
                    if ((iDBMValue & dialogEnabledSensors) > 0) {
                        listView.setItemChecked(i, true);
                    } else {
                        listView.setItemChecked(i, false);
                    }
                }
            }

        });


        Button mDoneButton = (Button) mDialog.findViewById(R.id.buttonEnableSensors);

        mDoneButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if (position == -1) {
		    				/*for (int i=0;i<mService.mShimmerConfigurationList.size();i++){
								ShimmerConfiguration sctemp = mService.mShimmerConfigurationList.get(i);
								sctemp.setEnabledSensors(dialogEnabledSensors);
								mShimmerConfigs[i+1][ExpandableListViewAdapter.ENABLED_SENSORS_POSITION]=Integer.toString(dialogEnabledSensors);
								mService.mShimmerConfigurationList.set(i, sctemp);
								mService.setEnabledSensors(dialogEnabledSensors, mService.mShimmerConfigurationList.get(i).getBluetoothAddress());
							}*/
                    Toast.makeText(getActivity(), "Since not all sensors are currently available for Shimmer 3, this is disabled for the time being. Please set your sensors individually.", Toast.LENGTH_LONG).show();
                } else {
                    ShimmerConfiguration sc = mService.mShimmerConfigurationList.get(position);
                    sc.setEnabledSensors(dialogEnabledSensors);
                    db.saveShimmerConfigurations("Temp", mService.mShimmerConfigurationList); // save changes to the db
                    mService.setEnabledSensors(sc.getEnabledSensors(), sc.getBluetoothAddress());
                }
                updateShimmerConfigurationList(mService.mShimmerConfigurationList);


                mDialog.dismiss();
            }
        });


        mDialog.show();

    }

    public int getShimmerVersion(int position) {
        return mService.mShimmerConfigurationList.get(position).getShimmerVersion();
    }

    @Override
    public void onStop() {
        super.onStop();
        //getActivity().unbindService(mConnection);
        //mServiceBind = false;
    }

    public void setup() {
        listViewShimmers = (ExpandableListView) rootView.findViewById(R.id.expandableListViewReport);
        db = mService.mDataBase;
        mService.enableGraphingHandler(false);
        mService.setHandler(mHandler);
        mServiceWRef = new WeakReference<MultiShimmerTemplateService>(mService);
        mService.mShimmerConfigurationList = db.getShimmerConfigurations("Temp");
        if (firstTime) {

            int pos = 0;
            deviceNames = new String[mService.mShimmerConfigurationList.size() + pos]; //+1 to include All Devices
            shimmerVersions = new String[mService.mShimmerConfigurationList.size() + pos]; //+1 to include All Devices
            deviceBluetoothAddresses = new String[mService.mShimmerConfigurationList.size() + pos]; //+1 to include All Devices
            mShimmerConfigs = new String[mService.mShimmerConfigurationList.size() + 1][mMaxNumberofChildsinList];
            numberofChilds = new int[mService.mShimmerConfigurationList.size() + pos];
            Arrays.fill(numberofChilds, 7);
            //initialize all Devices
	/*      	  		deviceNames[0]="All Devices";
	      	  		numberofChilds[0]=4;
	      	  		deviceBluetoothAddresses[0]="";
	      	  		mShimmerConfigs[0][0]="Enable Sensors";
	      	  		mShimmerConfigs[0][1]="Set Sampling Rate";
	      	  		mShimmerConfigs[0][2]="Set Accel Range";
	      	  		mShimmerConfigs[0][3]="Set GSR Range";*/

            //fill in the rest of the devices

            for (ShimmerConfiguration sc : mService.mShimmerConfigurationList) {
                if (sc.getShimmerVersion() == Shimmer.SHIMMER_3) {
                    deviceNames[pos] = sc.getDeviceName();
                    shimmerVersions[pos] = Integer.toString(sc.getShimmerVersion());
                    deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
                    mShimmerConfigs[pos][0] = Integer.toString(sc.getEnabledSensors());
                    mShimmerConfigs[pos][1] = Double.toString(sc.getSamplingRate());
                    mShimmerConfigs[pos][2] = Integer.toString(sc.getAccelRange());
                    mShimmerConfigs[pos][3] = Integer.toString(sc.getGyroRange());
                    mShimmerConfigs[pos][4] = Integer.toString(sc.getMagRange());
                    mShimmerConfigs[pos][5] = Integer.toString(sc.getPressureResolution());
                    mShimmerConfigs[pos][6] = Integer.toString(sc.getGSRRange());
                    mShimmerConfigs[pos][7] = Integer.toString(sc.getIntExpPower());
                    mShimmerConfigs[pos][8] = "Set EXG Setting";
                    mShimmerConfigs[pos][9] = "Set Device Name";
                    mShimmerConfigs[pos][10] = "Set Bluetooth Address";
                    mShimmerConfigs[pos][11] = "Delete";
                    numberofChilds[pos] = 12;
                    pos++;
                } else {
                    deviceNames[pos] = sc.getDeviceName();
                    shimmerVersions[pos] = Integer.toString(sc.getShimmerVersion());
                    deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
                    mShimmerConfigs[pos][0] = Integer.toString(sc.getEnabledSensors());
                    mShimmerConfigs[pos][1] = Double.toString(sc.getSamplingRate());
                    mShimmerConfigs[pos][2] = Integer.toString(sc.getAccelRange());
                    mShimmerConfigs[pos][3] = Integer.toString(sc.getMagRange());
                    mShimmerConfigs[pos][4] = Integer.toString(sc.getGSRRange());
                    mShimmerConfigs[pos][5] = "Set Device Name";
                    mShimmerConfigs[pos][6] = "Set Bluetooth Address";
                    mShimmerConfigs[pos][7] = "Delete";
                    numberofChilds[pos] = 8;
                    pos++;
                }
            }


            mAdapter = new ExpandableListViewAdapter("ConfigureActivity", deviceNames, shimmerVersions, mShimmerConfigs, getActivity(), listViewShimmers, numberofChilds, deviceBluetoothAddresses, mService, mService.getExapandableStates(this.getClass().getSimpleName()));
            listViewShimmers = (ExpandableListView) rootView.findViewById(R.id.expandableListViewReport);
            listViewShimmers.setAdapter(mAdapter);
            firstTime = false;
        }
        //now connect the sensor nodes
        mService.enableGraphingHandler(true);
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getActivity(), "Bluetooth not supported on device.", Toast.LENGTH_LONG).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, -1);
            }
        }

    }
}
