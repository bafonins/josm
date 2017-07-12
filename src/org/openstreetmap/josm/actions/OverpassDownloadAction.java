// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.preferences.server.OverpassServerPreference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.OverpassTurboQueryWizard;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Download map data from Overpass API server.
 * @since 8684
 */
public class OverpassDownloadAction extends JosmAction {

    /**
     * Constructs a new {@code OverpassDownloadAction}.
     */
    public OverpassDownloadAction() {
        super(tr("Download from Overpass API ..."), "download-overpass", tr("Download map data from Overpass API server."),
                // CHECKSTYLE.OFF: LineLength
                Shortcut.registerShortcut("file:download-overpass", tr("File: {0}", tr("Download from Overpass API ...")), KeyEvent.VK_DOWN, Shortcut.ALT_SHIFT),
                // CHECKSTYLE.ON: LineLength
                true, "overpassdownload/download", true);
        putValue("help", ht("/Action/OverpassDownload"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OverpassDownloadDialog dialog = OverpassDownloadDialog.getInstance();
        dialog.restoreSettings();
        dialog.setVisible(true);

        if (dialog.isCanceled()) {
            return;
        }

        dialog.rememberSettings();
        Bounds area = dialog.getSelectedDownloadArea();
        DownloadOsmTask task = new DownloadOsmTask();
        task.setZoomAfterDownload(dialog.isZoomToDownloadedDataRequired());
        Future<?> future = task.download(
                new OverpassDownloadReader(area, OverpassServerPreference.getOverpassServer(), dialog.getOverpassQuery()),
                dialog.isNewLayerRequired(), area, null);
        Main.worker.submit(new PostDownloadHandler(task, future));
    }

    private static final class DisableActionsFocusListener implements FocusListener {

        private final ActionMap actionMap;

        private DisableActionsFocusListener(ActionMap actionMap) {
            this.actionMap = actionMap;
        }

        @Override
        public void focusGained(FocusEvent e) {
            enableActions(false);
        }

        @Override
        public void focusLost(FocusEvent e) {
            enableActions(true);
        }

        private void enableActions(boolean enabled) {
            Object[] allKeys = actionMap.allKeys();
            if (allKeys != null) {
                for (Object key : allKeys) {
                    Action action = actionMap.get(key);
                    if (action != null) {
                        action.setEnabled(enabled);
                    }
                }
            }
        }
    }

    private static final class OverpassDownloadDialog extends DownloadDialog {

        private HistoryComboBox overpassWizard;
        private JosmTextArea overpassQuery;
        private static OverpassDownloadDialog instance;
        private static final CollectionProperty OVERPASS_WIZARD_HISTORY =
                new CollectionProperty("download.overpass.wizard", new ArrayList<String>());

        private OverpassDownloadDialog(Component parent) {
            super(parent, ht("/Action/OverpassDownload"));
            cbDownloadOsmData.setEnabled(false);
            cbDownloadOsmData.setSelected(false);
            cbDownloadGpxData.setVisible(false);
            cbDownloadNotes.setVisible(false);
            cbStartup.setVisible(false);
        }

        public static OverpassDownloadDialog getInstance() {
            if (instance == null) {
                instance = new OverpassDownloadDialog(Main.parent);
            }
            return instance;
        }

        @Override
        protected void buildMainPanelAboveDownloadSelections(JPanel pnl) {
            // needed for the invisible checkboxes cbDownloadGpxData, cbDownloadNotes
            pnl.add(new JLabel(), GBC.eol());

            DisableActionsFocusListener disableActionsFocusListener =
                    new DisableActionsFocusListener(slippyMapChooser.getNavigationComponentActionMap());

            String tooltip = tr("Build an Overpass query using the {0} tool", "Overpass Turbo Query Wizard");
            Action queryWizardAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    QueryWizardDialog.getInstance().showDialog();
                }
            };

            this.overpassWizard = new HistoryComboBox();
            this.overpassWizard.getEditorComponent().addFocusListener(disableActionsFocusListener);
            InputMapUtils.addEnterAction(overpassWizard.getEditorComponent(), queryWizardAction);

            JButton openQueryWizard = new JButton("Query Wizard");
            openQueryWizard.setToolTipText(tooltip);
            openQueryWizard.addActionListener(queryWizardAction);

            this.overpassQuery = new JosmTextArea(
                    "/*\n Place your Overpass query below or\n" +
                    "generate one using the Overpass Turbo Query Wizard\n */\n",
                    8, 80);
            this.overpassQuery.setFont(GuiHelper.getMonospacedFont(overpassQuery));
            this.overpassQuery.addFocusListener(disableActionsFocusListener);
            this.overpassQuery.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    overpassQuery.selectAll();
                }

                @Override
                public void focusLost(FocusEvent e) {

                }
            });

            JScrollPane scrollPane = new JScrollPane(overpassQuery);
            BasicArrowButton arrowButton = new BasicArrowButton(BasicArrowButton.SOUTH);
            arrowButton.addActionListener(e -> OverpassQueryHistoryPopup.show(arrowButton, OverpassDownloadDialog.this));

            JPanel pane = new JPanel(new BorderLayout());
            pane.add(scrollPane, BorderLayout.CENTER);
            pane.add(arrowButton, BorderLayout.EAST);

            GBC gbc = GBC.eol().fill(GBC.HORIZONTAL); gbc.ipady = 200;
            pnl.add(openQueryWizard, GBC.std().insets(5, 5, 5, 5));
            pnl.add(overpassWizard, GBC.eol().fill(GBC.HORIZONTAL));
            pnl.add(pane, gbc);
        }

        String getOverpassQuery() {
            return overpassQuery.getText();
        }

        void setOverpassQuery(String text) {
            overpassQuery.setText(text);
        }

        @Override
        public void restoreSettings() {
            super.restoreSettings();
            overpassWizard.setPossibleItems(OVERPASS_WIZARD_HISTORY.get());
        }

        @Override
        public void rememberSettings() {
            super.rememberSettings();
            overpassWizard.addCurrentItemToHistory();
            OVERPASS_WIZARD_HISTORY.put(overpassWizard.getHistory());
            OverpassQueryHistoryPopup.addToHistory(getOverpassQuery());
        }

        @Override
        protected void updateSizeCheck() {
            displaySizeCheckResult(false);
        }
    }

    private static final class QueryWizardDialog extends ExtendedDialog {

        private static QueryWizardDialog dialog;
        private final HistoryComboBox queryWizard;
        private final OverpassTurboQueryWizard overpassQueryBuilder;

        /**
         * Get an instance of {@link QueryWizardDialog}.
         */
        public static QueryWizardDialog getInstance() {
            if (dialog == null) {
                dialog = new QueryWizardDialog();
            }

            return dialog;
        }

        private static final String DESCRIPTION_STYLE =
                "<style type=\"text/css\">\n"
                + "body {font-family: sans-serif; }\n"
                + "table { border-spacing: 0pt;}\n"
                + "h3 {text-align: center; padding: 8px; }\n"
                + "td {border: 1px solid #dddddd; text-align: left; padding: 8px; }\n"
                + "#desc {width: 350px;}"
                + "</style>\n";

        private QueryWizardDialog() {
            super(Main.parent, "Overpass Turbo Query Wizard",
                    tr("Build query"), tr("Build query and execute"), tr("Cancel"));

            this.queryWizard = new HistoryComboBox();
            this.overpassQueryBuilder = OverpassTurboQueryWizard.getInstance();

            JPanel panel = new JPanel(new GridBagLayout());

            JLabel searchLabel = new JLabel(tr("Search :"));
            JTextComponent descPane = this.buildDescriptionSection();
            JScrollPane scroll = GuiHelper.embedInVerticalScrollPane(descPane);
            scroll.getVerticalScrollBar().setUnitIncrement(10); // make scrolling smooth

            panel.add(searchLabel, GBC.std().insets(0, 0, 0, 20).anchor(GBC.SOUTHEAST));
            panel.add(queryWizard, GBC.eol().insets(0, 0, 0, 15).fill(GBC.HORIZONTAL).anchor(GBC.SOUTH));
            panel.add(scroll, GBC.eol().fill(GBC.BOTH).anchor(GBC.CENTER));

            setCancelButton(2);
            setDefaultButton(1); // Build and execute button
            setContent(panel, false);
        }

        @Override
        public void buttonAction(int buttonIndex, ActionEvent evt) {
            super.buttonAction(buttonIndex, evt);
            switch (buttonIndex) {
                case 0: // Build query button
                    this.buildQueryAction();
                    break;
                case 1: // Build query and execute
                    this.buildAndExecuteAction();
                    break;
            }
        }

        private void buildQueryAction() {
            final String wizardSearchTerm = this.queryWizard.getText();

            try {
                String query = this.overpassQueryBuilder.constructQuery(wizardSearchTerm);
                OverpassDownloadDialog.getInstance().setOverpassQuery(query);
            } catch (UncheckedParseException ex) {
                Main.error(ex);
                JOptionPane.showMessageDialog(
                        OverpassDownloadDialog.getInstance(),
                        tr("<html>The Overpass wizard could not parse the following query:"
                                + Utils.joinAsHtmlUnorderedList(Collections.singleton(wizardSearchTerm))),
                        tr("Parse error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }

        private void buildAndExecuteAction() {
            this.buildQueryAction();
            // TODO: finish
        }

        private JTextComponent buildDescriptionSection() {
            JEditorPane descriptionSection = new JEditorPane("text/html", this.getDescriptionContent());
            descriptionSection.setEditable(false);
            descriptionSection.addHyperlinkListener(e -> {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                    OpenBrowser.displayUrl(e.getURL().toString());
                }
            });

            return descriptionSection;
        }

        private String getDescriptionContent() {
            return new StringBuilder("<html>")
                    .append(DESCRIPTION_STYLE)
                    .append("<body>")
                    .append("<h3>Query Wizard</h3>")
                    .append("<p>")
                    .append(tr("Allows you to interact with <i>Overpass API</i> by writing declarative, human-readable terms. "))
                    .append(tr("The <i>Query Wizard</i> tool will transform those to a valid overpass query. "))
                    .append(tr("For more detailed description see "))
                    .append(tr("<a href=\"{0}\">{1} Wiki</a>.", Main.getOSMWebsite() + "/wiki/Overpass_turbo/Wizard", "OSM"))
                    .append("</p>")
                    .append(tr("<h3>Hints</h3>"))
                    .append("<table>").append("<tr>").append("<td>")
                    .append(Utils.joinAsHtmlUnorderedList(Arrays.asList("<i>type:node</i>", "<i>type:relation</i>", "<i>type:way</i>")))
                    .append("</td>").append("<td>")
                    .append(tr("<span>Download objects of a certain type.</span>"))
                    .append("</td>").append("</tr>")
                    .append("<tr>").append("<td>")
                    .append(Utils.joinAsHtmlUnorderedList(
                            Arrays.asList("<i>key=value in <u>location</u></i>",
                                    "<i>key=value around <u>location</u></i>",
                                    "<i>key=value in bbox</i>")))
                    .append("</td>").append("<td>")
                    .append(tr("Download object by specifying a specific location. For example,"))
                    .append(Utils.joinAsHtmlUnorderedList(Arrays.asList(
                            tr("{0} all objects having {1} as attribute are downloaded.", "<i>tourism=hotel in Berlin</i> -", "'tourism=hotel'"),
                            tr("{0} all object with the corresponding key/value pair located around Berlin. Note, the default value for radius "+
                                    "is set to 1000m, but it can be changed in the generated query.", "<i>tourism=hotel around Berlin</i> -"),
                            tr("{0} all objects within the current selection that have {1} as attribute.", "<i>tourism=hotel in bbox</i> -",
                                    "'tourism=hotel'"))))
                    .append(tr("<span>Instead of <i>location</i> any valid place name can be used like address, city, etc.</span>"))
                    .append("</td>").append("</tr>")
                    .append("<tr>").append("<td>")
                    .append(Utils.joinAsHtmlUnorderedList(Arrays.asList("<i>key=value</i>", "<i>key=*</i>", "<i>key~regex</i>",
                            "<i>key!=value</i>", "<i>key!~regex</i>", "<i>key=\"combined value\"</i>")))
                    .append("</td>").append("<td>")
                    .append(tr("<span>Download objects that have some concrete key/value pair, only the key with any contents for the value, " +
                            "the value matching some regular expression. 'Not equal' operators are supported as well.</span>"))
                    .append("</td>").append("</tr>")
                    .append("<tr>").append("<td>")
                    .append(Utils.joinAsHtmlUnorderedList(Arrays.asList(
                            tr("<i>expression1 {0} expression2</i>", "or"),
                            tr("<i>expression1 {0} expression2</i>", "and"))))
                    .append("</td>").append("<td>")
                    .append(tr("<span>Basic logical operators can be used to create more sophisticated queries. Instead of 'or' - '|', '||' " +
                            "can be used, and minstead of 'and' - '&', '&&'.</span>"))
                    .append("</td>").append("</tr>").append("</table>")
                    .append("</body>")
                    .append("</html>")
                    .toString();
        }
    }

    static class OverpassQueryHistoryPopup extends JPopupMenu {

        static final CollectionProperty OVERPASS_QUERY_HISTORY = new CollectionProperty("download.overpass.query", new ArrayList<String>());
        static final IntegerProperty OVERPASS_QUERY_HISTORY_SIZE = new IntegerProperty("download.overpass.query.size", 12);

        OverpassQueryHistoryPopup(final OverpassDownloadDialog dialog) {
            final Collection<String> history = OVERPASS_QUERY_HISTORY.get();
            setLayout(new GridLayout((int) Math.ceil(history.size() / 2.), 2));
            for (final String i : history) {
                add(new OverpassQueryHistoryItem(i, dialog));
            }
        }

        static void show(final JComponent parent, final OverpassDownloadDialog dialog) {
            final OverpassQueryHistoryPopup menu = new OverpassQueryHistoryPopup(dialog);
            final Rectangle r = parent.getBounds();
            menu.show(parent.getParent(), r.x + r.width - (int) menu.getPreferredSize().getWidth(), r.y + r.height);
        }

        static void addToHistory(final String query) {
            final Deque<String> history = new LinkedList<>(OVERPASS_QUERY_HISTORY.get());
            if (!history.contains(query)) {
                history.add(query);
            }
            while (history.size() > OVERPASS_QUERY_HISTORY_SIZE.get()) {
                history.removeFirst();
            }
            OVERPASS_QUERY_HISTORY.put(history);
        }
    }

    static class OverpassQueryHistoryItem extends JMenuItem implements ActionListener {

        final String query;
        final OverpassDownloadDialog dialog;

        OverpassQueryHistoryItem(final String query, final OverpassDownloadDialog dialog) {
            this.query = query;
            this.dialog = dialog;
            setText("<html><pre style='width:300px;'>" +
                    Utils.escapeReservedCharactersHTML(Utils.restrictStringLines(query, 7)));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.setOverpassQuery(query);
        }
    }

}
