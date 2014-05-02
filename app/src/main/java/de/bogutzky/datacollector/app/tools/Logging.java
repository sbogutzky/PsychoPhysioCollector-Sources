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


public class Logging {
    private static final String TAG = "Logging";
    public File mOutputFile;
    boolean mFirstWrite = true;
    String[] mSensorNames;
    String[] mSensorFormats;
    String[] mSensorUnits;
    String mFileName = "";
    BufferedWriter mWriter = null;
    String mDelimiter = ","; // default is comma

    /**
     * @param filename is the filename which will be used

    public Logging(String filename) {
        mFileName = filename;
        File root = Environment.getExternalStorageDirectory();
        mOutputFile = new File(root, mFileName + ".csv");
    }*/

    /**
     * @param filename is the filename which will be used
     * @param delimiter is the delimiter which will be used

    public Logging(String filename, String delimiter) {
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
    public Logging(String filename, String delimiter, String directoryName) {
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
                int size = propertyCluster.size() / 2;
                mSensorNames = new String[size];
                mSensorFormats = new String[size];
                mSensorUnits = new String[size];
                int i = 0;
                Collection<String> unsortedKeys = propertyCluster.keys();
                List<String> keys = new ArrayList<String>(unsortedKeys);
                Collections.sort(keys);
                for (String key : keys) {
                    Collection<FormatCluster> formatClusters = propertyCluster.get(key);
                    if (!isStringInArray(key, mSensorNames)) {
                        for (FormatCluster formatCluster : formatClusters) {
                            if (formatCluster.mFormat.equals(format)) {
                                mSensorFormats[i] = formatCluster.mFormat;
                                mSensorUnits[i] = formatCluster.mUnits;
                                mSensorNames[i] = key;
                                Log.d(TAG, "Data column " + (i + 1) + ": " + key + " " + mSensorFormats[i] + " " + mSensorUnits[i]);
                                i++;
                            }
                        }
                    }
                }

//                // Write header to a file
//                for (int k = 0; k < mSensorNames.length; k++) {
//                    mWriter.write("\"" + objectCluster.mMyName + "\"");
//                    if(mSensorNames.length - 1 > k) {
//                        mWriter.write(mDelimiter);
//                    }
//                }
//                mWriter.newLine(); // Notepad recognized new lines as \r\n

                for (int k = 0; k < mSensorNames.length; k++) {
                    mWriter.write("\"" + mSensorNames[k] + "\"");
                    if (mSensorNames.length - 1 > k) {
                        mWriter.write(mDelimiter);
                    }
                }
                mWriter.newLine();

//                for (int k = 0; k < mSensorFormats.length; k++) {
//                    mWriter.write("\"" + mSensorFormats[k] + "\"");
//                    if(mSensorFormats.length - 1 > k) {
//                        mWriter.write(mDelimiter);
//                    }
//                }
//                mWriter.newLine();

                if (logUnits) {
                    for (int k = 0; k < mSensorUnits.length; k++) {
                        if (mSensorUnits[k].equals("u8")) {
                            mWriter.write("");
                        } else if (mSensorUnits[k].equals("i8")) {
                            mWriter.write("");
                        } else if (mSensorUnits[k].equals("u12")) {
                            mWriter.write("");
                        } else if (mSensorUnits[k].equals("u16")) {
                            mWriter.write("");
                        } else if (mSensorUnits[k].equals("i16")) {
                            mWriter.write("");
                        } else {
                            mWriter.write("\"" + mSensorUnits[k] + "\"");
                        }
                        if (mSensorUnits.length - 1 > k) {
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
            for (int k = 0; k < mSensorNames.length; k++) {
                Collection<FormatCluster> formatClusterCollection = objectCluster.mPropertyCluster.get(mSensorNames[k]);
                FormatCluster formatCluster = getCurrentFormatCluster(formatClusterCollection, format, mSensorUnits[k]);
                Log.d(TAG, "Write " + mSensorNames[k] + " data: " + formatCluster.mData + " " + formatCluster.mUnits);
                mWriter.write(Double.toString(formatCluster.mData));
                if (mSensorNames.length - 1 > k) {
                    mWriter.write(mDelimiter);
                }
            }
            mWriter.newLine();
            closeFile();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing in file", e);
        }
    }

    public void closeFile() {
        if (mWriter != null) {
            try {
                mWriter.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while closing in file", e);
            }
        }
    }

    private boolean isStringInArray(String string, String[] stringArray) {
        for (String stringFromArray : stringArray) {
            if (string.equals(stringFromArray)) {
                return true;
            }
        }
        return false;
    }

    private FormatCluster getCurrentFormatCluster(Collection<FormatCluster> formatClusterCollection, String format, String units) {
        for (FormatCluster formatCluster : formatClusterCollection) {
            if (formatCluster.mFormat.equals(format) && formatCluster.mUnits.equals(units)) {
                return formatCluster;
            }
        }
        return null;
    }

    public String getName() {
        return mFileName;
    }
}


