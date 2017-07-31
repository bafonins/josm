// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.gui;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.util.function.Consumer;
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

        // TODO: decompose
        if (disable) {
            this.walkMenuItems(it -> it.addPropertyChangeListener("enabled", ev -> {
                boolean enable = this.streamMenuItems().anyMatch(JMenuItem::isEnabled);
                this.setEnabled(enable);
            }));
        }
    }

    /**
     * Applies {@code c} to every {@link JMenuItem} within this menu.
     * @param c A functional interface to be used within this method.
     */
    public void walkMenuItems(Consumer<JMenuItem> c) {
        this.streamMenuItems().forEach(c);
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
}
