package org.openstreetmap.josm.gui;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.DefaultTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.SearchTextResultListPanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * A component to select user saved Overpass queries.
 */
public final class OverpassQueryList extends SearchTextResultListPanel<OverpassQueryList.SelectorItem> {

    /*
     * GUI elements
     */
    private final JTextComponent target;
    private final Component componentParent;
    private final JCheckBox onlySnippets = new JCheckBox(tr("Show only snippets"), SHOW_ONLY_SNIPPETS.get());
    private final JCheckBox onlyHistory = new JCheckBox(tr("Show only history"), SHOW_ONLY_HISTORY.get());
    private final JCheckBox all = new JCheckBox(tr("Show all"), SHOW_ALL.get());

    /*
     * All loaded elements within the list.
     */
    private final transient Map<String, SelectorItem> items;

    /*
     * Preferences
     */
    private static final String KEY_KEY = "key";
    private static final String QUERY_KEY = "query";
    private static final String USE_COUNT_KEY = "useCount";
    private static final String LAST_USE_KEY = "lastUse";
    private static final String PREFERENCE_ITEMS = "download.overpass.query";
    private static final BooleanProperty SHOW_ONLY_SNIPPETS =
            new BooleanProperty("download.overpass.only-snippets", false);
    private static final BooleanProperty SHOW_ONLY_HISTORY =
            new BooleanProperty("download.overpass.only-history", false);
    private static final BooleanProperty SHOW_ALL =
            new BooleanProperty("download.overpass.all", true);

    /**
     * Constructs a new {@code OverpassQueryList}.
     * @param parent The parent of this component.
     * @param target The text component to which the queries must be added.
     */
    public OverpassQueryList(Component parent, JTextComponent target) {
        this.target = target;
        this.componentParent = parent;
        this.items = this.restorePreferences();

        initFilterCheckBoxes();

        JPanel filterOptions = new JPanel();
        filterOptions.setLayout(new BoxLayout(filterOptions, BoxLayout.Y_AXIS));
        filterOptions.add(this.all);
        filterOptions.add(this.onlySnippets);
        filterOptions.add(this.onlyHistory);
        super.add(filterOptions, BorderLayout.SOUTH);

        super.lsResult.setCellRenderer(new OverpassQueryCellRendered());
        super.setDblClickListener(this::getDblClickListener);
        super.lsResult.addMouseListener(new OverpassQueryListMouseAdapter(lsResult, lsResultModel));

        filterItems();
    }

