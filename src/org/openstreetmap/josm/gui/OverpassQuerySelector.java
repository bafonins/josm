package org.openstreetmap.josm.gui;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.widgets.SearchTextResultListPanel;
import org.openstreetmap.josm.tools.Utils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * This class defines a panel that is used in
 * {@link org.openstreetmap.josm.actions.OverpassDownloadAction.OverpassDownloadDialog}
 * to save successfully executed overpass queries and allow the users to explicitly save
 * queries.
 */
public class OverpassQuerySelector extends SearchTextResultListPanel<OverpassQuerySelector.SelectorItem> {

    private final JCheckBox onlySnippets;
    private final JCheckBox onlyHistory;
    private final JCheckBox all;
    private final JTextComponent target;

    private final Set<SelectorItem> items;

    /*
     * Save preferences
     */
    private static final String PREFERENCES_SNIPPET = "download.overpass.query.snippet";
    private static final String PREFERENCES_HISTORY = "download.overpass.query.history";
    private static final BooleanProperty SHOW_ONLY_SNIPPETS = new BooleanProperty("download.overpass.only-snippets", false);
    private static final BooleanProperty SHOW_ONLY_HISTORY = new BooleanProperty("download.overpass.only-history", false);
    private static final BooleanProperty SHOW_ALL = new BooleanProperty("download.overpass.all", true);

    /**
     * Constructs a new {@code OverpassQuerySelector}.
     * @param target TODO
     */
    public OverpassQuerySelector(JTextComponent target) {
        this.onlySnippets = new JCheckBox(tr("Show only snippets"), SHOW_ONLY_SNIPPETS.get());
        this.onlyHistory = new JCheckBox(tr("Show only history"), SHOW_ONLY_HISTORY.get());
        this.all = new JCheckBox(tr("Show all"), SHOW_ALL.get());
        this.target = target;
        this.items = this.restorePreferences();

        ButtonGroup group = new ButtonGroup();
        group.add(this.onlyHistory);
        group.add(this.onlySnippets);
        group.add(this.all);

        JPanel filterOptions = new JPanel();
        filterOptions.setLayout(new BoxLayout(filterOptions, BoxLayout.Y_AXIS));
        filterOptions.add(this.all);
        filterOptions.add(this.onlySnippets);
        filterOptions.add(this.onlyHistory);

        super.lsResult.setCellRenderer(new OverpassQueryCellRendered());
        super.add(filterOptions, BorderLayout.NORTH);
        super.setDblClickListener(e -> {
            Optional<SelectorItem> selectedItem = this.getSelectedItem();

            if (!selectedItem.isPresent()) {
                return;
            }

            SelectorItem item = selectedItem.get();
            this.target.setText(item.getQuery());
        });

        filterItems();

        // TODO: add listeners

    }

    public synchronized Optional<SelectorItem> getSelectedItem() {
        int idx = lsResult.getSelectedIndex();
        if (lsResultModel.isEmpty() || (idx < 0 && idx > lsResultModel.getSize())) {
            return Optional.empty();
        }

        SelectorItem item = lsResultModel.getElementAt(idx);

        if (item instanceof UserHistory) {
            UserHistory history = (UserHistory) item;
            history.changeLastDateTimeToNow();
        } else {
            UserSnippet snippet = (UserSnippet) item;
            snippet.incrementUseCount();
        }

        filterItems();

        return Optional.of(item);
    }

    @Override
    public void setDblClickListener(ActionListener dblClickListener) {
        // this listener is already set within this class
    }

    @Override
    protected void filterItems() {
        String text = edSearchText.getText().toLowerCase(Locale.ENGLISH);
        boolean onlySnippets = this.onlySnippets.isSelected();
        boolean onlyHistory = this.onlyHistory.isSelected();

        super.lsResultModel.setItems(this.items.stream()
                .filter(item -> (item instanceof UserSnippet) && onlySnippets ||
                                (item instanceof UserHistory) && onlyHistory ||
                                (!onlyHistory && !onlySnippets))
                .filter(item -> item.itemKey.contains(text))
                .collect(Collectors.toList()));
    }

    /**
     * Loads the user saved items from {@link Main#pref}.
     * @return A set of the user saved items.
     */
    private Set<SelectorItem> restorePreferences() {
//        List<UserSnippet> snippets = new ArrayList<>(Main.pref
//                .getListOfStructs(PREFERENCES_SNIPPET, UserSnippet.class));
//        List<UserHistory> history = new ArrayList<>(Main.pref
//                .getListOfStructs(PREFERENCES_HISTORY, UserHistory.class));
//
//        return Stream.of(snippets, history)
//                .flatMap(Collection::stream)
//                .collect(Collectors.toSet());
        return new HashSet<>(Arrays.asList(new UserHistory("history #1", "test query", LocalDateTime.now()),
                new UserHistory("history #2", "test query2", LocalDateTime.now()),
                new UserSnippet("snipper #1", "test query 3", 1)));
    }

