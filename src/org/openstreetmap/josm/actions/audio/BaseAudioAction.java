package org.openstreetmap.josm.actions.audio;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Shortcut;

import java.util.stream.Stream;

public abstract class BaseAudioAction extends JosmAction{

    public BaseAudioAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);

        // initial check
        setEnabled(isAudioMarkerPreset());

        // reevaluate presence of audio markers each time layouts change
        Main.getLayerManager().addActiveLayerChangeListener(ev -> setEnabled(isAudioMarkerPreset()));
    }

    protected boolean isAudioMarkerPreset() {
        return Main.getLayerManager().getLayers().stream()
                    .filter(l -> l instanceof MarkerLayer)
                    .map(ml -> (MarkerLayer) ml)
                    .flatMap(ml -> Stream.of(ml.data))
                    .anyMatch(m -> m instanceof AudioMarker);
    }
}
