//The PPG to HR calculation is only limited to 51.2/102.4 Hz, reason is because a higher sampling rate might be too expensive computationally for it to be on the main thread 

/*
 * Rev 0.1
 */

package com.shimmerresearch.multishimmertemplate;


import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.shimmerresearch.adapters.ExpandableListViewAdapter;
import com.shimmerresearch.algorithms.ShimmerPPG.PpgSignalProcessing;
import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.database.ShimmerConfiguration;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.service.MultiShimmerTemplateService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import de.bogutzky.datacollector.app.R;
import pl.flex_it.androidplot.XYSeriesShimmer;

/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link ItemListActivity} in two-pane mode (on tablets) or a
 * {@link ItemDetailActivity} on handsets.
 * Converts the PPG signal of first connected shimmer with GSR and Int ADC A13 enabled
 * into a heart rate. Internal exp power can be enabled from this fragment when shimmer
 * is not streaming. Works best for sampling frequencies of 51.2Hz, 102.4Hz.
 */
public class PPGFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    final static Integer X_AXIS_LENGTH = 500;
    public static TextView textViewHeartRateOutput;
    public static HashMap<String, XYSeriesShimmer> mPlotSeriesMapPPG = new HashMap<String, XYSeriesShimmer>(100);
    public static HashMap<String, LineAndPointFormatter> mPlotFormatMapPPG = new HashMap<String, LineAndPointFormatter>(100);
    public static HashMap<String, List<Number>> mPlotDataMapPPG = new HashMap<String, List<Number>>(1);
    public static HashMap<String, XYSeriesShimmer> mPlotSeriesMapHR = new HashMap<String, XYSeriesShimmer>(100);
    public static HashMap<String, LineAndPointFormatter> mPlotFormatMapHR = new HashMap<String, LineAndPointFormatter>(100);
    public static HashMap<String, List<Number>> mPlotDataMapHR = new HashMap<String, List<Number>>(1);
    static String mSensorView = "";
    static Dialog dialog;
    static LineAndPointFormatter lineAndPointFormatter;
    private static Shimmer shimmerPPG;
    private static CheckBox checkboxIntExpPower;
    private static XYPlot ppgPlot;
    private static XYPlot heartRatePlot;
    private static int mNumberOfBeatsToAverage;
    private static PpgSignalProcessing mHeartRateCalculation;
    private static Button btnNumberOfBeatsToAverage;
    private static Boolean mNewPPGSignalProcessingActivity = true;
    private static String mSeriesNamePPG = "Internal ADC A13";
    private static String mSeriesNameHR = "Heart Rate";
    private static Integer INVALID_OUTPUT = -1;
    private static Boolean firstTimeToast = true;
    private static int mCount = 0;
    private static int mRefreshLimit = 10;
    private static Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Shimmer.MESSAGE_TOAST:
                    Log.d("toast", msg.getData().getString(Shimmer.TOAST));

                case Shimmer.MESSAGE_READ:
                    checkboxIntExpPower.setEnabled(false);
                    ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                    if (shimmerPPG != null) {
                        mRefreshLimit = (int) shimmerPPG.getSamplingRate() / 40;
                        if (mRefreshLimit < 1) {
                            mRefreshLimit = 1;
                        }

                        if (mNewPPGSignalProcessingActivity) {
                            mHeartRateCalculation = new PpgSignalProcessing(shimmerPPG.getSamplingRate(), mNumberOfBeatsToAverage);
                            mNewPPGSignalProcessingActivity = false;
                            textViewHeartRateOutput.setText("");
                        }

                        if (objectCluster.mBluetoothAddress.equals(shimmerPPG.getBluetoothAddress()) && shimmerPPG.getInternalExpPower() == 1 && shimmerPPG.getSamplingRate() < 104) {
                            int dataArrayPPG = 0;
                            Collection<FormatCluster> formatCluster = objectCluster.mPropertyCluster.get("Internal ADC A13");
                            FormatCluster cal = ((FormatCluster) ObjectCluster.returnFormatCluster(formatCluster, "CAL"));
                            if (cal != null) {
                                dataArrayPPG = (int) ((FormatCluster) ObjectCluster.returnFormatCluster(formatCluster, "CAL")).mData;
                            }

                            double heartRate = mHeartRateCalculation.ppgToHrConversion(dataArrayPPG);
                            //double heartRate = mHeartRateCalculation.getHeartRate();

                            if (heartRate == INVALID_OUTPUT) {
                                heartRate = Double.NaN;
                                textViewHeartRateOutput.setText(Double.toString(heartRate));
                            } else {
                                textViewHeartRateOutput.setText(Long.toString(Math.round(heartRate)) + " beats per minute");
                            }

                            //PPG plot
                            mPlotFormatMapPPG.put(mSeriesNamePPG, new LineAndPointFormatter(Color.BLUE, null, null));
                            List<Number> dataPPG;
                            if (mPlotDataMapPPG.get(mSeriesNamePPG) != null) {
                                dataPPG = mPlotDataMapPPG.get(mSeriesNamePPG);
                            } else {
                                dataPPG = new ArrayList<Number>();
                            }
                            if (dataPPG.size() > X_AXIS_LENGTH) {
                                dataPPG.clear();
                            }
                            dataPPG.add(dataArrayPPG);
                            mPlotDataMapPPG.put(mSeriesNamePPG, dataPPG);

                            //check if the series exists
                            LineAndPointFormatter lapfPPG;
                            if (mPlotSeriesMapPPG.get(mSeriesNamePPG) != null) {
                                mPlotSeriesMapPPG.get(mSeriesNamePPG).updateData(dataPPG);
                                lapfPPG = mPlotFormatMapPPG.get(mSeriesNamePPG);
                            } else {
                                XYSeriesShimmer seriesPPG = new XYSeriesShimmer(dataPPG, 0, mSeriesNamePPG);
                                mPlotSeriesMapPPG.put(mSeriesNamePPG, seriesPPG);
                                lapfPPG = mPlotFormatMapPPG.get(mSeriesNamePPG);
                                ppgPlot.addSeries(mPlotSeriesMapPPG.get(mSeriesNamePPG), lapfPPG);
                            }


                            //Heart rate plot
                            mPlotFormatMapHR.put(mSeriesNameHR, new LineAndPointFormatter(Color.BLUE, null, null));
                            List<Number> dataHR;
                            if (mPlotDataMapHR.get(mSeriesNameHR) != null) {
                                dataHR = mPlotDataMapHR.get(mSeriesNameHR);
                            } else {
                                dataHR = new ArrayList<Number>();
                            }
                            if (dataHR.size() > X_AXIS_LENGTH) {
                                dataHR.clear();
                            }
                            dataHR.add(heartRate);
                            mPlotDataMapHR.put(mSeriesNameHR, dataHR);

                            //check if the series exists
                            LineAndPointFormatter lapfHR;
                            if (mPlotSeriesMapHR.get(mSeriesNameHR) != null) {
                                mPlotSeriesMapHR.get(mSeriesNameHR).updateData(dataHR);
                                lapfHR = mPlotFormatMapHR.get(mSeriesNameHR);
                            } else {
                                XYSeriesShimmer seriesHR = new XYSeriesShimmer(dataHR, 0, mSeriesNameHR);
                                mPlotSeriesMapHR.put(mSeriesNameHR, seriesHR);
                                lapfHR = mPlotFormatMapHR.get(mSeriesNameHR);
                                heartRatePlot.addSeries(mPlotSeriesMapHR.get(mSeriesNameHR), lapfHR);
                            }
                        } else if (objectCluster.mBluetoothAddress.equals(shimmerPPG.getBluetoothAddress()) && shimmerPPG.getInternalExpPower() == 0) {
                            if (firstTimeToast) {
                                firstTimeToast = false;
                                //Toast.makeText(getActivity(), "Ensure that internal exp power is enabled", Toast.LENGTH_SHORT).show();
                            }
                        }

                        mCount++;
                        if (mCount % mRefreshLimit == 0) {
                            ppgPlot.redraw();
                            heartRatePlot.redraw();
                        }
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
    boolean firstTime = true;
    View rootView;
    MultiShimmerTemplateService mService;
    private TextView textViewShimmerId;
    private Boolean mGsrEnabled = false;


    /**
     * The dummy content this fragment is presenting.
     */
    private Boolean mIntAdcA13Enabled = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PPGFragment() {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        rootView = inflater.inflate(R.layout.ppg_main, container, false);

        textViewShimmerId = (TextView) rootView.findViewById(R.id.textViewShimmerId);
        textViewHeartRateOutput = (TextView) rootView.findViewById(R.id.textViewHeartRate);
        checkboxIntExpPower = (CheckBox) rootView.findViewById(R.id.checkBoxEnableExpPower);
        btnNumberOfBeatsToAverage = (Button) rootView.findViewById(R.id.buttonSetBeatsToAverage);
        ppgPlot = (XYPlot) rootView.findViewById(R.id.ppgPlot);
        heartRatePlot = (XYPlot) rootView.findViewById(R.id.heartRatePlot);
        lineAndPointFormatter = new LineAndPointFormatter(Color.rgb(0, 0, 0), null, null);
        Paint paint = lineAndPointFormatter.getLinePaint();
        paint.setStrokeWidth(1);
        lineAndPointFormatter.setLinePaint(paint);
        ppgPlot.setDomainStepMode(XYStepMode.SUBDIVIDE);
        heartRatePlot.setDomainStepMode(XYStepMode.SUBDIVIDE);

        ppgPlot.getLegendWidget().setSize(new SizeMetrics(0, SizeLayoutType.ABSOLUTE, 0, SizeLayoutType.ABSOLUTE));
        heartRatePlot.getLegendWidget().setSize(new SizeMetrics(0, SizeLayoutType.ABSOLUTE, 0, SizeLayoutType.ABSOLUTE));

        // thin out domain/range tick labels so they dont overlap each other:
        ppgPlot.setTicksPerDomainLabel(5);
        ppgPlot.setTicksPerRangeLabel(3);
        ppgPlot.disableAllMarkup();
        heartRatePlot.setTicksPerDomainLabel(5);
        heartRatePlot.setTicksPerRangeLabel(3);
        heartRatePlot.disableAllMarkup();

        // freeze the range boundaries:
        Paint gridLinePaint = new Paint();
        gridLinePaint.setColor(Color.parseColor("#D6D6D6"));
        ppgPlot.setRangeBoundaries(-100, 100, BoundaryMode.AUTO);
        ppgPlot.setDomainBoundaries(0, X_AXIS_LENGTH, BoundaryMode.FIXED);
        ppgPlot.getGraphWidget().setMargins(30, 20, 20, 20);
        ppgPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
        ppgPlot.setBorderPaint(gridLinePaint);
        ppgPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
        ppgPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        ppgPlot.getGraphWidget().setGridLinePaint(gridLinePaint);
        ppgPlot.getGraphWidget().setDomainOriginLabelPaint(gridLinePaint);
        ppgPlot.getGraphWidget().setDomainLabelPaint(gridLinePaint);
        ppgPlot.getGraphWidget().setDomainOriginLinePaint(gridLinePaint);
        ppgPlot.getGraphWidget().setRangeOriginLabelPaint(gridLinePaint);
        ppgPlot.getGraphWidget().setRangeOriginLinePaint(gridLinePaint);

        heartRatePlot.setRangeBoundaries(-100, 100, BoundaryMode.AUTO);
        heartRatePlot.setDomainBoundaries(0, X_AXIS_LENGTH, BoundaryMode.FIXED);
        heartRatePlot.getGraphWidget().setMargins(30, 20, 20, 20);
        heartRatePlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
        heartRatePlot.setBorderPaint(gridLinePaint);
        heartRatePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
        heartRatePlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        heartRatePlot.getGraphWidget().setGridLinePaint(gridLinePaint);
        heartRatePlot.getGraphWidget().setDomainOriginLabelPaint(gridLinePaint);
        heartRatePlot.getGraphWidget().setDomainLabelPaint(gridLinePaint);
        heartRatePlot.getGraphWidget().setDomainOriginLinePaint(gridLinePaint);
        heartRatePlot.getGraphWidget().setRangeOriginLabelPaint(gridLinePaint);
        heartRatePlot.getGraphWidget().setRangeOriginLinePaint(gridLinePaint);
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


    }

    public void getConnectedShimmerSettings() {
        List<ShimmerConfiguration> shimmerConfigurationList = mService.getConnectedDevices();
        deviceBluetoothAddresses = new String[shimmerConfigurationList.size()];
        mEnabledSensorNames = new String[shimmerConfigurationList.size()][Shimmer.MAX_NUMBER_OF_SIGNALS];

        int pos = 0;
        for (ShimmerConfiguration sc : shimmerConfigurationList) {
            deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
            Shimmer shimmer = mService.getShimmer(deviceBluetoothAddresses[pos]);
            mEnabledSensorNames[pos] = shimmer.getListofEnabledSensorSignals();

            mGsrEnabled = false;
            mIntAdcA13Enabled = false;
            for (int i = 0; i < mEnabledSensorNames[pos].length; i++) {
                if (mEnabledSensorNames[pos][i].equals("GSR")) {
                    mGsrEnabled = true;
                } else if (mEnabledSensorNames[pos][i].equals("Internal ADC A13")) {
                    mIntAdcA13Enabled = true;
                }
            }

            if (mGsrEnabled && mIntAdcA13Enabled) {
                shimmerPPG = shimmer;
            }
            pos++;
        }


        if (shimmerPPG != null) {
            textViewShimmerId.setText(shimmerPPG.getBluetoothAddress());
            if (shimmerPPG.getSamplingRate() > 0 && shimmerPPG.getSamplingRate() < 105) { //allow it to only work with 51.2 or 102.4
                if (shimmerPPG.getInternalExpPower() == 1) {
                    checkboxIntExpPower.setChecked(true);
                } else {
                    checkboxIntExpPower.setChecked(false);
                }
                mNewPPGSignalProcessingActivity = true;
            } else {
                Toast.makeText(getActivity(), "Ensure device is connected and correct sensor configuration and sampling rate are used, press the help button for further information.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), "Ensure device is connected and correct sensor configuration and sampling rate are used, press the help button for further information.", Toast.LENGTH_SHORT).show();
        }

    }

    public void onResume() {
        super.onResume();
        firstTime = true;

        ppgPlot.removeSeries(mPlotSeriesMapPPG.get(mSeriesNamePPG));
        mPlotSeriesMapPPG.remove(mSeriesNamePPG);
        mPlotFormatMapPPG.remove(mSeriesNamePPG);
        heartRatePlot.removeSeries(mPlotSeriesMapHR.get(mSeriesNameHR));
        mPlotSeriesMapHR.remove(mSeriesNameHR);
        mPlotFormatMapHR.remove(mSeriesNameHR);
    }

    public void onPause() {
        super.onPause();

        mPlotSeriesMapPPG.clear();
        mPlotFormatMapPPG.clear();
        mPlotDataMapPPG.clear();
        mPlotSeriesMapHR.clear();
        mPlotFormatMapHR.clear();
        mPlotDataMapHR.clear();
        ppgPlot.clear();
        heartRatePlot.clear();

    }

    @Override
    public void onStop() {
        super.onStop();
        //getActivity().unbindService(mConnection);
        //mServiceBind = false;
    }


    public void showReplaceDialog(String text) {
        dialog.setContentView(R.layout.ppg_dialog);
        dialog.setTitle("Number of beats to average");

        TextView textView = (TextView) dialog.findViewById(R.id.textViewDialog);
        textView.setText(text);
        final EditText editText = (EditText) dialog.findViewById(R.id.editTextNumberOfBeats);
        editText.setText(Integer.toString(mNumberOfBeatsToAverage));

        Button buttonDone = (Button) dialog.findViewById(R.id.buttonDone);
        buttonDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mNumberOfBeatsToAverage = Integer.parseInt(editText.getText().toString());
                mService.setNumberOfBeatsToAve(mNumberOfBeatsToAverage);
                mNewPPGSignalProcessingActivity = true;
                dialog.dismiss();
            }
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    public void setup() {
        db = mService.mDataBase;
        mService.mShimmerConfigurationList = db.getShimmerConfigurations("Temp");
        mService.setGraphHandler(mHandler, "");
        mService.enableGraphingHandler(true);
        dialog = new Dialog(getActivity());
        mNumberOfBeatsToAverage = mService.getNumberOfBeatsToAve();
        checkboxIntExpPower.setEnabled(true);
        firstTimeToast = true;
        getConnectedShimmerSettings();

        checkboxIntExpPower.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (checkboxIntExpPower.isChecked()) {
                    if (shimmerPPG != null && shimmerPPG.getStreamingStatus() == false) {
                        shimmerPPG.writeInternalExpPower(1);
                        mNewPPGSignalProcessingActivity = true;
                    } else if (shimmerPPG != null && shimmerPPG.getStreamingStatus() == true) {
                        checkboxIntExpPower.setChecked(false);
                        Toast.makeText(getActivity(), "Cannot enable internal exp power while device is stremaing", Toast.LENGTH_LONG).show();
                    } else {
                        checkboxIntExpPower.setChecked(false);
                        Toast.makeText(getActivity(), "Ensure device is connected and Internal ADC A13 and GSR sensors are enabled", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (shimmerPPG != null && shimmerPPG.getStreamingStatus() == false) {
                        shimmerPPG.writeInternalExpPower(0);
                        mNewPPGSignalProcessingActivity = false;
                    } else if (shimmerPPG != null && shimmerPPG.getStreamingStatus() == true) {
                        checkboxIntExpPower.setChecked(true);
                        Toast.makeText(getActivity(), "Cannot disable internal exp power while device is stremaing", Toast.LENGTH_LONG).show();
                    } else {
                        checkboxIntExpPower.setChecked(true);
                        Toast.makeText(getActivity(), "Ensure device is connected and Internal ADC A13 and GSR sensors are enabled", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        btnNumberOfBeatsToAverage.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                showReplaceDialog("Set Number Of Beats To Average: ");
            }
        });
    }
}
