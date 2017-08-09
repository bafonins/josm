package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

import javax.swing.JPanel;
import java.util.Optional;

public abstract class AbstractDownloadSourcePanel<T> extends JPanel {

    /**
     * The data for {@link DownloadSource} represented by this panel.
     */
    protected T data;

    /**
     * Gets the data.
     * @return Returns the data.
     */
    public abstract T getData();

    /**
     * Gets the download source of this panel.
     * @return Returns the download source of this panel.
     */
    public abstract DownloadSource<T> getDownloadSource();

    /**
     * Saves the current user preferences devoted to the data source.
     */
    public abstract void rememberSettings();

    /**
     * Restores the latest user preferences devoted to the data source.
     */
    public abstract void restoreSettings();

    /**
     * Returns The currently selected download area.
     * Note, that not every {@link DownloadSource} might need the bbox to proceed,
     * because of that it is optional.
     * @return An {@link Optional} of the currently selected download area.
     */
    public abstract Optional<Bounds> getSelectedDownloadArea();

    /**
     * Returns The previously saved bounding box from preferences for the data source.
     * @return An {@link Optional} of the saved bounding box in preferences.
     */
    public static Optional<Bounds> getSavedDownloadBounds() {
        return Optional.empty();
    }

}
