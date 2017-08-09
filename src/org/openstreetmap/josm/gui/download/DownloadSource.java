package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

public interface DownloadSource<T> {

    /**
     * Creates a panel with GUI specific for the download source.
     * @return Returns {@link AbstractDownloadSourcePanel}.
     */
    AbstractDownloadSourcePanel<T> createPanel();

    /**
     * Performs the logic needed in case if the user triggered the download
     * action in {@link DownloadDialog}.
     * @param bbox The bounding box.
     * @param data The data.
     * @return Returns {@code true} if the required procedure of handling the
     * download action succeeded and {@link DownloadDialog} can be closed, e.g. validation,
     * otherwise {@code false}.
     */
    boolean handleDownload(Bounds bbox, T data);

    /**
     * Performs the logic needed in case if the user triggered the cancel
     * action in {@link DownloadDialog}.
     */
    void handleCancel();


}
