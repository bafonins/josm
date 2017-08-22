// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.AbstractDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Pair;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Class defines the way data is fetched from the OSM server.
 */
public class OSMDownloadSource implements DownloadSource<OSMDownloadSource.OSMDownloadData> {

    @Override
    public AbstractDownloadSourcePanel<OSMDownloadData> createPanel() {
        return new OSMDownloadSourcePanel(this);
    }

    @Override
    public void doDownload(Bounds bbox, OSMDownloadData data, DownloadSettings settings) {
        boolean zoom = settings.zoomToData();
        boolean newLayer = settings.asNewLayer();
        List<Pair<AbstractDownloadTask<?>, Future<?>>> tasks = new ArrayList<>();

        if (data.isDownloadOSMData()) {
            DownloadOsmTask task = new DownloadOsmTask();
            task.setZoomAfterDownload(zoom && !data.isDownloadGPX() && !data.isDownloadNotes());
            Future<?> future = task.download(newLayer, bbox, null);
            Main.worker.submit(new PostDownloadHandler(task, future));
            if (zoom) {
                tasks.add(new Pair<>(task, future));
            }
        }

        if (data.isDownloadGPX()) {
            DownloadGpsTask task = new DownloadGpsTask();
            task.setZoomAfterDownload(zoom && !data.isDownloadOSMData() && !data.isDownloadNotes());
            Future<?> future = task.download(newLayer, bbox, null);
            Main.worker.submit(new PostDownloadHandler(task, future));
            if (zoom) {
                tasks.add(new Pair<>(task, future));
            }
        }

        if (data.isDownloadNotes()) {
            DownloadNotesTask task = new DownloadNotesTask();
            task.setZoomAfterDownload(zoom && !data.isDownloadOSMData() && !data.isDownloadGPX());
            Future<?> future = task.download(false, bbox, null);
            Main.worker.submit(new PostDownloadHandler(task, future));
            if (zoom) {
                tasks.add(new Pair<>(task, future));
            }
        }

        if (zoom && tasks.size() > 1) {
            Main.worker.submit(() -> {
                ProjectionBounds bounds = null;
                // Wait for completion of download jobs
                for (Pair<AbstractDownloadTask<?>, Future<?>> p : tasks) {
                    try {
                        p.b.get();
                        ProjectionBounds b = p.a.getDownloadProjectionBounds();
                        if (bounds == null) {
                            bounds = b;
                        } else if (b != null) {
                            bounds.extend(b);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Main.warn(ex);
                    }
                }
                // Zoom to the larger download bounds
                if (Main.map != null && bounds != null) {
                    final ProjectionBounds pb = bounds;
                    GuiHelper.runInEDTAndWait(() -> Main.map.mapView.zoomTo(new ViewportData(pb)));
                }
            });
        }
    }

    @Override
    public String getLabel() {
        return tr("Download from OSM");
    }

    @Override
    public void addGui(DownloadDialog dialog) {
        dialog.addDownloadSource(this);
    }

    @Override
    public boolean onlyExpert() {
        return false;
    }

    /**
     * The GUI representation of the OSM download source.
     */
    public static class OSMDownloadSourcePanel extends AbstractDownloadSourcePanel<OSMDownloadData> {

        private final JCheckBox cbDownloadOsmData;
        private final JCheckBox cbDownloadGpxData;
        private final JCheckBox cbDownloadNotes;

        private static final BooleanProperty DOWNLOAD_OSM = new BooleanProperty("download.osm.data", true);
        private static final BooleanProperty DOWNLOAD_GPS = new BooleanProperty("download.osm.gps", false);
        private static final BooleanProperty DOWNLOAD_NOTES = new BooleanProperty("download.osm.notes", false);

        public OSMDownloadSourcePanel(OSMDownloadSource ds) {
            super(ds);
            setLayout(new GridBagLayout());

            // adding the download tasks
            add(new JLabel(tr("Data Sources and Types:")), GBC.std().insets(5, 5, 1, 5).anchor(GBC.CENTER));
            cbDownloadOsmData = new JCheckBox(tr("OpenStreetMap data"), true);
            cbDownloadOsmData.setToolTipText(tr("Select to download OSM data in the selected download area."));

            cbDownloadGpxData = new JCheckBox(tr("Raw GPS data"));
            cbDownloadGpxData.setToolTipText(tr("Select to download GPS traces in the selected download area."));

            cbDownloadNotes = new JCheckBox(tr("Notes"));
            cbDownloadNotes.setToolTipText(tr("Select to download notes in the selected download area."));

            add(cbDownloadOsmData, GBC.std().insets(1, 5, 1, 5));
            add(cbDownloadGpxData, GBC.std().insets(1, 5, 1, 5));
            add(cbDownloadNotes, GBC.eol().insets(1, 5, 1, 5));
        }

        @Override
        public OSMDownloadData getData() {
            return new OSMDownloadData(
                    isDownloadOsmData(),
                    isDownloadNotes(),
                    isDownloadGpxData());
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
        public boolean checkDownload(Bounds bbox, DownloadSettings settings) {
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

        @Override
        public Icon getIcon() {
            return ImageProvider.get("download");
        }
    }

    /**
     * Encapsulates data that is required to download from the OSM server.
     */
    static class OSMDownloadData {
        private boolean downloadOSMData;
        private boolean downloadNotes;
        private boolean downloadGPX;

        OSMDownloadData(boolean downloadOSMData, boolean downloadNotes, boolean downloadGPX) {
            this.downloadOSMData = downloadOSMData;
            this.downloadNotes = downloadNotes;
            this.downloadGPX = downloadGPX;
        }

        boolean isDownloadOSMData() {
            return downloadOSMData;
        }

        boolean isDownloadNotes() {
            return downloadNotes;
        }

        boolean isDownloadGPX() {
            return downloadGPX;
        }
    }
}
