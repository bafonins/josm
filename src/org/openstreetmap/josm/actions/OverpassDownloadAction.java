// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.OverpassQueryList;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.preferences.server.OverpassServerPreference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.GBC;
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
        Optional<Bounds> selectedArea = dialog.getSelectedDownloadArea();
        String overpassQuery = dialog.getOverpassQuery();

        /*
         * Absence of the selected area can be justified only if the overpass query
         * is not restricted to bbox.
         */
        if (!selectedArea.isPresent() && overpassQuery.contains("{{bbox}}")) {
            JOptionPane.showMessageDialog(
                    dialog,
                    tr("Please select a download area first."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        /*
         * A callback that is passed to PostDownloadReporter that is called once the download task
         * has finished. According to the number of errors happened, their type we decide whether we
         * want to save the last query in OverpassQueryList.
         */
        Consumer<Collection> errorReporter = (errors) -> {

            boolean success = errors.isEmpty() || (errors.size() == 1 && errors.stream()
                    .map(Object::toString)
                    .anyMatch(err -> err.equals("No data found in this area.")));

            if (success) {
                dialog.saveHistoricItemOnSuccess();
            }
        };

        /*
         * In order to support queries generated by the Overpass Turbo Query Wizard tool
         * which do not require the area to be specified.
         */
        Bounds area = selectedArea.orElseGet(() -> new Bounds(0, 0, 0, 0));
        DownloadOsmTask task = new DownloadOsmTask();
        task.setZoomAfterDownload(dialog.isZoomToDownloadedDataRequired());
        Future<?> future = task.download(
                new OverpassDownloadReader(area, OverpassServerPreference.getOverpassServer(), dialog.getOverpassQuery()),
                dialog.isNewLayerRequired(), area, null);
        Main.worker.submit(new PostDownloadHandler(task, future, errorReporter));
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

        private JosmTextArea overpassQuery;
        private OverpassQueryList overpassQueryList;
        private static OverpassDownloadDialog instance;
        private static final BooleanProperty OVERPASS_QUERY_LIST_OPENED =
                new BooleanProperty("download.overpass.query-list.opened", false);

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

            String tooltip = tr("Build an Overpass query using the Overpass Turbo Query Wizard tool");
            Action queryWizardAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    QueryWizardDialog.getInstance().showDialog();
                }
            };

            JButton openQueryWizard = new JButton("Query Wizard");
            openQueryWizard.setToolTipText(tooltip);
            openQueryWizard.addActionListener(queryWizardAction);

            // CHECKSTYLE.OFF: LineLength
            this.overpassQuery = new JosmTextArea(
                    "/*\n" +
                    tr("Place your Overpass query below or \n generate one using the Overpass Turbo Query Wizard")
                    + "\n*/",
                    8, 80);
            // CHECKSTYLE.ON: LineLength
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

            this.overpassQueryList = new OverpassQueryList(this, this.overpassQuery);
            overpassQueryList.setToolTipText(tr("Show/hide Overpass snippet list"));
            overpassQueryList.setVisible(OVERPASS_QUERY_LIST_OPENED.get());
            overpassQueryList.setPreferredSize(new Dimension(350, 300));
            JScrollPane scrollPane = new JScrollPane(overpassQuery);
            BasicArrowButton arrowButton = new BasicArrowButton(overpassQueryList.isVisible()
                ? BasicArrowButton.EAST
                : BasicArrowButton.WEST);
            arrowButton.addActionListener(e ->  {
                if (overpassQueryList.isVisible()) {
                    overpassQueryList.setVisible(false);
                    arrowButton.setDirection(BasicArrowButton.WEST);
                    OVERPASS_QUERY_LIST_OPENED.put(false);
                } else {
                    overpassQueryList.setVisible(true);
                    arrowButton.setDirection(BasicArrowButton.EAST);
                    OVERPASS_QUERY_LIST_OPENED.put(false);
                }
            });

            JPanel innerPanel = new JPanel(new BorderLayout());
            innerPanel.add(scrollPane, BorderLayout.CENTER);
            innerPanel.add(arrowButton, BorderLayout.EAST);

            JPanel pane = new JPanel(new BorderLayout());
            pane.add(innerPanel, BorderLayout.CENTER);
            pane.add(overpassQueryList, BorderLayout.EAST);

            GBC gbc = GBC.eol().fill(GBC.HORIZONTAL); gbc.ipady = 200;
            pnl.add(openQueryWizard, GBC.std().insets(5, 5, 5, 5));
            pnl.add(pane, gbc);
        }

        String getOverpassQuery() {
            return overpassQuery.getText();
        }

        void setOverpassQuery(String text) {
            overpassQuery.setText(text);
        }

        /**
         * Adds the current query to {@link OverpassQueryList}.
         */
        void saveHistoricItemOnSuccess() {
            overpassQueryList.saveHistoricItem(overpassQuery.getText());
        }

        @Override
        protected void updateSizeCheck() {
            displaySizeCheckResult(false);
        }

        /**
         * Triggers the download action to fire.
         */
        private void triggerDownload() {
            super.btnDownload.doClick();
        }
    }

    private static final class QueryWizardDialog extends ExtendedDialog {

        private static QueryWizardDialog dialog;
        private final HistoryComboBox queryWizard;
        private final OverpassTurboQueryWizard overpassQueryBuilder;
        private static final CollectionProperty OVERPASS_WIZARD_HISTORY =
                new CollectionProperty("download.overpass.wizard", new ArrayList<String>());

        // dialog buttons
        private static final int BUILD_QUERY = 0;
        private static final int BUILD_AN_EXECUTE_QUERY = 1;
        private static final int CANCEL = 2;

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
                + "table { border-spacing: 0pt;}\n"
                + "h3 {text-align: center; padding: 8px;}\n"
                + "td {border: 1px solid #dddddd; text-align: left; padding: 8px;}\n"
                + "#desc {width: 350px;}"
                + "</style>\n";

        private QueryWizardDialog() {
            super(OverpassDownloadDialog.getInstance(), tr("Overpass Turbo Query Wizard"),
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

            queryWizard.setPossibleItems(OVERPASS_WIZARD_HISTORY.get());

            setCancelButton(CANCEL);
            setDefaultButton(BUILD_AN_EXECUTE_QUERY + 1); // Build and execute button
            setContent(panel, false);
        }

        @Override
        public void buttonAction(int buttonIndex, ActionEvent evt) {
            switch (buttonIndex) {
                case BUILD_QUERY:
                    if (this.buildQueryAction()) {
                        this.saveHistory();
                        super.buttonAction(BUILD_QUERY, evt);
                    }
                    break;
                case BUILD_AN_EXECUTE_QUERY:
                    if (this.buildQueryAction()) {
                        this.saveHistory();
                        super.buttonAction(BUILD_AN_EXECUTE_QUERY, evt);

                        OverpassDownloadDialog.getInstance().triggerDownload();
                    }
                    break;
                default:
                    super.buttonAction(buttonIndex, evt);

            }
        }

        /**
         * Saves the latest, successfully parsed search term.
         */
        private void saveHistory() {
            queryWizard.addCurrentItemToHistory();
            OVERPASS_WIZARD_HISTORY.put(queryWizard.getHistory());
        }

        /**
         * Tries to process a search term using {@link OverpassTurboQueryWizard}. If the term cannot
         * be parsed, the the corresponding dialog is shown.
         * @param searchTerm The search term to parse.
         * @return {@link Optional#empty()} if an exception was thrown when parsing, meaning
         * that the term cannot be processed, or non-empty {@link Optional} containing the result
         * of parsing.
         */
        private Optional<String> tryParseSearchTerm(String searchTerm) {
            try {
                String query = this.overpassQueryBuilder.constructQuery(searchTerm);

                return Optional.of(query);
            } catch (UncheckedParseException ex) {
                Main.error(ex);
                JOptionPane.showMessageDialog(
                        OverpassDownloadDialog.getInstance(),
                        "<html>" +
                         tr("The Overpass wizard could not parse the following query:") +
                         Utils.joinAsHtmlUnorderedList(Collections.singleton(searchTerm)) +
                         "</html>",
                        tr("Parse error"),
                        JOptionPane.ERROR_MESSAGE
                );

                return Optional.empty();
            }
        }

        /**
         * Builds an Overpass query out from {@link QueryWizardDialog#queryWizard} contents.
         * @return {@code true} if the query successfully built, {@code false} otherwise.
         */
        private boolean buildQueryAction() {
            final String wizardSearchTerm = this.queryWizard.getText();

            Optional<String> q = this.tryParseSearchTerm(wizardSearchTerm);
            if (q.isPresent()) {
                String query = q.get();
                OverpassDownloadDialog.getInstance().setOverpassQuery(query);

                return true;
            }

            return false;
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
                    .append("<h3>")
                    .append(tr("Query Wizard"))
                    .append("</h3>")
                    .append("<p>")
                    .append(tr("Allows you to interact with <i>Overpass API</i> by writing declarative, human-readable terms."))
                    .append(tr("The <i>Query Wizard</i> tool will transform those to a valid overpass query."))
                    .append(tr("For more detailed description see "))
                    .append(tr("<a href=\"{0}\">OSM Wiki</a>.", Main.getOSMWebsite() + "/wiki/Overpass_turbo/Wizard"))
                    .append("</p>")
                    .append("<h3>").append(tr("Hints")).append("</h3>")
                    .append("<table>").append("<tr>").append("<td>")
                    .append(Utils.joinAsHtmlUnorderedList(Arrays.asList("<i>type:node</i>", "<i>type:relation</i>", "<i>type:way</i>")))
                    .append("</td>").append("<td>")
                    .append("<span>").append(tr("Download objects of a certain type.")).append("</span>")
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
                    .append("<span>")
                    .append(tr("Instead of <i>location</i> any valid place name can be used like address, city, etc."))
                    .append("</span>")
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
                    .append("<span>")
                    .append(tr("Basic logical operators can be used to create more sophisticated queries. Instead of 'or' - '|', '||' " +
                            "can be used, and instead of 'and' - '&', '&&'."))
                    .append("</span>")
                    .append("</td>").append("</tr>").append("</table>")
                    .append("</body>")
                    .append("</html>")
                    .toString();
        }
    }
}
