/**
 * The MIT License (MIT)
 Copyright (c) 2016 Copyright (c) 2016 University of Applied Sciences Bremen

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 and associated documentation files (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included
 in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.bogutzky.psychophysiocollector.app.data.management;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
