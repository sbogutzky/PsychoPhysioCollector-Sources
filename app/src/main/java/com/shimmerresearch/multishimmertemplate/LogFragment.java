package com.shimmerresearch.multishimmertemplate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.adapters.ExpandableListViewAdapter;
import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.multishimmertemplate.menucontent.MenuContent;
import com.shimmerresearch.service.MultiShimmerTemplateService;
import com.shimmerresearch.tools.Logging;

import java.util.ArrayList;
import java.util.List;

import de.bogutzky.datacollector.app.R;

public class LogFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";
    public static Boolean mLoggingEnabled = false;
    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Shimmer.MESSAGE_TOAST:
                    Log.d("toast", msg.getData().getString(Shimmer.TOAST));

                case Shimmer.MESSAGE_READ:
                    if ((msg.obj instanceof ObjectCluster)) {
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        String bluetoothAddress;
                        if (connectedShimmers.size() > 0) {
                            bluetoothAddress = connectedShimmers.get(0).toString();
                        } else {
                            bluetoothAddress = "";
                            Toast.makeText(getActivity(), "No Device Connected", Toast.LENGTH_LONG).show();
                        }
                        if (mLoggingEnabled && objectCluster.mBluetoothAddress.equals(bluetoothAddress)) {
                            mLog.logData(objectCluster);
                        }
                    }
                    break;
            }
        }
    };
    static String mSensorView = "";
    static Dialog dialog;
    private static Logging mLog;
    private static TextView mTVShimmerId;
    private static Button mBtnStartLogging;
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
    MultiShimmerTemplateService mService;
    private boolean mServiceBind = false;
    public ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d("Shimmer", "SERRRVVVIIICE");
            Log.d("ShimmerService", "service connected from main activity");
            com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder binder = (com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder) service;
            mService = binder.getService();
            mServiceBind = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBind = false;
        }
    };
    private EditText mEditTextLogFileName;
    private String mFileName;
    private List<String> connectedShimmers = new ArrayList<String>();
    private List<String> streamingShimmers = new ArrayList<String>();
    /**
     * The dummy content this fragment is presenting.
     */
    private MenuContent.MenuItem mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */


    public LogFragment() {
    }

    public static void showReplaceDialog(String text) {
        dialog.setContentView(R.layout.log_popup);
        dialog.setTitle("Replace File?");

        TextView tv = (TextView) dialog.findViewById(R.id.textViewDialog);
        tv.setText(text);

        Button buttonYes = (Button) dialog.findViewById(R.id.ButtonYes);
        Button buttonNo = (Button) dialog.findViewById(R.id.ButtonNo);
        buttonYes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mLoggingEnabled = true;
                dialog.dismiss();
            }
        });

        buttonNo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mLoggingEnabled = false;
                mBtnStartLogging.setText("Start Logging");
                mBtnStartLogging.setBackgroundColor(Color.GREEN);
                dialog.dismiss();
            }
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            String itemString = getArguments().getString(ARG_ITEM_ID);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        rootView = inflater.inflate(R.layout.log_main, container, false);

        mEditTextLogFileName = (EditText) rootView.findViewById(R.id.editTextLogFileName);
        mEditTextLogFileName.setText("msttest");
        mBtnStartLogging = (Button) rootView.findViewById(R.id.buttonStartLogging);
        mBtnStartLogging.setBackgroundColor(Color.GREEN);
        mBtnStartLogging.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (mBtnStartLogging.getText().equals("Start Logging")) {
                    if (connectedShimmers.size() > 0) {
                        if (streamingShimmers.contains(connectedShimmers.get(0))) {
                            mBtnStartLogging.setText("Stop Logging");
                            mBtnStartLogging.setBackgroundColor(Color.RED);
                            mFileName = mEditTextLogFileName.getText().toString();
                            mLog = new Logging(mFileName, ",", "MultiShimmerTemplate");
                            //TODO check this. Moved from Logging.java
                            if (mLog.mOutputFile.exists()) {
                                showReplaceDialog("File already exist in file system. Would you like to overwrite it?");
                            } else {
                                mLoggingEnabled = true;
                            }
                        } else {
                            mLoggingEnabled = false;
                            setLogging(mLoggingEnabled);
                            Toast.makeText(getActivity(), "Connected Device Is Not Streaming", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        mLoggingEnabled = false;
                        setLogging(mLoggingEnabled);
                        Toast.makeText(getActivity(), "No Device Connected", Toast.LENGTH_LONG).show();
                    }
                } else {
                    mLoggingEnabled = false;
                    setLogging(mLoggingEnabled);
                    mLog.closeFile();
                }
            }
        });

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
        if (mLoggingEnabled) {
            mBtnStartLogging.setText("Stop Logging");
            mBtnStartLogging.setBackgroundColor(Color.RED);
            mEditTextLogFileName.setText(mLog.getName());
        }
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

    public void setLogging(boolean enable) {
        if (enable) {
            mLoggingEnabled = true;
            mBtnStartLogging.setText("Stop Logging");
            mBtnStartLogging.setBackgroundColor(Color.RED);
        } else {
            mLoggingEnabled = false;
            mBtnStartLogging.setText("Start Logging");
            mBtnStartLogging.setBackgroundColor(Color.GREEN);
        }
    }

    public void setup() {
        db = mService.mDataBase;
        mService.mShimmerConfigurationList = db.getShimmerConfigurations("Temp");
        mService.setGraphHandler(mHandler, "");
        mService.enableGraphingHandler(true);
        mTVShimmerId = (TextView) rootView.findViewById(R.id.textViewShimmerId);
        connectedShimmers = ControlFragment.connectedShimmerAddresses;
        streamingShimmers = ControlFragment.streamingShimmerAddresses;
        dialog = new Dialog(getActivity());
        if (connectedShimmers.size() > 0) {
            String bluetoothAddress = connectedShimmers.get(0).toString();
            mTVShimmerId.setText(bluetoothAddress);
        } else {
            mTVShimmerId.setText("No Shimmer Connected");
        }

    }
}
