package de.bogutzky.data_collector.app.tools;

import android.os.Environment;
import android.util.Log;

import com.google.common.collect.Multimap;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class Logger {
    private static final String TAG = "Logger";
    private File outputFile;
    private boolean isFirstWrite = true;
    private ArrayList <String> sensorNames;
    private ArrayList <String> sensorFormats;
    private ArrayList <String> sensorUnits;
    private ArrayList <ObjectCluster> objectClusters;
    private String filename = "";
    private BufferedWriter writer;
    private String delimiter = ","; // default is comma

    /**
     * @param filename is the filename which will be used
     * @param delimiter is the delimiter which will be used
     * @param directoryName will create a new directory if it does not exist
     */
    public Logger(String filename, String delimiter, String directoryName) {
        this.filename = filename;
        this.delimiter = delimiter;

        File root = new File(Environment.getExternalStorageDirectory() + "/" + directoryName);

        if (!root.exists()) {
            if (root.mkdir()) {
                Log.d(TAG, "Directory " + directoryName + " created");
            }
        }
        outputFile = new File(root, this.filename + ".csv");
        objectClusters = new ArrayList<ObjectCluster>();
    }


    /**
     * This function takes an object cluster and logs all the data within it. User should note that the function will write over prior files with the same name.
     *
     * @param objectCluster data which will be written into the file
     */
    private void logData(ObjectCluster objectCluster, String format, Boolean logUnits) {
        try {
            if (isFirstWrite) {
                writer = new BufferedWriter(new FileWriter(outputFile, false));

                // First retrieve all the unique keys from the objectCluster
                Multimap<String, FormatCluster> propertyCluster = objectCluster.mPropertyCluster;
                sensorNames = new ArrayList<String>();
                sensorFormats = new ArrayList<String>();
                sensorUnits = new ArrayList<String>();
                int i = 0;
                Collection<String> unsortedKeys = propertyCluster.keys();
                List<String> keys = new ArrayList<String>(unsortedKeys);
                Collections.sort(keys);
                for (String key : keys) {
                    Collection<FormatCluster> formatClusters = propertyCluster.get(key);
                    if (!sensorNames.contains(key)) {
                        for (FormatCluster formatCluster : formatClusters) {
                            if (formatCluster.mFormat.equals(format)) {
                                sensorFormats.add(formatCluster.mFormat);
                                sensorUnits.add(formatCluster.mUnits);
                                sensorNames.add(key);
                                Log.d(TAG, "Data column " + (i + 1) + ": " + key + " " + sensorFormats.get(i) + " " + sensorUnits.get(i));
                                i++;
                            }
                        }
                    }
                }

//                // Write header to a file
//                for (int k = 0; k < sensorNames.size(); k++) {
//                    writer.write("\"" + objectCluster.mMyName + "\"");
//                    if(sensorNames.size()- 1 > k) {
//                        writer.write(delimiter);
//                    }
//                }
//                writer.newLine(); // Notepad recognized new lines as \r\n

                for (int k = 0; k < sensorNames.size(); k++) {
                    writer.write("\"" + sensorNames.get(k) + "\"");
                    if (sensorNames.size() - 1 > k) {
                        writer.write(delimiter);
                    }
                }
                writer.newLine();

//                for (int k = 0; k < sensorFormats.size(); k++) {
//                    writer.write("\"" + sensorFormats.get(k) + "\"");
//                    if(sensorFormats.size() - 1 > k) {
//                        writer.write(delimiter);
//                    }
//                }
//                writer.newLine();

                if (logUnits) {
                    for (int k = 0; k < sensorUnits.size(); k++) {
                        if (sensorUnits.get(k).equals("u8")) {
                            writer.write("");
                        } else if (sensorUnits.get(k).equals("i8")) {
                            writer.write("");
                        } else if (sensorUnits.get(k).equals("u12")) {
                            writer.write("");
                        } else if (sensorUnits.get(k).equals("u16")) {
                            writer.write("");
                        } else if (sensorUnits.get(k).equals("i16")) {
                            writer.write("");
                        } else {
                            writer.write("\"" + sensorUnits.get(k) + "\"");
                        }
                        if (sensorUnits.size() - 1 > k) {
                            writer.write(delimiter);
                        }
                    }
                    writer.newLine();
                }
                closeFile();
                isFirstWrite = false;
            }
            writer = new BufferedWriter(new FileWriter(outputFile, true));

            // Write data
            for (int k = 0; k < sensorNames.size(); k++) {
                Collection<FormatCluster> formatClusterCollection = objectCluster.mPropertyCluster.get(sensorNames.get(k));
                FormatCluster formatCluster = getCurrentFormatCluster(formatClusterCollection, format, sensorUnits.get(k));
                //Log.d(TAG, "Write " + sensorNames.get(k) + " data in file "+ filename +": " + formatCluster.mData + " " + formatCluster.mUnits);
                writer.write(Double.toString(formatCluster.mData));
                if (sensorNames.size() - 1 > k) {
                    writer.write(delimiter);
                }
            }
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }

    private void closeFile() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while closing in file", e);
            }
        }
    }

    private FormatCluster getCurrentFormatCluster(Collection<FormatCluster> formatClusterCollection, String format, String units) {
        for (FormatCluster formatCluster : formatClusterCollection) {
            if (formatCluster.mFormat.equals(format) && formatCluster.mUnits.equals(units)) {
                return formatCluster;
            }
        }
        return null;
    }

    public void addObjectCluster(ObjectCluster objectCluster) {
        objectClusters.add(objectCluster);
    }

    public void writeObjectClusters(String format, Boolean logUnits) {

        ArrayList<ObjectCluster> currentObjectClusters = new ArrayList<ObjectCluster>(objectClusters);
        Log.d(TAG, "Write " + currentObjectClusters.size() + " objectCluster");
        objectClusters.clear();
        for (ObjectCluster objectCluster : currentObjectClusters) {
            logData(objectCluster, format, logUnits);
        }
    }
}


