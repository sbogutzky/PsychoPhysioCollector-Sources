package de.bogutzky.psychophysiocollector.app.bioharness;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.bogutzky.psychophysiocollector.app.GraphView;
import de.bogutzky.psychophysiocollector.app.R;
import de.bogutzky.psychophysiocollector.app.Utils;
import de.bogutzky.psychophysiocollector.app.data.management.WriteDataTask;
import de.bogutzky.psychophysiocollector.app.data.management.WriteDataTaskParams;

public class BioHarnessHandler extends Handler {
    private static final String TAG = "BioHarnessHandler";

    private Activity activity;
    private File root;
    private int[] batchRowCounts = {0, 0};
    private int[] maxBatchCounts;
    private Double[][][] buffer;
    private Double[][][] buffer0;
    private Double[][][] buffer1;

    private long startTimestamp;
    private Double[] incrementedTimestamp;
    private Double lastValidEcgVoltage;
    private boolean[] isFirstDataRow = {true, true};
    private boolean isLogging = false;
    private GraphView graphView;

    public BioHarnessHandler(Activity activity, int[] maxBatchCounts) {
        this.activity = activity;
        this.maxBatchCounts = maxBatchCounts;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public void writeHeader(String filename, String[] header) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, filename), true));
            String outputString = "";
            if(activity instanceof BioHarnessHandlerInterface) {
                outputString += ((BioHarnessHandlerInterface) activity).getHeaderComments();
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

    public void setDirectoryName(String directoryName) {
        if(activity instanceof BioHarnessHandlerInterface) {
            this.root = ((BioHarnessHandlerInterface) activity).getStorageDirectory(directoryName);
        } else {
            this.root = null;
        }
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public void startStreaming() {
        this.buffer0 = new Double[2][][];
        this.buffer1 = new Double[2][][];
        this.buffer = new Double[2][][];

        this.buffer0[BioHarnessConstants.RtoR_STORE_ID] = new Double[this.maxBatchCounts[BioHarnessConstants.RtoR_STORE_ID]][2];
        this.buffer1[BioHarnessConstants.RtoR_STORE_ID] = new Double[this.maxBatchCounts[BioHarnessConstants.RtoR_STORE_ID]][2];
        this.buffer[BioHarnessConstants.RtoR_STORE_ID] = buffer0[BioHarnessConstants.RtoR_STORE_ID];

        this.buffer0[BioHarnessConstants.ECG_STORE_ID] = new Double[this.maxBatchCounts[BioHarnessConstants.ECG_STORE_ID]][2];
        this.buffer1[BioHarnessConstants.ECG_STORE_ID] = new Double[this.maxBatchCounts[BioHarnessConstants.ECG_STORE_ID]][2];
        this.buffer[BioHarnessConstants.ECG_STORE_ID] = buffer0[BioHarnessConstants.ECG_STORE_ID];
        this.isLogging = true;

        this.incrementedTimestamp = new Double[2];
    }

    public void stopStreaming() {
        this.isLogging = false;

        String footerComments = null;
        if(activity instanceof BioHarnessHandlerInterface) {
            footerComments = ((BioHarnessHandlerInterface) activity).getFooterComments();
        }
        writeValues(activity.getString(R.string.file_name_rr_interval), this.buffer[BioHarnessConstants.RtoR_STORE_ID], 2, batchRowCounts[BioHarnessConstants.RtoR_STORE_ID], footerComments);
        writeValues(activity.getString(R.string.file_name_ecg), this.buffer[BioHarnessConstants.ECG_STORE_ID], 2, batchRowCounts[BioHarnessConstants.ECG_STORE_ID], footerComments);

        this.isFirstDataRow = new boolean[]{true, true, true};
    }

    public void handleMessage(Message msg) {

        switch (msg.what) {
            case BioHarnessConstants.BH_READY:
                Toast.makeText(activity, "BioHarness " + activity.getString(R.string.is_ready), Toast.LENGTH_LONG).show();
                break;

            case BioHarnessConstants.RtoR_MSG_ID:
                if(isLogging) {
                    long timestamp = msg.getData().getLong("Timestamp");
                    int rrInterval = msg.getData().getInt("rrInterval");

                    this.buffer[BioHarnessConstants.RtoR_STORE_ID][batchRowCounts[BioHarnessConstants.RtoR_STORE_ID]][1] = rrInterval / 1.0;

                    if (this.isFirstDataRow[BioHarnessConstants.RtoR_STORE_ID]) {

                        // Time difference between start the evaluation and here
                        Double bioHarnessStartTimestamp = timestamp / 1.0;
                        //Double timeDifference = bioHarnessStartTimestamp - this.startTimestamp;
                        Double timeDifference = (System.currentTimeMillis() - rrInterval / 1000.0) - this.startTimestamp / 1.0;
                        this.incrementedTimestamp[BioHarnessConstants.RtoR_STORE_ID] = timeDifference;

                        Log.d(TAG, "Time difference: " + timeDifference + " ms");
                        Log.d(TAG, "Start timestamp: " + Utils.getDateString(this.startTimestamp, "dd/MM/yyyy hh:mm:ss.SSS"));
                        Log.d(TAG, "First data row timestamp: " + Utils.getDateString(System.currentTimeMillis(), "dd/MM/yyyy hh:mm:ss.SSS"));
                        Log.d(TAG, "BioHarness start timestamp: " + Utils.getDateString(bioHarnessStartTimestamp.longValue(), "dd/MM/yyyy hh:mm:ss.SSS"));
                        writeHeader(activity.getString(R.string.file_name_rr_interval), new String[] {activity.getString(R.string.file_header_timestamp), activity.getString(R.string.file_header_rr_interval)});
                        this.isFirstDataRow[BioHarnessConstants.RtoR_STORE_ID] = false;
                    }

                    this.buffer[BioHarnessConstants.RtoR_STORE_ID][batchRowCounts[BioHarnessConstants.RtoR_STORE_ID]][0] = this.incrementedTimestamp[BioHarnessConstants.RtoR_STORE_ID];
                    this.incrementedTimestamp[BioHarnessConstants.RtoR_STORE_ID] += this.buffer[BioHarnessConstants.RtoR_STORE_ID][batchRowCounts[BioHarnessConstants.RtoR_STORE_ID]][1];
                    this.buffer[BioHarnessConstants.RtoR_STORE_ID][batchRowCounts[BioHarnessConstants.RtoR_STORE_ID]][1] /= 1000.0;

                    //Log.d(TAG, "Timestamp: " + this.buffer[BioHarnessConstants.RtoR_STORE_ID][batchRowCounts[BioHarnessConstants.RtoR_STORE_ID]][0]  + " ms / RR-Interval: " + this.buffer[BioHarnessConstants.RtoR_STORE_ID][batchRowCounts[BioHarnessConstants.RtoR_STORE_ID]][1] + " s");

                    batchRowCounts[BioHarnessConstants.RtoR_STORE_ID]++;
                    if (batchRowCounts[BioHarnessConstants.RtoR_STORE_ID] == maxBatchCounts[BioHarnessConstants.RtoR_STORE_ID]) {
                        writeValues(activity.getString(R.string.file_name_rr_interval), this.buffer[BioHarnessConstants.RtoR_STORE_ID], 2, batchRowCounts[BioHarnessConstants.RtoR_STORE_ID], null); // "# BatchRowCount: " + batchRowCounts
                        batchRowCounts[BioHarnessConstants.RtoR_STORE_ID] = 0;
                        if (this.buffer[BioHarnessConstants.RtoR_STORE_ID] == this.buffer0[BioHarnessConstants.RtoR_STORE_ID]) {
                            this.buffer[BioHarnessConstants.RtoR_STORE_ID] = this.buffer1[BioHarnessConstants.RtoR_STORE_ID];
                        } else {
                            this.buffer[BioHarnessConstants.RtoR_STORE_ID] = this.buffer0[BioHarnessConstants.RtoR_STORE_ID];
                        }
                    }
                }
                break;

            case BioHarnessConstants.ECG_MSG_ID:

                long timestamp = msg.getData().getLong("Timestamp");
                short voltage = msg.getData().getShort("Voltage");
                double ecgVoltage = voltage * 0.013405;
                if (ecgVoltage < 10.0) {
                    lastValidEcgVoltage = ecgVoltage;
                }
                if (graphView != null)
                    graphView.setDataWithAdjustment(new Double[]{lastValidEcgVoltage}, activity.getString(R.string.file_name_ecg), "u8");

                if (isLogging) {
                    this.buffer[BioHarnessConstants.ECG_STORE_ID][batchRowCounts[BioHarnessConstants.ECG_STORE_ID]][1] = lastValidEcgVoltage;

                    if (this.isFirstDataRow[BioHarnessConstants.ECG_STORE_ID]) {

                        // Time difference between start the evaluation and here
                        Double bioHarnessStartTimestamp = timestamp / 1.0;
                        //Double timeDifference = bioHarnessStartTimestamp - this.startTimestamp;
                        Double timeDifference = (System.currentTimeMillis() - 25.2) - this.startTimestamp / 1.0;
                        this.incrementedTimestamp[BioHarnessConstants.ECG_STORE_ID] = timeDifference;

                        Log.d(TAG, "Time difference: " + timeDifference + " ms");
                        Log.d(TAG, "Start timestamp: " + Utils.getDateString(this.startTimestamp, "dd/MM/yyyy hh:mm:ss.SSS"));
                        Log.d(TAG, "First data row timestamp: " + Utils.getDateString(System.currentTimeMillis(), "dd/MM/yyyy hh:mm:ss.SSS"));
                        Log.d(TAG, "BioHarness start timestamp: " + Utils.getDateString(bioHarnessStartTimestamp.longValue(), "dd/MM/yyyy hh:mm:ss.SSS"));
                        writeHeader(activity.getString(R.string.file_name_ecg), new String[]{activity.getString(R.string.file_header_timestamp), activity.getString(R.string.file_header_voltage)});
                        this.isFirstDataRow[BioHarnessConstants.ECG_STORE_ID] = false;
                    }

                    this.buffer[BioHarnessConstants.ECG_STORE_ID][batchRowCounts[BioHarnessConstants.ECG_STORE_ID]][0] = this.incrementedTimestamp[BioHarnessConstants.ECG_STORE_ID];
                    this.incrementedTimestamp[BioHarnessConstants.ECG_STORE_ID] += 1000.0/250.0;

                    //Log.d(TAG, "Timestamp: " + this.buffer[BioHarnessConstants.ECG_STORE_ID][batchRowCounts[BioHarnessConstants.ECG_STORE_ID]][0]  + " ms / Voltage: " + this.buffer[BioHarnessConstants.ECG_STORE_ID][batchRowCounts[BioHarnessConstants.ECG_STORE_ID]][1] + " mV");

                    batchRowCounts[BioHarnessConstants.ECG_STORE_ID]++;
                    if (batchRowCounts[BioHarnessConstants.ECG_STORE_ID] == maxBatchCounts[BioHarnessConstants.ECG_STORE_ID]) {
                        writeValues(activity.getString(R.string.file_name_ecg), this.buffer[BioHarnessConstants.ECG_STORE_ID], 2, batchRowCounts[BioHarnessConstants.ECG_STORE_ID], null); // "# BatchRowCount: " + batchRowCounts
                        batchRowCounts[BioHarnessConstants.ECG_STORE_ID] = 0;
                        if (this.buffer[BioHarnessConstants.ECG_STORE_ID] == this.buffer0[BioHarnessConstants.ECG_STORE_ID]) {
                            this.buffer[BioHarnessConstants.ECG_STORE_ID] = this.buffer1[BioHarnessConstants.ECG_STORE_ID];
                        } else {
                            this.buffer[BioHarnessConstants.ECG_STORE_ID] = this.buffer0[BioHarnessConstants.ECG_STORE_ID];
                        }
                    }
                }
                break;
        }
    }

    private void writeValues(String filename, Double[][] buffer, int fields, int batchRowCount, String batchComments) {
        new WriteDataTask().execute(new WriteDataTaskParams(this.root, filename, buffer, fields, batchRowCount, batchComments));
    }

    public void setGraphView(GraphView graphView) {
        this.graphView = graphView;
    }
}
