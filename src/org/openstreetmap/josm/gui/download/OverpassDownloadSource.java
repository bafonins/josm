package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

import java.util.Optional;

import static org.openstreetmap.josm.tools.I18n.tr;

public class OverpassDownloadSource implements DownloadSource<String> {


    @Override
    public AbstractDownloadSourcePanel<String> createPanel() {
        return new OverpassDownloadSourcePanel();
    }

    @Override
    public void doDownload(Bounds bbox, String data, DownloadSettings settings) {
        // TODO: implement this
    }

    @Override
    public String getLabel() {
        return tr("Download from Overpass API");
    }

    @Override
    public void addGui(DownloadDialog dialog) {
        dialog.addDownloadSource(createPanel(), getLabel());
    }

    class OverpassDownloadSourcePanel extends AbstractDownloadSourcePanel<String> {

        @Override
        public DownloadSource<String> getDownloadSource() {
            return OverpassDownloadSource.this;
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
