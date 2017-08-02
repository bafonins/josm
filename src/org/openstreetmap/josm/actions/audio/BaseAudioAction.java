// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Shortcut;

public abstract class BaseAudioAction extends JosmAction {

    public BaseAudioAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);

        // initial check
        setEnabled(isAudioMarkerPreset());

        // reevaluate presence of audio markers each time layouts change
        Main.getLayerManager().addLayerChangeListener(getLayerChangeListener());
    }

    protected boolean isAudioMarkerPreset() {
        return Main.getLayerManager().getLayers().stream()
                .filter(l -> l instanceof MarkerLayer)
                .map(ml -> (MarkerLayer) ml)
                .flatMap(ml -> ml.data.stream())
                .anyMatch(m -> m instanceof AudioMarker);
    }

    private LayerManager.LayerChangeListener getLayerChangeListener() {
        return new LayerManager.LayerChangeListener() {
            @Override
            public void layerAdded(LayerManager.LayerAddEvent e) {
                setEnabled(isAudioMarkerPreset());
            }

            @Override
            public void layerRemoving(LayerManager.LayerRemoveEvent e) {
                setEnabled(isAudioMarkerPreset());
            }

            @Override
            public void layerOrderChanged(LayerManager.LayerOrderChangeEvent e) {
                // nothing
            }
        };
    }
}
