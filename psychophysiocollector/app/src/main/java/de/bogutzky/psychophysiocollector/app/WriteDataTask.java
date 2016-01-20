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
        File root = params[0].root;
        String filename = params[0].filename;
        Double[][] data = params[0].data;
        int numberOfCols = params[0].numberOfCols;
        int numberOfRows = params[0].numberOfRows;
        String batchComments = params[0].batchComments;

        writeToFile(root, filename, data, numberOfCols, numberOfRows, batchComments);

        return null;
    }

    private void writeToFile(File root, String filename, Double[][] data, int numberOfCols, int numberOfRows, String batchComments) {

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(root, filename), true));

            for (int i = 0; i < numberOfRows; i++) {
                Double[] row = data[i];
                if (row[0] != null) {
                    String outputString = "";
                    for (int j = 0; j < numberOfCols; j++) {
                        if (numberOfCols - 1 != j) {
                            outputString += Double.toString(row[j]) + ",";
                        } else {
                            outputString += Double.toString(row[j]);
                        }
                    }
                    writer.write(outputString);
                    writer.newLine();
                }
            }

            if(batchComments != null) {
                writer.write(batchComments);
                writer.newLine();
            }
            writer.flush();
            writer.close();

            Log.d(TAG, "Wrote data in " + filename);
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }
}
