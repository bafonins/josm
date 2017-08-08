package org.openstreetmap.josm.gui.download;

import javax.swing.JPanel;

/**
 * This abstract class defines the basis of any data source that is used and might be
 * used by JOSM in the future.
 */
public abstract class DownloadDataSource extends JPanel {

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
     * @return Returns {@code true} if the required logic succeeded, e.g. validation,
     * otherwise {@code false}.
     */
    public boolean handleDownloadAction() {
        return true;
    }

    /**
     * Performs the logic needed in case if the user triggered the cancel
     * action in {@link DownloadDialog}.
     */
    public void handleCancelAction() {

    }
}
