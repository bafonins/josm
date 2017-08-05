// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Base class for every action related to audio content.
 */
public abstract class BaseAudioAction extends JosmAction {

    public BaseAudioAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);

        // initial check
        setEnabled(isAudioMarkerPresent());
    }

    /**
     * Checks if there is at least one {@link AudioMarker} is present in the current layout.
     * @return {@code true} if at least one {@link AudioMarker} is present in the current
     * layout, {@code false} otherwise.
     */
    protected boolean isAudioMarkerPresent() {
        return Main.getLayerManager().getLayers().stream()
                .filter(l -> l instanceof MarkerLayer)
                .map(ml -> (MarkerLayer) ml)
                .flatMap(ml -> ml.data.stream())
                .anyMatch(m -> m instanceof AudioMarker);
    }

    @Override
    protected void updateEnabledState() {
        super.updateEnabledState();

        boolean enabled = this.isAudioMarkerPresent();
        setEnabled(enabled);
    }
}
