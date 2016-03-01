package de.bogutzky.psychophysiocollector.app.shimmer.imu;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import de.bogutzky.psychophysiocollector.app.GraphView;
import de.bogutzky.psychophysiocollector.app.R;
import de.bogutzky.psychophysiocollector.app.Utils;
import de.bogutzky.psychophysiocollector.app.data.management.WriteDataTask;
import de.bogutzky.psychophysiocollector.app.data.management.WriteDataTaskParams;

@SuppressLint("HandlerLeak")
public class ShimmerImuHandler extends Handler {

    private static final String TAG = "ShimmerImuHandler";
    private Activity activity;
    private File root;
    private String filename;
    private Vibrator vibrator;
    private int batchRowCount = 0;
    private int maxBatchCount;
    private Double[][] buffer;
    private Double[][] buffer0;
    private Double[][] buffer1;
    private String[] fields;
    private long[] vibratorPatternConnectionLost = {0, 100, 100, 100, 100, 100, 100, 100};
    private long startTimestamp;
    private long timeDifference;
    private Double imuStartTimestamp;
    private boolean isFirstDataRow = true;
    private GraphView graphView;

    public ShimmerImuHandler(Activity activity, String filename, int maxBatchCount) {
        this.activity = activity;
        this.filename = filename;
        this.vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        this.maxBatchCount = maxBatchCount;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public void setGraphView(GraphView graphView) {
        this.graphView = graphView;
    }

    public void writeHeader(String[] header) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));
            String outputString = "";
            if(activity instanceof ShimmerImuHandlerInterface) {
                outputString += ((ShimmerImuHandlerInterface) activity).getHeaderComments();
            }
            for (int i = 0; i < header.length; i++) {
                if (header.length - 1 != i) {
                    outputString += header[i] + ",";
                } else {
                    outputString += header[i] + "";
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
        this.fields = fields;
        this.buffer0 = new Double[maxBatchCount][fields.length];
        this.buffer1 = new Double[maxBatchCount][fields.length];
        this.buffer = buffer0;
    }

    public void setDirectoryName(String directoryName) {
        if(activity instanceof ShimmerImuHandlerInterface) {
            this.root = ((ShimmerImuHandlerInterface) activity).getStorageDirectory(directoryName);
        } else {
            this.root = null;
        }
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
                    for (int i = 0; i < fields.length; i++) {
                        Collection<FormatCluster> clusterCollection = objectCluster.mPropertyCluster.get(fields[i]);
                        if (i < fields.length) {
                            if (!clusterCollection.isEmpty()) {
                                FormatCluster formatCluster = ObjectCluster.returnFormatCluster(clusterCollection, "CAL");
                                this.buffer[batchRowCount][i] = formatCluster.mData;
                            }
                        }
                    }
                    if(this.isFirstDataRow) {
                        // Time difference between start the evaluation and here
                        this.timeDifference = System.currentTimeMillis() - this.startTimestamp;
                        this.imuStartTimestamp = this.buffer[batchRowCount][0];
                        Log.d(TAG, "Time difference: " + timeDifference + " ms");
                        Log.d(TAG, "Start timestamp: " + Utils.getDateString(this.startTimestamp, "dd/MM/yyyy hh:mm:ss.SSS"));
                        Log.d(TAG, "Shimmer IMU start timestamp: " + Utils.getDateString(this.imuStartTimestamp.longValue(), "dd/MM/yyyy hh:mm:ss.SSS"));
                        this.isFirstDataRow = false;
                    }

                    this.buffer[batchRowCount][0] = (this.buffer[batchRowCount][0] - this.imuStartTimestamp) + this.timeDifference;

                    if(graphView != null)
                        graphView.setDataWithAdjustment(buffer[batchRowCount], filename, "i8");

                    batchRowCount++;
                    if (batchRowCount == maxBatchCount) {
                        writeValues(null); // "# BatchRowCount: " + batchRowCount
                        batchRowCount = 0;
                        if (this.buffer == this.buffer0) {
                            this.buffer = this.buffer1;
                        } else {
                            this.buffer = this.buffer0;
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
                        if(activity instanceof ShimmerImuHandlerInterface) {
                            ((ShimmerImuHandlerInterface) activity).connectionResetted();
                        }
                        break;
                    case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                        Log.d(TAG, "Fully initialized: " + bluetoothAddress);
                        String btRadioID = bluetoothAddress.replace(":", "").substring(8).toUpperCase();
                        Toast.makeText(activity, btRadioID + " " + activity.getString(R.string.is_ready), Toast.LENGTH_LONG).show();
                        break;
                    case Shimmer.MSG_STATE_STREAMING:
                        Log.d(TAG, "Streaming: " + bluetoothAddress);
                        break;
                    case Shimmer.MSG_STATE_STOP_STREAMING:
                        Log.d(TAG, "Stop streaming: " + bluetoothAddress);
                        String footerComments = null;
                        if(activity instanceof ShimmerImuHandlerInterface) {
                            footerComments = ((ShimmerImuHandlerInterface) activity).getFooterComments();
                        }
                        writeValues(footerComments);
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

    private void writeValues(String batchComments) {
        new WriteDataTask().execute(new WriteDataTaskParams(this.root, this.filename, this.buffer, this.fields.length, this.batchRowCount, batchComments));
    }
}
