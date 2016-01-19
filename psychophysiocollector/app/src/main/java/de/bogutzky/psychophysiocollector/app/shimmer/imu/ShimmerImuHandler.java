package de.bogutzky.psychophysiocollector.app.shimmer.imu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import de.bogutzky.psychophysiocollector.app.MainActivity;
import de.bogutzky.psychophysiocollector.app.R;
import de.bogutzky.psychophysiocollector.app.WriteDataTask;
import de.bogutzky.psychophysiocollector.app.WriteDataTaskParams;

@SuppressLint("HandlerLeak")
public class ShimmerImuHandler extends Handler {

    private static final String TAG = "ShimmerImuHandler";
    private Vibrator vibrator;
    private MainActivity mainActivity;
    private String filename;
    private File root;
    private int i = 0;
    private int maxValueCount;
    private Double[][] values;
    private Double[][] values0;
    private Double[][] values1;
    private String[] fields;
    private long[] vibratorPatternConnectionLost = {0, 100, 100, 100, 100, 100, 100, 100};
    private long startTimestamp;
    private long timeDifference;
    private Double imuStartTimestamp;
    private boolean isFirstDataRow = true;
    //float[] dataArray;

    public ShimmerImuHandler(MainActivity mainActivity, String filename, int maxValueCount) {
        this.mainActivity = mainActivity;
        this.vibrator = (Vibrator) mainActivity.getSystemService(Context.VIBRATOR_SERVICE);
        this.filename = filename;
        this.maxValueCount = maxValueCount;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public void setHeader(String[] header) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));
            String outputString = mainActivity.getLoggingHeaderString();
            outputString += "";
            for (int k = 0; k < header.length; k++) {
                if (header.length - 1 != k) {
                    outputString += header[k] + ",";
                } else {
                    outputString += header[k] + "";
                }
            }
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }

    public void setFields(String[] fields) {
        //dataArray = new float[fields.length - 1];
        this.fields = fields;
        this.values0 = new Double[maxValueCount][fields.length];
        this.values1 = new Double[maxValueCount][fields.length];
        this.values = values0;
    }

    public void setDirectoryName(String directoryName) {
        this.root = mainActivity.getStorageDir(directoryName);
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what) {
            case Shimmer.MESSAGE_READ:

                if (msg.obj instanceof ObjectCluster) {
                    ObjectCluster objectCluster = (ObjectCluster) msg.obj;

                    //int graphDataCounter = 0;
                    for (int j = 0; j < fields.length; j++) {
                        Collection<FormatCluster> clusterCollection = objectCluster.mPropertyCluster.get(fields[j]);
                        if (j < fields.length) {
                            if (!clusterCollection.isEmpty()) {
                                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(clusterCollection, "CAL");
                                this.values[i][j] = formatCluster.mData;

                                //if(graphShowing && graphAdress.equals(this.bluetoothAdress)) {
                                //if(j != 0 && j != fields.length) {
                                //dataArray[graphDataCounter] = Float.valueOf(values[i][j]);
                                //graphDataCounter++;
                                //}
                                //}
                            }
                        }
                    }
                    if(this.isFirstDataRow) {
                        // Time difference between start the evaluation and here
                        this.timeDifference = System.currentTimeMillis() - this.startTimestamp;
                        Log.d(TAG, "Time difference: " + this.timeDifference + " ms");
                        this.imuStartTimestamp = this.values[i][0];
                        Log.d(TAG, "IMU start timestamp: " + this.imuStartTimestamp + " ms");
                        this.isFirstDataRow = false;
                    }

                    this.values[i][0] = (this.values[i][0] - this.imuStartTimestamp) + this.timeDifference;

                    //values[i][0] = decimalFormat.format(Double.valueOf(values[i][0]));
                    //if(graphShowing && graphAdress.equals(this.bluetoothAdress)) {
                    //graphView.setDataWithAdjustment(dataArray, graphAdress, "i8");
                    //}
                    i++;
                    if (i == maxValueCount) {
                        i = 0;
                        writeValues(this.values);
                        if (this.values == this.values0) {
                            this.values = this.values1;
                        } else {
                            this.values = this.values0;
                        }
                    }
                }

                break;

            case Shimmer.MESSAGE_TOAST:
                Log.d(TAG, msg.getData().getString(Shimmer.TOAST));
                if ("Device connection was lost".equals(msg.getData().getString(Shimmer.TOAST))) {
                    this.vibrator.vibrate(vibratorPatternConnectionLost, -1);
                }
                break;

            case Shimmer.MESSAGE_STATE_CHANGE:
                String bluetoothAddress = "None";
                if ((msg.obj instanceof ObjectCluster)) {
                    ObjectCluster objectCluster = (ObjectCluster) msg.obj;
                    bluetoothAddress = objectCluster.mBluetoothAddress;
                }
                switch (msg.arg1) {
                    case Shimmer.STATE_CONNECTED:
                        Log.d(TAG, "Connected: " + bluetoothAddress);
                        break;
                    case Shimmer.STATE_CONNECTING:
                        Log.d(TAG, "Connecting: " + bluetoothAddress);
                        break;
                    case Shimmer.STATE_NONE:
                        Log.d(TAG, "None State: " + bluetoothAddress);
                        break;
                    case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                        Log.d(TAG, "Fully initialized: " + bluetoothAddress);
                        String btRadioID = bluetoothAddress.replace(":", "").substring(8).toUpperCase();
                        Toast.makeText(mainActivity, btRadioID + " " + mainActivity.getString(R.string.is_ready), Toast.LENGTH_LONG).show();
                        break;
                    case Shimmer.MSG_STATE_STREAMING:
                        Log.d(TAG, "Streaming: " + bluetoothAddress);
                        break;
                    case Shimmer.MSG_STATE_STOP_STREAMING:
                        Log.d(TAG, "Stop streaming: " + bluetoothAddress);
                        this.isFirstDataRow = true;
                        break;
                    case Shimmer.MESSAGE_STOP_STREAMING_COMPLETE:
                        Log.d(TAG, "Stop streaming complete:" + bluetoothAddress);
                        break;
                }
                break;

            case Shimmer.MESSAGE_PACKET_LOSS_DETECTED:
                Log.d(TAG, "Packet loss detected");
                break;
        }
    }

    public void writeValues(Double[][] values) {
        WriteDataTask task = new WriteDataTask();
        task.execute(new WriteDataTaskParams(values, this.filename, this.root, this.fields.length, false, ""));


        //if(footer)
        //writeData(values,this.filename, fields.length, true, getLoggingFooterString(), slot);
        //else
        //writeData(values,this.filename, fields.length, false, "", slot);
    }
}
