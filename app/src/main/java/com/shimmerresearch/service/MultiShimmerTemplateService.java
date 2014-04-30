package com.shimmerresearch.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.database.DatabaseHandler;
import com.shimmerresearch.database.ShimmerConfiguration;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.multishimmertemplate.menucontent.MenuContent;
import com.shimmerresearch.tools.Logging;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ArrayBlockingQueue;

import javax.vecmath.Matrix3d;

public class MultiShimmerTemplateService extends Service {
    public static final int MESSAGE_NEW_ARROW_ANGLE = 33;
    public static final int MESSAGE_CONFIGURATION_CHANGE = 34;
    private static final String TAG = "MyService";
    public static Logging shimmerLog1 = null;
    public static HashMap<String, Object> mMultiShimmer = new HashMap<String, Object>(7);
    public static HashMap<String, Logging> mLogShimmer = new HashMap<String, Logging>(7);
    public static HashMap<String, boolean[]> mExpandableStates = new HashMap<String, boolean[]>(7); // the max for bluetooth should be 7
    public static boolean mExercisePause = false;
    public static boolean mExerciseRest = false;
    public static List<String> mBluetoothAddresstoConnect = new ArrayList<String>(0);
    public static List<ShimmerConfiguration> mTempShimmerConfigurationList = new ArrayList<ShimmerConfiguration>(0);
    public static List<String> mDeviceNametoConnect = new ArrayList<String>(0);
    static boolean mNewData1 = false;
    static boolean mNewData2 = false;
    static int[] temp = {0, 0, 0};
    static boolean mExerciseStarted = false;
    static int mPatientExerciseLogID;
    static int mSetNumber = 1;
    static HashMap<String, ArrayBlockingQueue<ObjectCluster>> mFiFoMapTemp;
    static HashMap<String, ObjectCluster> mMapObjectTemp;
    private static Handler mHandlerGraph = null;
    private static boolean mGraphing = false;
    private static String mGraphBluetoothAddress = ""; //used to filter the msgs passed to the handler
    private static Matrix3d m3ds1 = new Matrix3d();
    private static Matrix3d m3ds2 = new Matrix3d();
    private static WeakReference<MultiShimmerTemplateService> mService;
    private static String mDelimiter = "\t";
    private static int MAX_NUMBER_OF_RX = 200; //not sure what the correct number should be, would be depending on the Android device and how much is the max data possibly buffered
    private static int mCountOrder = 0;
    private static boolean mRecordGesture = false;
    private final IBinder mBinder = new LocalBinder();
    public Shimmer shimmerDevice1 = null;
    public List<String> list = new ArrayList<String>(0);
    public DatabaseHandler mDataBase;
    public List<ShimmerConfiguration> mShimmerConfigurationList = new ArrayList<ShimmerConfiguration>();
    public static final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) { // handlers have a what identifier which is used to identify the type of msg
                case Shimmer.MESSAGE_READ:
                    if ((msg.obj instanceof ObjectCluster)) {    // within each msg an object can be include, objectclusters are used to represent the data structure of the shimmer device
                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                        if (mGraphing == true && (objectCluster.mBluetoothAddress.equals(mGraphBluetoothAddress) || mGraphBluetoothAddress.equals(""))) {
                            mHandlerGraph.obtainMessage(Shimmer.MESSAGE_READ, objectCluster).sendToTarget();

                        }

                    }
                    break;
                case Shimmer.MESSAGE_TOAST:
                    Log.d("toast", msg.getData().getString(Shimmer.TOAST));

                    break;
                case Shimmer.MESSAGE_STATE_CHANGE:
                    if (mHandlerGraph != null) {
                        mHandlerGraph.obtainMessage(Shimmer.MESSAGE_STATE_CHANGE, msg.arg1, -1, msg.obj).sendToTarget();
                    }
                    switch (msg.arg1) {
                        case Shimmer.STATE_CONNECTED:
                            //check to see if there are other Shimmer Devices which need to be connected
                            Log.d("Shimmer", "Connected Broadcast");
                            //sendBroadcast(intent);

                            break;
                        case Shimmer.STATE_CONNECTING:

                            break;
                        case Shimmer.STATE_NONE:
                            Log.d("Shimmer", "NO_State" + ((ObjectCluster) msg.obj).mBluetoothAddress);
                            mMultiShimmer.remove(((ObjectCluster) msg.obj).mBluetoothAddress);


                            if (mBluetoothAddresstoConnect.size() != 0) {
                                mBluetoothAddresstoConnect.remove(0);
                                mDeviceNametoConnect.remove(0);
                                if (mBluetoothAddresstoConnect.size() != 0) {
                                    MultiShimmerTemplateService service = mService.get();
                                    service.connectShimmer(mBluetoothAddresstoConnect.get(0), mDeviceNametoConnect.get(0));
                                }
                            }

                            if (mTempShimmerConfigurationList.size() != 0) {
                                mTempShimmerConfigurationList.remove(0);
                                if (mTempShimmerConfigurationList.size() != 0) {
                                    MultiShimmerTemplateService service = mService.get();
                                    service.connectandConfigureShimmer(mTempShimmerConfigurationList.get(0));
                                }
                            }

                            //sendBroadcast(intent);
                            break;
                        case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                            Log.d("Shimmer", "Fully Initialized");
                            //sendBroadcast(intent);
                            if (mBluetoothAddresstoConnect.size() != 0) {
                                mBluetoothAddresstoConnect.remove(0);
                                mDeviceNametoConnect.remove(0);
                                if (mBluetoothAddresstoConnect.size() != 0) {
                                    MultiShimmerTemplateService service = mService.get();
                                    service.connectShimmer(mBluetoothAddresstoConnect.get(0), mDeviceNametoConnect.get(0));
                                }
                            }

                            if (mTempShimmerConfigurationList.size() != 0) {
                                mTempShimmerConfigurationList.remove(0);
                                if (mTempShimmerConfigurationList.size() != 0) {
                                    MultiShimmerTemplateService service = mService.get();
                                    service.connectandConfigureShimmer(mTempShimmerConfigurationList.get(0));
                                }
                            }

                            MultiShimmerTemplateService service = mService.get();
                            for (int i = 0; i < service.mShimmerConfigurationList.size(); i++) {
                                ShimmerConfiguration sc = service.mShimmerConfigurationList.get(i);
                                if (sc.getBluetoothAddress().equals(((ObjectCluster) msg.obj).mBluetoothAddress)) {
                                    sc.setShimmerVersion(service.getShimmerVersion(((ObjectCluster) msg.obj).mBluetoothAddress));
                                    Shimmer shimmer = service.getShimmer(((ObjectCluster) msg.obj).mBluetoothAddress);
                                    if (shimmer.getShimmerVersion() == Shimmer.SHIMMER_3) {
                                        sc.setEnabledSensors(shimmer.getEnabledSensors());
                                        sc.setShimmerVersion(shimmer.getShimmerVersion());
                                        sc.setAccelRange(shimmer.getAccelRange());
                                        sc.setMagRange(shimmer.getMagRange());
                                        sc.setGyroRange(shimmer.getGyroRange());
                                        sc.setPressureResolution(shimmer.getPressureResolution());
                                        sc.setLowPowerAccelEnabled(shimmer.getLowPowerAccelEnabled());
                                        sc.setLowPowerMagEnabled(shimmer.getLowPowerMagEnabled());
                                        sc.setLowPowerGyroEnabled(shimmer.getLowPowerGyroEnabled());
                                        sc.setGSRRange(shimmer.getGSRRange());
                                        sc.setIntExpPower(shimmer.getInternalExpPower());
                                    } else {
                                        sc.setEnabledSensors(shimmer.getEnabledSensors());
                                        sc.setShimmerVersion(shimmer.getShimmerVersion());
                                        sc.setAccelRange(shimmer.getAccelRange());
                                        sc.setMagRange(shimmer.getMagRange());
                                        sc.setGSRRange(shimmer.getGSRRange());
                                        sc.setLowPowerMagEnabled(shimmer.getLowPowerMagEnabled());
                                    }
                                    service.mShimmerConfigurationList.set(i, sc);
                                    service.mDataBase.saveShimmerConfigurations("Temp", service.mShimmerConfigurationList);
                                }
                            }
                            mHandlerGraph.obtainMessage(MESSAGE_CONFIGURATION_CHANGE, 1, 1, 1).sendToTarget();
                            break;
                    }
                    break;
                case Shimmer.MESSAGE_PACKET_LOSS_DETECTED:
                    Log.d("SHIMMERPACKETRR2", "Detected");
                    mHandlerGraph.obtainMessage(Shimmer.MESSAGE_PACKET_LOSS_DETECTED, msg.obj).sendToTarget();
                    break;
            }
        }
    };
    public MenuContent mMenuContent = new MenuContent();
    public List<String> mConnectedShimmerBtAddresses = new ArrayList<String>();
    boolean[][] mPlotSelectedSignals = new boolean[7][Shimmer.MAX_NUMBER_OF_SIGNALS];
    boolean[][] mPlotSelectedSignalsFormat = new boolean[7][Shimmer.MAX_NUMBER_OF_SIGNALS];
    int[][] mGroupChildColor = new int[7][Shimmer.MAX_NUMBER_OF_SIGNALS];
    File outputFile;
    private boolean mDisablePacketLossMsgs = false;
    private int mNumberOfBeatsToAverage = 5;

    public String getDelimiter() {
        return mDelimiter;
    }

    public void setDelimiter(String mDelimiter) {
        this.mDelimiter = mDelimiter;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        //Toast.makeText(this, "My Service Created", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onCreate");
        mDataBase = new DatabaseHandler(this);
        for (int[] row : mGroupChildColor) {
            Arrays.fill(row, Color.rgb(0, 0, 0));
        }
        mService = new WeakReference<MultiShimmerTemplateService>(this);

        //store the expandable states of the listviews in the plot, configure and plot activity
        //store the checkbox state of the plot activity

    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }

    }

    public void disconnectAllDevices() {
        //Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }
        mMultiShimmer.clear();
        mLogShimmer.clear();
        mConnectedShimmerBtAddresses.clear();
    }


    @Override
    public void onStart(Intent intent, int startid) {

    }


    public void connectShimmer(String bluetoothAddress, String deviceName) {
        Log.d("Shimmer", "net Connection");
        Shimmer shimmerDevice = new Shimmer(this, mHandler, deviceName, false);
        mMultiShimmer.remove(bluetoothAddress);
        if (mMultiShimmer.get(bluetoothAddress) == null) {
            mMultiShimmer.put(bluetoothAddress, shimmerDevice);
            ((Shimmer) mMultiShimmer.get(bluetoothAddress)).connect(bluetoothAddress, "default");
            if (!(mConnectedShimmerBtAddresses.contains(bluetoothAddress))) {
                mConnectedShimmerBtAddresses.add(bluetoothAddress);
            }
        }
    }


    public void connectandConfigureShimmer(ShimmerConfiguration shimmerConfiguration) {
        Log.d("Shimmer", "net Connection");
        Shimmer shimmerDevice;
        if (shimmerConfiguration.getShimmerVersion() == 3) {
            shimmerDevice = new Shimmer(this, mHandler, shimmerConfiguration.getDeviceName(), shimmerConfiguration.getSamplingRate(), shimmerConfiguration.getAccelRange(), shimmerConfiguration.getGSRRange(), shimmerConfiguration.getEnabledSensors(), false, shimmerConfiguration.isLowPowerAccelEnabled(), shimmerConfiguration.isLowPowerGyroEnabled(), shimmerConfiguration.isLowPowerMagEnabled(), shimmerConfiguration.getGyroRange(), shimmerConfiguration.getMagRange());
        } else if (shimmerConfiguration.getShimmerVersion() == 2) {
            shimmerDevice = new Shimmer(this, mHandler, shimmerConfiguration.getDeviceName(), shimmerConfiguration.getSamplingRate(), shimmerConfiguration.getAccelRange(), shimmerConfiguration.getGSRRange(), shimmerConfiguration.getEnabledSensors(), false, shimmerConfiguration.getMagRange());
        } else {
            shimmerDevice = new Shimmer(this, mHandler, shimmerConfiguration.getDeviceName(), false);
        }
        //check to see if the device is already connected
        if (mMultiShimmer.get(shimmerConfiguration.getBluetoothAddress()) != null) {
            // is the device connected?
            if (((Shimmer) mMultiShimmer.get(shimmerConfiguration.getBluetoothAddress())).getShimmerState() == Shimmer.STATE_CONNECTED) {
                Toast.makeText(this, "Device " + shimmerConfiguration.getBluetoothAddress() + " Already Connected", Toast.LENGTH_LONG).show();
            } else {
                mMultiShimmer.remove(shimmerConfiguration.getBluetoothAddress());
            }
        }

        if (mMultiShimmer.get(shimmerConfiguration.getBluetoothAddress()) == null && !shimmerConfiguration.getBluetoothAddress().isEmpty()) {
            mMultiShimmer.put(shimmerConfiguration.getBluetoothAddress(), shimmerDevice);
            ((Shimmer) mMultiShimmer.get(shimmerConfiguration.getBluetoothAddress())).connect(shimmerConfiguration.getBluetoothAddress(), "default");
            if (!(mConnectedShimmerBtAddresses.contains(shimmerConfiguration.getBluetoothAddress()))) {
                mConnectedShimmerBtAddresses.add(shimmerConfiguration.getBluetoothAddress());
            }
            resetPlotActivity();
        }
    }

    public void connectAllShimmer(String[] bluetoothAddress, String[] deviceName) {
        Log.d("Shimmer", "net Connection");
        for (int i = 0; i < bluetoothAddress.length; i++) {
            Shimmer shimmerDevice = new Shimmer(this, mHandler, deviceName[i], false);
            mMultiShimmer.remove(bluetoothAddress[i]);
            if (mMultiShimmer.get(bluetoothAddress[i]) == null) {
                mMultiShimmer.put(bluetoothAddress[i], shimmerDevice);
                ((Shimmer) mMultiShimmer.get(bluetoothAddress[i])).connect(bluetoothAddress[i], "default");
                if (!(mConnectedShimmerBtAddresses.contains(bluetoothAddress[i]))) {
                    mConnectedShimmerBtAddresses.add(bluetoothAddress[i]);
                }
            }
        }
    }

    public void connectAllShimmerWithConfig(List<ShimmerConfiguration> shimmerConfigurationList) {
        //this has not been tested fully, furthermore parallel connections dont seem to work all the time, please use the sequential method
        Log.d("Shimmer", "net Connection");
        for (ShimmerConfiguration sc : shimmerConfigurationList) {
            connectandConfigureShimmer(sc);
            if (!(mConnectedShimmerBtAddresses.contains(sc.getBluetoothAddress()))) {
                mConnectedShimmerBtAddresses.add(sc.getBluetoothAddress());
            }
        }
    }


    public void connectAllShimmerSequentially(String[] bluetoothAddress, String[] deviceName) {
        Log.d("Shimmer", "net Connection");
        Collections.addAll(mBluetoothAddresstoConnect, bluetoothAddress);
        Collections.addAll(mDeviceNametoConnect, deviceName);
        //dont connect if it is already in the hashmap
        if (isShimmerConnected(mBluetoothAddresstoConnect.get(0)) == false) {
            connectShimmer(mBluetoothAddresstoConnect.get(0), mDeviceNametoConnect.get(0));
            if (!(mConnectedShimmerBtAddresses.contains(mBluetoothAddresstoConnect.get(0)))) {
                mConnectedShimmerBtAddresses.add(mBluetoothAddresstoConnect.get(0));
            }
        }

    }

    public void connectAllShimmerSequentiallyWithConfig(List<ShimmerConfiguration> shimmerConfigurationList) {
        Log.d("Shimmer", "net Connection");

        mTempShimmerConfigurationList = new ArrayList<ShimmerConfiguration>(shimmerConfigurationList);
        if (mTempShimmerConfigurationList.size() != 0) {
            int pos = 0;
            List<Integer> connectedLocations = new ArrayList<Integer>();
            for (ShimmerConfiguration sc : mTempShimmerConfigurationList) {
                if (isShimmerConnected(sc.getBluetoothAddress())) {
                    connectedLocations.add(pos);
                }
                pos++;
            }

            ListIterator<Integer> li = connectedLocations.listIterator(connectedLocations.size());

            // Iterate in reverse.
            while (li.hasPrevious()) {
                mTempShimmerConfigurationList.remove(li.previous().intValue());
            }

            //Collections.copy(scl, shimmerConfigurationList);
            //Collections.addAll(mBluetoothAddresstoConnect, bluetoothAddress);
            //Collections.addAll(mDeviceNametoConnect, deviceName);
            //dont connect if it is already in the hashmap
            if (mTempShimmerConfigurationList.size() != 0) {
                connectandConfigureShimmer(mTempShimmerConfigurationList.get(0));
                if (!(mConnectedShimmerBtAddresses.contains(mTempShimmerConfigurationList.get(0).getBluetoothAddress()))) {
                    mConnectedShimmerBtAddresses.add(mTempShimmerConfigurationList.get(0).getBluetoothAddress());
                }
            }
        }


    }

    public void onStop() {
        //Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            stemp.stop();
        }
    }

    public void toggleAllLEDS() {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.toggleLed();
            }
        }
    }

    public void stopStreamingAllDevices() {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getStreamingStatus() == true) {
                stemp.stopStreaming();

            }
        }
    }

    public void startStreamingAllDevices() {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getStreamingStatus() == false) { // if in connected state and the device is not streaming already
                stemp.startStreaming();
            }
        }
    }

    public void setAllSampingRate(double samplingRate) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeSamplingRate(samplingRate);
            }
        }
    }

    public void setAllAccelRange(int accelRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        if (noDevicesStreaming()) {
            while (iterator.hasNext()) {
                Shimmer stemp = (Shimmer) iterator.next();
                if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                    stemp.writeAccelRange(accelRange);
                }
            }
        } else {
            Toast.makeText(this, "In order to configure, please stop streaming", Toast.LENGTH_LONG).show();
        }
    }

    public void setAllGSRRange(int gsrRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        if (noDevicesStreaming()) {
            while (iterator.hasNext()) {
                Shimmer stemp = (Shimmer) iterator.next();
                if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                    stemp.writeGSRRange(gsrRange);
                }
            }
        } else {
            Toast.makeText(this, "In order to configure, please stop streaming", Toast.LENGTH_LONG).show();
        }

    }

    public void setAllEnabledSensors(int enabledSensors) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                stemp.writeEnabledSensors(enabledSensors);
            }
        }
    }

    public void setEnabledSensors(int enabledSensors, String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress) && stemp.getStreamingStatus() == false) {
                stemp.writeEnabledSensors(enabledSensors);
            } else if (stemp.getStreamingStatus() == true) {
                Toast.makeText(this, "In order to configure, please stop streaming", Toast.LENGTH_LONG).show();
            }
        }
        resetPlotActivity();
    }

    public void toggleLED(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.toggleLed();
            }
        }
    }

    public boolean isShimmerConnected(String bluetoothAddress) {
        boolean found = false;
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                found = true;
            }
        }
        return found;
    }

    public int getEnabledSensors(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int enabledSensors = 0;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                enabledSensors = stemp.getEnabledSensors();
            }
        }
        return enabledSensors;
    }

    public void writeSamplingRate(String bluetoothAddress, double samplingRate) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeSamplingRate(samplingRate);
            }
        }
    }

    public void writeAccelRange(String bluetoothAddress, int accelRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeAccelRange(accelRange);
            }
        }
    }

    public void writeGSRRange(String bluetoothAddress, int gsrRange) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeGSRRange(gsrRange);
            }
        }
    }

    public double getSamplingRate(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        double SRate = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                SRate = stemp.getSamplingRate();
            }
        }
        return SRate;
    }

    public int getAccelRange(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int aRange = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                aRange = stemp.getAccelRange();
            }
        }
        return aRange;
    }

    public int getShimmerState(String bluetoothAddress) {

        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int status = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                status = stemp.getShimmerState();
                Log.d("ShimmerState", Integer.toString(status));
            }
        }
        return status;

    }

    public int getGSRRange(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int gRange = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                gRange = stemp.getGSRRange();
            }
        }
        return gRange;
    }

    public void startStreaming(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress) && stemp.getStreamingStatus() == false) {
                stemp.startStreaming();
            }
        }
    }

    public void stopStreaming(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress) && stemp.getStreamingStatus() == true) {
                stemp.stopStreaming();
            }
        }
    }

    public void disconnectShimmer(String bluetoothAddress) {
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.stop();
                mMultiShimmer.remove(bluetoothAddress);
            }
        }
        mLogShimmer.remove(bluetoothAddress);
        mMultiShimmer.remove(bluetoothAddress);
        mConnectedShimmerBtAddresses.remove(bluetoothAddress);
    }

    public void disconnectShimmerNew(String bluetoothAddress) {
        if (mMultiShimmer.get(bluetoothAddress) == null) {

        } else {
            Shimmer stemp = (Shimmer) mMultiShimmer.get(bluetoothAddress);
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.stop();
            }
            mMultiShimmer.remove(bluetoothAddress);
            mLogShimmer.remove(bluetoothAddress);
            mConnectedShimmerBtAddresses.remove(bluetoothAddress);
        }

    }

    public void setGraphHandler(Handler handler, String bluetoothAddress) {
        mHandlerGraph = handler;
        mGraphBluetoothAddress = bluetoothAddress;
    }

    public void setHandler(Handler handler) {
        mHandlerGraph = handler;
    }

    public void enableGraphingHandler(boolean setting) {
        mGraphing = setting;
    }

    public boolean allShimmersDisconnected() {
        boolean allDisconnect = true;

        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        if (iterator.hasNext()) {
        }
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() != Shimmer.STATE_NONE) {
                allDisconnect = false;
            }
        }
        return allDisconnect;
    }

    public boolean allShimmerInitialized() {
        boolean allConnected = true;

        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();

            if (stemp.getShimmerState() != Shimmer.STATE_CONNECTED) {
                allConnected = false;
            }

        }
        return allConnected;
    }

    public void testService() {
        Log.d("ShimmerServiceTest", "Test Test Test");
    }

    public void removeShimmer(String bluetoothAddress) {
        mMultiShimmer.remove(bluetoothAddress);
    }

    public List<ShimmerConfiguration> getStreamingDevices() {
        // TODO Auto-generated method stub
        List<ShimmerConfiguration> mShimmerConfigurationList = new ArrayList<ShimmerConfiguration>();
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getStreamingStatus() == true) {
                mShimmerConfigurationList.add(new ShimmerConfiguration(stemp.getDeviceName(), stemp.getBluetoothAddress(), -1, stemp.getEnabledSensors(), stemp.getSamplingRate(), stemp.getAccelRange(), stemp.getGSRRange(), stemp.getShimmerVersion(), stemp.getLowPowerAccelEnabled(), stemp.getLowPowerGyroEnabled(), stemp.getLowPowerMagEnabled(), stemp.getGyroRange(), stemp.getMagRange(), stemp.getPressureResolution(), stemp.getInternalExpPower()));
            }
        }
        return mShimmerConfigurationList;
    }

    public List<ShimmerConfiguration> getConnectedDevices() {
        // TODO Auto-generated method stub
        List<ShimmerConfiguration> mShimmerConfigurationList = new ArrayList<ShimmerConfiguration>();
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED) {
                mShimmerConfigurationList.add(new ShimmerConfiguration(stemp.getDeviceName(), stemp.getBluetoothAddress(), -1, stemp.getEnabledSensors(), stemp.getSamplingRate(), stemp.getAccelRange(), stemp.getGSRRange(), stemp.getShimmerVersion(), stemp.getLowPowerAccelEnabled(), stemp.getLowPowerGyroEnabled(), stemp.getLowPowerMagEnabled(), stemp.getGyroRange(), stemp.getMagRange(), stemp.getPressureResolution(), stemp.getInternalExpPower()));
            }
        }
        return mShimmerConfigurationList;
    }

    public void writePressureResolution(String bluetoothAddress, int setting) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writePressureResolution(setting);
            }
        }
    }

    public void writeEXGSetting(String bluetoothAddress, int setting) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                if (setting == 0) {
                    stemp.enableDefaultECGConfiguration();
                } else if (setting == 1) {
                    stemp.enableDefaultEMGConfiguration();
                } else if (setting == 2) {
                    stemp.enableEXGTestSignal();

                }

            } else {

            }
        }
        resetPlotActivity();
    }

    public boolean noDevicesStreaming() {
        boolean noneStreaming = true;

        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();

            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getStreamingStatus() == true) {
                noneStreaming = false;
            }

        }
        return noneStreaming;
    }

    public void writeGyroRange(String bluetoothAddress, int range) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeGyroRange(range);
            }
        }
    }

    public void setAccelLowPower(String bluetoothAddress, int accelLowPower) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                if (accelLowPower == 1) {
                    stemp.enableLowPowerAccel(true);
                } else {
                    stemp.enableLowPowerAccel(false);
                }
            }
        }
    }

    public void setGyroLowPower(String bluetoothAddress, int lowPower) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                if (lowPower == 1) {
                    stemp.enableLowPowerGyro(true);
                } else {
                    stemp.enableLowPowerGyro(false);
                }
            }
        }
    }

    public void setMagLowPower(String bluetoothAddress, int lowPower) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                if (lowPower == 1) {
                    stemp.enableLowPowerMag(true);
                } else {
                    stemp.enableLowPowerMag(false);
                }
            }
        }
    }

    public void writeMagRange(String bluetoothAddress, int range) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeMagRange(range);
            }
        }
    }

    public boolean allDevicesStreaming() {
        boolean streaming = true;

        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();

            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getStreamingStatus() == false) {
                streaming = false;
            }

        }
        return streaming;
    }

    public boolean deviceStreaming(String address) {

        boolean streaming = false;
        if (mMultiShimmer.get(address) != null) {
            Shimmer stemp = (Shimmer) mMultiShimmer.get(address);
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getStreamingStatus() == true) {
                streaming = true;
            }
        }

        return streaming;

    }

    public void writeSamplingRateAllDevices(double samplingRate) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        if (noDevicesStreaming()) {
            while (iterator.hasNext()) {
                Shimmer stemp = (Shimmer) iterator.next();
                if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getStreamingStatus() == false) { // if in connected state and the device is not streaming already
                    stemp.writeSamplingRate(samplingRate);
                }
            }
        } else {
            Toast.makeText(this, "In order to configure, please stop streaming", Toast.LENGTH_LONG).show();
        }
    }

    public void storeGroupChildColor(int[][] groupChildColor) {
        mGroupChildColor = groupChildColor;
    }

    public int[][] getGroupChildColor() {
        return mGroupChildColor;
    }

    public void storeExapandableStates(String activityName, boolean[] expandableStates) {
        //a true in States means the listview is in an expanded state
        mExpandableStates.put(activityName, expandableStates);
    }

    public void removeExapandableStates(String activityName) {
        //a true in States means the listview is in an expanded state
        mExpandableStates.remove(activityName);
    }

    public void storePlotSelectedSignals(boolean[][] selectedSignals) {
        //a true in States means the listview is in an expanded state
        mPlotSelectedSignals = selectedSignals;
    }

    public boolean[][] getPlotSelectedSignals() {
        //a true in States means the listview is in an expanded state
        return mPlotSelectedSignals;
    }

    public boolean[] getExapandableStates(String activityName) {
        //a true in States means the listview is in an expanded state
        return mExpandableStates.get(activityName);
    }

    public void removePlotSelectedSignals() {
        // TODO Auto-generated method stub
        mPlotSelectedSignals = null;
    }

    public void removePlotSelectedSignalsFormat() {
        // TODO Auto-generated method stub
        mPlotSelectedSignalsFormat = null;
    }

    public void storePlotSelectedSignalFormats(boolean[][] checkBoxFormatStates) {
        // TODO Auto-generated method stub
        mPlotSelectedSignalsFormat = checkBoxFormatStates;
    }

    public boolean[][] getPlotSelectedSignalsFormat() {
        //a true in States means the listview is in an expanded state
        return mPlotSelectedSignalsFormat;
    }

    public File getoutputFile() {
        return outputFile;
    }

    public void resetExpStates() {
        mMultiShimmer = new HashMap<String, Object>(7);
        mLogShimmer = new HashMap<String, Logging>(7);
        mExpandableStates = new HashMap<String, boolean[]>(7); // the max for bluetooth should be 7
    }

    public boolean isPacketLossMsgDisabled() {
        return mDisablePacketLossMsgs;
    }

    public void setPacketLossMsgDisabled(boolean value) {
        mDisablePacketLossMsgs = value;
    }

    public double getPacketReceptionRate(String address) {
        double prr = 100;
        if (mMultiShimmer.get(address) != null) {
            Shimmer stemp = (Shimmer) mMultiShimmer.get(address);
            prr = stemp.getPacketReceptionRate();
        }

        return prr;
    }

    public Shimmer getShimmer(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Shimmer shimmer = null;
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                return stemp;
            }
        }
        return shimmer;
    }

    public int getShimmerVersion(String bluetoothAddress) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        int version = -1;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                version = stemp.getShimmerVersion();
            }
        }
        return version;
    }

    /**
     * @param enabledSensors This takes in the current list of enabled sensors
     * @param sensorToCheck  This takes in a single sensor which is to be enabled
     * @return enabledSensors This returns the new set of enabled sensors, where any sensors which conflicts with sensorToCheck is disabled on the bitmap, so sensorToCheck can be accomodated (e.g. for Shimmer2 using ECG will disable EMG,GSR,..basically any daughter board)
     */
    public int sensorConflictCheckandCorrection(int enabledSensors, int sensorToCheck, int shimmerVersion) {

        if (shimmerVersion == Shimmer.SHIMMER_2R || shimmerVersion == Shimmer.SHIMMER_2) {
            if ((sensorToCheck & Shimmer.SENSOR_GYRO) > 0 || (sensorToCheck & Shimmer.SENSOR_MAG) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_ECG);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EMG);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_GSR);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_STRAIN);
            } else if ((sensorToCheck & Shimmer.SENSOR_STRAIN) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_ECG);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EMG);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_GSR);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_GYRO);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_MAG);
            } else if ((sensorToCheck & Shimmer.SENSOR_GSR) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_ECG);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EMG);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_STRAIN);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_GYRO);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_MAG);
            } else if ((sensorToCheck & Shimmer.SENSOR_ECG) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_GSR);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EMG);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_STRAIN);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_GYRO);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_MAG);
            } else if ((sensorToCheck & Shimmer.SENSOR_EMG) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_GSR);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_ECG);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_STRAIN);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_GYRO);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_MAG);
            } else if ((sensorToCheck & Shimmer.SENSOR_HEART) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EXP_BOARD_A0);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EXP_BOARD_A7);
            } else if ((sensorToCheck & Shimmer.SENSOR_EXP_BOARD_A0) > 0 || (sensorToCheck & Shimmer.SENSOR_EXP_BOARD_A7) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_HEART);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_BATT);
            } else if ((sensorToCheck & Shimmer.SENSOR_BATT) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EXP_BOARD_A0);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EXP_BOARD_A7);
            }
        } else {
            if ((sensorToCheck & Shimmer.SENSOR_EXG1_24BIT) > 0 || (sensorToCheck & Shimmer.SENSOR_EXG2_24BIT) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EXG1_16BIT);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EXG2_16BIT);
            }
            if ((sensorToCheck & Shimmer.SENSOR_EXG1_16BIT) > 0 || (sensorToCheck & Shimmer.SENSOR_EXG2_16BIT) > 0) {
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EXG1_24BIT);
                enabledSensors = disableBit(enabledSensors, Shimmer.SENSOR_EXG2_24BIT);
            }
        }
        enabledSensors = enabledSensors ^ sensorToCheck;
        return enabledSensors;
    }

    private int disableBit(int number, int disablebitvalue) {
        if ((number & disablebitvalue) > 0) {
            number = number ^ disablebitvalue;
        }
        return number;
    }

    public boolean is3DOrientationEnabled(String address) {
        boolean ans = false;
        if (mMultiShimmer.get(address) != null) {
            Shimmer stemp = (Shimmer) mMultiShimmer.get(address);
            ans = stemp.is3DOrientatioEnabled();
        }
        return ans;
    }

    public boolean isShimmerInitialized(String bluetoothAddress) {

        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        boolean initialized = false;
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                initialized = stemp.getInitialized();

            }
        }
        return initialized;

    }

    public void resetPlotActivity() {
        removePlotGroupChildColor();
        removeExapandableStates("PlotActivity");
        removePlotSelectedSignals();
        removePlotSelectedSignalsFormat();

    }

    public void removePlotGroupChildColor() {
        mGroupChildColor = null;
    }

    public int getNumberOfBeatsToAve() {
        return mNumberOfBeatsToAverage;
    }

    public void setNumberOfBeatsToAve(int numberOfBeats) {
        mNumberOfBeatsToAverage = numberOfBeats;
    }

    public void writeIntExpPower(String bluetoothAddress, int setting) {
        // TODO Auto-generated method stub
        Collection<Object> colS = mMultiShimmer.values();
        Iterator<Object> iterator = colS.iterator();
        while (iterator.hasNext()) {
            Shimmer stemp = (Shimmer) iterator.next();
            if (stemp.getShimmerState() == Shimmer.STATE_CONNECTED && stemp.getBluetoothAddress().equals(bluetoothAddress)) {
                stemp.writeInternalExpPower(setting);
            }
        }
    }

    public class LocalBinder extends Binder {
        public MultiShimmerTemplateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MultiShimmerTemplateService.this;
        }
    }
}
