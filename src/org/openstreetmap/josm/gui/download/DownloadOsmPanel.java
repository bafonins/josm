package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
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

public class DownloadOsmPanel extends BoundingBoxDataSource {

    private static final BooleanProperty DOWNLOAD_AUTORUN = new BooleanProperty("download.osm.autorun", false);
    private static final BooleanProperty DOWNLOAD_OSM = new BooleanProperty("download.osm.osmdata", true);
    private static final BooleanProperty DOWNLOAD_GPS = new BooleanProperty("download.osm.gps", false);
    private static final BooleanProperty DOWNLOAD_NOTES = new BooleanProperty("download.osm.notes", false);
    private static final BooleanProperty DOWNLOAD_NEWLAYER = new BooleanProperty("download.osm.newlayer", false);
    private static final BooleanProperty DOWNLOAD_ZOOMTODATA = new BooleanProperty("download.osm.zoomtodata", true);

    protected final JCheckBox cbDownloadOsmData;
    protected final JCheckBox cbDownloadGpxData;
    protected final JCheckBox cbDownloadNotes;

    protected final JCheckBox cbNewLayer;
    protected final JCheckBox cbStartup;
    protected final JCheckBox cbZoomToDownloadedData;

    protected final JLabel sizeCheck = new JLabel();

    private Bounds currentBounds;

    public DownloadOsmPanel() {
        setLayout(new GridBagLayout());

        // size check depends on selected data source
        final ChangeListener checkboxChangeListener = e -> updateSizeCheck();

        // adding the download tasks
        add(new JLabel(tr("Data Sources and Types:")), GBC.std().insets(5, 5, 1, 5));

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
        add(cbDownloadGpxData, GBC.std().insets(5, 5, 1, 5));
        add(cbDownloadNotes, GBC.eol().insets(50, 5, 1, 5));

        // TODO: add download selection

        cbNewLayer = new JCheckBox(tr("Download as new layer"));
        cbNewLayer.setToolTipText(tr("<html>Select to download data into a new data layer.<br>"
                +"Unselect to download into the currently active data layer.</html>"));
        cbStartup = new JCheckBox(tr("Open this dialog on startup"));
        cbStartup.setToolTipText(
                tr("<html>Autostart ''Download from OSM'' dialog every time JOSM is started.<br>" +
                        "You can open it manually from File menu or toolbar.</html>"));
        cbStartup.addActionListener(e -> DOWNLOAD_AUTORUN.put(cbStartup.isSelected()));
        cbZoomToDownloadedData = new JCheckBox(tr("Zoom to downloaded data"));
        cbZoomToDownloadedData.setToolTipText(tr("Select to zoom to entire newly downloaded data."));

        add(cbNewLayer, GBC.std().anchor(GBC.WEST).insets(5, 5, 5, 5));
        add(cbStartup, GBC.std().anchor(GBC.WEST).insets(15, 5, 5, 5));
        add(cbZoomToDownloadedData, GBC.std().anchor(GBC.WEST).insets(15, 5, 5, 5));

        Font labelFont = sizeCheck.getFont();
        sizeCheck.setFont(labelFont.deriveFont(Font.PLAIN, labelFont.getSize()));

        if (!ExpertToggleAction.isExpert()) {
            JLabel infoLabel = new JLabel(
                    tr("Use left click&drag to select area, arrows or right mouse button to scroll map, wheel or +/- to zoom."));
            add(infoLabel, GBC.eol().anchor(GBC.SOUTH).insets(0, 0, 0, 0));
        }
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

    /**
     * Replies true if the user requires to download into a new layer
     *
     * @return true if the user requires to download into a new layer
     */
    public boolean isNewLayerRequired() {
        return cbNewLayer.isSelected();
    }

    /**
     * Replies true if the user requires to zoom to new downloaded data
     *
     * @return true if the user requires to zoom to new downloaded data
     * @since 11658
     */
    public boolean isZoomToDownloadedDataRequired() {
        return cbZoomToDownloadedData.isSelected();
    }

    /**
     * Determines if the dialog autorun is enabled in preferences.
     * @return {@code true} if the download dialog must be open at startup, {@code false} otherwise
     */
    public static boolean isAutorunEnabled() {
        return DOWNLOAD_AUTORUN.get();
    }

    @Override
    public void rememberSettings() {
        DOWNLOAD_OSM.put(cbDownloadOsmData.isSelected());
        DOWNLOAD_GPS.put(cbDownloadGpxData.isSelected());
        DOWNLOAD_NOTES.put(cbDownloadNotes.isSelected());
        DOWNLOAD_NEWLAYER.put(cbNewLayer.isSelected());
        DOWNLOAD_ZOOMTODATA.put(cbZoomToDownloadedData.isSelected());
        if (currentBounds != null) {
            Main.pref.put("osm-download.bounds", currentBounds.encodeAsString(";"));
        }
    }

    @Override
    public void restoreSettings() {
        cbDownloadOsmData.setSelected(DOWNLOAD_OSM.get());
        cbDownloadGpxData.setSelected(DOWNLOAD_GPS.get());
        cbDownloadNotes.setSelected(DOWNLOAD_NOTES.get());
        cbNewLayer.setSelected(DOWNLOAD_NEWLAYER.get());
        cbStartup.setSelected(DOWNLOAD_AUTORUN.get());
        cbZoomToDownloadedData.setSelected(DOWNLOAD_ZOOMTODATA.get());
    }

    @Override
    public Optional<Bounds> getSelectedDownloadArea() {
        return Optional.ofNullable(currentBounds);
    }

    @Override
    public boolean handleDownloadAction() {
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

        return true;
    }

    /**
     * TODO: finish
     */
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

    /**
     * TODO: finish
     * @param isAreaTooLarge
     */
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
