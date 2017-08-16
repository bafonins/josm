package org.openstreetmap.josm.gui.download;

public class DownloadSettings {

    private boolean downloadAsNewLayer;
    private boolean zoomToDownloadedData;

    public DownloadSettings(boolean downloadAsNewLayer, boolean zoomToDownloadedData) {
        this.downloadAsNewLayer = downloadAsNewLayer;
        this.zoomToDownloadedData = zoomToDownloadedData;
    }

    public boolean asNewLayer() {
        return this.downloadAsNewLayer;
    }

    public boolean zoomToData() {
        return this.zoomToDownloadedData;
    }
}