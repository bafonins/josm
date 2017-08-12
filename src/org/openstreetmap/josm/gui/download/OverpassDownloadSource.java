package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

import java.util.Optional;

public class OverpassDownloadSource implements DownloadSource<OverpassDownloadSource.OverpassDownloadSettings> {


    @Override
    public AbstractDownloadSourcePanel<OverpassDownloadSettings> createPanel() {
        return null;
    }

    @Override
    public void doDownload(OverpassDownloadSettings data) {

    }

    @Override
    public String getLabel() {
        return null;
    }

    class OverpassDownloadSourcePanel extends AbstractDownloadSourcePanel<OverpassDownloadSettings> {

        @Override
        public DownloadSource<OverpassDownloadSettings> getDownloadSource() {
            return null;
        }

        @Override
        public void rememberSettings() {

        }

        @Override
        public void restoreSettings() {

        }

        @Override
        public Optional<Bounds> getSelectedDownloadArea() {
            return null;
        }

        @Override
        boolean handleDownload(OverpassDownloadSettings data) {
            return false;
        }
    }

    class OverpassDownloadSettings extends DownloadSettings {

        public OverpassDownloadSettings(boolean downloadAsNewLayer, boolean zoomToDownloadedData) {
            super(downloadAsNewLayer, zoomToDownloadedData);
        }
    }
}
