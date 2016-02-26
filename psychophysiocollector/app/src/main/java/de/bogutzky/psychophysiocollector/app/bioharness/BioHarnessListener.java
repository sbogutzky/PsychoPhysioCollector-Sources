package de.bogutzky.psychophysiocollector.app.bioharness;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.bogutzky.psychophysiocollector.app.Utils;
import zephyr.android.BioHarnessBT.*;

public class BioHarnessListener extends ConnectListenerImpl {
    private static final String TAG = "BioHarnessListener";

    public BioHarnessHandler bioHarnessHandler;
    private final int GP_MSG_ID = 0x20;
    private final int BREATHING_MSG_ID = 0x21;
    private final int ECG_MSG_ID = 0x22;
    private final int RtoR_MSG_ID = 0x24;
    private final int ACCEL_100mg_MSG_ID = 0x2A;
//    private final int SUMMARY_MSG_ID = 0x2B;

    private final int POSTURE = 0x103;
    private final int HEART_RATE = 0x100;
    private final int RESPIRATION_RATE = 0x101;
    private final int SKIN_TEMPERATURE = 0x102;
    private final int PEAK_ACCLERATION = 0x104;

    public boolean isHeartRateEnabled() {
        return heartRateEnabled;
    }

    private boolean heartRateEnabled = false;

    public boolean isSkinTemperatureEnabled() {
        return skinTemperatureEnabled;
    }

    private boolean skinTemperatureEnabled = false;

    /* Creating the different Objects for different types of Packets */
    private GeneralPacketInfo generalPacketInfo = new GeneralPacketInfo();
    private ECGPacketInfo ecgPacketInfo = new ECGPacketInfo();
    private BreathingPacketInfo breathingInfoPacket = new BreathingPacketInfo();
    private RtoRPacketInfo rtoRPacketInfo = new RtoRPacketInfo();
    private AccelerometerPacketInfo accelerometerPacketInfo = new AccelerometerPacketInfo();
    // private SummaryPacketInfo SummaryInfoPacket = new SummaryPacketInfo();

    private PacketTypeRequest RqPacketType = new PacketTypeRequest();

    public BioHarnessListener(Handler handler, BioHarnessHandler bioHarnessHandler) {
        super(handler, null);
        this.bioHarnessHandler = bioHarnessHandler;
    }

    public void Connected(ConnectedEvent<BTClient> eventArgs) {
        Log.d(TAG, String.format("Connected to BioHarness %s.", eventArgs.getSource().getDevice().getName()));
        Message msg = new Message();
        msg.what = 101; //ready msg
        bioHarnessHandler.sendMessage(msg);

        /* Use this object to enable or disable the different Packet types */
        RqPacketType.GP_ENABLE = false;
        RqPacketType.RtoR_ENABLE = true;
        RqPacketType.ECG_ENABLE = false;
        RqPacketType.ACCELEROMETER_ENABLE = false;
        RqPacketType.BREATHING_ENABLE = false;
        RqPacketType.LOGGING_ENABLE = false;
        RqPacketType.SUMMARY_ENABLE = false;

        //Creates a new ZephyrProtocol object and passes it the BTComms object
        ZephyrProtocol zephyrProtocol = new ZephyrProtocol(eventArgs.getSource().getComms(), RqPacketType);
        zephyrProtocol.addZephyrPacketEventListener(new ZephyrPacketListener() {
            public void ReceivedPacket(ZephyrPacketEvent eventArgs) {
                ZephyrPacketArgs msg = eventArgs.getPacket();
                int msgID = msg.getMsgID();
                byte[] dataArray = msg.getBytes();
                switch (msgID) {

                    case GP_MSG_ID:
                        processPacketGeneral(dataArray);
                        break;

                    case BREATHING_MSG_ID:
                        processPacketBreath(dataArray);
                        break;

                    case ECG_MSG_ID:
                        processPacketEcg(dataArray);
                        break;

                    case RtoR_MSG_ID:
                        processPacketRtoR(dataArray);
                        break;

                    case ACCEL_100mg_MSG_ID:
                        processPacketAccel(dataArray);
                        break;
                }
            }
        });
    }

    private void processPacketGeneral(byte[] dataArray) {
        long timestamp = TimeConverter.timeToEpoch(
                generalPacketInfo.GetTSYear(dataArray),
                generalPacketInfo.GetTSMonth(dataArray),
                generalPacketInfo.GetTSDay(dataArray),
                generalPacketInfo.GetMsofDay(dataArray)
        );
        Message message;
        Bundle bundle = new Bundle();

        if(heartRateEnabled) {
            int heartRate = generalPacketInfo.GetHeartRate(dataArray);
            message = bioHarnessHandler.obtainMessage(HEART_RATE);
            bundle.putString("HeartRate", String.valueOf(heartRate));
            bundle.putLong("Timestamp", timestamp);
            message.setData(bundle);
            bioHarnessHandler.sendMessage(message);
        }

        double RespRate = generalPacketInfo.GetRespirationRate(dataArray);
        message = bioHarnessHandler.obtainMessage(RESPIRATION_RATE);
        bundle.putString("RespirationRate", String.valueOf(RespRate));
        bundle.putLong("Timestamp", timestamp);
        message.setData(bundle);
        bioHarnessHandler.sendMessage(message);

        if(skinTemperatureEnabled) {
            double SkinTempDbl = generalPacketInfo.GetSkinTemperature(dataArray);
            message = bioHarnessHandler.obtainMessage(SKIN_TEMPERATURE);
            bundle.putLong("Timestamp", timestamp);
            bundle.putString("SkinTemperature", String.valueOf(SkinTempDbl));
            message.setData(bundle);
            bioHarnessHandler.sendMessage(message);
        }

        int PostureInt = generalPacketInfo.GetPosture(dataArray);
        message = bioHarnessHandler.obtainMessage(POSTURE);
        bundle.putString("Posture", String.valueOf(PostureInt));
        bundle.putLong("Timestamp", timestamp);
        message.setData(bundle);
        bioHarnessHandler.sendMessage(message);

        double PeakAccDbl = generalPacketInfo.GetPeakAcceleration(dataArray);
        message = bioHarnessHandler.obtainMessage(PEAK_ACCLERATION);
        bundle.putString("PeakAcceleration", String.valueOf(PeakAccDbl));
        bundle.putLong("Timestamp", timestamp);
        message.setData(bundle);
        bioHarnessHandler.sendMessage(message);
    }

