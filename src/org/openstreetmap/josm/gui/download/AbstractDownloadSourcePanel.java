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
    public T getData() {
        return data;
    }

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

    /**
     * Performs the logic needed in case if the user triggered the download
     * action in {@link DownloadDialog}.
     * @param bbox The bounding box.
     * @param data The data.
     * @param settings The settings of the download task.
     * @return Returns {@code true} if the required procedure of handling the
     * download action succeeded and {@link DownloadDialog} can be closed, e.g. validation,
     * otherwise {@code false}.
     */
    abstract boolean handleDownload(Bounds bbox, T data, DownloadSettings settings);

    /**
     * Performs the logic needed in case if the user triggered the cancel
     * action in {@link DownloadDialog}.
     */
    public void handleCancel() {
        // nothing, let download dialog to close
    }

}
