package de.bogutzky.psychophysiocollector.app;

import java.io.File;

/**
 * Created by on 28.12.2015.
 */
public class WriteDataTaskParams {
    String filename;
    Double[][] values;
    File root;
    int numberOfFields;
    String batchComments;

    public WriteDataTaskParams(Double[][] values, String filename, File root, int numberOfFields, String batchComments) {
        this.filename = filename;
        this.values = values;
        this.root = root;
        this.numberOfFields = numberOfFields;
        this.batchComments = batchComments;
    }
}