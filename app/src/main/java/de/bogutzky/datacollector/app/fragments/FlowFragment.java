package de.bogutzky.datacollector.app.fragments;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.bogutzky.datacollector.app.R;
import de.bogutzky.datacollector.app.tools.Logger;

public class FlowFragment extends Fragment implements SensorEventListener {

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
                    if (connectedShimmers.size() > 0) {
                        if (loggingEnabled) {
                            if ((msg.obj instanceof ObjectCluster)) {
                                ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                                objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
                                new LogData().execute(objectCluster);
                            }
                        }
                    } else {
                        Toast.makeText(getActivity(), "No Device Connected", Toast.LENGTH_LONG).show();
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

    private static final String ACCELEROMETER = "Internal Accelerometer";
    private static final String GYROSCOPE = "Internal Gyroscope";
    private static final String GPS = "GPS";

    // Internal sensors
    private SensorManager mSensorManager;
    private android.hardware.Sensor mAccelerometer;
    private android.hardware.Sensor mGyroscope;

    // Location manager
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // Internal sensors
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);

        // Location manager
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        startCollectingInternalSensorData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flow, container, false);

        editTextLoggingFileName = (EditText) view.findViewById(R.id.editTextLogFileName);

        textViewTimer = (TextView) view.findViewById(R.id.timer);
        buttonStartLogging = (Button) view.findViewById(R.id.buttonStartLogging);
        buttonStartLogging.setBackgroundColor(Color.GREEN);
        buttonStartLogging.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (buttonStartLogging.getText().equals(getActivity().getString(R.string.start_logging))) {
                    if (connectedShimmers.size() > 0) {
                        if (streamingShimmers.contains(connectedShimmers.get(0))) {
                            setLoggers();
                            setLoggingEnabled(true);
                            startTimer();
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

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH-mm-ss");
        String dateString = simpleDateFormat.format(new Date());
        String timeString = simpleTimeFormat.format(new Date());
        String directoryName = "DataCollector/" + dateString + "_" + timeString;
        for (String bluetoothAddress : streamingShimmers) {
            String btRadioID = bluetoothAddress.replace(":", "").substring(8).toUpperCase();
            Logger logger = new Logger("sensor_" + btRadioID, ",", directoryName);
            loggers.put(bluetoothAddress, logger);
        }
        Logger scaleLogger = new Logger("scale", ",", directoryName);
        loggers.put(SCALE, scaleLogger);
        Logger accelerometerLogger = new Logger("accelerometer", ",", directoryName);
        loggers.put(ACCELEROMETER, accelerometerLogger);
        Logger gyroscopeLogger = new Logger("gyroscope", ",", directoryName);
        loggers.put(GYROSCOPE, gyroscopeLogger);
        Logger gpsLogger = new Logger("gps", ",", directoryName);
        loggers.put(GPS, gpsLogger);
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

    @Override
    public void onDestroyView() {
        stopCollectingInternalSensorData();
        super.onDestroyView();
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

    class LogData extends AsyncTask<ObjectCluster, Integer, String> {

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
        objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
        for(int i = 1; i <= items; i++) {
            int identifier = getResources().getIdentifier("q" + i, "id", getActivity().getPackageName());
            if(identifier != 0) {
                RatingBar ratingBar = (RatingBar)dialog.findViewById(identifier);
                objectCluster.mPropertyCluster.put("Item " + String.format("%02d", i), new FormatCluster("CAL", "n. u.", (int) ratingBar.getRating()));
            }
        }
        new LogData().execute(objectCluster);
    }

    // Internal sensors
    private void startCollectingInternalSensorData() {

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        mLocationListener = new GPSListener();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }

    private void stopCollectingInternalSensorData() {
        mSensorManager.unregisterListener(this);
        mLocationManager.removeUpdates(mLocationListener);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(loggingEnabled) {
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                ObjectCluster objectCluster = new ObjectCluster(ACCELEROMETER, ACCELEROMETER);
                objectCluster.mPropertyCluster.put("Timestamp", new FormatCluster("CAL", "nSecs", event.timestamp));
                objectCluster.mPropertyCluster.put("Accelerometer X", new FormatCluster("CAL", "m/s^2", event.values[0]));
                objectCluster.mPropertyCluster.put("Accelerometer Y", new FormatCluster("CAL", "m/s^2", event.values[1]));
                objectCluster.mPropertyCluster.put("Accelerometer Z", new FormatCluster("CAL", "m/s^2", event.values[2]));
                objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
                new LogData().execute(objectCluster);
            }
            if (event.sensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE) {
                ObjectCluster objectCluster = new ObjectCluster(GYROSCOPE, GYROSCOPE);
                objectCluster.mPropertyCluster.put("Timestamp", new FormatCluster("CAL", "nSecs", event.timestamp));
                objectCluster.mPropertyCluster.put("Gyroscope X", new FormatCluster("CAL", "deg/s", event.values[0] * 180 / Math.PI));
                objectCluster.mPropertyCluster.put("Gyroscope Y", new FormatCluster("CAL", "deg/s", event.values[1] * 180 / Math.PI));
                objectCluster.mPropertyCluster.put("Gyroscope Z", new FormatCluster("CAL", "deg/s", event.values[2] * 180 / Math.PI));
                objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", System.currentTimeMillis()));
                new LogData().execute(objectCluster);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    public class GPSListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (loggingEnabled) {
                ObjectCluster objectCluster = new ObjectCluster(GPS, GPS);
                objectCluster.mPropertyCluster.put("System Timestamp", new FormatCluster("CAL", "mSecs", location.getTime()));
                objectCluster.mPropertyCluster.put("Latitude", new FormatCluster("CAL", "mSecs", location.getLatitude()));
                objectCluster.mPropertyCluster.put("Longitude", new FormatCluster("CAL", "mSecs", location.getLongitude()));
                objectCluster.mPropertyCluster.put("Altitude", new FormatCluster("CAL", "mSecs", location.getAltitude()));
                new LogData().execute(objectCluster);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(getActivity(), "GPS ist nicht verfügbar.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }
}
