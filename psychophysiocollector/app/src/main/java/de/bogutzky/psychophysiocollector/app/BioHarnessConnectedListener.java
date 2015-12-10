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
    // private AccelerometerPacketInfo AccInfoPacket = new AccelerometerPacketInfo();
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


//                    case GP_MSG_ID:
//                        Message text1;
//                        Bundle b1 = new Bundle();
//                        //***************Displaying the Heart Rate********************************
//                        if(heartRateEnabled) {
//                            int HRate = GPInfo.GetHeartRate(dataArray);
//                            text1 = messageHandler.obtainMessage(HEART_RATE);
//                            b1.putString("HeartRate", String.valueOf(HRate));
//                            b1.putLong("Timestamp", System.currentTimeMillis());
//                            text1.setData(b1);
//                            messageHandler.sendMessage(text1);
//                        }
//
//                        //***************Displaying the Respiration Rate********************************
//                        double RespRate = GPInfo.GetRespirationRate(dataArray);
//
//                        text1 = messageHandler.obtainMessage(RESPIRATION_RATE);
//                        b1.putString("RespirationRate", String.valueOf(RespRate));
//                        b1.putLong("Timestamp", System.currentTimeMillis());
//                        text1.setData(b1);
//                        messageHandler.sendMessage(text1);
//                        //System.out.println("Respiration Rate is "+ RespRate);
//
//                        //***************Displaying the Skin Temperature*******************************
//
//                        if(skinTemperatureEnabled) {
//                            double SkinTempDbl = GPInfo.GetSkinTemperature(dataArray);
//                            text1 = messageHandler.obtainMessage(SKIN_TEMPERATURE);
//                            //Bundle b1 = new Bundle();
//                            b1.putLong("Timestamp", System.currentTimeMillis());
//                            b1.putString("SkinTemperature", String.valueOf(SkinTempDbl));
//                            text1.setData(b1);
//                            messageHandler.sendMessage(text1);
//                        }
//
//                        //***************Displaying the Posture******************************************
//
//                        int PostureInt = GPInfo.GetPosture(dataArray);
//                        text1 = messageHandler.obtainMessage(POSTURE);
//                        b1.putString("Posture", String.valueOf(PostureInt));
//                        b1.putLong("Timestamp", System.currentTimeMillis());
//                        text1.setData(b1);
//                        messageHandler.sendMessage(text1);
//                        //System.out.println("Posture is "+ PostureInt);
//                        //***************Displaying the Peak Acceleration******************************************
//
//                        double PeakAccDbl = GPInfo.GetPeakAcceleration(dataArray);
//                        text1 = messageHandler.obtainMessage(PEAK_ACCLERATION);
//                        b1.putString("PeakAcceleration", String.valueOf(PeakAccDbl));
//                        b1.putLong("Timestamp", System.currentTimeMillis());
//                        text1.setData(b1);
//                        messageHandler.sendMessage(text1);
//                        //System.out.println("Peak Acceleration is "+ PeakAccDbl)
//
//                        break;
//                    case BREATHING_MSG_ID:
//					/*Do what you want. Printing Sequence Number for now*/
//                        //System.out.println("Breathing Packet Sequence Number is "+BreathingInfoPacket.GetSeqNum(dataArray));
//                        break;
//                    case ECG_MSG_ID:
//					/*Do what you want. Printing Sequence Number for now*/
//                        //System.out.println("ECG Packet Sequence Number is "+ECGInfoPacket.GetSeqNum(dataArray));
//                        break;
//                    case RtoR_MSG_ID:
//                        //System.out.println("R to R Packet Sequence Number is "+RtoRInfoPacket.GetSeqNum(dataArray));
//                        processPacketRtoR(dataArray);
//                        break;
//                    case ACCEL_100mg_MSG_ID:
//					/*Do what you want. Printing Sequence Number for now*/
//                        //System.out.println("Accelerometry Packet Sequence Number is "+AccInfoPacket.GetSeqNum(dataArray));
//                        break;
//                    case SUMMARY_MSG_ID:
//					/*Do what you want. Printing Sequence Number for now*/
//                        //System.out.println("Summary Packet Sequence Number is "+SummaryInfoPacket.GetSeqNum(dataArray));
//                        break;

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