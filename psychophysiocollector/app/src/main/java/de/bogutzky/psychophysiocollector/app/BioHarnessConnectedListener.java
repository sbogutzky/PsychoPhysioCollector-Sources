package de.bogutzky.psychophysiocollector.app;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import zephyr.android.BioHarnessBT.*;

public class BioHarnessConnectedListener extends ConnectListenerImpl {
    private Handler messageHandler;
    final int GP_MSG_ID = 0x20;
    final int BREATHING_MSG_ID = 0x21;
    final int ECG_MSG_ID = 0x22;
    final int RtoR_MSG_ID = 0x24;
    final int ACCEL_100mg_MSG_ID = 0x2A;
    final int SUMMARY_MSG_ID = 0x2B;

    private final int HEART_RATE = 0x100;
    private final int RESPIRATION_RATE = 0x101;
    private final int SKIN_TEMPERATURE = 0x102;
    private final int POSTURE = 0x103;
    private final int PEAK_ACCLERATION = 0x104;
    private final int RR_INTERVAL = 0x105;

    public boolean isHeartRateEnabled() {
        return heartRateEnabled;
    }

    private boolean heartRateEnabled = false;

    public boolean isSkinTemperatureEnabled() {
        return skinTemperatureEnabled;
    }

    private boolean skinTemperatureEnabled = false;

    /* Creating the different Objects for different types of Packets */
    private GeneralPacketInfo GPInfo = new GeneralPacketInfo();
    // private ECGPacketInfo ECGInfoPacket = new ECGPacketInfo();
    // private BreathingPacketInfo BreathingInfoPacket = new BreathingPacketInfo();
    private RtoRPacketInfo RtoRInfoPacket = new RtoRPacketInfo();
    private AccelerometerPacketInfo accInfoPacket = new AccelerometerPacketInfo();
    // private SummaryPacketInfo SummaryInfoPacket = new SummaryPacketInfo();

    private PacketTypeRequest RqPacketType = new PacketTypeRequest();

    public BioHarnessConnectedListener(Handler handler, Handler _NewHandler) {
        super(handler, null);
        messageHandler = _NewHandler;
    }

