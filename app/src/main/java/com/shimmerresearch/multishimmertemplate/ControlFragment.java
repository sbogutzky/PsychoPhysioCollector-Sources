package com.shimmerresearch.multishimmertemplate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.shimmerresearch.adapters.ExpandableListViewAdapter;
import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.database.ShimmerConfiguration;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.service.MultiShimmerTemplateService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.bogutzky.datacollector.app.R;


/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link ItemListActivity} in two-pane mode (on tablets) or a
 * {@link ItemDetailActivity} on handsets.
 */
public class ControlFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    public static List<String> connectedShimmerAddresses = new ArrayList<String>();
    public static List<String> streamingShimmerAddresses = new ArrayList<String>();
    static ExpandableListViewAdapter mAdapter;
    static View[] viewArray;
    static int countDisplayPRR = 0;
    private static Handler mHandler = new Handler() {


        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Shimmer.MESSAGE_PACKET_LOSS_DETECTED:
                    Log.d("SHIMMERPACKETRR3", "Detected");
                    if (countDisplayPRR % 1000 == 0) { //this is to prevent the UI from having to overwork when the PRR is very low
                        countDisplayPRR++;
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                case Shimmer.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Shimmer.STATE_CONNECTED:
                            //check to see if there are other Shimmer Devices which need to be connected
                            //sendBroadcast(intent);

                            connectedShimmerAddresses.add(((ObjectCluster) msg.obj).mBluetoothAddress);

                            break;
                        case Shimmer.STATE_CONNECTING:
                            mAdapter.notifyDataSetChanged();
                            break;
                        case Shimmer.STATE_NONE:
                            connectedShimmerAddresses.remove(((ObjectCluster) msg.obj).mBluetoothAddress);
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
                            mAdapter.notifyDataSetChanged();

                            break;
                        case Shimmer.MSG_STATE_STREAMING:
                            Log.d("ShimmerCA", "Streaming");
                            streamingShimmerAddresses.add(((ObjectCluster) msg.obj).mBluetoothAddress);
                            mAdapter.notifyDataSetChanged();
                            break;
                        case Shimmer.MSG_STATE_STOP_STREAMING:
                            Log.d("ShimmerCA", "Streaming");
                            streamingShimmerAddresses.remove(((ObjectCluster) msg.obj).mBluetoothAddress);
                            mAdapter.notifyDataSetChanged();
                            break;
                    }
                    break;

            }
        }
    };
    public final int MSG_BLUETOOTH_ADDRESS = 1;
    public final int MSG_CONFIGURE_SHIMMER = 2;
    public final int MSG_CONFIGURE_SENSORS_SHIMMER = 3;
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */


    MultiShimmerTemplateService mService;
    ListView listViewGestures;
    List<String> listValues = new ArrayList<String>();
    ExpandableListView listViewShimmers;
    DatabaseHandler db;
    String[] deviceNames;
    String[] deviceBluetoothAddresses;
    String[] shimmerVersions;
    String[][] mShimmerCommands;
    int numberofChilds[];
    ImageButton mButtonAddDevice;
    int tempPosition;
    boolean firstTime = true;
    View rootView = null;
    String mItemString;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ControlFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItemString = getArguments().getString(ARG_ITEM_ID);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //this values should be loaded from a database, but for now this will do, when you exit this fragment this list should be saved to a database
        rootView = inflater.inflate(R.layout.control_main, container, false);

        if (getActivity().getClass().getSimpleName().equals("ItemListActivity")) {
            this.mService = ((ItemListActivity) getActivity()).mService;

        } else {
            this.mService = ((ItemDetailActivity) getActivity()).mService;

        }

        if (mService != null) {
            setup();
        }

        // Show the dummy content as text in a TextView.
		/*if (mItem != null) {
			((TextView) rootView.findViewById(R.id.item_detail))
					.setText(mItem.content);
		}*/

        return rootView;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d("Activity Name", activity.getClass().getSimpleName());

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

    public void updateShimmerConfigurationList(List<ShimmerConfiguration> shimmerConfigurationList) {
        deviceNames = new String[shimmerConfigurationList.size() + 1]; //+1 to include All Devices
        shimmerVersions = new String[shimmerConfigurationList.size() + 1]; //+1 to include All Devices
        deviceBluetoothAddresses = new String[shimmerConfigurationList.size() + 1]; //+1 to include All Devices
        mShimmerCommands = new String[shimmerConfigurationList.size() + 1][5];
        numberofChilds = new int[shimmerConfigurationList.size() + 1];
        viewArray = new View[shimmerConfigurationList.size() + 1];
        Arrays.fill(numberofChilds, 5);
        //initialize all Devices
        deviceNames[0] = "All Devices";
        deviceBluetoothAddresses[0] = "";
        mShimmerCommands[0][0] = "Connect";
        mShimmerCommands[0][1] = "Disconnect";
        mShimmerCommands[0][2] = "Toggle LED";
        mShimmerCommands[0][3] = "Start Streaming";
        mShimmerCommands[0][4] = "Stop Streaming";
        //fill in the rest of the devices
        int pos = 1;
        for (ShimmerConfiguration sc : shimmerConfigurationList) {
            deviceNames[pos] = sc.getDeviceName();
            shimmerVersions[pos] = Integer.toString(sc.getShimmerVersion());
            deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
            mShimmerCommands[pos][0] = "Connect";
            mShimmerCommands[pos][1] = "Disconnect";
            mShimmerCommands[pos][2] = "Toggle LED";
            mShimmerCommands[pos][3] = "Start Streaming";
            mShimmerCommands[pos][4] = "Stop Streaming";
            pos++;
        }
        boolean[] temp = mService.getExapandableStates(this.getClass().getSimpleName());
        if (temp != null) {
            if (temp.length != deviceNames.length) {
                mService.removeExapandableStates(this.getClass().getSimpleName());
            }
        }
        mAdapter = new ExpandableListViewAdapter("ControlActivity", deviceNames, shimmerVersions, mShimmerCommands, getActivity(), listViewShimmers, numberofChilds, deviceBluetoothAddresses, mService, mService.getExapandableStates(this.getClass().getSimpleName()));
        listViewShimmers = (ExpandableListView) rootView.findViewById(R.id.expandableListViewReport);
        listViewShimmers.setAdapter(mAdapter);

    }

    public void onPause() {
        super.onPause();
        if (mService != null) {
            db.saveShimmerConfigurations("Temp", mService.mShimmerConfigurationList);
            //store the states of the listview
            mService.storeExapandableStates("ControlActivity", mAdapter.getExpandableStates());
        }
    }

    public void connectShimmer(int position) {
        mService.connectandConfigureShimmer(mService.mShimmerConfigurationList.get(position));
    }

    public void connectAllShimmer() {
        mService.connectAllShimmerSequentiallyWithConfig(mService.mShimmerConfigurationList);
    }

    public void disconnectAllShimmer() {
        mService.disconnectAllDevices();
    }

    public void disconnectShimmer(int position) {
        mService.disconnectShimmerNew(mService.mShimmerConfigurationList.get(position).getBluetoothAddress());
    }

    public void toggleLed(int position) {
        mService.toggleLED(mService.mShimmerConfigurationList.get(position).getBluetoothAddress());
    }

    public void toggleAllLeds() {
        mService.toggleAllLEDS();
    }

    public void startStreamingAllDevices() {
        mService.startStreamingAllDevices();
    }

    public void stopStreamingAllDevices() {
        mService.stopStreamingAllDevices();

    }

    public void startStreaming(int position) {
        mService.startStreaming(mService.mShimmerConfigurationList.get(position).getBluetoothAddress());
    }

    public void stopStreaming(int position) {
        mService.stopStreaming(mService.mShimmerConfigurationList.get(position).getBluetoothAddress());
    }

    @Override
    public void onStop() {
        super.onStop();
        //getActivity().unbindService(mConnection);
        //mServiceBind = false;
    }


    public void setup() {
        db = mService.mDataBase;
        mService.setHandler(mHandler);
        listViewShimmers = (ExpandableListView) rootView.findViewById(R.id.expandableListViewReport);

        mService.mShimmerConfigurationList = db.getShimmerConfigurations("Temp");
        updateShimmerConfigurationList(mService.mShimmerConfigurationList);
        for (ShimmerConfiguration sc : mService.mShimmerConfigurationList) {
            Log.d("ShimmerDB", sc.getDeviceName());
            Log.d("ShimmerDB", Double.toString(sc.getSamplingRate()));
        }
        //now connect the sensor nodes
        mService.enableGraphingHandler(false);
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
