package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

import java.util.Optional;

/**
 * This class defines the basis of any data source that requires interraction with
 * bounding boxes.
 */
public abstract class BoundingBoxDataSource extends DownloadDataSource {

    /**
     * Returns The currently selected download area.
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
