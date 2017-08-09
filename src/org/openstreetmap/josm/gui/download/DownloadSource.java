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
     * @param bbox The bounding box within which the data is considered.
     * @param data The required data for the download source.
     * @param settings The settings of the download task.
     */
    void doDownload(Bounds bbox, T data, DownloadSettings settings);
}
