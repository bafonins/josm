// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.gui;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Represents a menu containing a popup menu.
 */
public class JosmMenu extends JMenu {

    /**
     * Constructs a new instance of {@link JosmMenu}.
     * @param label The label to be displayed on the menu.
     */
    public JosmMenu(String label) {
        super(label);
    }

    /**
     * Gets a stream of {@link JMenuItem} objects contained within this menu.
     * @return A stream of {@link JMenuItem} contained within this menu.
     */
    public Stream<JMenuItem> streamMenuItems() {
        return IntStream.range(0, this.getItemCount())
                .mapToObj(this::getItem)
                .filter(Objects::nonNull)
                .filter(JMenuItem.class::isInstance);
    }

    /**
     * Adds a listener to the underlying {@link javax.swing.JPopupMenu} to react
     * on {@link JMenuItem} addition/removal. Mainly, adds {@link PropertyChangeListener} to every
     * item that disables the menu if none of containing items are enabled.
     */
    public void addPopupContainerListener() {
        enabledPropertyChangeListener(null); // initial check

        super.getPopupMenu().addContainerListener(new ContainerListener() {

            /**
             * Keeps references to all listeners that were added, in order to be able to
             * delete them later if needed.
             */
            private final List<PropertyChangeListener> listeners = new ArrayList<>();

            @Override
            public void componentAdded(ContainerEvent e) {
                Component child = e.getChild();
                if (child != null && child instanceof JMenuItem) {
                    PropertyChangeListener l = ev -> enabledPropertyChangeListener(ev);
                    listeners.add(l);
                    child.addPropertyChangeListener("enabled", l);

                    enabledPropertyChangeListener(null);
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                Component child = e.getChild();
                if (child != null && child instanceof JMenuItem) {
                    PropertyChangeListener[] ls = child.getPropertyChangeListeners("enabled");
                    Arrays.stream(ls)
                            .filter(this.listeners::contains)
                            .findFirst()
                            .ifPresent(l -> {
                                this.listeners.remove(l);
                                child.removePropertyChangeListener("enabled", l);

                                enabledPropertyChangeListener(null);
                            });
                }
            }
        });
    }

    private void enabledPropertyChangeListener(PropertyChangeEvent ev) {
        boolean enable = this.streamMenuItems().anyMatch(Component::isEnabled);
        this.setEnabled(enable);
    }
}
