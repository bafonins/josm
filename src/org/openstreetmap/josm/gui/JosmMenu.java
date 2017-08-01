// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.gui;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.Component;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Represents a menu with containing a popup menu.
 */
public class JosmMenu extends JMenu {

    /**
     * Constructs a new instance of {@link JosmMenu}.
     * @param label The label to be displayed on the menu.
     */
    public JosmMenu(String label) {
        this(label, false);
    }

    /**
     * Constructs a new instance of {@link JosmMenu}.
     * @param label The label to be displayed on the menu.
     * @param disable The flag defining if a listener must be added to each menu item, such that
     *                the menu becomes disabled when none of the items are enabled.
     */
    public JosmMenu(String label, boolean disable) {
        super(label);

        if (disable) {
            setEnabled(false);
            addPopupContainerListener();
        }
    }

    /**
     * Gets a stream of {@link JMenuItem} objects contained within this menu.
     * @return A stream of {@link JMenuItem} contained within this menu.
     */
    public Stream<JMenuItem> streamMenuItems() {
        return IntStream.range(0, this.getItemCount())
                .mapToObj(this::getItem)
                .filter(JMenuItem.class::isInstance);
    }

    /**
     * Adds a listener to the underlying {@link javax.swing.JPopupMenu} to react
     * on {@link JMenuItem} addition. Mainly, adds {@link PropertyChangeListener} to every
     * item that disables the menu if none of containing items are enabled.
     */
    private void addPopupContainerListener() {
        getPopupMenu().addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                super.componentAdded(e);

                Component child = e.getChild();
                if (child != null && child instanceof JMenuItem) {
                    child.addPropertyChangeListener("enabled", ev -> enabledPropertyChangeListener(ev));

                    boolean menuEnabled = isEnabled();
                    setEnabled(menuEnabled || child.isEnabled());
                }
            }
        });
    }

    private void enabledPropertyChangeListener(PropertyChangeEvent ev) {
        boolean enable = this.streamMenuItems().anyMatch(Component::isEnabled);
        this.setEnabled(enable);
    }
}
