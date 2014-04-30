package com.shimmerresearch.multishimmertemplate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
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
import android.widget.TextView;

import com.shimmerresearch.adapters.ExpandableListViewAdapter;
import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.service.MultiShimmerTemplateService;

import de.bogutzky.datacollector.app.R;


/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link ItemListActivity} in two-pane mode (on tablets) or a
 * {@link ItemDetailActivity} on handsets.
 */
public class BlankFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    static String mSensorView = "";
    static Dialog dialog;
    static TextView mTVmsgreceived;
    private static Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Shimmer.MESSAGE_TOAST:
                    Log.d("toast", msg.getData().getString(Shimmer.TOAST));

                case Shimmer.MESSAGE_READ:
                    if (mTVmsgreceived.getText().toString().equals("Data Received")) {

                    } else {
                        mTVmsgreceived.setText("Data Received");
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
    ExpandableListView listViewShimmers;
    DatabaseHandler db;
    ExpandableListViewAdapter mAdapter;
    String[] deviceNames;
    String[] deviceBluetoothAddresses;
    String[][] mEnabledSensorNames;
    int numberofChilds[];
    boolean firstTime = true;
    View rootView;
    /**
     * The dummy content this fragment is presenting.
     */
    MultiShimmerTemplateService mService;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BlankFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            //mItem = MenuContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }

        getActivity().invalidateOptionsMenu();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        rootView = inflater.inflate(R.layout.blank_main, container, false);

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

    public void onResume() {
        super.onResume();
        firstTime = true;


    }

    public void onPause() {
        super.onPause();


    }

    @Override
    public void onStop() {
        super.onStop();
        //getActivity().unbindService(mConnection);
        //mServiceBind = false;
    }

    public void setup() {
        db = mService.mDataBase;
        mService.mShimmerConfigurationList = db.getShimmerConfigurations("Temp");
        mTVmsgreceived = (TextView) rootView.findViewById(R.id.textViewDataReceived);
        mService.setGraphHandler(mHandler, "");
        mService.enableGraphingHandler(true);

    }
}
