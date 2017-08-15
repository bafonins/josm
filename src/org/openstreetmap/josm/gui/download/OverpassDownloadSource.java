package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;

import java.util.Optional;

import static org.openstreetmap.josm.tools.I18n.tr;

public class OverpassDownloadSource implements DownloadSource<String> {


    @Override
    public AbstractDownloadSourcePanel<String> createPanel() {
        return new OverpassDownloadSourcePanel(this);
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

    public static class OverpassDownloadSourcePanel extends AbstractDownloadSourcePanel<String> {

        private JosmTextArea overpassQuery;
        private OverpassQueryList overpassQueryList;

        private static final BooleanProperty OVERPASS_QUERY_LIST_OPENED =
                new BooleanProperty("download.overpass.query-list.opened", false);
        private static final String ACTION_IMG_SUBDIR = "dialogs";

        public OverpassDownloadSourcePanel(OverpassDownloadSource ds) {
            super(ds);
        }

        @Override
        public String getData() {
            return overpassQuery.getText();
        }

        @Override
        public void rememberSettings() {

        }

        @Override
        public void restoreSettings() {

        }

        @Override
        public boolean handleDownload(Bounds bbox, String data, DownloadSettings settings) {
            return false;
        }
    }
}
