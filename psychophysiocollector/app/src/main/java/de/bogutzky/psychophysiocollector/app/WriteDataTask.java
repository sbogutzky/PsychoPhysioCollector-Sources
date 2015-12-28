package de.bogutzky.psychophysiocollector.app;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by on 28.12.2015.
 */
public class WriteDataTask extends AsyncTask<WriteDataTaskParams, Void, Integer> {

    private static final String TAG = "WriteDataTask";
    private MainActivity activity;

    @Override
    protected Integer doInBackground(WriteDataTaskParams... params) {
        String[][] data = params[0].values;
        String filename = params[0].filename;
        File root = params[0].root;
        int fields = params[0].fields;
        boolean footer = params[0].footer;
        String footerString = params[0].footerString;
        int writingSlot = params[0].writingSlot;
        activity = params[0].activity;

        Log.d(TAG, "Write data in " + filename);
        writeToFileMethod(data, filename, root, fields, footer, footerString);
        return writingSlot;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        //Log.v(TAG, "post execute: " + integer);
        if(integer == 1) {
            activity.setWritingData(false);
        } else {
            activity.setSecondWritingData(false);
        }
    }

    private void writeToFileMethod(String[][] data, String filename, File root, int fields, boolean footer, String footerString) {
        String[][] copies = new String[data.length][fields];
        System.arraycopy(data, 0, copies, 0, data.length - 1);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(root, filename), true));

            for (String[] copy : copies) {
                if (copy[0] != null) {
                    String outputString = "";
                    for (int k = 0; k < fields; k++) {
                        if (fields - 1 != k) {
                            outputString += copy[k] + ",";
                        } else {
                            outputString += copy[k];
                        }
                    }
                    writer.write(outputString);
                    writer.newLine();
                }
            }
            if(footer) {
                writer.write(footerString);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }
}
