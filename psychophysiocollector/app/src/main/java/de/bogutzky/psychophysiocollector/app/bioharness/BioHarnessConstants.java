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

package de.bogutzky.psychophysiocollector.app.bioharness;

public class BioHarnessConstants {
    public static final int BH_READY = 101;
    public static final int RtoR_MSG_ID = 0x24;
    public static final int GP_MSG_ID = 0x20;
    public static final int BREATHING_MSG_ID = 0x21;
    public static final int ECG_MSG_ID = 0x22;
    public static final int ACCEL_100mg_MSG_ID = 0x2A;
    public static final int SUMMARY_MSG_ID = 0x2B;

    public static final int POSTURE = 0x103;
    public static final int HEART_RATE = 0x100;
    public static final int RESPIRATION_RATE = 0x101;
    public static final int SKIN_TEMPERATURE = 0x102;
    public static final int PEAK_ACCELERATION = 0x104;

    public static final int RtoR_STORE_ID = 0;
    public static final int GP_STORE_ID = 4;
    public static final int BREATHING_STORE_ID = 2;
    public static final int ECG_STORE_ID = 1;
    public static final int ACCEL_STORE_ID = 3;
    public static final int SUMMARY_STORE_ID = 5;
}
