package de.bogutzky.datacollector.app.fragments;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.multishimmertemplate.ControlFragment;
import com.shimmerresearch.multishimmertemplate.ItemDetailActivity;
import com.shimmerresearch.multishimmertemplate.ItemListActivity;
import com.shimmerresearch.service.MultiShimmerTemplateService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.bogutzky.datacollector.app.R;
import de.bogutzky.datacollector.app.tools.Logger;

public class FlowFragment extends Fragment {

    private static final String TAG = "FlowFragment";
    private List<String> connectedShimmers = new ArrayList<String>();
    private List<String> streamingShimmers = new ArrayList<String>();
    private HashMap <String, Logger> loggers;
    private Boolean loggingEnabled = false;
    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Shimmer.MESSAGE_TOAST:
                    Log.d(TAG, msg.getData().getString(Shimmer.TOAST));

                case Shimmer.MESSAGE_READ:
                    if ((msg.obj instanceof ObjectCluster)) {
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        if (connectedShimmers.size() > 0) {
                            if (loggingEnabled) {
                                new logData().execute(objectCluster);
                            }
                        } else {
                            Toast.makeText(getActivity(), "No Device Connected", Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
            }
        }
    };
    private View view;
    private EditText editTextLoggingFileName;
    private Button buttonStartLogging;
    private MultiShimmerTemplateService multiShimmerTemplateService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.log_main, container, false);

        editTextLoggingFileName = (EditText) view.findViewById(R.id.editTextLogFileName);
        editTextLoggingFileName.setText("testfile");
        buttonStartLogging = (Button) view.findViewById(R.id.buttonStartLogging);
        buttonStartLogging.setBackgroundColor(Color.GREEN);
        buttonStartLogging.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (buttonStartLogging.getText().equals("Start Logging")) {
                    if (connectedShimmers.size() > 0) {
                        if (streamingShimmers.contains(connectedShimmers.get(0))) {
                            buttonStartLogging.setText("Stop Logging");
                            buttonStartLogging.setBackgroundColor(Color.RED);

                            setLoggers();
                            setLoggingEnabled(true);
                        } else {
                            setLoggingEnabled(false);
                            Toast.makeText(getActivity(), "Connected Device Is Not Streaming", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        setLoggingEnabled(false);
                        Toast.makeText(getActivity(), "No Device Connected", Toast.LENGTH_LONG).show();
                    }
                } else {
                    setLoggingEnabled(false);
                }
            }
        });

        if (getActivity().getClass().getSimpleName().equals("ItemListActivity")) {
            this.multiShimmerTemplateService = ((ItemListActivity) getActivity()).getService();
        } else {
            this.multiShimmerTemplateService = ((ItemDetailActivity) getActivity()).getService();
        }

        if (multiShimmerTemplateService != null) {
            setup();
        }

        return view;
    }

    private void setLoggers() {
        loggers = new HashMap<String, Logger>();
        int i = 0;
        String filename = editTextLoggingFileName.getText().toString();
        for (String bluetoothAddress : streamingShimmers) {
            Logger logger = new Logger(filename + "_" + i, ",", "DataCollector");
            loggers.put(bluetoothAddress, logger);
            i++;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (loggingEnabled) {
            buttonStartLogging.setText("Stop Logging");
            buttonStartLogging.setBackgroundColor(Color.RED);
            //TODO: Text setzen
            //editTextLoggingFileName.setText(logger.Filename());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!isMultiShimmerTemplateRunning()) {
            Intent intent = new Intent(getActivity(), MultiShimmerTemplateService.class);
            getActivity().startService(intent);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setup() {
        DatabaseHandler db = multiShimmerTemplateService.mDataBase;
        multiShimmerTemplateService.mShimmerConfigurationList = db.getShimmerConfigurations("Temp");
        multiShimmerTemplateService.setGraphHandler(mHandler, "");
        multiShimmerTemplateService.enableGraphingHandler(true);
        TextView mTVShimmerId = (TextView) view.findViewById(R.id.textViewShimmerId);
        connectedShimmers = ControlFragment.connectedShimmerAddresses;
        streamingShimmers = ControlFragment.streamingShimmerAddresses;
        if (connectedShimmers.size() > 0) {
            String bluetoothAddress = connectedShimmers.get(0);
            mTVShimmerId.setText(bluetoothAddress);
        } else {
            mTVShimmerId.setText("No Shimmer Connected");
        }
    }

    private boolean isMultiShimmerTemplateRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.shimmerresearch.service.MultiShimmerTemplateService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void setLoggingEnabled(Boolean loggingEnabled) {
        if (loggingEnabled) {
            buttonStartLogging.setText("Stop Logging");
            buttonStartLogging.setBackgroundColor(Color.RED);
        } else {
            buttonStartLogging.setText("Start Logging");
            buttonStartLogging.setBackgroundColor(Color.GREEN);
        }
        this.loggingEnabled = loggingEnabled;
    }

    public void setMultiShimmerTemplateService(MultiShimmerTemplateService multiShimmerTemplateService) {
        this.multiShimmerTemplateService = multiShimmerTemplateService;
    }

    class logData extends AsyncTask<ObjectCluster, Integer, String> {

        @Override
        protected String doInBackground(ObjectCluster... objectClusters) {
            ObjectCluster objectCluster = objectClusters[0];
            Logger logger = loggers.get(objectCluster.mBluetoothAddress);
            logger.logData(objectClusters[0], "CAL", false);
            return null;
        }
    }
}
