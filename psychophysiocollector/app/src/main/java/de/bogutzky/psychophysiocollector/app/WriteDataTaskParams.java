package de.bogutzky.psychophysiocollector.app;

import java.io.File;

/**
 * Created by on 28.12.2015.
 */
public class WriteDataTaskParams {
    String filename;
    String[][] values;
    File root;
    int fields;
    boolean footer;
    String footerString;
    int writingSlot;
    MainActivity activity;

    WriteDataTaskParams(String[][] values, String filename, File root, int fields, boolean footer, String footerString, int writingSlot, MainActivity activity) {
        this.filename = filename;
        this.values = values;
        this.root = root;
        this.fields = fields;
        this.footer = footer;
        this.footerString = footerString;
        this.writingSlot = writingSlot;
        this.activity = activity;
    }
}