package jsky.app.ot.gemini.obslog;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.event.ObsExecEvent;
import edu.gemini.spModel.obs.InstrumentService;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obsrecord.ObsVisit;
import edu.gemini.spModel.obsrecord.UniqueConfig;
import edu.gemini.spModel.type.DisplayableSpType;
import jsky.app.ot.OTOptions;
import jsky.app.ot.util.OtColor;
import jsky.util.gui.Resources;
import jsky.util.gui.DropDownListBoxWidget;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

/**
 * GUI component for the ObsLog editor component.
 * @author rnorris
 */
class ObslogGUI extends JPanel {
    private final DataAnalysisComponent tabDataAnalysis = new DataAnalysisComponent("Data Analysis");
    private final VisitsComponent tabVisits = new VisitsComponent("Visits");
    private final CommentsComponent tabComments = new CommentsComponent("Comments");

    static final DateFormat OBSLOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z") {{
        setTimeZone(TimeZone.getTimeZone("UTC"));
    }};

    private static final Icon BLANK          = Resources.getIcon("eclipse/blank.gif");
    private static final Icon GREEN_DOT      = Resources.getIcon("bullet/bullet_green.png");
    private static final Icon GREY_DOT       = Resources.getIcon("bullet/bullet_grey.png");
    private static final Icon ORANGE_DOT     = Resources.getIcon("bullet/bullet_orange.png");
    private static final Icon PALE_GREEN_DOT = Resources.getIcon("bullet/bullet_pale_green.gif");
    private static final Icon RED_DOT        = Resources.getIcon("bullet/bullet_red.png");
    private static final Icon WHITE_DOT      = Resources.getIcon("bullet/bullet_white.gif");

    private static final Map<DataflowStatus, Icon> STATUS_ICON = new HashMap<>();

    static {
        STATUS_ICON.put(DataflowStatus.Archived$.MODULE$,         PALE_GREEN_DOT);
        STATUS_ICON.put(DataflowStatus.NeedsQa$.MODULE$,          WHITE_DOT);
        STATUS_ICON.put(DataflowStatus.SyncPending$.MODULE$,      GREY_DOT);
        STATUS_ICON.put(DataflowStatus.CheckRequested$.MODULE$,   ORANGE_DOT);
        STATUS_ICON.put(DataflowStatus.UpdateFailure$.MODULE$,    RED_DOT);
        STATUS_ICON.put(DataflowStatus.UpdateInProgress$.MODULE$, GREY_DOT);
        STATUS_ICON.put(DataflowStatus.SummitOnly$.MODULE$,       GREY_DOT);
        STATUS_ICON.put(DataflowStatus.Diverged$.MODULE$,         GREY_DOT);
        STATUS_ICON.put(DataflowStatus.InSync$.MODULE$,           GREEN_DOT);
    }

    private static final String ZONE_STRING = ZoneId.systemDefault().getDisplayName(TextStyle.SHORT, Locale.getDefault());


    private static final TableCellRenderer STATUS_RENDERER = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel lab = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            final String text;
            final Icon   icon;

            // When clearing the selection as a result of resetting the model,
            // the table is temporarily rendered with a null DataflowStatus value.
            final DataflowStatus dfs = (DataflowStatus) value;
            if (dfs == null) {
                text = "";
                icon = BLANK;
            } else {
                text = dfs.description();
                icon = STATUS_ICON.getOrDefault(dfs, BLANK);
            }

            lab.setText(text);
            lab.setIcon(icon);

            return lab;
        }
    };


    ObslogGUI() {
        setLayout(new BorderLayout());
        add(new JTabbedPane() {{
            add(tabComments);
            add(tabDataAnalysis);
            add(tabVisits);
        }}, BorderLayout.CENTER);
    }

    /**
     * Sets the data object, replacing or updating the content as appropriate.
     */
    void setup(Option<Instrument> inst, ObsClass oc, ObsLog obsLog) {
        // And tell the tabs that the world has changed.
        tabDataAnalysis.setObsLog(obsLog);
        tabVisits.setObsLog(inst, oc, obsLog);
        tabComments.setObsLog(obsLog);
    }


    class AbstractDatasetRecordTable extends JTable implements ObslogTableModels {

        AbstractDatasetRecordTable() {
            setAutoResizeMode(AUTO_RESIZE_OFF);
        }

        AbstractDatasetRecordTable(TableModel model) {
            super(model);
            setAutoResizeMode(AUTO_RESIZE_OFF);
        }

        public TableCellRenderer getCellRenderer(int row, int col) {
            final TableCellRenderer renderer = super.getCellRenderer(row, col);
            if (renderer instanceof JLabel && dataModel instanceof AbstractDatasetRecordTableModel) {
                if (((AbstractDatasetRecordTableModel) dataModel).isUnavailable(row)) {
                    ((Component) renderer).setForeground(Color.LIGHT_GRAY);
                } else {
                    ((Component) renderer).setForeground(null);
                }
            }
            return renderer;
        }

        /**
         * This is very, very bad.
         * (Shane, and Java Swing I suppose, are to blame for this one.)
         */
        void sizeColumnsToFitData() {
            final TableColumnModel colModel = getColumnModel();
            final TableModel model = getModel();
            final int rows = model.getRowCount();
            final TableCellRenderer headerRenderer;
            try {
                headerRenderer = getDefaultRenderer(String.class);
            } catch (NullPointerException ex) {
                // Sorry, the table doesn't seem to be ready to be re-sized.
                return;
            }

            for (int col = 0; col < model.getColumnCount(); ++col) {

                // Start with the width of the column header
                Component component = headerRenderer.getTableCellRendererComponent(this, model.getColumnName(col), false, false, -1, col);
                int size = component.getPreferredSize().width;

                // Check the width of each item in the column to get the maximum width
                for (int row = 0; row < rows; ++row) {
                    final TableCellRenderer renderer = getCellRenderer(row, col);
                    component = prepareRenderer(renderer, row, col);
                    final int tmp = component.getPreferredSize().width;
                    if (tmp > size) size = tmp;
                }

                size += 10; // add a bit of padding

                // Resize the column
                final TableColumn tc = colModel.getColumn(col);
                tc.setPreferredWidth(size);
                tc.setMinWidth(size);
                tc.setMaxWidth(size);
            }
        }

    }

    /**
     * Functional interface for extracting a value of an arbitrary type A from
     * a DatasetRecord.
     */
    interface DatasetRecordExtractor<A> {
        A getValue(DatasetRecord r);
    }

    /**
     * Information extracted from an ActiveRequest object for formatting and
     * display.
     */
    private static final class RequestDetail {
        final QaRequestStatus status;
        final Instant when;
        final int retry;

        RequestDetail(QaRequestStatus status, Instant when, int retry) {
            this.status = status;
            this.when   = when;
            this.retry  = retry;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final RequestDetail that = (RequestDetail) o;
            return Objects.equals(retry, that.retry) &&
                    Objects.equals(status, that.status) &&
                    Objects.equals(when, that.when);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, when, retry);
        }

        String formatWhen() {
            final ZoneId z = ZoneId.systemDefault();
            final OffsetDateTime    whenOff = OffsetDateTime.ofInstant(when, z);
            final OffsetDateTime    nowOff  = OffsetDateTime.ofInstant(Instant.now(), z);
            final LocalDate        whenDate = whenOff.toLocalDate();
            final LocalDate         nowDate = nowOff.toLocalDate();

            final DateTimeFormatter dateFmt = DateTimeFormatter.ISO_LOCAL_DATE;
            final String         timeString = DateTimeFormatter.ISO_LOCAL_TIME.format(whenOff.toLocalTime());

            return (whenDate.equals(nowDate)) ? timeString :
                    dateFmt.format(whenDate) + " " + timeString;
        }

        static RequestDetail fromActiveRequest(SummitState.ActiveRequest ar) {
            return new RequestDetail(ar.status(), ar.when(), ar.retryCount());
        }

        static Optional<RequestDetail> fromSummitState(SummitState ss) {
            return (ss instanceof SummitState.ActiveRequest) ?
                Optional.of(fromActiveRequest((SummitState.ActiveRequest) ss)) :
                Optional.empty();
        }

        static Optional<RequestDetail> fromDatasetRecord(DatasetRecord dr) {
            return fromSummitState(dr.exec().summit());
        }
    }

    /**
     * Component that shows a table of {@link edu.gemini.spModel.dataset.DatasetExecRecord}s with
     * editable fields for QA State. Editing is disabled unless the OT is running in on site mode.
     *
     * @author rnorris
     */
    private class DataAnalysisComponent extends JPanel implements ObslogTableModels {

        private final DataAnalysisTable table = new DataAnalysisTable();
        private final JPanel editArea = new Editor();
        private Optional<Boolean> isStaffMode = Optional.empty();

        DataAnalysisComponent(String name) {
            super(new BorderLayout());
            setName(name);
        }

        private void reconfigure() {
            final boolean isStaff = OTOptions.isStaffGlobally();

            // if (isStaffMode.forall(_ =/= isStaff)) {
            if (!isStaffMode.isPresent() || isStaffMode.get() != isStaff) {
                isStaffMode = Optional.of(isStaff);

                removeAll();
                add(new JScrollPane(table) {{
                    if (isStaff) setBorder(BorderFactory.createEmptyBorder());
                    setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                }}, BorderLayout.CENTER);

                if (isStaff) {
                    final JPanel editPanel = new JPanel(new BorderLayout());
                    editPanel.add(new Header("Select multiple rows for bulk updating."), BorderLayout.NORTH);
                    editPanel.add(editArea, BorderLayout.CENTER);
                    add(editPanel, BorderLayout.SOUTH);
                }
            }
        }

        void setObsLog(final ObsLog obsLog) {
            reconfigure();
            table.setModel(new DatasetAnalysisTableModel(obsLog));
            table.setEnabled(true);
            editArea.setEnabled(true);
        }

        // OT-424: bulk editing support
        class Editor extends JPanel implements ListSelectionListener {

            final JComboBox<DatasetQaState> qa;
            final JTextPane textPane = new JTextPane() {{
                setBackground(OtColor.BG_GREY);
                setContentType("text/html");
                setEditable(false);
                putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            }};
            final JScrollPane scrollPane = new JScrollPane(textPane) {{
               setBorder(BorderFactory.createEmptyBorder());
               setPreferredSize(new Dimension(1, 50));
            }};
            boolean adjusting; // sigh

            public Editor() {
                super(new GridBagLayout());
                setBorder(BorderFactory.createEmptyBorder(10,5,5,5));

                // QA State
                add(new JLabel("QA State:"), new GridBagConstraints() {{
                    gridx  = 0;
                    gridy  = 0;
                    insets = new Insets(0, 2, 0, 5);
                }});

                add(qa = new DropDownListBoxWidget<DatasetQaState>() {{
                    setChoices(DatasetQaState.values());
                    setRenderer(new SpTypeCellRenderer());
                    setSelectedItem(null);
                    addActionListener(ae -> {
                        if (!adjusting) {
                            setCommonValue(DatasetAnalysisTableModel.COL_QA_STATE, getSelectedItem());
                        }
                    });
                }}, new GridBagConstraints() {{
                    gridx   = 1;
                    gridy   = 0;
                    anchor  = WEST;
                    weightx = 1.0;
                }});

                table.editArea = this;
                table.getSelectionModel().addListSelectionListener(this);

            }

            public void setEnabled(boolean enable) {
                super.setEnabled(enable);
                qa.setEnabled(enable);
                if (!enable) {
                    adjusting = true;
                    qa.setSelectedItem(null);
                    textPane.setText(null);
                    adjusting = false;
                }
            }

            public void valueChanged(ListSelectionEvent lse) {
                if (lse.getValueIsAdjusting() || table.adjusting) return;
                final ListSelectionModel lsm = (ListSelectionModel) lse.getSource();
                if (!lsm.isSelectionEmpty()) {
                    setEnabled(true);
                    adjusting = true;
                    qa.setSelectedItem(getCommonValue(dr -> dr.qa().qaState).orElse(null));

                    final Optional<RequestDetail> detail = getCommonValue(RequestDetail::fromDatasetRecord).orElse(Optional.empty());

                    remove(scrollPane);

                    detail.ifPresent(d -> {
                        add(scrollPane, new GridBagConstraints() {{
                            gridx     = 0;
                            gridy     = 1;
                            gridwidth = 2;
                            fill      = BOTH;
                            weightx   = 1.0;
                            weighty   = 1.0;
                            insets    = new Insets(10, 0, 0, 0);
                        }});

                        textPane.setText(String.format("<html><body><b>%s (%s)</b> %s</body></html>", d.formatWhen(), ZONE_STRING, d.status.description()));
                        textPane.setCaretPosition(0);
                    });

                    revalidate();
                    adjusting = false;
                } else {
                    setEnabled(false);
                }
            }

            private <A> Optional<A> getCommonValue(DatasetRecordExtractor<A> ex) {
                final ListSelectionModel lsm = table.getSelectionModel();
                A a = null;
                if (!lsm.isSelectionEmpty()) {
                    final DatasetAnalysisTableModel model = (DatasetAnalysisTableModel) table.getModel();
                    for (int row = lsm.getMinSelectionIndex(); row <= lsm.getMaxSelectionIndex(); row++) {
                        if (lsm.isSelectedIndex(row)) {
                            final A a0 = ex.getValue(model.getRecordAt(row));
                            if (a == null) {
                                a = a0;
                            } else if (!a.equals(a0)) {
                                a = null;
                                break;
                            }
                        }
                    }
                }
                return Optional.ofNullable(a);
            }

            private void setCommonValue(int col, Object value) {
                final ListSelectionModel lsm = table.getSelectionModel();
                if (!lsm.isSelectionEmpty()) {
                    final TableModel model = table.getModel();
                    for (int row = lsm.getMinSelectionIndex(); row <= lsm.getMaxSelectionIndex(); row++) {
                        if (lsm.isSelectedIndex(row)) {
                            model.setValueAt(value, row, col);
                        }
                    }
                }
            }

        }

        final class SpTypeCellRenderer extends DefaultListCellRenderer {
            public Component getListCellRendererComponent(JList<?> arg0, Object o, int arg2, boolean arg3, boolean arg4) {
                if (o != null) {
                    if (o instanceof DisplayableSpType) {
                        o = ((DisplayableSpType) o).displayValue();
                    }
                }
                return super.getListCellRendererComponent(arg0, o, arg2, arg3, arg4);
            }
        }

        final class DataAnalysisTable extends AbstractDatasetRecordTable {

            JPanel editArea;
            boolean adjusting = false;

            DataAnalysisTable() {
                setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }

            @Override
            public void setModel(TableModel newModel) {

                // OT-640: Preserve selection
                if (newModel instanceof DatasetAnalysisTableModel && getModel() instanceof DatasetAnalysisTableModel) {

                    final DatasetAnalysisTableModel prev = (DatasetAnalysisTableModel) getModel();
                    final DatasetAnalysisTableModel next = (DatasetAnalysisTableModel) newModel;

                    // Collect the set of labels. This may be inefficient but it's safe.
                    final Collection<DatasetLabel> selection = new TreeSet<>();
                    final ListSelectionModel lsm = getSelectionModel();
                    for (int i = lsm.getMinSelectionIndex(); i <= lsm.getMaxSelectionIndex(); i++) {
                        if (lsm.isSelectedIndex(i))
                            selection.add(prev.records.get(i).label());
                    }

                    // Swap the model
                    adjusting = true;
                    super.setModel(next);
                    adjusting = false;

                    // And restore the selection
                    for (int i = 0; i < next.records.size(); i++) {
                        if (selection.contains(next.records.get(i).label()))
                            lsm.addSelectionInterval(i, i);
                    }
                    editArea.setEnabled(!lsm.isSelectionEmpty());

                    // And reset the rendering stuff since the model has changed
                    attachDescriptionRenderer(DatasetAnalysisTableModel.COL_QA_STATE);
                } else {
                    super.setModel(newModel);
                }

                if (newModel instanceof DatasetAnalysisTableModel) {
                    final TableColumn col = getColumnModel().getColumn(DatasetAnalysisTableModel.COL_STATUS);
                    col.setCellRenderer(STATUS_RENDERER);
                }
                sizeColumnsToFitData();
            }


            private void attachDescriptionRenderer(int i) {
                final TableColumn col = getColumnModel().getColumn(i);
                col.setCellRenderer(new DefaultTableCellRenderer() {
                    protected void setValue(Object o) {
                        if (o != null && o instanceof DisplayableSpType)
                            o = ((DisplayableSpType) o).displayValue();
                        super.setValue(o);
                    }
                });
            }


        }

        private final class Header extends JLabel {

            public Header(String text) {
                super(text);
                setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            }

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                final Dimension d = getSize();
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(0, (int) d.getHeight() - 1, (int) d.getWidth() - 1, (int) d.getHeight() - 1);
            }

        }

    }


    /**
     * Component that shows a table of {@link edu.gemini.spModel.dataset.DatasetExecRecord}s with an
     * editable detail view for comments. Editing is disabled unless the OT is running in -onsite mode.
     *
     * @author rnorris
     */
    private final class CommentsComponent extends JPanel implements ObslogTableModels, ListSelectionListener {

        private final CommentsTable table;
        private final JTextArea area = new JTextArea();

        private final DocumentListener docListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent de) {
                updateText();
            }

            public void removeUpdate(DocumentEvent de) {
                updateText();
            }

            public void insertUpdate(DocumentEvent de) {
                updateText();
            }

            private void updateText() {
                final int row = table.getSelectedRow();
                if (row > -1) {
                    table.getModel().setValueAt(area.getText(), row, CommentTableModel.COL_COMMENT);
                    // The comment width may have changed, let's expand it
                    table.expandCommentColumn();
                }
            }
        };

        CommentsComponent(String name) {
            super(new BorderLayout());
            setName(name);

            table = new CommentsTable();
            table.getSelectionModel().addListSelectionListener(this);

            area.setEditable(OTOptions.isStaffGlobally());

            area.getDocument().addDocumentListener(docListener);

            final JPanel panel = new JPanel(new BorderLayout());
            panel.add(new Header("Select a row to " + (OTOptions.isStaffGlobally() ? "view or edit its comment." : "view its comment in full.")), BorderLayout.NORTH);
            panel.add(new JScrollPane(area) {{
                setBorder(BorderFactory.createEmptyBorder());
            }}, BorderLayout.CENTER);
            add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table) {{
                setBorder(BorderFactory.createEmptyBorder());
                setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            }}, panel) {{
                setResizeWeight(0.75);
            }});

        }

        void setObsLog(final ObsLog obsLog) {
            area.setEditable(OTOptions.isStaffGlobally()); // the answer can change as keys are added/removed
            table.setModel(new CommentTableModel(obsLog));
            table.setEnabled(true);
        }

        // Called when the user clicks on a row in the table
        public void valueChanged(ListSelectionEvent lse) {
            if (lse.getValueIsAdjusting()) return;
            final ListSelectionModel lsm = (ListSelectionModel) lse.getSource();
            if (lsm.isSelectionEmpty()) {
                area.setText("");
                area.setEnabled(false);
            } else {
                final int row = lsm.getMinSelectionIndex();
                final String comment = (String) table.getModel().getValueAt(row, CommentTableModel.COL_COMMENT);
                if (comment != null) {
                    area.getDocument().removeDocumentListener(docListener);
                    try {
                        area.setText(comment);
                        area.setSelectionStart(comment.length());
                        area.setSelectionEnd(comment.length());
                    } finally {
                        area.getDocument().addDocumentListener(docListener);
                    }
                }
                area.setEnabled(true);
                area.requestFocusInWindow();
            }

        }

        private class CommentsTable extends AbstractDatasetRecordTable {

            CommentsTable() {
                super();
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                getSelectionModel().addListSelectionListener(this);
            }

            /**
             * Overridden to preserve the selected item, if possible.
             */
            @Override
            public void setModel(TableModel model) {
                final int row = getSelectedRow();
                if (row == -1) {
                    super.setModel(model);
                } else {
                    final Object label = getModel().getValueAt(row, CommentTableModel.COL_LABEL);
                    super.setModel(model);
                    for (int i = 0; i < getModel().getRowCount(); i++) {
                        final Object other = getModel().getValueAt(i, CommentTableModel.COL_LABEL);
                        if (label.equals(other)) {
                            getSelectionModel().setSelectionInterval(i, i);
                            break;
                        }
                    }
                }
                if (model instanceof CommentTableModel) {
                    attachCommentRenderer(CommentTableModel.COL_COMMENT);
                    sizeColumnsToFitData();
                }
            }

            // Recalculates the width of the columns comment
            void expandCommentColumn() {
                TableColumn column = getColumnModel().getColumn(CommentTableModel.COL_COMMENT);
                Component headerRenderer = getTableHeader().getDefaultRenderer()    .getTableCellRendererComponent(this, column.getHeaderValue(), false, false, -1, CommentTableModel.COL_COMMENT);

                int preferredWidth = headerRenderer.getPreferredSize().width;
                for (int i = 0; i < getModel().getRowCount(); i++) {
                    final TableCellRenderer cellRenderer = this.getCellRenderer(i, CommentTableModel.COL_COMMENT);
                    final Component c = table.prepareRenderer(cellRenderer, i, CommentTableModel.COL_COMMENT);
                    final int width = c.getPreferredSize().width + table.getIntercellSpacing().width;
                    preferredWidth = Math.max(preferredWidth, width);
                }

                // Give it a bit of space to breathe
                int gutter = table.getFontMetrics(table.getFont()).charWidth('A');
                preferredWidth += gutter;

                // NB The order of these calls is critical to get the width correct
                // DON'T reorder
                column.setMinWidth(preferredWidth);
                column.setMaxWidth(preferredWidth);
                column.setPreferredWidth(preferredWidth);
            }

            private void attachCommentRenderer(int i) {
                final TableColumn col = getColumnModel().getColumn(i);
                col.setCellRenderer(new DefaultTableCellRenderer() {
                    protected void setValue(Object o) {
                        // Truncate the comment if it is really long.  There is
                        // a scroll bar but it could be a ridiculously long
                        // comment.  Arbitrary cutoff set to 200.
                        if ((o != null) && o instanceof String) {
                            final String s = (String) o;
                            o = (s.length() <= 203) ? s : s.substring(0, 200) + "...";
                        }
                        super.setValue(o);
                    }
                });
            }

        }

        private class Header extends JLabel {

            public Header(String text) {
                super(text);
                setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            }

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                final Dimension d = getSize();
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(0, (int) d.getHeight() - 1, (int) d.getWidth() - 1, (int) d.getHeight() - 1);
            }

        }

    }

    /**
     * A component that shows the visits in an ObsLogDataObject as a hierarchical tree.
     * This control is read-only.
     *
     * @author rnorris
     */
    private final class VisitsComponent extends JScrollPane implements ObslogTableModels {

        private final DateFormat OBSLOG_DATE_FORMAT = ObslogGUI.OBSLOG_DATE_FORMAT;
        private final Box clientArea;

        VisitsComponent(String name) {
            super(new Box(BoxLayout.Y_AXIS));
            clientArea = (Box) getViewport().getView();
            clientArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
            setName(name);
        }

        void setObsLog(final Option<Instrument> inst, final ObsClass oc, final ObsLog obsLog) {
            clientArea.removeAll();
            addVisits(inst, oc, clientArea, obsLog);
            revalidate();
        }

        private void addVisits(Option<Instrument> inst, ObsClass oc, Container parent, ObsLog obsLog) {
            final ObsVisit[] visits = obsLog.getVisits(inst, oc);
            for (final ObsVisit visit : visits) {
                final String title;
                final DatasetLabel[] labels = visit.getAllDatasetLabels();
                switch (labels.length) {
                    case 0:
                        title = "Visit " + OBSLOG_DATE_FORMAT.format(new Date(visit.getStartTime()));
                        break;
                    case 1:
                        title = "Visit " + OBSLOG_DATE_FORMAT.format(new Date(visit.getStartTime())) + " (Dataset " + labels[0].getIndex() + ")";
                        break;
                    default:
                        title = "Visit " + OBSLOG_DATE_FORMAT.format(new Date(visit.getStartTime())) + " (Datasets " + labels[0].getIndex() + "-" + labels[labels.length - 1].getIndex() + ")";
                }
                final CollapsableContainer cc = new CollapsableContainer(parent, title, true);
                addDataSets(cc, labels, obsLog);
                addUniqueConfigs(cc, visit.getUniqueConfigs());
                addEvents(cc, visit.getEvents());
                parent.add(cc);
            }
        }

        private void addDataSets(Container parent, DatasetLabel[] labels, ObsLog obsLog) {
            if (labels.length > 0) {
                final CollapsableContainer cc = new CollapsableContainer(this, "Datasets", true);
                final List<DatasetRecord> records = new ArrayList<>(labels.length);
                for (DatasetLabel label : labels) {
                    records.add(obsLog.getDatasetRecord(label));
                }
                cc.add(tableWithHeader(new DatasetRecordTable(new DatasetAnalysisTableModel(obsLog, records))));
                parent.add(cc);
            }
        }

        private void addUniqueConfigs(Container parent, UniqueConfig[] uniqueConfigs) {
            for (int i = 0; i < uniqueConfigs.length; i++) {
                final UniqueConfig config = uniqueConfigs[i];
                final String title;
                final DatasetLabel[] labels = config.getDatasetLabels();
                switch (labels.length) {
                    case 0:
                        title = "Config " + (i + 1);
                        break;
                    case 1:
                        title = "Config " + (i + 1) + " (Dataset " + labels[0].getIndex() + ")";
                        break;
                    default:
                        title = "Config " + (i + 1) + " (Datasets " + labels[0].getIndex() + "-" + labels[labels.length - 1].getIndex() + ")";
                }
                final CollapsableContainer cc = new CollapsableContainer(this, title, false);
                cc.add(tableWithHeader(new JTable(new ConfigTableModel(config.getConfig()))));
                parent.add(cc);
            }
        }

        private void addEvents(Container parent, ObsExecEvent[] events) {
            if (events.length > 0) {
                final String title;
                switch (events.length) {
                    case 1:
                        title = "Event (" + OBSLOG_DATE_FORMAT.format(new Date(events[0].getTimestamp())) + ")";
                        break;
                    default:
                        title = "Events (" + OBSLOG_DATE_FORMAT.format(new Date(events[0].getTimestamp())) + " to " + OBSLOG_DATE_FORMAT.format(new Date(events[events.length - 1].getTimestamp())) + ")";
                        break;
                }
                final CollapsableContainer cc = new CollapsableContainer(this, title, false);
                cc.add(tableWithHeader(new JTable(new EventTableModel(events))));
                parent.add(cc);
            }
        }

        private Component tableWithHeader(JTable t) {
            final JPanel p = new JPanel(new BorderLayout());
            p.add(t.getTableHeader(), BorderLayout.NORTH);
            p.add(t, BorderLayout.CENTER);
            return p;
        }

        private class DatasetRecordTable extends AbstractDatasetRecordTable {

            DatasetRecordTable(TableModel model) {
                super(model);
                setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            }

            @Override
            public void setModel(TableModel model) {
                super.setModel(model);
                if (model instanceof DatasetAnalysisTableModel) {
                    attachDescriptionRenderer(DatasetAnalysisTableModel.COL_QA_STATE);
                }
            }

            private void attachDescriptionRenderer(int i) {
                final TableColumn col = getColumnModel().getColumn(i);
                col.setCellRenderer(new DefaultTableCellRenderer() {
                    protected void setValue(Object o) {
                        if (o != null && o instanceof DisplayableSpType)
                            o = ((DisplayableSpType) o).displayValue();
                        super.setValue(o);
                    }
                });
            }

        }

    }


}
