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
}
