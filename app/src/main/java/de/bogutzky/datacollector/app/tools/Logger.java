package de.bogutzky.datacollector.app.tools;

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
    public File mOutputFile;
    boolean mFirstWrite = true;
    ArrayList <String> mSensorNames;
    ArrayList <String> mSensorFormats;
    ArrayList <String> mSensorUnits;
    String mFileName = "";
    BufferedWriter mWriter = null;
    String mDelimiter = ","; // default is comma

    /**
     * @param filename is the filename which will be used

    public Logger(String filename) {
        mFileName = filename;
        File root = Environment.getExternalStorageDirectory();
        mOutputFile = new File(root, mFileName + ".csv");
    }*/

    /**
     * @param filename is the filename which will be used
     * @param delimiter is the delimiter which will be used

    public Logger(String filename, String delimiter) {
        mFileName = filename;
        mDelimiter = delimiter;
        File root = Environment.getExternalStorageDirectory();
        mOutputFile = new File(root, mFileName + ".csv");
    }*/

    /**
     * @param filename is the filename which will be used
     * @param delimiter is the delimiter which will be used
     * @param directoryName will create a new directory if it does not exist
     */
    public Logger(String filename, String delimiter, String directoryName) {
        mFileName = filename;
        mDelimiter = delimiter;

        File root = new File(Environment.getExternalStorageDirectory() + "/" + directoryName);

        if (!root.exists()) {
            if (root.mkdir()) {
                Log.d(TAG, "Directory " + directoryName + " created");
            }
        }
        mOutputFile = new File(root, mFileName + ".csv");
    }


    /**
     * This function takes an object cluster and logs all the data within it. User should note that the function will write over prior files with the same name.
     *
     * @param objectCluster data which will be written into the file
     */
    public void logData(ObjectCluster objectCluster, String format, Boolean logUnits) {
        try {
            if (mFirstWrite) {
                mWriter = new BufferedWriter(new FileWriter(mOutputFile, false));

                // First retrieve all the unique keys from the objectCluster
                Multimap<String, FormatCluster> propertyCluster = objectCluster.mPropertyCluster;
                mSensorNames = new ArrayList<String>();
                mSensorFormats = new ArrayList<String>();
                mSensorUnits = new ArrayList<String>();
                int i = 0;
                Collection<String> unsortedKeys = propertyCluster.keys();
                List<String> keys = new ArrayList<String>(unsortedKeys);
                Collections.sort(keys);
                for (String key : keys) {
                    Collection<FormatCluster> formatClusters = propertyCluster.get(key);
                    if (!mSensorNames.contains(key)) {
                        for (FormatCluster formatCluster : formatClusters) {
                            if (formatCluster.mFormat.equals(format)) {
                                mSensorFormats.add(formatCluster.mFormat);
                                mSensorUnits.add(formatCluster.mUnits);
                                mSensorNames.add(key);
                                Log.d(TAG, "Data column " + (i + 1) + ": " + key + " " + mSensorFormats.get(i) + " " + mSensorUnits.get(i));
                                i++;
                            }
                        }
                    }
                }

//                // Write header to a file
//                for (int k = 0; k < mSensorNames.size(); k++) {
//                    mWriter.write("\"" + objectCluster.mMyName + "\"");
//                    if(mSensorNames.size()- 1 > k) {
//                        mWriter.write(mDelimiter);
//                    }
//                }
//                mWriter.newLine(); // Notepad recognized new lines as \r\n

                for (int k = 0; k < mSensorNames.size(); k++) {
                    mWriter.write("\"" + mSensorNames.get(k) + "\"");
                    if (mSensorNames.size() - 1 > k) {
                        mWriter.write(mDelimiter);
                    }
                }
                mWriter.newLine();

//                for (int k = 0; k < mSensorFormats.size(); k++) {
//                    mWriter.write("\"" + mSensorFormats.get(k) + "\"");
//                    if(mSensorFormats.size() - 1 > k) {
//                        mWriter.write(mDelimiter);
//                    }
//                }
//                mWriter.newLine();

                if (logUnits) {
                    for (int k = 0; k < mSensorUnits.size(); k++) {
                        if (mSensorUnits.get(k).equals("u8")) {
                            mWriter.write("");
                        } else if (mSensorUnits.get(k).equals("i8")) {
                            mWriter.write("");
                        } else if (mSensorUnits.get(k).equals("u12")) {
                            mWriter.write("");
                        } else if (mSensorUnits.get(k).equals("u16")) {
                            mWriter.write("");
                        } else if (mSensorUnits.get(k).equals("i16")) {
                            mWriter.write("");
                        } else {
                            mWriter.write("\"" + mSensorUnits.get(k) + "\"");
                        }
                        if (mSensorUnits.size() - 1 > k) {
                            mWriter.write(mDelimiter);
                        }
                    }
                    mWriter.newLine();
                }
                closeFile();
                mFirstWrite = false;
            }
            mWriter = new BufferedWriter(new FileWriter(mOutputFile, true));

            // Write data
            for (int k = 0; k < mSensorNames.size(); k++) {
                Collection<FormatCluster> formatClusterCollection = objectCluster.mPropertyCluster.get(mSensorNames.get(k));
                FormatCluster formatCluster = getCurrentFormatCluster(formatClusterCollection, format, mSensorUnits.get(k));
                Log.d(TAG, "Write " + mSensorNames.get(k) + " data in file "+ mFileName +": " + formatCluster.mData + " " + formatCluster.mUnits);
                mWriter.write(Double.toString(formatCluster.mData));
                if (mSensorNames.size() - 1 > k) {
                    mWriter.write(mDelimiter);
                }
            }
            mWriter.newLine();
            mWriter.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }

    public void closeFile() {
        if (mWriter != null) {
            try {
                mWriter.flush();
                mWriter.close();
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
}


