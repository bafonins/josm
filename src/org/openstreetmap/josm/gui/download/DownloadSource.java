package org.openstreetmap.josm.gui.download;


import org.openstreetmap.josm.data.Bounds;

public interface DownloadSource<T> {

    /**
     * Creates a panel with GUI specific for the download source.
     * @return Returns {@link AbstractDownloadSourcePanel}.
     */
    AbstractDownloadSourcePanel<T> createPanel();

    /**
     * Downloads the data.
     * @param data The required data for the download source.
     */
    void doDownload(Bounds bbox, T data, DownloadSettings settings);

    /**
     * Returns a string representation of this download source.
     * @return A string representation of this download source.
     */
    String getLabel();

    /**
     * Add a download source to the dialog, see {@link DownloadDialog}.
     * @param dialog The download dialog.
     */
    void addGui(final DownloadDialog dialog);

    /**
     * Defines whether this download source should be visible only in the expert mode.
     * @return Returns {@code true} if the download source should be visible only in the
     * expert mode, {@code false} otherwise.
     */
    boolean onlyExpert();
}
