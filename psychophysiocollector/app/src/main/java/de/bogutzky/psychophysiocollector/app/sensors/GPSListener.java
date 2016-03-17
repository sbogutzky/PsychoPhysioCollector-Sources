/**
 * The MIT License (MIT)
 Copyright (c) 2016 Simon Bogutzky, Jan Christoph Schrader

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

package de.bogutzky.psychophysiocollector.app.sensors;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.bogutzky.psychophysiocollector.app.MainActivity;
import de.bogutzky.psychophysiocollector.app.R;
import de.bogutzky.psychophysiocollector.app.data.management.WriteDataTask;
import de.bogutzky.psychophysiocollector.app.data.management.WriteDataTaskParams;

public class GPSListener implements LocationListener {
    private static final String TAG = "GPSListener";
    private String filename;
    private File root;
    private int i = 0;
    private int maxValueCount;
    private Double[][] values;
    private MainActivity activity;

    private float lastLocationAccuracy;

    public GPSListener(String filename, String directoryName, int maxValueCount, MainActivity activity) {
        this.filename = filename;
        this.activity = activity;

        this.root = activity.getStorageDirectory(directoryName);

        this.maxValueCount = maxValueCount;
        this.values = new Double[maxValueCount][4];

        this.lastLocationAccuracy = 0;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(this.root, this.filename), true));
            String outputString = activity.getHeaderComments();
            outputString += "" + activity.getString(R.string.file_header_timestamp) + "," + activity.getString(R.string.file_header_gps_latitude) + "," + activity.getString(R.string.file_header_gps_longitude) + "," + activity.getString(R.string.file_header_gps_altitude);
            writer.write(outputString);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double time = location.getTime() - activity.getStartTimestamp();
        values[i][0] = time;
        values[i][1] = location.getLatitude();
        values[i][2] = location.getLongitude();
        values[i][3] = location.getAltitude();

        i++;
        if (i > maxValueCount - 1) {
            writeValues(this.filename, values, 4, values.length, null);
            this.values = new Double[maxValueCount][4];
            i = 0;
        }
        if (lastLocationAccuracy - location.getAccuracy() > 5.0) {
            activity.setGpsStatusText(activity.getString(R.string.info_connected_fix_received) + activity.getString(R.string.accuracy) + location.getAccuracy());
            lastLocationAccuracy = location.getAccuracy();
        } else {
            activity.setGpsStatusText(activity.getString(R.string.info_connected_fix_received));
        }
    }

    public void stopStreaming() {
        String footer = activity.getFooterComments();
        writeValues(this.filename, values, 4, values.length, footer);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(activity, activity.getString(R.string.gps_not_available), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void writeValues(String filename, Double[][] buffer, int fields, int batchRowCount, String batchComments) {
        new WriteDataTask().execute(new WriteDataTaskParams(this.root, filename, buffer, fields, batchRowCount, batchComments));
    }
}