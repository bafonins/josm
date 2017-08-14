package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.tools.GBC;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Optional;

import static org.openstreetmap.josm.tools.I18n.tr;

public class OSMDownloadSource implements DownloadSource<Object> {

    @Override
    public AbstractDownloadSourcePanel<Object> createPanel() {
        return new OSMDownloadSourcePanel();
    }

    @Override
    public void doDownload(Bounds bbox, Object data, DownloadSettings settings) {
        // TODO: implement download
    }

    @Override
    public String getLabel() {
        return tr("Download from OSM");
    }

    @Override
    public void addGui(DownloadDialog dialog) {
        dialog.addDownloadSource(createPanel(), getLabel());
    }

    class OSMDownloadSourcePanel extends AbstractDownloadSourcePanel<Object> {

        private Bounds currentBounds;

        private final JCheckBox cbDownloadOsmData;
        private final JCheckBox cbDownloadGpxData;
        private final JCheckBox cbDownloadNotes;
        private final JLabel sizeCheck = new JLabel();

        private final BooleanProperty DOWNLOAD_OSM = new BooleanProperty("download.osm.data", true);
        private final BooleanProperty DOWNLOAD_GPS = new BooleanProperty("download.osm.gps", false);
        private final BooleanProperty DOWNLOAD_NOTES = new BooleanProperty("download.osm.notes", false);

        public OSMDownloadSourcePanel() {
            super();
            setLayout(new GridBagLayout());

            // size check depends on selected data source
            final ChangeListener checkboxChangeListener = e -> updateSizeCheck();

            // adding the download tasks
            add(new JLabel(tr("Data Sources and Types:")), GBC.std().insets(5, 5, 1, 5).anchor(GBC.CENTER));
            cbDownloadOsmData = new JCheckBox(tr("OpenStreetMap data"), true);
            cbDownloadOsmData.setToolTipText(tr("Select to download OSM data in the selected download area."));
            cbDownloadOsmData.getModel().addChangeListener(checkboxChangeListener);
            cbDownloadGpxData = new JCheckBox(tr("Raw GPS data"));
            cbDownloadGpxData.setToolTipText(tr("Select to download GPS traces in the selected download area."));
            cbDownloadGpxData.getModel().addChangeListener(checkboxChangeListener);
            cbDownloadNotes = new JCheckBox(tr("Notes"));
            cbDownloadNotes.setToolTipText(tr("Select to download notes in the selected download area."));
            cbDownloadNotes.getModel().addChangeListener(checkboxChangeListener);

            add(cbDownloadOsmData, GBC.std().insets(1, 5, 1, 5));
            add(cbDownloadGpxData, GBC.std().insets(1, 5, 1, 5));
            add(cbDownloadNotes, GBC.eol().insets(1, 5, 1, 5));
            
            Font labelFont = sizeCheck.getFont();
            sizeCheck.setFont(labelFont.deriveFont(Font.PLAIN, labelFont.getSize()));

            add(sizeCheck, GBC.eol().anchor(GBC.EAST).insets(5, 5, 5, 2));
        }

        @Override
        public DownloadSource<Object> getDownloadSource() {
            return OSMDownloadSource.this;
        }

        @Override
        public void rememberSettings() {
            DOWNLOAD_OSM.put(isDownloadOsmData());
            DOWNLOAD_GPS.put(isDownloadGpxData());
            DOWNLOAD_NOTES.put(isDownloadNotes());
        }

        @Override
        public void restoreSettings() {
            cbDownloadOsmData.setSelected(DOWNLOAD_OSM.get());
            cbDownloadGpxData.setSelected(DOWNLOAD_GPS.get());
            cbDownloadNotes.setSelected(DOWNLOAD_NOTES.get());
        }

        @Override
        public Optional<Bounds> getSelectedDownloadArea() {
            return Optional.ofNullable(this.currentBounds);
        }

        @Override
        boolean handleDownload(Bounds bbox, Object data, DownloadSettings settings) {
            /*
             * It is mandatory to specify the area to download from OSM.
             */
            if (bbox == null) {
                JOptionPane.showMessageDialog(
                        this.getParent(),
                        tr("Please select a download area first."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );

                return false;
            }

            /*
             * Checks if the user selected the type of data to download. At least one the following
             * must be chosen : raw osm data, gpx data, notes.
             * If none of those are selected, then the corresponding dialog is shown to inform the user.
             */
            if (!isDownloadOsmData() && !isDownloadGpxData() && !isDownloadNotes()) {
                JOptionPane.showMessageDialog(
                        this.getParent(),
                        tr("<html>Neither <strong>{0}</strong> nor <strong>{1}</strong> nor <strong>{2}</strong> is enabled.<br>"
                                        + "Please choose to either download OSM data, or GPX data, or Notes, or all.</html>",
                                cbDownloadOsmData.getText(),
                                cbDownloadGpxData.getText(),
                                cbDownloadNotes.getText()
                        ),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );

                return false;
            }

            this.rememberSettings();

            return true;
        }

        /**
         * Replies true if the user selected to download OSM data
         *
         * @return true if the user selected to download OSM data
         */
        public boolean isDownloadOsmData() {
            return cbDownloadOsmData.isSelected();
        }

        /**
         * Replies true if the user selected to download GPX data
         *
         * @return true if the user selected to download GPX data
         */
        public boolean isDownloadGpxData() {
            return cbDownloadGpxData.isSelected();
        }

        /**
         * Replies true if user selected to download notes
         *
         * @return true if user selected to download notes
         */
        public boolean isDownloadNotes() {
            return cbDownloadNotes.isSelected();
        }

        protected void updateSizeCheck() {
            boolean isAreaTooLarge = false;
            if (currentBounds == null) {
                sizeCheck.setText(tr("No area selected yet"));
                sizeCheck.setForeground(Color.darkGray);
            } else if (isDownloadNotes() && !isDownloadOsmData() && !isDownloadGpxData()) {
                // see max_note_request_area in
                // https://github.com/openstreetmap/openstreetmap-website/blob/master/config/example.application.yml
                isAreaTooLarge = currentBounds.getArea() > Main.pref.getDouble("osm-server.max-request-area-notes", 25);
            } else {
                // see max_request_area in
                // https://github.com/openstreetmap/openstreetmap-website/blob/master/config/example.application.yml
                isAreaTooLarge = currentBounds.getArea() > Main.pref.getDouble("osm-server.max-request-area", 0.25);
            }
            displaySizeCheckResult(isAreaTooLarge);
        }

        protected void displaySizeCheckResult(boolean isAreaTooLarge) {
            if (isAreaTooLarge) {
                sizeCheck.setText(tr("Download area too large; will probably be rejected by server"));
                sizeCheck.setForeground(Color.red);
            } else {
                sizeCheck.setText(tr("Download area ok, size probably acceptable to server"));
                sizeCheck.setForeground(Color.darkGray);
            }
        }
    }
}