    /**
     * Returns currently selected element from the list.
     * @return An {@link Optional#empty()} if nothing is selected, otherwise
     * the idem is returned.
     */
    public synchronized Optional<SelectorItem> getSelectedItem() {
        int idx = lsResult.getSelectedIndex();
        if (lsResultModel.getSize() == 0 || idx == -1) {
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

    /**
     * Removes currently selected item, saves the current state to preferences and
     * updates the view.
     */
    private synchronized void removeSelectedItem() {
        Optional<SelectorItem> it = this.getSelectedItem();

        if (!it.isPresent()) {
            JOptionPane.showMessageDialog(
                    componentParent,
                    tr("Please select an item first"));
            return;
        }

        SelectorItem item = it.get();
        if (this.items.remove(item.getKey(), item)) {
            savePreferences();
            filterItems();
        }
    }

    /**
     * Opens {@link EditItemDialog} for the selected item, saves the current state
     * to preferences and updates the view.
     */
    private synchronized void editSelectedItem() {
        Optional<SelectorItem> it = this.getSelectedItem();

        if (!it.isPresent()) {
            JOptionPane.showMessageDialog(
                    componentParent,
                    tr("Please select an item first"));
            return;
        }

        SelectorItem item = it.get();

        EditItemDialog dialog = new EditItemDialog(
                componentParent,
                tr("Edit item"),
                item.getKey(),
                item.getQuery(),
                new String[] { tr("Save") });
        dialog.showDialog();

        Optional<SelectorItem> editedItem = dialog.getOutputItem();
        editedItem.ifPresent(i -> {
            this.items.remove(i.getKey(), i);

            if (item instanceof UserHistory) {
                UserHistory ht = (UserHistory) item;
                this.items.put(ht.getKey(), ht.toUserSnippet());
            } else if (item instanceof UserSnippet) {
                UserSnippet st = (UserSnippet) item;
                this.items.put(st.getKey(), new UserSnippet(i.getKey(), i.getQuery(), st.getUseCount()));
            }

            savePreferences();
            filterItems();
        });
    }

    /**
     * Opens {@link EditItemDialog}, saves the state to preferences if a new item is added
     * and updates the view.
     */
    private synchronized void addNewItem() {
        EditItemDialog dialog = new EditItemDialog(componentParent, tr("Add snippet"), tr("Add"));
        dialog.showDialog();

        Optional<SelectorItem> newItem = dialog.getOutputItem();
        newItem.ifPresent(i -> {
            items.put(i.getKey(), new UserSnippet(i.getKey(), i.getQuery(), 1));
            savePreferences();
            filterItems();
        });
    }

    @Override
    public void setDblClickListener(ActionListener dblClickListener) {
        // this listener is already set within this class
    }

    @Override
    protected void filterItems() {
        String text = edSearchText.getText().toLowerCase(Locale.ENGLISH);
        boolean snippets = this.onlySnippets.isSelected();
        boolean history = this.onlyHistory.isSelected();
        boolean allElements = this.all.isSelected();

        super.lsResultModel.setItems(this.items.values().stream()
                .filter(item -> (item instanceof UserSnippet) && snippets ||
                                (item instanceof UserHistory) && history || allElements)
                .filter(item -> item.itemKey.contains(text))
                .collect(Collectors.toList()));
    }

    private void initFilterCheckBoxes() {
        ButtonGroup group = new ButtonGroup();
        group.add(this.onlyHistory);
        group.add(this.onlySnippets);
        group.add(this.all);

        this.all.addActionListener(l -> SHOW_ALL.put(this.all.isSelected()));
        this.onlyHistory.addActionListener(l -> SHOW_ONLY_HISTORY.put(this.onlyHistory.isSelected()));
        this.onlySnippets.addActionListener(l -> SHOW_ONLY_SNIPPETS.put(this.onlySnippets.isSelected()));
        ActionListener listener = e -> filterItems();
        Collections.list(group.getElements()).forEach(cb -> cb.addActionListener(listener));
    }

    private void getDblClickListener(ActionEvent e) {
        Optional<SelectorItem> selectedItem = this.getSelectedItem();

        if (!selectedItem.isPresent()) {
            return;
        }

        SelectorItem item = selectedItem.get();
        this.target.setText(item.getQuery());
    }

    /**
     * Saves all elements from the list to {@link Main#pref}.
     */
    private void savePreferences() {
        Collection<Map<String, String>> toSave = new ArrayList<>(this.items.size());
        for (SelectorItem item : this.items.values()) {
            Map<String, String> it = new HashMap<>();
            it.put(KEY_KEY, item.getKey());
            it.put(QUERY_KEY, item.getQuery());

            if (item instanceof UserHistory) {
                it.put(LAST_USE_KEY, ((UserHistory) item).getLastUseDateTime().toString());
            } else {
                it.put(USE_COUNT_KEY, Integer.toString(((UserSnippet) item).getUseCount()));
            }

            toSave.add(it);
        }

        Main.pref.putListOfStructs(PREFERENCE_ITEMS, toSave);
    }

    /**
     * Loads the user saved items from {@link Main#pref}.
     * @return A set of the user saved items.
     */
    private Map<String, SelectorItem> restorePreferences() {
        Collection<Map<String, String>> toRetrieve =
                Main.pref.getListOfStructs(PREFERENCE_ITEMS, Collections.emptyList());
        Map<String, SelectorItem> result = new HashMap<>();

        for (Map<String, String> entry : toRetrieve) {
            String key = entry.get(KEY_KEY);
            String query = entry.get(QUERY_KEY);

            if (entry.containsKey(USE_COUNT_KEY)) {
                result.put(key, new UserSnippet(key, query, Integer.parseInt(entry.get(USE_COUNT_KEY))));
            } if (entry.containsKey(LAST_USE_KEY)) {
                result.put(key, new UserHistory(key, query, LocalDateTime.parse(entry.get(LAST_USE_KEY))));
            }
        }

        return result;
    }

    private class OverpassQueryListMouseAdapter extends MouseAdapter {

        private final JList list;
        private final ResultListModel model;
        private final JPopupMenu emptySelectionPopup = new JPopupMenu();
        private final JPopupMenu elementPopup = new JPopupMenu();

        public OverpassQueryListMouseAdapter(JList list, ResultListModel listModel) {
            this.list = list;
            this.model = listModel;

            this.initPopupMenus();
        }

        /*
         * Do not select the closest element if the user clicked on
         * an empty area within the list.
         */
        private int locationToIndex(Point p) {
            int idx = list.locationToIndex(p);

            if (idx != -1 && !list.getCellBounds(idx, idx).contains(p)) {
                return -1;
            } else {
                return idx;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            super.mouseClicked(e);
            if (SwingUtilities.isRightMouseButton(e)) {
                int index = locationToIndex(e.getPoint());

                if (model.getSize() == 0 || index == -1) {
                    list.clearSelection();
                    emptySelectionPopup.show(list, e.getX(), e.getY());
                } else {
                    list.setSelectedIndex(index);
                    list.ensureIndexIsVisible(index);
                    elementPopup.show(list, e.getX(), e.getY());
                }
            }
        }

        private void initPopupMenus() {
            String addLabel = tr("Add");
            JMenuItem add = new JMenuItem(addLabel);
            JMenuItem add2 = new JMenuItem(addLabel);
            JMenuItem edit = new JMenuItem(tr("Edit"));
            JMenuItem remove = new JMenuItem(tr("Remove"));
            add.addActionListener(l -> addNewItem());
            add2.addActionListener(l -> addNewItem());
            edit.addActionListener(l -> editSelectedItem());
            remove.addActionListener(l -> removeSelectedItem());
            this.emptySelectionPopup.add(add);
            this.elementPopup.add(add2);
            this.elementPopup.add(edit);
            this.elementPopup.add(remove);
        }
    }

    /**
     * This class defines the way each element is rendered in the list.
     */
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

            Font font = list.getFont();
            if (isSelected) {
                setFont(new Font(font.getFontName(), Font.BOLD, font.getSize() + 2));
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setFont(new Font(font.getFontName(), Font.PLAIN, font.getSize() + 2));
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setEnabled(list.isEnabled());
            setText(value.getKey());

            if (isSelected && cellHasFocus) {
                setBorder(new CompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 1),
                        BorderFactory.createEmptyBorder(2, 0, 2, 0)));
            } else {
                setBorder(new CompoundBorder(
                        null,
                        BorderFactory.createEmptyBorder(2, 0, 2, 0)));
            }

            return this;
        }
    }