    private void processPacketBreath(byte[] dataArray) {

        // Extract timestamp
        long timestamp = TimeConverter.timeToEpoch(
                breathingInfoPacket.GetTSYear(dataArray),
                breathingInfoPacket.GetTSMonth(dataArray),
                breathingInfoPacket.GetTSDay(dataArray),
                breathingInfoPacket.GetMsofDay(dataArray)
        );

        // Extract Breathing Data
        short[] samples = breathingInfoPacket.GetBreathingSamples(dataArray);

        // Convert values and send message
        for (short sample : samples) {
            Message message = new Message();
            message.what = BREATHING_MSG_ID;
            Bundle bundle = new Bundle();

            bundle.putShort("Interval", sample);
            bundle.putLong("Timestamp", timestamp);
            message.setData(bundle);
            bioHarnessHandler.sendMessage(message);
        }
    }

    private void processPacketEcg(byte[] dataArray) {

        // Extract timestamp
        long timestamp = TimeConverter.timeToEpoch(
                ecgPacketInfo.GetTSYear(dataArray),
                ecgPacketInfo.GetTSMonth(dataArray),
                ecgPacketInfo.GetTSDay(dataArray),
                ecgPacketInfo.GetMsofDay(dataArray)
        );

        // Extract ECG Data
        short[] samples = ecgPacketInfo.GetECGSamples(dataArray);

        // Convert values and send message
        for (short sample : samples) {
            Message message = new Message();
            message.what = ECG_MSG_ID;
            Bundle bundle = new Bundle();

            bundle.putShort("Voltage", sample);
            bundle.putLong("Timestamp", timestamp);
            message.setData(bundle);
            bioHarnessHandler.sendMessage(message);
        }
    }

    private void processPacketAccel(byte[] dataArray) {

        // Extract timestamp
        long timestamp = TimeConverter.timeToEpoch(
                accelerometerPacketInfo.GetTSYear(dataArray),
                accelerometerPacketInfo.GetTSMonth(dataArray),
                accelerometerPacketInfo.GetTSDay(dataArray),
                accelerometerPacketInfo.GetMsofDay(dataArray)
        );

        accelerometerPacketInfo.UnpackAccelerationData(dataArray);
        double[] samplesX = accelerometerPacketInfo.GetX_axisAccnData();
        double[] samplesY = accelerometerPacketInfo.GetY_axisAccnData();
        double[] samplesZ = accelerometerPacketInfo.GetZ_axisAccnData();

        // Convert values and send message
        for (int i = 0; i < samplesX.length; i++) {
            Message message = new Message();
            message.what =  ACCEL_100mg_MSG_ID;
            Bundle bundle = new Bundle();

            double accelerationX = samplesX[i];
            double accelerationY = samplesY[i];
            double accelerationZ = samplesZ[i];

            bundle.putDouble("AccelerationX", accelerationX);
            bundle.putDouble("AccelerationY", accelerationY);
            bundle.putDouble("AccelerationZ", accelerationZ);
            bundle.putLong("Timestamp", timestamp);
            message.setData(bundle);
            bioHarnessHandler.sendMessage(message);
        }

    }

    private short lastRRInterval = 0;
    private void processPacketRtoR(byte[] dataArray) {

        // Extract timestamp
        long timestamp = TimeConverter.timeToEpoch(
                rtoRPacketInfo.GetTSYear(dataArray),
                rtoRPacketInfo.GetTSMonth(dataArray),
                rtoRPacketInfo.GetTSDay(dataArray),
                rtoRPacketInfo.GetMsofDay(dataArray)
        );

        // Extract RtoR Data
        int[] samples = rtoRPacketInfo.GetRtoRSamples(dataArray);

        // Convert values and send message

        int rrIntervalCount = 0;
        for (int sample : samples) {
            short currentRRInterval = (short) sample;
            if (lastRRInterval != currentRRInterval) {
                rrIntervalCount++;
                lastRRInterval = currentRRInterval;
                int rrInterval = Math.abs(lastRRInterval);

                Message message = new Message();
                message.what = RtoR_MSG_ID;
                Bundle bundle = new Bundle();
                bundle.putInt("rrInterval", rrInterval);
                bundle.putLong("Timestamp", timestamp);
                message.setData(bundle);
                bioHarnessHandler.sendMessage(message);
            }
        }
        //Log.d(TAG, rrIntervalCount + " RR-Intervals at " + Utils.getDateString(timestamp, "dd/MM/yyyy hh:mm:ss.SSS"));
    }
}
