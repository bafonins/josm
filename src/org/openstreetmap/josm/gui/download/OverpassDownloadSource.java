package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

import java.util.Optional;

public class OverpassDownloadSource implements DownloadSource<String> {


    @Override
    public AbstractDownloadSourcePanel<String> createPanel() {
        return null;
    }

    @Override
    public void doDownload(Bounds bbox, String data, DownloadSettings settings) {

    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public void addGui(DownloadDialog dialog) {

    }

    class OverpassDownloadSourcePanel extends AbstractDownloadSourcePanel<String> {

        @Override
        public DownloadSource<String> getDownloadSource() {
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
        boolean handleDownload(Bounds bbox, String data, DownloadSettings settings) {
            return false;
        }
    }
}