    /**
     * Dialog that provides functionality to add/edit an item from the list.
     */
    private final class EditItemDialog extends ExtendedDialog {

        private final JTextField name;
        private final JosmTextArea query;
        private final int initialNameHash;

        private final transient AbstractTextComponentValidator queryValidator;
        private final transient AbstractTextComponentValidator nameValidator;

        private static final int SUCCESS_BTN = 0;
        private static final int CANCEL_BTN = 1;

        /**
         * Added/Edited object to be returned. If {@link Optional#empty()} then probably
         * the user closed the dialog, otherwise {@link SelectorItem} is present.
         */
        private transient Optional<SelectorItem> outputItem = Optional.empty();

        public EditItemDialog(Component parent, String title, String... buttonTexts) {
            this(parent, title, "", "", buttonTexts);
        }

        public EditItemDialog(
                Component parent,
                String title,
                String nameToEdit,
                String queryToEdit,
                String... buttonTexts) {
            super(parent, title, buttonTexts);

            this.initialNameHash = nameToEdit.hashCode();

            this.name = new JTextField(nameToEdit);
            this.query = new JosmTextArea(queryToEdit);

            this.queryValidator = new DefaultTextComponentValidator(this.query, "", tr("Query cannot be empty"));
            this.nameValidator = new AbstractTextComponentValidator(this.name) {
                @Override
                public void validate() {
                    if (isValid()) {
                        feedbackValid(tr("This name can be used for the item"));
                    } else {
                        feedbackInvalid(tr("Item with this name already exists"));
                    }
                }

                @Override
                public boolean isValid() {
                    String currentName = name.getText();
                    int currentHash = currentName.hashCode();

                    return !Utils.isStripEmpty(currentName) &&
                            !(currentHash != initialNameHash &&
                                    items.containsKey(currentName));
                }
            };

            this.name.getDocument().addDocumentListener(this.nameValidator);
            this.query.getDocument().addDocumentListener(this.queryValidator);

            JPanel panel = new JPanel(new GridBagLayout());
            JScrollPane queryScrollPane = GuiHelper.embedInVerticalScrollPane(this.query);
            queryScrollPane.getVerticalScrollBar().setUnitIncrement(10); // make scrolling smooth

            GBC constraint = GBC.eol().insets(8, 0, 8, 8).anchor(GBC.CENTER).fill(GBC.HORIZONTAL);
            constraint.ipady = 250;
            panel.add(this.name, GBC.eol().insets(5).anchor(GBC.SOUTHEAST).fill(GBC.HORIZONTAL));
            panel.add(queryScrollPane, constraint);

            setDefaultButton(SUCCESS_BTN);
            setCancelButton(CANCEL_BTN);
            setPreferredSize(new Dimension(400, 400));
            setContent(panel, false);
        }

        public Optional<SelectorItem> getOutputItem() {
            return this.outputItem;
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            if (buttonIndex == SUCCESS_BTN) {
                if (!this.nameValidator.isValid()) {
                    JOptionPane.showMessageDialog(
                            componentParent,
                            tr("The item cannot be created with provided name"),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE);
                } else if (!this.queryValidator.isValid()) {
                    JOptionPane.showMessageDialog(
                            componentParent,
                            tr("The item cannot be created with an empty query"),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    this.outputItem = Optional.of(new SelectorItem(this.name.getText(), this.query.getText()));
                    super.buttonAction(buttonIndex, evt);
                }
            } else {
                super.buttonAction(buttonIndex, evt);
            }
        }
    }

    public class SelectorItem {
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

            return itemKey.equals(that.itemKey);
        }

        @Override
        public int hashCode() {
            return itemKey.hashCode();
        }
    }

    public class UserSnippet extends SelectorItem implements Comparable<UserSnippet> {
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
            String end = "\',\n";
            return "UserSnippet{" +
                    "itemKey='" + this.getKey() + end +
                    "query='" + this.getQuery() + end +
                    "useCount=" + useCount +
                    '}';
        }
    }

    public class UserHistory extends SelectorItem implements Comparable<UserHistory> {

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
