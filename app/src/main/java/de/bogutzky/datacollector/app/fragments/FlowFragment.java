package de.bogutzky.datacollector.app.fragments;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
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
import java.util.List;

import de.bogutzky.datacollector.app.R;
import de.bogutzky.datacollector.app.tools.Logging;

public class FlowFragment extends Fragment {

    private static final String TAG = "FlowFragment";
    private Dialog dialog;
    private List<String> connectedShimmers = new ArrayList<String>();
    private List<String> streamingShimmers = new ArrayList<String>();
    private Logging logging;
    private Boolean loggingEnabled = false;
    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Shimmer.MESSAGE_TOAST:
                    Log.d(TAG, msg.getData().getString(Shimmer.TOAST));

                case Shimmer.MESSAGE_READ:
                    if ((msg.obj instanceof ObjectCluster)) {
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        String bluetoothAddress;
                        if (connectedShimmers.size() > 0) {
                            bluetoothAddress = connectedShimmers.get(0);
                        } else {
                            bluetoothAddress = "";
                            Toast.makeText(getActivity(), "No Device Connected", Toast.LENGTH_LONG).show();
                        }
                        if (loggingEnabled && objectCluster.mBluetoothAddress.equals(bluetoothAddress)) {
                            //logging.logData(objectCluster);
                            new logData().execute(objectCluster);
                        }
                    }
                    break;
            }
        }
    };
    private View view;
    private EditText editTextLoggingFileName;
    private Button buttonStartLogging;
    private String filename;
    private MultiShimmerTemplateService multiShimmerTemplateService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                            filename = editTextLoggingFileName.getText().toString();
                            logging = new Logging(filename, ",", "DataCollector");
                            if (logging.mOutputFile.exists()) {
                                showReplaceDialog("File already exist in file system. Would you like to overwrite it?");
                            } else {
                                setLoggingEnabled(true);
                            }
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
                    logging.closeFile();
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

    @Override
    public void onResume() {
        super.onResume();
        if (loggingEnabled) {
            buttonStartLogging.setText("Stop Logging");
            buttonStartLogging.setBackgroundColor(Color.RED);
            editTextLoggingFileName.setText(logging.getName());
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

    private void showReplaceDialog(String text) {
        dialog.setContentView(R.layout.log_popup);
        dialog.setTitle("Replace File?");

        TextView tv = (TextView) dialog.findViewById(R.id.textViewDialog);
        tv.setText(text);

        Button buttonYes = (Button) dialog.findViewById(R.id.ButtonYes);
        Button buttonNo = (Button) dialog.findViewById(R.id.ButtonNo);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                loggingEnabled = true;
                dialog.dismiss();
            }
        });

        buttonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                loggingEnabled = false;
                buttonStartLogging.setText("Start Logging");
                buttonStartLogging.setBackgroundColor(Color.GREEN);
                dialog.dismiss();
            }
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    public void setup() {
        DatabaseHandler db = multiShimmerTemplateService.mDataBase;
        multiShimmerTemplateService.mShimmerConfigurationList = db.getShimmerConfigurations("Temp");
        multiShimmerTemplateService.setGraphHandler(mHandler, "");
        multiShimmerTemplateService.enableGraphingHandler(true);
        TextView mTVShimmerId = (TextView) view.findViewById(R.id.textViewShimmerId);
        connectedShimmers = ControlFragment.connectedShimmerAddresses;
        streamingShimmers = ControlFragment.streamingShimmerAddresses;
        dialog = new Dialog(getActivity());
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
            logging.logData(objectClusters[0], "CAL", false);
            return null;
        }
    }
}
