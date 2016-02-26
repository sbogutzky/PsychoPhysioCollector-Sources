package de.bogutzky.psychophysiocollector.app.bioharness;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.bogutzky.psychophysiocollector.app.Utils;

public class BioHarnessHandler extends Handler {
    private static final String TAG = "BioHarnessHandler";

    private final int RtoR_MSG_ID = 0x24;

    private int batchRowCount = 0;
    private int maxBatchCount;
    private Double[][] buffer;
    private Double[][] buffer0;
    private Double[][] buffer1;

    private long startTimestamp;
    private Double incrementedTimestamp;
    private boolean isFirstDataRow = true;
    private boolean isLogging = false;

    public BioHarnessHandler(int maxBatchCount) {
        this.maxBatchCount = maxBatchCount;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public void startStreaming() {
        this.buffer0 = new Double[maxBatchCount][2];
        this.buffer1 = new Double[maxBatchCount][2];
        this.buffer = buffer0;
        this.isLogging = true;
    }

    public void stopStreaming() {
        this.isLogging = false;
        this.isFirstDataRow = true;
    }

    public void handleMessage(Message msg) {

        switch (msg.what) {
            case RtoR_MSG_ID:
                if(isLogging) {
                    int rrInterval = msg.getData().getInt("rrInterval");
                    long timestamp = msg.getData().getLong("Timestamp");

                    this.buffer[batchRowCount][1] = rrInterval / 1.0;

                    if (this.isFirstDataRow) {

                        // Time difference between start the evaluation and here
                        Double bioHarnessStartTimestamp = timestamp / 1.0;
                        Double timeDifference = bioHarnessStartTimestamp - this.startTimestamp;
                        this.incrementedTimestamp = timeDifference;

                        //TODO: Negative time difference
                        Log.d(TAG, "Time difference: " + timeDifference + " ms");
                        Log.d(TAG, "Start timestamp: " + Utils.getDateString(this.startTimestamp, "dd/MM/yyyy hh:mm:ss.SSS"));
                        Log.d(TAG, "BioHarness start timestamp: " + Utils.getDateString(bioHarnessStartTimestamp.longValue(), "dd/MM/yyyy hh:mm:ss.SSS"));
                        this.buffer[batchRowCount][0] = timeDifference;
                        this.isFirstDataRow = false;
                    }

                    this.buffer[batchRowCount][0] = this.incrementedTimestamp;
                    this.incrementedTimestamp += this.buffer[batchRowCount][1];

                    Log.d(TAG, "Timestamp: " + this.buffer[batchRowCount][0] / 1000.0 + " s / RR-Interval: " + this.buffer[batchRowCount][1] + " ms");

                    batchRowCount++;
                    if (batchRowCount == maxBatchCount) {
                        //writeValues(null); // "# BatchRowCount: " + batchRowCount
                        batchRowCount = 0;
                        if (this.buffer == this.buffer0) {
                            this.buffer = this.buffer1;
                        } else {
                            this.buffer = this.buffer0;
                        }
                    }
                }
                break;
        }

        /*
        if(!fileStorageCreated && loggingEnabled) {
            createBioHarnessFiles();
            fileStorageCreated = true;
        }
        if(msg.what == 101) {
            notifyBHReady();
        }
        if(loggingEnabled) {
            double time = 0;
            long timestamp = 0;
            switch (msg.what) {
                case RtoR_MSG_ID:
                    int rrInterval = msg.getData().getInt("rrInterval");
                    timestamp = msg.getData().getLong("Timestamp");
                    if(firstRRIntervalTimestamp == 0L) {
                        firstRRIntervalTimestamp = timestamp;
                        rrTime = System.currentTimeMillis() - startTimestamp;
                    }
                    rrTime += rrInterval;

                    bhRRIntervalValues[bhRRIntervalValueCount][0] = rrTime;
                    bhRRIntervalValues[bhRRIntervalValueCount][1] = Double.parseDouble(String.valueOf(rrInterval));
                    bhRRIntervalValueCount++;
                    if(bhRRIntervalValueCount >= bhRRIntervalValues.length) {
                        if(!writingData) {
                            bhRRIntervalValueCount = 0;
                            setWritingData(true);
                            writeData(bhRRIntervalValues, getString(R.string.file_name_rr_interval), 2, bhRRIntervalValues.length, "");
                            bhRRIntervalValues = new Double[DATA_ARRAY_SIZE][2];
                        } else if(!secondWritingData) {
                            bhRRIntervalValueCount = 0;
                            setSecondWritingData(true);
                            writeData(bhRRIntervalValues, getString(R.string.file_name_rr_interval), 2, bhRRIntervalValues.length, "");
                            bhRRIntervalValues = new Double[DATA_ARRAY_SIZE][2];
                        } else {
                            bhRRIntervalValues = resizeArray(bhRRIntervalValues);
                        }
                    }
                    Log.v(TAG, "Logge RR interval mit Timestamp: " + time);
                    break;
                /*
                case HEART_RATE:
                    String HeartRatetext = msg.getData().getString("HeartRate");
                    timestamp = msg.getData().getLong("Timestamp");
                    if(firstbhHeartRateTimestamp == 0L) {
                        firstbhHeartRateTimestamp = timestamp;
                        bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                    }
                    time = bhStartTimestamp + (timestamp - firstbhHeartRateTimestamp) / 1000.0;
                    bhHeartRateValues[bhHeartRateValueCount][0] = time;
                    bhHeartRateValues[bhHeartRateValueCount][1] = Double.parseDouble(HeartRatetext);
                    bhHeartRateValueCount++;
                    System.out.println("Heart Rate Info is " + HeartRatetext);
                    if(bhHeartRateValueCount >= bhHeartRateValues.length) {
                        if(!writingData) {
                            bhHeartRateValueCount = 0;
                            setWritingData(true);
                            writeData(bhHeartRateValues, getString(R.string.file_name_heart_rate), 2, bhHeartRateValues.length, "");
                            bhHeartRateValues = new Double[DATA_ARRAY_SIZE][2];
                        } else if(!secondWritingData) {
                            bhHeartRateValueCount = 0;
                            setSecondWritingData(true);
                            writeData(bhHeartRateValues, getString(R.string.file_name_heart_rate), 2, bhHeartRateValues.length, "");
                            bhHeartRateValues = new Double[DATA_ARRAY_SIZE][2];
                        } else {
                            bhHeartRateValues = resizeArray(bhHeartRateValues);
                        }
                    }
                    break;

                case RESPIRATION_RATE:
                    String RespirationRatetext = msg.getData().getString("RespirationRate");
                    if(RespirationRatetext != null) {
                        timestamp = msg.getData().getLong("Timestamp");
                        if(firstRespirationRateTimestamp == 0L) {
                            firstRespirationRateTimestamp = timestamp;
                            bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                        }
                        time = bhStartTimestamp + (timestamp - firstRespirationRateTimestamp);
                        bhRespirationRateValues[bhRespirationRateValueCount][0] = time;
                        bhRespirationRateValues[bhRespirationRateValueCount][1] = Double.parseDouble(RespirationRatetext);
                        bhRespirationRateValueCount++;
                        System.out.println("RespirationRate Info is " + RespirationRatetext);
                        if(bhRespirationRateValueCount >= bhRespirationRateValues.length) {
                            if(!writingData) {
                                bhRespirationRateValueCount = 0;
                                setWritingData(true);
                                writeData(bhRespirationRateValues, getString(R.string.file_name_respiration_rate), 2, bhRespirationRateValues.length, "");
                                bhRespirationRateValues = new Double[DATA_ARRAY_SIZE][2];
                            } else if(!secondWritingData) {
                                bhRespirationRateValueCount = 0;
                                setSecondWritingData(true);
                                writeData(bhRespirationRateValues, getString(R.string.file_name_respiration_rate), 2, bhRespirationRateValues.length, "");
                                bhRespirationRateValues = new Double[DATA_ARRAY_SIZE][2];
                            } else {
                                bhRespirationRateValues = resizeArray(bhRespirationRateValues);
                            }
                        }
                    }
                    break;

                case SKIN_TEMPERATURE:
                    String SkinTemperaturetext = msg.getData().getString("SkinTemperature");
                    timestamp = msg.getData().getLong("Timestamp");
                    if(firstSkinTemperatureTimestamp == 0L) {
                        firstSkinTemperatureTimestamp = timestamp;
                        bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                    }
                    time = bhStartTimestamp + (timestamp - firstSkinTemperatureTimestamp);
                    bhSkinTemperatureValues[bhSkinTemperatureValueCount][0] = time;
                    bhSkinTemperatureValues[bhSkinTemperatureValueCount][1] = Double.parseDouble(SkinTemperaturetext);
                    bhSkinTemperatureValueCount++;
                    System.out.println("SkinTemperature Info is " + SkinTemperaturetext);
                    if(bhSkinTemperatureValueCount >= bhSkinTemperatureValues.length) {
                        if(!writingData) {
                            bhSkinTemperatureValueCount = 0;
                            setWritingData(true);
                            writeData(bhSkinTemperatureValues, getString(R.string.file_name_skin_temperature), 2, bhSkinTemperatureValues.length, "");
                            bhSkinTemperatureValues = new Double[DATA_ARRAY_SIZE][2];
                        } else if(!secondWritingData) {
                            bhSkinTemperatureValueCount = 0;
                            setSecondWritingData(true);
                            writeData(bhSkinTemperatureValues, getString(R.string.file_name_skin_temperature), 2, bhSkinTemperatureValues.length, "");
                            bhSkinTemperatureValues = new Double[DATA_ARRAY_SIZE][2];
                        } else {
                            bhSkinTemperatureValues = resizeArray(bhSkinTemperatureValues);
                        }
                    }
                    break;

                case POSTURE:
                    String PostureText = msg.getData().getString("Posture");
                    timestamp = msg.getData().getLong("Timestamp");
                    if(firstbhPostureTimestamp == 0L) {
                        firstbhPostureTimestamp = timestamp;
                        bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                    }
                    time = bhStartTimestamp + (timestamp - firstbhPostureTimestamp);
                    bhPostureValues[bhPostureValueCount][0] = time;
                    bhPostureValues[bhPostureValueCount][1] = Double.parseDouble(PostureText);
                    bhPostureValueCount++;
                    System.out.println("Posture Info is " + PostureText);
                    if(bhPostureValueCount >= bhPostureValues.length) {
                        if(!writingData) {
                            bhPostureValueCount = 0;
                            setWritingData(true);
                            writeData(bhPostureValues, getString(R.string.file_name_posture), 2, bhPostureValues.length, "");
                            bhPostureValues = new Double[DATA_ARRAY_SIZE][2];
                        } else if(!secondWritingData) {
                            bhPostureValueCount = 0;
                            setSecondWritingData(true);
                            writeData(bhPostureValues, getString(R.string.file_name_posture), 2, bhPostureValues.length, "");
                            bhPostureValues = new Double[DATA_ARRAY_SIZE][2];
                        } else {
                            bhPostureValues = resizeArray(bhPostureValues);
                        }
                    }
                    break;

                case PEAK_ACCLERATION:
                    String PeakAccText = msg.getData().getString("PeakAcceleration");
                    timestamp = msg.getData().getLong("Timestamp");

                    if(firstPeakAccelerationTimestamp == 0L) {
                        firstPeakAccelerationTimestamp = timestamp;
                        bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                    }
                    time = bhStartTimestamp + (timestamp - firstPeakAccelerationTimestamp);

                    bhPeakAccelerationValues[bhPeakAccelerationValueCount][0] = time;
                    bhPeakAccelerationValues[bhPeakAccelerationValueCount][1] = Double.parseDouble(PeakAccText);
                    bhPeakAccelerationValueCount++;
                    System.out.println("PeakAcceleration Info is " + PeakAccText);

                    if(bhPeakAccelerationValueCount >= bhPeakAccelerationValues.length) {
                        if(!writingData) {
                            bhPeakAccelerationValueCount = 0;
                            setWritingData(true);
                            writeData(bhPeakAccelerationValues, getString(R.string.file_name_peak_acceleration), 2, bhPeakAccelerationValues.length, "");
                            bhPeakAccelerationValues = new Double[DATA_ARRAY_SIZE][2];
                        } else if(!secondWritingData) {
                            bhPeakAccelerationValueCount = 0;
                            setSecondWritingData(true);
                            writeData(bhPeakAccelerationValues, getString(R.string.file_name_peak_acceleration), 2, bhPeakAccelerationValues.length, "");
                            bhPeakAccelerationValues = new Double[DATA_ARRAY_SIZE][2];
                        } else {
                            bhPeakAccelerationValues = resizeArray(bhPeakAccelerationValues);
                        }
                    }
                    break;
                ////////////////////////////////
                case BREATHING_MSG_ID:
                    timestamp = msg.getData().getLong("Timestamp");
                    short interval = msg.getData().getShort("Interval");
                    if(firstBreathingTimestamp == 0L) {
                        firstBreathingTimestamp = timestamp;
                        bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                    }
                    time = bhStartTimestamp + (timestamp - firstBreathingTimestamp);
                    bhBreathingValues[bhBreathingValueCount][0] = time;
                    bhBreathingValues[bhBreathingValueCount][1] = Double.parseDouble(String.valueOf(interval));
                    bhBreathingValueCount++;
                    if(bhBreathingValueCount >= bhBreathingValues.length) {
                        if(!writingData) {
                            bhBreathingValueCount = 0;
                            setWritingData(true);
                            writeData(bhBreathingValues, getString(R.string.file_name_breathing), 2, bhBreathingValues.length, "");
                            bhBreathingValues = new Double[DATA_ARRAY_SIZE][2];
                        } else if(!secondWritingData) {
                            bhBreathingValueCount = 0;
                            setSecondWritingData(true);
                            writeData(bhBreathingValues, getString(R.string.file_name_breathing), 2, bhBreathingValues.length, "");
                            bhBreathingValues = new Double[DATA_ARRAY_SIZE][2];
                        } else {
                            bhBreathingValues = resizeArray(bhBreathingValues);
                        }
                    }
                    break;
                case ECG_MSG_ID:
                    timestamp = msg.getData().getLong("Timestamp");
                    short voltage = msg.getData().getShort("Voltage");
                    if(firstEcgTimestamp == 0L) {
                        firstEcgTimestamp = timestamp;
                        bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                    }
                    time = bhStartTimestamp + (timestamp - firstEcgTimestamp);
                    bhEcgValues[bhEcgValueCount][0] = time;
                    bhEcgValues[bhEcgValueCount][1] = Double.parseDouble(String.valueOf(voltage));
                    bhEcgValueCount++;
                    if(bhEcgValueCount >= bhEcgValues.length) {
                        if(!writingData) {
                            bhEcgValueCount = 0;
                            setWritingData(true);
                            writeData(bhEcgValues, getString(R.string.file_name_ecg), 2, bhEcgValues.length, "");
                            bhEcgValues = new Double[DATA_ARRAY_SIZE][2];
                        } else if(!secondWritingData) {
                            bhEcgValueCount = 0;
                            setSecondWritingData(true);
                            writeData(bhEcgValues, getString(R.string.file_name_ecg), 2, bhEcgValues.length, "");
                            bhEcgValues = new Double[DATA_ARRAY_SIZE][2];
                        } else {
                            bhEcgValues = resizeArray(bhEcgValues);
                        }
                    }
                    break;
                case ACCEL_100mg_MSG_ID:
                    timestamp = msg.getData().getLong("Timestamp");
                    double acc_x = msg.getData().getDouble("AccelerationX");
                    double acc_y = msg.getData().getDouble("AccelerationY");
                    double acc_z = msg.getData().getDouble("AccelerationZ");
                    if(firstAxisAccelerationTimestamp == 0L) {
                        firstAxisAccelerationTimestamp = timestamp;
                        bhStartTimestamp = System.currentTimeMillis() - startTimestamp;
                    }
                    time = bhStartTimestamp + (timestamp - firstAxisAccelerationTimestamp);
                    bhAxisAccelerationValues[bhAxisAccelerationValueCount][0] = time;
                    bhAxisAccelerationValues[bhAxisAccelerationValueCount][0] = acc_x;
                    bhAxisAccelerationValues[bhAxisAccelerationValueCount][0] = acc_y;
                    bhAxisAccelerationValues[bhAxisAccelerationValueCount][0] = acc_z;
                    bhAxisAccelerationValueCount++;
                    if(bhAxisAccelerationValueCount >= bhAxisAccelerationValues.length) {
                        if(!writingData) {
                            bhAxisAccelerationValueCount = 0;
                            setWritingData(true);
                            writeData(bhAxisAccelerationValues, getString(R.string.file_name_axis_acceleration), 2, bhAxisAccelerationValues.length, "");
                            bhAxisAccelerationValues = new Double[DATA_ARRAY_SIZE][4];
                        } else if(!secondWritingData) {
                            bhAxisAccelerationValueCount = 0;
                            setSecondWritingData(true);
                            writeData(bhAxisAccelerationValues, getString(R.string.file_name_axis_acceleration), 2, bhAxisAccelerationValues.length, "");
                            bhAxisAccelerationValues = new Double[DATA_ARRAY_SIZE][4];
                        } else {
                            bhAxisAccelerationValues = resizeArray(bhAxisAccelerationValues);
                        }
                    }
                    break;

            }
        } */
    }
}
