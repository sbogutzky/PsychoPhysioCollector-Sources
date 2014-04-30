package com.shimmerresearch.multishimmertemplate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.shimmerresearch.adapters.ExpandableListViewAdapter;
import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.database.ShimmerConfiguration;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.multishimmertemplate.menucontent.MenuContent;
import com.shimmerresearch.service.MultiShimmerTemplateService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import de.bogutzky.datacollector.app.R;
import pl.flex_it.androidplot.XYSeriesShimmer;


/**
 * A fragment representing a single Item detail screen. This fragment is either
 * contained in a {@link ItemListActivity} in two-pane mode (on tablets) or a
 * {@link ItemDetailActivity} on handsets.
 */
public class PlotFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    final static int X_AXIS_LENGTH = 500;
    public static HashMap<String, XYSeriesShimmer> mPlotSeriesMap = new HashMap<String, XYSeriesShimmer>(100);
    public static HashMap<String, LineAndPointFormatter> mPlotFormatMap = new HashMap<String, LineAndPointFormatter>(100);
    public static HashMap<String, List<Number>> mPlotDataMap = new HashMap<String, List<Number>>(10);
    static int mCount = 1;
    static String mSensorView = "";
    static List<Number> dataList = new ArrayList<Number>();
    static List<Number> dataTimeList = new ArrayList<Number>();
    static XYSeriesShimmer series1;
    static LineAndPointFormatter lineAndPointFormatter;
    static Dialog dialog;
    private static XYPlot dynamicPlot;
    static private String[] mBluetoothAddressforPlot = new String[7];
    static private String[][] mSensorsforPlot = new String[7][Shimmer.MAX_NUMBER_OF_SIGNALS];
    static private String[][] mSensorsforPlotFormat = new String[7][Shimmer.MAX_NUMBER_OF_SIGNALS];
    private static Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Shimmer.MESSAGE_TOAST:
                    Log.d("toast", msg.getData().getString(Shimmer.TOAST));

                case Shimmer.MESSAGE_READ:
                    Log.d("ShimmerGraph", "Received");
                    if ((msg.obj instanceof ObjectCluster)) {

                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        //log data
                        Log.d("ShimmerPacket", objectCluster.mBluetoothAddress);
                        Collection<FormatCluster> ofFormatstemp = objectCluster.mPropertyCluster.get("AccelerometerX");  // first retrieve all the possible formats for the current sensor device
                        FormatCluster formatClustertemp = ((FormatCluster) ObjectCluster.returnFormatCluster(ofFormatstemp, "CAL"));
                        if (formatClustertemp != null) {
                            Log.d("ShimmerData", objectCluster.mBluetoothAddress + " : " + Double.toString(formatClustertemp.mData));
                        }


                        //first check what signals have been selected in the checkbox

                        //iterate through every bluetooth address
                        for (int i = 0; i < mBluetoothAddressforPlot.length; i++) {
                            //if it is the corresponding datapacket
                            if (!mBluetoothAddressforPlot[i].equals("")) {
                                // for every bluetooth address look through the sensors if it is the correct packet
                                if (objectCluster.mBluetoothAddress.equals(mBluetoothAddressforPlot[i])) {
                                    for (int k = 0; k < mSensorsforPlot[0].length; k++) {
                                        if (!mSensorsforPlot[i][k].equals("")) {
                                            Log.d("ShimmerPLOT", mSensorsforPlot[i][k]);
                                            Collection<FormatCluster> ofFormats = objectCluster.mPropertyCluster.get(mSensorsforPlot[i][k]);  // first retrieve all the possible formats for the current sensor device
                                            Log.d("ShimmerPLOT", mSensorsforPlotFormat[i][k]);
                                            FormatCluster formatCluster = ((FormatCluster) ObjectCluster.returnFormatCluster(ofFormats, mSensorsforPlotFormat[i][k]));
                                            if (formatCluster != null) {
                                                Log.d("ShimmerPLOT", mBluetoothAddressforPlot[i] + " : " + mSensorsforPlot[i][k]);
                                                String seriesName = mBluetoothAddressforPlot[i] + " : " + mSensorsforPlot[i][k];

                                                //first check if there is data

                                                List<Number> data;
                                                if (mPlotDataMap.get(seriesName) != null) {
                                                    data = mPlotDataMap.get(seriesName);
                                                } else {
                                                    data = new ArrayList<Number>();
                                                }
                                                if (data.size() > X_AXIS_LENGTH) {
                                                    data.clear();
                                                }
                                                data.add(formatCluster.mData);
                                                mPlotDataMap.put(seriesName, data);

                                                //next check if the series exist
                                                LineAndPointFormatter lapf;
                                                if (mPlotSeriesMap.get(seriesName) != null) {
                                                    //if the series exist get the line format
                                                    //dynamicPlot.removeSeries(mPlotSeriesMap.get(seriesName));
                                                    mPlotSeriesMap.get(seriesName).updateData(data);
                                                    lapf = mPlotFormatMap.get(seriesName);
                                                } else {
                                                    //generate a random line and point format
                                                    //lapf = new LineAndPointFormatter(Color.rgb((int) (255*Math.random()), (int) (255*Math.random()), (int) (255*Math.random())), null, null);
                                                    XYSeriesShimmer series = new XYSeriesShimmer(data, 0, seriesName);
                                                    mPlotSeriesMap.put(seriesName, series);
                                                    //mPlotFormatMap.put(seriesName, lapf);
                                                    lapf = mPlotFormatMap.get(seriesName);
                                                    dynamicPlot.addSeries(mPlotSeriesMap.get(seriesName), lapf);

                                                    //change the font color on the CheckBox
                                                }


                                            }
                                        }
                                    }
                                }
                            }
                        }
                        dynamicPlot.redraw();
                        //filter data by address and signals

                        //remove all the series

                        //create the new series

                        //plot them






					 	/*Collection<FormatCluster> ofFormats = objectCluster.mPropertyCluster.get("AccelerometerX");  // first retrieve all the possible formats for the current sensor device
                         FormatCluster formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(ofFormats,"Calibrated"));
			 	    	if (formatCluster != null) {
			 	    		//Obtain data for text view
			 	    		dataList.add(formatCluster.mData);
			 	    		dataTimeList.add(mCount);
			 	    		mCount++;
			 	    		dynamicPlot.removeSeries(series1);
			 	    		series1 = new SimpleXYSeries(dataTimeList, dataList, "Test");
			 	    		dynamicPlot.addSeries(series1, lineAndPointFormatter);
			 	    		dynamicPlot.redraw();

			 	    		//int[] dataValues={(int)formatCluster.mData,0,0};
			 	    		//mGraphView.setDataWithAdjustment(dataValues,"Shimmer : " + deviceName,"");
			 	    		if (mCount%100==0){
			 	    			dynamicPlot.setDomainBoundaries(mCount, mCount+100, BoundaryMode.FIXED);
			 	    			dataList.clear();
			 	    			dataTimeList.clear();
			 	    			dynamicPlot.clear();
			 	    		}
			 	    	}*/

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
    int tempPosition;
    View tempViewChild;
    boolean firstTime = true;
    View rootView;
    MultiShimmerTemplateService mService;
    private boolean mServiceBind = false;
    /**
     * The dummy content this fragment is presenting.
     */
    private MenuContent.MenuItem mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PlotFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            String itemString = getArguments().getString(ARG_ITEM_ID);
            //mItem = MenuContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }

        getActivity().invalidateOptionsMenu();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        //this values should be loaded from a database, but for now this will do, when you exit this fragment this list should be saved to a database
        rootView = inflater.inflate(R.layout.plot_main, container, false);

        Arrays.fill(mBluetoothAddressforPlot, "");
        for (String[] row : mSensorsforPlot) {
            Arrays.fill(row, "");
        }

        for (String[] row : mSensorsforPlotFormat) {
            Arrays.fill(row, "RAW");
        }


        // get handles to our View defined in layout.xml:
        dynamicPlot = (XYPlot) rootView.findViewById(R.id.dynamicPlot);
        // only display whole numbers in domain labels
        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));
        dataList.clear();
        dataTimeList.clear();
        lineAndPointFormatter = new LineAndPointFormatter(Color.rgb(0, 0, 0), null, null);
        Paint paint = lineAndPointFormatter.getLinePaint();
        paint.setStrokeWidth(1);
        lineAndPointFormatter.setLinePaint(paint);
        dynamicPlot.setDomainStepMode(XYStepMode.SUBDIVIDE);
        //dynamicPlot.setDomainStepValue(series1.size());

        dynamicPlot.getLegendWidget().setSize(new SizeMetrics(0, SizeLayoutType.ABSOLUTE, 0, SizeLayoutType.ABSOLUTE));

        // thin out domain/range tick labels so they dont overlap each other:
        dynamicPlot.setTicksPerDomainLabel(5);
        dynamicPlot.setTicksPerRangeLabel(3);
        dynamicPlot.disableAllMarkup();

        // freeze the range boundaries:
        Paint gridLinePaint = new Paint();
        gridLinePaint.setColor(Color.parseColor("#D6D6D6"));
        dynamicPlot.setRangeBoundaries(-100, 100, BoundaryMode.AUTO);
        dynamicPlot.setDomainBoundaries(0, X_AXIS_LENGTH, BoundaryMode.FIXED);
        dynamicPlot.getGraphWidget().setMargins(30, 20, 20, 20);
        dynamicPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
        dynamicPlot.setBorderPaint(gridLinePaint);
        dynamicPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
        dynamicPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        dynamicPlot.getGraphWidget().setGridLinePaint(gridLinePaint);
        dynamicPlot.getGraphWidget().setDomainOriginLabelPaint(gridLinePaint);
        dynamicPlot.getGraphWidget().setDomainLabelPaint(gridLinePaint);
        dynamicPlot.getGraphWidget().setDomainOriginLinePaint(gridLinePaint);
        dynamicPlot.getGraphWidget().setRangeOriginLabelPaint(gridLinePaint);
        dynamicPlot.getGraphWidget().setRangeOriginLinePaint(gridLinePaint);
        //dynamicPlot.setBackgroundColor(Color.rgb(205, 226, 244));
        Log.d("ShimmerH", "OnCreate");
        firstTime = true;

        listViewShimmers = (ExpandableListView) rootView.findViewById(R.id.expandableListViewReport);


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

    }

    /*
     public ServiceConnection mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName arg0, IBinder service) {
                // TODO Auto-generated method stub
                Log.d("Shimmer","SERRRVVVIIICE");
                Log.d("ShimmerService", "service connected from main activity");
                  com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder binder = (com.shimmerresearch.service.MultiShimmerTemplateService.LocalBinder) service;
                  mService = binder.getService();
                  mServiceBind = true;

            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                // TODO Auto-generated method stub
                mServiceBind=false;
            }
        };
*/
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
        //save configuration settings
        db.saveShimmerConfigurations("Temp", mService.mShimmerConfigurationList);

        //query service get the deviceNames and Bluetooth addresses which are streaming
        shimmerConfigurationList = mService.getStreamingDevices();
        deviceNames = new String[shimmerConfigurationList.size()]; //+1 to include All Devices
        deviceBluetoothAddresses = new String[shimmerConfigurationList.size()]; //+1 to include All Devices
        mEnabledSensorNames = new String[shimmerConfigurationList.size()][Shimmer.MAX_NUMBER_OF_SIGNALS];// up to 9 (eg accel x, accel y , accel z, gyro...,mag...,ExpB0,ExpB7
        numberofChilds = new int[shimmerConfigurationList.size()];

        int pos = 0;
        for (ShimmerConfiguration sc : shimmerConfigurationList) {
            deviceNames[pos] = sc.getDeviceName();
            deviceBluetoothAddresses[pos] = sc.getBluetoothAddress();
            Shimmer shimmer = mService.getShimmer(deviceBluetoothAddresses[pos]);
            mEnabledSensorNames[pos] = shimmer.getListofEnabledSensorSignals();
            numberofChilds[pos] = getNumberofChildren(sc.getEnabledSensors(), sc.getBluetoothAddress());
            pos++;
        }

        boolean[] temp = mService.getExapandableStates(this.getClass().getSimpleName());
        if (temp != null) {
            if (temp.length != deviceNames.length) {
                mService.removeExapandableStates("PlotActivity");
                mService.storeGroupChildColor(null);
                mService.removePlotSelectedSignals();
                mService.removePlotSelectedSignalsFormat();
            }
        }


        mAdapter = new ExpandableListViewAdapter("PlotActivity", deviceNames, mEnabledSensorNames, getActivity(), listViewShimmers, numberofChilds, deviceBluetoothAddresses, mService, mService.getExapandableStates(this.getClass().getSimpleName()), mService.getPlotSelectedSignals(), mService.getGroupChildColor(), mService.getPlotSelectedSignalsFormat());
        listViewShimmers = (ExpandableListView) rootView.findViewById(R.id.expandableListViewReport);
        listViewShimmers.setAdapter(mAdapter);
        listViewShimmers.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    }

    public int getNumberofChildren(int enabledSensors, String bluetoothAddress) {
        int count = 1; //timestamp
        int shimmerVersion = mService.getShimmerVersion(bluetoothAddress);
        if (shimmerVersion == Shimmer.SHIMMER_SR30 || shimmerVersion == Shimmer.SHIMMER_3) {
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_ACCEL) > 0) {
                count = count + 3;
            }
            if (((enabledSensors & 0xFFFF) & Shimmer.SENSOR_DACCEL) > 0) {
                count = count + 3;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_GYRO) > 0) {
                count = count + 3;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_MAG) > 0) {
                count = count + 3;
            }
            if (((enabledSensors & 0xFFFF) & Shimmer.SENSOR_BATT) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFFFFFF) & Shimmer.SENSOR_EXT_ADC_A15) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFFFFFF) & Shimmer.SENSOR_EXT_ADC_A7) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFFFFFF) & Shimmer.SENSOR_EXT_ADC_A6) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFFFFFF) & Shimmer.SENSOR_INT_ADC_A1) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFFFFFF) & Shimmer.SENSOR_INT_ADC_A12) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFFFFFF) & Shimmer.SENSOR_INT_ADC_A13) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFFFFFF) & Shimmer.SENSOR_INT_ADC_A14) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFFFFFF) & Shimmer.SENSOR_GSR) > 0) {
                count = count + 1;
            }
            if ((enabledSensors & Shimmer.SENSOR_BMP180) > 0) {
                count = count + 2;
            }
            if ((enabledSensors & 0x10) > 0) {
                count = count + 3;
            }
            if ((enabledSensors & 0x08) > 0) {
                count = count + 3;
            }
            if ((enabledSensors & 0x080000) > 0) {
                count = count + 3;
            }
            if ((enabledSensors & 0x100000) > 0) {
                count = count + 3;
            }
            if ((((enabledSensors & 0xFF) & Shimmer.SENSOR_ACCEL) > 0 || (((enabledSensors & 0xFFFF) & Shimmer.SENSOR_DACCEL) > 0)) && ((enabledSensors & 0xFF) & Shimmer.SENSOR_GYRO) > 0 && ((enabledSensors & 0xFF) & Shimmer.SENSOR_MAG) > 0 && mService.is3DOrientationEnabled(bluetoothAddress)) {
                count = count + 8; //axis angle and quartenion
            }
        } else {
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_ACCEL) > 0) {
                count = count + 3;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_GYRO) > 0) {
                count = count + 3;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_MAG) > 0) {
                count = count + 3;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_GSR) > 0) {
                count = count + 1;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_ECG) > 0) {
                count = count + 2;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_EMG) > 0) {
                count++;
            }
            if (((enabledSensors & 0xFF00) & Shimmer.SENSOR_STRAIN) > 0) { //because there is strain gauge high and low add twice
                count++;
                count++;
            }
            if (((enabledSensors & 0xFF00) & Shimmer.SENSOR_HEART) > 0) {
                count++;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_EXP_BOARD_A0) > 0) {
                count++;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_EXP_BOARD_A7) > 0) {
                count++;
            }
            if (((enabledSensors & 0xFF) & Shimmer.SENSOR_ACCEL) > 0 && ((enabledSensors & 0xFF) & Shimmer.SENSOR_GYRO) > 0 && ((enabledSensors & 0xFF) & Shimmer.SENSOR_MAG) > 0 && mService.is3DOrientationEnabled(bluetoothAddress)) {
                count = count + 8; //axis angle and quartenion
            }
        }
        Shimmer shimmer = mService.getShimmer(bluetoothAddress);
        if (shimmer != null) {

        }
        return count;

    }

    public void setFilteredSignals(int groupPosition, int childPostion, String bluetoothAddress, String signalName) {

        //the hashmap entry <bluetoothaddress> : <signalname>
        if (bluetoothAddress.equals("")) {
            String seriesName = mBluetoothAddressforPlot[groupPosition] + " : " + mSensorsforPlot[groupPosition][childPostion];
            if (mPlotDataMap.get(seriesName) != null) {
                dynamicPlot.removeSeries(mPlotSeriesMap.get(seriesName));
                mPlotSeriesMap.remove(seriesName);
                mPlotFormatMap.remove(seriesName);
            }
        } else {
            //a checkbox has been selected, clear the data so it can start afresh
            mPlotDataMap.clear();
        }
        mSensorsforPlot[groupPosition][childPostion] = signalName;

        //if no sensors remove the address as well
        boolean noOtherSensor = true;
        for (int i = 0; i < mSensorsforPlot[groupPosition].length; i++) {
            if (!mSensorsforPlot[groupPosition][i].equals("")) {
                noOtherSensor = false;
            }
        }
        if (noOtherSensor) {
            mBluetoothAddressforPlot[groupPosition] = "";
        } else if (!bluetoothAddress.equals("")) {
            mBluetoothAddressforPlot[groupPosition] = bluetoothAddress;
        }
    }

    public void setSensorsforPlotFormat(int groupPosition, int childPosition, boolean calibrated) {
        if (calibrated) {
            mSensorsforPlotFormat[groupPosition][childPosition] = "CAL";
        } else {
            mSensorsforPlotFormat[groupPosition][childPosition] = "RAW";
        }

    }

    public void setPlotFormat(String bluetoothAddress, String signal, int color) {
        String seriesName = bluetoothAddress + " : " + signal;
        mPlotFormatMap.put(seriesName, new LineAndPointFormatter(color, null, null));
    }

    public void onResume() {
        super.onResume();
        firstTime = true;

        //this is needed if you switch off the screen to make sure the selected signals are plotted
        if (getActivity().getClass().getSimpleName().equals("ItemListActivity")) {
            this.mService = ((ItemListActivity) getActivity()).mService;

        } else {
            this.mService = ((ItemDetailActivity) getActivity()).mService;

        }
        if (mService != null) {
            setup();
        }
    }

    public void onPause() {
        super.onPause();
        mPlotSeriesMap.clear();
        mPlotFormatMap.clear();
        mPlotDataMap.clear();
        dynamicPlot.clear();
        mBluetoothAddressforPlot = new String[7];
        mSensorsforPlot = new String[7][Shimmer.MAX_NUMBER_OF_SIGNALS];
        Arrays.fill(mBluetoothAddressforPlot, "");
        for (String[] row : mSensorsforPlot) {
            Arrays.fill(row, "");
        }

        mService.storeGroupChildColor(mAdapter.getGroupChildColor());
        mService.storeExapandableStates("PlotActivity", mAdapter.getExpandableStates());
        mService.storePlotSelectedSignals(mAdapter.getCheckBoxStates());
        mService.storePlotSelectedSignalFormats(mAdapter.getCheckBoxFormatStates());
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
        if (firstTime) {
            updateShimmerConfigurationList(mService.mShimmerConfigurationList);
            firstTime = false;
        }
        //now connect the sensor nodes

        mService.setGraphHandler(mHandler, "");
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
