package de.bogutzky.psychophysiocollector.app.data.management;

import java.io.File;

/**
 * Created by on 28.12.2015.
 */
public class WriteDataTaskParams {
    File root;
    String filename;
    Double[][] data;
    int numberOfCols;
    int numberOfRows;
    String batchComments;

    public WriteDataTaskParams(File root, String filename, Double[][] data, int numberOfCols, int numberOfRows, String batchComments) {
        this.root = root;
        this.filename = filename;
        this.data = data;
        this.numberOfCols = numberOfCols;
        this.numberOfRows = numberOfRows;
        this.batchComments = batchComments;
    }
}