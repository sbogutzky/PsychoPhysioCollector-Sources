package de.bogutzky.psychophysiocollector.app.bioharness;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import zephyr.android.BioHarnessBT.*;

public class BioHarnessListener extends ConnectListenerImpl {
    private static final String TAG = "BioHarnessListener";

    public BioHarnessHandler bioHarnessHandler;

    /* Creating the different Objects for different types of Packets */
    //private GeneralPacketInfo generalPacketInfo = new GeneralPacketInfo();
    private ECGPacketInfo ecgPacketInfo = new ECGPacketInfo();
    //private BreathingPacketInfo breathingInfoPacket = new BreathingPacketInfo();
    private RtoRPacketInfo rtoRPacketInfo = new RtoRPacketInfo();
    //private AccelerometerPacketInfo accelerometerPacketInfo = new AccelerometerPacketInfo();
    //private SummaryPacketInfo SummaryInfoPacket = new SummaryPacketInfo();

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
        RqPacketType.ECG_ENABLE = true;
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

                    case BioHarnessConstants.GP_MSG_ID:
                        //processPacketGeneral(dataArray);
                        break;

                    case BioHarnessConstants.BREATHING_MSG_ID:
                        //processPacketBreath(dataArray);
                        break;

                    case BioHarnessConstants.ECG_MSG_ID:
                        processPacketEcg(dataArray);
                        break;

                    case BioHarnessConstants.RtoR_MSG_ID:
                        processPacketRtoR(dataArray);
                        break;

                    case BioHarnessConstants.ACCEL_100mg_MSG_ID:
                        //processPacketAccel(dataArray);
                        break;
                }
            }
        });
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
        for (int sample : samples) {
            short currentRRInterval = (short) sample;
            if (lastRRInterval != currentRRInterval) {
                lastRRInterval = currentRRInterval;
                int rrInterval = Math.abs(lastRRInterval);

                Message message = new Message();
                message.what = BioHarnessConstants.RtoR_MSG_ID;
                Bundle bundle = new Bundle();
                bundle.putInt("rrInterval", rrInterval);
                bundle.putLong("Timestamp", timestamp);
                message.setData(bundle);
                bioHarnessHandler.sendMessage(message);
            }
        }
        //Log.d(TAG,  "RR-Intervals at " + Utils.getDateString(timestamp, "dd/MM/yyyy hh:mm:ss.SSS"));
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
            message.what = BioHarnessConstants.ECG_MSG_ID;
            Bundle bundle = new Bundle();

            bundle.putShort("Voltage", sample);
            bundle.putLong("Timestamp", timestamp);
            message.setData(bundle);
            bioHarnessHandler.sendMessage(message);
        }
        //Log.d(TAG, samples.length + " ECG samples at " + Utils.getDateString(timestamp, "dd/MM/yyyy hh:mm:ss.SSS"));
    }

    /*
    private void processPacketGeneral(byte[] dataArray) {
        long timestamp = TimeConverter.timeToEpoch(
                generalPacketInfo.GetTSYear(dataArray),
                generalPacketInfo.GetTSMonth(dataArray),
                generalPacketInfo.GetTSDay(dataArray),
                generalPacketInfo.GetMsofDay(dataArray)
        );
        Message message;
        Bundle bundle = new Bundle();

        int heartRate = generalPacketInfo.GetHeartRate(dataArray);
        message = bioHarnessHandler.obtainMessage(BioHarnessConstants.HEART_RATE);
        bundle.putString("HeartRate", String.valueOf(heartRate));
        bundle.putLong("Timestamp", timestamp);
        message.setData(bundle);
        bioHarnessHandler.sendMessage(message);

        double RespRate = generalPacketInfo.GetRespirationRate(dataArray);
        message = bioHarnessHandler.obtainMessage(BioHarnessConstants.RESPIRATION_RATE);
        bundle.putString("RespirationRate", String.valueOf(RespRate));
        bundle.putLong("Timestamp", timestamp);
        message.setData(bundle);
        bioHarnessHandler.sendMessage(message);

        double SkinTempDbl = generalPacketInfo.GetSkinTemperature(dataArray);
        message = bioHarnessHandler.obtainMessage(BioHarnessConstants.SKIN_TEMPERATURE);
        bundle.putLong("Timestamp", timestamp);
        bundle.putString("SkinTemperature", String.valueOf(SkinTempDbl));
        message.setData(bundle);
        bioHarnessHandler.sendMessage(message);

        int PostureInt = generalPacketInfo.GetPosture(dataArray);
        message = bioHarnessHandler.obtainMessage(BioHarnessConstants.POSTURE);
        bundle.putString("Posture", String.valueOf(PostureInt));
        bundle.putLong("Timestamp", timestamp);
        message.setData(bundle);
        bioHarnessHandler.sendMessage(message);

        double PeakAccDbl = generalPacketInfo.GetPeakAcceleration(dataArray);
        message = bioHarnessHandler.obtainMessage(BioHarnessConstants.PEAK_ACCLERATION);
        bundle.putString("PeakAcceleration", String.valueOf(PeakAccDbl));
        bundle.putLong("Timestamp", timestamp);
        message.setData(bundle);
        bioHarnessHandler.sendMessage(message);
    }
    */

    /*
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
            message.what = BioHarnessConstants.BREATHING_MSG_ID;
            Bundle bundle = new Bundle();

            bundle.putShort("Interval", sample);
            bundle.putLong("Timestamp", timestamp);
            message.setData(bundle);
            bioHarnessHandler.sendMessage(message);
        }
    } */

    /*
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
            message.what = BioHarnessConstants.ACCEL_100mg_MSG_ID;
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
    */
}
