package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

import javax.swing.JPanel;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractDownloadSourcePanel<T> extends JPanel {

    public AbstractDownloadSourcePanel(DownloadSource<T> downloadSource) {
        Objects.requireNonNull(downloadSource);
        this.downloadSource = downloadSource;
    }

    /**
     * The download source of this panel.
     */
    protected DownloadSource<T> downloadSource;

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
    public DownloadSource<T> getDownloadSource() {
        return this.downloadSource;
    }

    /**
     * Saves the current user preferences devoted to the data source.
     */
    public abstract void rememberSettings();

    /**
     * Restores the latest user preferences devoted to the data source.
     */
    public abstract void restoreSettings();

    /**
     * Performs the logic needed in case if the user triggered the download
     * action in {@link DownloadDialog}.
     * @param data The data.
     * @return Returns {@code true} if the required procedure of handling the
     * download action succeeded and {@link DownloadDialog} can be closed, e.g. validation,
     * otherwise {@code false}.
     */
    public abstract boolean handleDownload(Bounds bbox, T data, DownloadSettings settings);

    /**
     * Performs the logic needed in case if the user triggered the cancel
     * action in {@link DownloadDialog}.
     */
    public void handleCancel() {
        // nothing, let download dialog to close
        // override if necessary
    }
}