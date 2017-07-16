package org.openstreetmap.josm.gui;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.widgets.SearchTextResultListPanel;
import org.openstreetmap.josm.tools.Utils;

import javax.swing.JCheckBox;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    private final Set<SelectorItem> items;

    /*
     * Save preferences
     */
    private static final String PREFERENCES_SNIPPET = "download.overpass.query.snippet";
    private static final String PREFERENCES_HISTORY = "download.overpass.query.history";
    private static final BooleanProperty SHOW_ONLY_SNIPPETS = new BooleanProperty("download.overpass.only-snippets", false);
    private static final BooleanProperty SHOW_ONLY_HISTORY = new BooleanProperty("download.overpass.only-history", false);

    /**
     * Constructs a new {@code OverpassQuerySelector}.
     * @param showOnlySnippets if {@code true} the "Show only shippets" checkbox is checked and
     *                         only the users saved snippets are displayed.
     * @param showOnlyHistory if {@code true} the "Show only history" checkbox is checked and
     *                        only the users executed queries are displayed.
     */
    public OverpassQuerySelector(boolean showOnlySnippets, boolean showOnlyHistory) {
        this.onlySnippets = new JCheckBox(tr("Show only snippets"), showOnlySnippets);
        this.onlyHistory = new JCheckBox(tr("Show only history"), showOnlyHistory);
        this.items = this.restorePreferences();
    }

    @Override
    protected void filterItems() {
        String text = edSearchText.getText().toLowerCase(Locale.ENGLISH);
        boolean onlySnippets = this.onlySnippets.isSelected();
        boolean onlyHistory = this.onlyHistory.isSelected();

    }

    /**
     * Loads the user saved items from {@link Main#pref}.
     * @return A set of the user saved items.
     */
    private Set<SelectorItem> restorePreferences() {
        List<UserSnippet> snippets = new ArrayList<>(
                Main.pref.getListOfStructs(PREFERENCES_SNIPPET, UserSnippet.class)
        );
        List<UserHistory> history = new ArrayList<>(
                Main.pref.getListOfStructs(PREFERENCES_HISTORY, UserHistory.class)
        );

        return Stream.of(snippets, history)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    class SelectorItem {
        private final String itemKey;
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
    }

    class UserHistory extends SelectorItem implements Comparable<UserHistory> {

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
    }
}