    private static class OverpassQueryCellRendered extends JLabel implements ListCellRenderer<SelectorItem> {

        public OverpassQueryCellRendered() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends SelectorItem> list,
                SelectorItem value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setFont(list.getFont());
            setEnabled(list.isEnabled());
            setText(value.getKey());


            if (isSelected && cellHasFocus) {
                setBorder(new CompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 2),
                        BorderFactory.createEmptyBorder(2, 0, 2, 0)));
            } else {
                setBorder(new CompoundBorder(
                        null,
                        BorderFactory.createEmptyBorder(2, 0, 2, 0)));
            }

            return this;
        }
    }

    class SelectorItem {
        @Preferences.pref
        private final String itemKey;
        @Preferences.pref
        private final String query;

        /**
         * Constructs a new {@code SelectorItem}.
         * @param key The key of this item.
         * @param query The query of the item.
         * @exception NullPointerException if any parameter is {@code null}.
         * @exception IllegalArgumentException if any parameter is empty.
         */
        public SelectorItem(String key, String query) {
            super();
            Objects.requireNonNull(key);
            Objects.requireNonNull(query);

            if (Utils.isStripEmpty(key)) {
                throw new IllegalArgumentException("The key of the item cannot be empty");
            }
            if (Utils.isStripEmpty(query)) {
                throw new IllegalArgumentException("The query cannot be empty");
            }

            this.itemKey = key;
            this.query = query;
        }

        /**
         * Gets the key (a string that is displayed in the selector) of this item.
         * @return A string representing the key of this item.
         */
        public String getKey() {
            return this.itemKey;
        }

        /**
         * Gets the overpass query of this item.
         * @return A string representing the overpass query of this item.
         */
        public String getQuery() {
            return this.query;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SelectorItem)) return false;

            SelectorItem that = (SelectorItem) o;

            if (!itemKey.equals(that.itemKey)) return false;
            return getQuery().equals(that.getQuery());
        }

        @Override
        public int hashCode() {
            int result = itemKey.hashCode();
            return result;
        }
    }

    class UserSnippet extends SelectorItem implements Comparable<UserSnippet> {
        @Preferences.pref
        private int useCount;

        public UserSnippet(String key, String query, int useCount) {
            super(key, query);
            this.useCount = useCount;
        }

        public int getUseCount() {
            return this.useCount;
        }

        public int incrementUseCount() {
            return ++this.useCount;
        }

        @Override
        public int compareTo(UserSnippet o) {
            return o.getUseCount() - this.getUseCount();
        }

        @Override
        public String toString() {
            return "UserSnippet{" +
                    "itemKey='" + this.getKey() + "\',\n" +
                    "query='" + this.getQuery() + "\',\n" +
                    "useCount=" + useCount +
                    '}';
        }
    }

    class UserHistory extends SelectorItem implements Comparable<UserHistory> {

        @Preferences.pref
        private LocalDateTime lastUse;

        public UserHistory(String key, String query, LocalDateTime lastUseDateTime) {
            super(key, query);
            this.lastUse = lastUseDateTime;
        }

        @Override
        public int compareTo(UserHistory o) {
            return this.getLastUseDateTime().compareTo(o.getLastUseDateTime());
        }

        /**
         * Gets the date when the history item was used for the last time.
         * @return The date when the item was used for the last time.
         */
        public LocalDateTime getLastUseDateTime() {
            return this.lastUse;
        }

        /**
         * Transforms a {@link UserHistory} item into a {@link UserSnippet} object.
         * Can be used if the user decides to make a snippet out of already used query which
         * already is represented in the selector, but as the history item.
         * @return A new {@link UserSnippet} object with initial usage count equal to 1.
         */
        public UserSnippet toUserSnippet() {
            return new UserSnippet(
                    this.getKey(),
                    this.getQuery(),
                    1
            );
        }

        public void changeLastDateTimeToNow() {
            this.lastUse = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return "UserHistory{" +
                    "itemKey='" + this.getKey() + "\',\n" +
                    ", query='" + this.getQuery() + "\',\n" +
                    ", lastUse=" + lastUse +
                    '}';
        }
    }
}
