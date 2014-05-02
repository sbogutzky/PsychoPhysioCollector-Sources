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
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.driver.FormatCluster;
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
    private EditText editTextLoggingFileName;
    private Button buttonStartLogging;
    private MultiShimmerTemplateService multiShimmerTemplateService;

    private static final int TIMER_UPDATE = 1;
    private static final int TIMER_END = 2;
    private static final int TIMER_CYCLE_IN_MIN = 1;
    private TextView textViewTimer;
    private Handler timerHandler;
    private Thread timerThread;
    private boolean timerShouldContinue = false;

    private static final String SCALE = "Flow Short Scale";
    private static final int SCALE_ITEM_COUNT = 16;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flow, container, false);

        editTextLoggingFileName = (EditText) view.findViewById(R.id.editTextLogFileName);
        editTextLoggingFileName.setText("testfile");
        textViewTimer = (TextView) view.findViewById(R.id.timer);
        buttonStartLogging = (Button) view.findViewById(R.id.buttonStartLogging);
        buttonStartLogging.setBackgroundColor(Color.GREEN);
        buttonStartLogging.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (buttonStartLogging.getText().equals(getActivity().getString(R.string.start_logging))) {
                    //if (connectedShimmers.size() > 0) {
                        //if (streamingShimmers.contains(connectedShimmers.get(0))) {
                            setLoggingEnabled(true);
                            startTimer();
                            setLoggers();
                        //} else {
                          //  setLoggingEnabled(false);
                          //  Toast.makeText(getActivity(), "Connected Device Is Not Streaming", Toast.LENGTH_LONG).show();
                        //}
                    //} else {
                      //  setLoggingEnabled(false);
                      //  Toast.makeText(getActivity(), "No Device Connected", Toast.LENGTH_LONG).show();
                    //}
                } else {
                    setLoggingEnabled(false);
                    stopTimer();
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
        Logger logger = new Logger(filename + "_" + i, ",", "DataCollector");
        loggers.put(SCALE, logger);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (loggingEnabled) {
            buttonStartLogging.setText(getActivity().getString(R.string.stop_logging));
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
        connectedShimmers = ControlFragment.connectedShimmerAddresses;
        streamingShimmers = ControlFragment.streamingShimmerAddresses;
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
            buttonStartLogging.setText(getActivity().getString(R.string.stop_logging));
            buttonStartLogging.setBackgroundColor(Color.RED);
        } else {
            buttonStartLogging.setText(getActivity().getString(R.string.start_logging));
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

    private void startTimer() {
        timerHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch(msg.what) {
                    case TIMER_UPDATE:
                        if(textViewTimer.getVisibility() == View.INVISIBLE) {
                            textViewTimer.setVisibility(View.VISIBLE);
                            textViewTimer.requestLayout();
                        }
                        int minutes = msg.arg1 / 1000 / 60;
                        int seconds = msg.arg1 / 1000 % 60;
                        String time = String.format("%02d:%02d", minutes, seconds);
                        textViewTimer.setText(time);
                        break;

                    case TIMER_END:
                        textViewTimer.setVisibility(View.INVISIBLE);
                        showLikertScaleDialog();
                        break;
                }
            }
        };

        long timerInterval = (long) (1000 * 60 * TIMER_CYCLE_IN_MIN);
        final long endTime = System.currentTimeMillis() + timerInterval;

        timerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                timerShouldContinue = true;
                while(now < endTime && timerShouldContinue) {
                    Message message = new Message();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    message.what = TIMER_UPDATE;
                    message.arg1 = (int)(endTime - now);
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

    private void stopTimer() {
        if(timerThread.isAlive()) {
            timerShouldContinue = false;
            textViewTimer.setVisibility(View.INVISIBLE);
            timerThread.interrupt();
        }
        timerThread = null;
    }

    private void showLikertScaleDialog() {
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.flow_short_scale);
        dialog.setTitle(getActivity().getString(R.string.feedback));
        dialog.setCancelable(false);

        Button saveButton = (Button) dialog.findViewById(R.id.button_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                saveItems(dialog, SCALE, SCALE_ITEM_COUNT);
                dialog.dismiss();
                if(loggingEnabled) {
                    startTimer();
                }
            }
        });
        dialog.show();
    }

    private void saveItems(final Dialog dialog, String scale, int items) {

        ObjectCluster objectCluster = new ObjectCluster(scale, scale);
        objectCluster.mPropertyCluster.put("Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
        for(int i = 1; i <= items; i++) {
            int identifier = getResources().getIdentifier("q" + i, "id", getActivity().getPackageName());
            if(identifier != 0) {
                RatingBar ratingBar = (RatingBar)dialog.findViewById(identifier);
                objectCluster.mPropertyCluster.put("Item " + String.format("%02d", i), new FormatCluster("CAL", "n. u.", (int) ratingBar.getRating()));
            }
        }
        new logData().execute(objectCluster);
    }
}