    public void Connected(ConnectedEvent<BTClient> eventArgs) {
        System.out.println(String.format("Connected to BioHarness %s.", eventArgs.getSource().getDevice().getName()));
        Message msg = new Message();
        msg.what = 101; //ready msg
        messageHandler.sendMessage(msg);

        /* Use this object to enable or disable the different Packet types */
        RqPacketType.GP_ENABLE = true;
        RqPacketType.RtoR_ENABLE = true;
        RqPacketType.ECG_ENABLE = false;
        RqPacketType.ACCELEROMETER_ENABLE = false;
        RqPacketType.BREATHING_ENABLE = false;
        RqPacketType.LOGGING_ENABLE = true;
        RqPacketType.SUMMARY_ENABLE = true;


        //Creates a new ZephyrProtocol object and passes it the BTComms object
        ZephyrProtocol _protocol = new ZephyrProtocol(eventArgs.getSource().getComms(), RqPacketType);
        //ZephyrProtocol _protocol = new ZephyrProtocol(eventArgs.getSource().getComms(), );
        _protocol.addZephyrPacketEventListener(new ZephyrPacketListener() {
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
                GPInfo.GetTSYear(dataArray),
                GPInfo.GetTSMonth(dataArray),
                GPInfo.GetTSDay(dataArray),
                GPInfo.GetMsofDay(dataArray)
        );
        Message msg;
        Bundle bundle = new Bundle();
        if(heartRateEnabled) {
            int heartRate = GPInfo.GetHeartRate(dataArray);
            msg = messageHandler.obtainMessage(HEART_RATE);
            bundle.putString("HeartRate", String.valueOf(heartRate));
            bundle.putLong("Timestamp", timestamp);
            msg.setData(bundle);
            messageHandler.sendMessage(msg);
        }

        double RespRate = GPInfo.GetRespirationRate(dataArray);
        msg = messageHandler.obtainMessage(RESPIRATION_RATE);
        bundle.putString("RespirationRate", String.valueOf(RespRate));
        bundle.putLong("Timestamp", timestamp);
        msg.setData(bundle);
        messageHandler.sendMessage(msg);

        if(skinTemperatureEnabled) {
            double SkinTempDbl = GPInfo.GetSkinTemperature(dataArray);
            msg = messageHandler.obtainMessage(SKIN_TEMPERATURE);
            bundle.putLong("Timestamp", timestamp);
            bundle.putString("SkinTemperature", String.valueOf(SkinTempDbl));
            msg.setData(bundle);
            messageHandler.sendMessage(msg);
        }

        int PostureInt = GPInfo.GetPosture(dataArray);
        msg = messageHandler.obtainMessage(POSTURE);
        bundle.putString("Posture", String.valueOf(PostureInt));
        bundle.putLong("Timestamp", timestamp);
        msg.setData(bundle);
        messageHandler.sendMessage(msg);

        double PeakAccDbl = GPInfo.GetPeakAcceleration(dataArray);
        msg = messageHandler.obtainMessage(PEAK_ACCLERATION);
        bundle.putString("PeakAcceleration", String.valueOf(PeakAccDbl));
        bundle.putLong("Timestamp", timestamp);
        msg.setData(bundle);
        messageHandler.sendMessage(msg);
    }

    private void processPacketBreath(byte[] dataArray) {

    }

    private void processPacketEcg(byte[] dataArray) {

    }

    private void processPacketAccel(byte[] dataArray) {
        double[] samplesX;
        double[] samplesY;
        double[] samplesZ;
        long timestamp = TimeConverter.timeToEpoch(
                accInfoPacket.GetTSYear(dataArray),
                accInfoPacket.GetTSMonth(dataArray),
                accInfoPacket.GetTSDay(dataArray),
                accInfoPacket.GetMsofDay(dataArray)
        );

        accInfoPacket.UnpackAccelerationData(dataArray);
        samplesX = accInfoPacket.GetX_axisAccnData();
        String[] strSamplesX = new String[samplesX.length];
        samplesY = accInfoPacket.GetY_axisAccnData();
        String[] strSamplesY = new String[samplesY.length];
        samplesZ = accInfoPacket.GetZ_axisAccnData();
        String[] strSamplesZ = new String[samplesZ.length];

        // Convert double values to String
        for (int i = 0; i < samplesX.length; i++) {
            strSamplesX[i] = Double.toString(samplesX[i]);
        }

        for (int i = 0; i < samplesY.length; i++) {
            strSamplesY[i] = Double.toString(samplesY[i]);
        }

        for (int i = 0; i < samplesZ.length; i++) {
            strSamplesZ[i] = Double.toString(samplesZ[i]);
        }
    }



    /**
     * Process all the info retrieved with the RtoR Packet and send them to the DA
     *
     * @param dataArray
     *      The RtoR Packet binary representation
     */
    private short lastRRInterval = 0;
    private void processPacketRtoR(byte[] dataArray) {

        // Extract timestamp
        long timestamp = TimeConverter.timeToEpoch(
                RtoRInfoPacket.GetTSYear(dataArray),
                RtoRInfoPacket.GetTSMonth(dataArray),
                RtoRInfoPacket.GetTSDay(dataArray),
                RtoRInfoPacket.GetMsofDay(dataArray)
        );


        // Extract RtoR Data
        int[] samples = RtoRInfoPacket.GetRtoRSamples(dataArray);

        // Convert values
        for (int sample : samples) {
            short currentRRInterval = (short) sample;
            if (lastRRInterval != currentRRInterval) {
                lastRRInterval = currentRRInterval;
                int rrInterval = Math.abs(lastRRInterval);
                Log.d("BioHarnessCListener", "RR-Interval: " + rrInterval);

                Message rrMessage = new Message();
                rrMessage.what = RR_INTERVAL;
                Bundle rrBundle = new Bundle();
                rrBundle.putInt("rrInterval", rrInterval);
                rrBundle.putLong("Timestamp", timestamp);
                rrMessage.setData(rrBundle);
                messageHandler.sendMessage(rrMessage);
            }
        }
    }
}