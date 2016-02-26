package de.bogutzky.psychophysiocollector.app.bioharness;

import java.io.File;

public interface BioHarnessHandlerInterface {

    String getHeaderComments();
    String getFooterComments();
    File getStorageDirectory(String directoryName);
}
