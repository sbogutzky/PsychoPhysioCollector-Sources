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
public class WriteDataTask extends AsyncTask<WriteDataTaskParams, Void, Void> {

    private static final String TAG = "WriteDataTask";

    @Override
    protected Void doInBackground(WriteDataTaskParams... params) {
        Double[][] values = params[0].values;
        String filename = params[0].filename;
        File root = params[0].root;
        int numberOfFields = params[0].numberOfFields;

        boolean writeFooter = params[0].writeFooter;
        String footer = params[0].footer;

        writeToFile(values, filename, root, numberOfFields, writeFooter, footer);

        return null;
    }

    private void writeToFile(Double[][] values, String filename, File root, int numberOfFields, boolean writeFooter, String footer) {

        Log.d(TAG, "Write data in " + filename);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(root, filename), true));

            for (Double[] value : values) {
                if (value[0] != null) {
                    String outputString = "";
                    for (int k = 0; k < numberOfFields; k++) {
                        if (numberOfFields - 1 != k) {
                            outputString += Double.toString(value[k]) + ",";
                        } else {
                            outputString += Double.toString(value[k]);
                        }
                    }
                    writer.write(outputString);
                    writer.newLine();
                }
            }

            if(writeFooter) {
                writer.write(footer);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }
}
