package jsky.app.ot.gemini.obslog;

import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.event.ObsExecEvent;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obsrecord.ObsVisit;
import edu.gemini.spModel.obsrecord.UniqueConfig;
import edu.gemini.spModel.type.DisplayableSpType;
import jsky.app.ot.OTOptions;
import jsky.util.gui.DropDownListBoxWidget;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * GUI component for the ObsLog editor component.
 * @author rnorris
 */
public class ObslogGUI extends JPanel {

	private static final int COL_WIDTH_LABEL = 150;
	private static final int COL_WIDTH_FILENAME = 125;

	private final DataAnalysisComponent tabDataAnalysis = new DataAnalysisComponent("Data Analysis");
	private final VisitsComponent tabVisits = new VisitsComponent("Visits");
	private final CommentsComponent tabComments = new CommentsComponent("Comments");

    static final DateFormat OBSLOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z") {{
		setTimeZone(TimeZone.getTimeZone("UTC"));
	}};

	public ObslogGUI() {
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
	void setup(ObsLog obsLog) {

		// And tell the tabs that the world has changed.
		tabDataAnalysis.setObsLog(obsLog);
		tabVisits.setObsLog(obsLog);
		tabComments.setObsLog(obsLog);

	}



class AbstractDatasetRecordTable extends JTable implements ObslogTableModels {

	protected AbstractDatasetRecordTable() {
	}

	protected AbstractDatasetRecordTable(TableModel model) {
		super(model);
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

	/** This is very, very bad. **/
	protected void lockColumnWidth(int colIndex, int width) {
		final TableColumn col = getColumnModel().getColumn(colIndex);
		col.setMinWidth(width);
//		col.setMaxWidth(width);
		col.setPreferredWidth(width);
	}

}

/**
 * Component that shows a table of {@link edu.gemini.spModel.dataset.DatasetExecRecord}s with
 * editable fields for QA State. Editing is disabled unless the OT is running in on site mode.
 * @author rnorris
 */
class DataAnalysisComponent extends JPanel implements ObslogTableModels {

	private final DataAnalysisTable table = new DataAnalysisTable();
	private final JPanel editArea = new Editor();

	public DataAnalysisComponent(String name) {
		super(new BorderLayout());
		setName(name);

        if (OTOptions.isStaffGlobally()) {
            configureForStaff();
		} else {
            configureForPi();
		}
	}

    private boolean isConfiguredForStaffEditing() {
        for (Component c : getComponents()) if (c instanceof JSplitPane) return true;
        return false;
    }

    private void reconfigure() {
        if (isConfiguredForStaffEditing() != OTOptions.isStaffGlobally()) {
            removeAll();
            if (OTOptions.isStaffGlobally()) {
                configureForStaff();
            } else {
                configureForPi();
            }
        }
    }

    private void configureForPi() {
        add(new JScrollPane(table));
    }
    private void configureForStaff() {
        // OT-424: bulk editing support
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(new Header("Select multiple rows for bulk updating."), BorderLayout.NORTH);
        panel.add(editArea, BorderLayout.CENTER);
        add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table) {{
                setBorder(BorderFactory.createEmptyBorder());
            }}, panel) {{
            setResizeWeight(1.0);
            setEnabled(false);
        }});
    }

	void setObsLog(final ObsLog obsLog) {
        reconfigure();
        table.setModel(new DatasetAnalysisTableModel(obsLog));
		table.setEnabled(true);
		editArea.setEnabled(true);
	}

	// OT-424: bulk editing support
	class Editor extends JPanel implements ListSelectionListener {

		final JComboBox qa; // , df;
		boolean adjusting; // sigh

		public Editor() {
			super(new FlowLayout());
			setBorder(BorderFactory.createEmptyBorder());

			// QA State
			add(new JLabel("QA State:"));
			add(qa = new DropDownListBoxWidget() {{
				setChoices(DatasetQaState.values());
				setRenderer(new SpTypeCellRenderer());
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (!adjusting) {
							setCommonValue(DatasetAnalysisTableModel.COL_QA_STATE, getSelectedItem());
                        }
                    }
				});
			}});

			// Dataflow
//			add(new JLabel("Dataflow:"));
//			add(df = new DropDownListBoxWidget() {{
//				setChoices(DataflowStep.TYPES);
//				setRenderer(new SPTypeBaseListCellRenderer());
//				addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						if (!adjusting)
//							setCommonValue(DatasetAnalysisTableModel.COL_DATAFLOW, getSelectedItem());
//					}
//				});
//			}});

			table.editArea = this;
			table.getSelectionModel().addListSelectionListener(this);

		}

		public void setEnabled(boolean enable) {
			super.setEnabled(enable);
			qa.setEnabled(enable);
//			df.setEnabled(enable);
			if (!enable) {
				adjusting = true;
				qa.setSelectedItem(null);
//				df.setSelectedItem(null);
				adjusting = false;
			}
		}

		public void valueChanged(ListSelectionEvent lse) {
			if (lse.getValueIsAdjusting() || table.adjusting ) return;
			final ListSelectionModel lsm = (ListSelectionModel) lse.getSource();
			if (!lsm.isSelectionEmpty()) {
				setEnabled(true);
				adjusting = true;
				qa.setSelectedItem(getCommonValue(DatasetAnalysisTableModel.COL_QA_STATE));
//				df.setSelectedItem(getCommonValue(DatasetAnalysisTableModel.COL_DATAFLOW));
				adjusting = false;
			} else {
				setEnabled(false);
			}
		}

//		private boolean isEditableSelection() {
//			ListSelectionModel lsm = table.getSelectionModel();
//			if (!lsm.isSelectionEmpty()) {
////
//// RCN: we need to leave this feature off for now due to migration issues.
////
////				DatasetAnalysisTableModel model = (DatasetAnalysisTableModel) table.getModel();
////				for (int row = lsm.getMinSelectionIndex(); row <= lsm.getMaxSelectionIndex(); row++) {
////					if (lsm.isSelectedIndex(row) && model.isUnavailable(row))
////						return false;
////				}
//
//				return true;
//			}
//			return false;
//		}

		private Object getCommonValue(int col) {
			Object ret = null;
			final ListSelectionModel lsm = table.getSelectionModel();
			if (!lsm.isSelectionEmpty()) {
				final TableModel model = table.getModel();
				for (int row = lsm.getMinSelectionIndex(); row <= lsm.getMaxSelectionIndex(); row++) {
					if (lsm.isSelectedIndex(row)) {
						final Object val = model.getValueAt(row, col);
						if (ret == null) {
							ret = val;
						} else if (ret != val) {
							return null;
						}
					}
				}
			}
			return ret;
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

	class SpTypeCellRenderer extends DefaultListCellRenderer {
		public Component getListCellRendererComponent(JList arg0, Object o, int arg2, boolean arg3, boolean arg4) {
			if (o != null) {
                            if (o instanceof DisplayableSpType) {
                                o = ((DisplayableSpType) o).displayValue();
                            }
			}
			return super.getListCellRendererComponent(arg0, o, arg2, arg3, arg4);
		}
	}

	class DataAnalysisTable extends AbstractDatasetRecordTable {

		JPanel editArea;
		boolean adjusting = false;

		public DataAnalysisTable() {
			setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}

		@Override
        public void setModel(TableModel newModel) {

			// OT-640: Preserve selection
			if (newModel instanceof DatasetAnalysisTableModel && getModel() instanceof DatasetAnalysisTableModel) {

				final DatasetAnalysisTableModel prev = (DatasetAnalysisTableModel) getModel();
				final DatasetAnalysisTableModel next = (DatasetAnalysisTableModel) newModel;

				// Collect the set of labels. This may be inefficient but it's safe.
				final Collection<DatasetLabel> selection = new TreeSet<DatasetLabel>();
				final ListSelectionModel lsm = getSelectionModel();
				for (int i = lsm.getMinSelectionIndex(); i <= lsm.getMaxSelectionIndex(); i++) {
					if (lsm.isSelectedIndex(i))
						selection.add(prev.records.get(i).getLabel());
				}

				// Swap the model
				adjusting = true;
				super.setModel(next);
				adjusting = false;

				// And restore the selection
				for (int i = 0; i < next.records.size(); i++) {
					if (selection.contains(next.records.get(i).getLabel()))
						lsm.addSelectionInterval(i, i);
				}
				editArea.setEnabled(!lsm.isSelectionEmpty());

				// And reset the rendering stuff since the model has changed
				attachDescriptionRenderer(DatasetAnalysisTableModel.COL_QA_STATE);
				attachDescriptionRenderer(DatasetAnalysisTableModel.COL_GSA_STATE);
				lockColumnWidth(DatasetAnalysisTableModel.COL_LABEL, ObslogGUI.COL_WIDTH_LABEL);
				lockColumnWidth(DatasetAnalysisTableModel.COL_FILENAME, ObslogGUI.COL_WIDTH_FILENAME);

			} else {
				super.setModel(newModel);
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
 * Component that shows a table of {@link edu.gemini.spModel.dataset.DatasetExecRecord}s with an
 * editable detail view for comments. Editing is disabled unless the OT is running in -onsite mode.
 * @author rnorris
 * @version $Id: ObslogGUI.java 11863 2008-07-30 20:05:28Z swalker $
 */
class CommentsComponent extends JPanel implements ObslogTableModels, ListSelectionListener {

	private final JTable table;
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
            if (row > -1) table.getModel().setValueAt(area.getText(), row, CommentTableModel.COL_COMMENT);
        }
    };

	public CommentsComponent(String name) {
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

		public CommentsTable() {
			super();
			setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);
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
				lockColumnWidth(DatasetAnalysisTableModel.COL_LABEL, ObslogGUI.COL_WIDTH_LABEL);
				lockColumnWidth(DatasetAnalysisTableModel.COL_FILENAME, ObslogGUI.COL_WIDTH_FILENAME);
			}
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
 * @author rnorris
 */
class VisitsComponent extends JScrollPane implements ObslogTableModels {

	private final DateFormat OBSLOG_DATE_FORMAT = ObslogGUI.OBSLOG_DATE_FORMAT;
	private final Box clientArea;

	public VisitsComponent(String name) {
		super(new Box(BoxLayout.Y_AXIS));
		clientArea = (Box) getViewport().getView();
		clientArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
		setName(name);
	}

	void setObsLog(final ObsLog obsLog) {
		clientArea.removeAll();
		addVisits(clientArea, obsLog);
		revalidate();
	}

	private void addVisits(Container parent, ObsLog obsLog) {
		final ObsVisit[] visits = obsLog.getVisits();
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
			final List<DatasetRecord> records = new ArrayList<DatasetRecord>(labels.length);
			for (int i = 0; i < labels.length; i++) {
                records.add(obsLog.getDatasetRecord(labels[i]));
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
				case 0: 		title = "Config " + (i + 1); break	;
				case 1: 		title = "Config " + (i + 1) + " (Dataset " + labels[0].getIndex() + ")"; break;
				default:		title = "Config " + (i + 1) + " (Datasets " + labels[0].getIndex() + "-" + labels[labels.length - 1].getIndex() + ")";
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
				case 1: 		title = "Event (" + OBSLOG_DATE_FORMAT.format(new Date(events[0].getTimestamp())) + ")"; break;
				default: 	title = "Events (" + OBSLOG_DATE_FORMAT.format(new Date(events[0].getTimestamp())) + " to " + OBSLOG_DATE_FORMAT.format(new Date(events[events.length - 1].getTimestamp())) + ")"; break;
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

		public DatasetRecordTable(TableModel model) {
			super(model);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		@Override
        public void setModel(TableModel model) {
			super.setModel(model);
			if (model instanceof DatasetAnalysisTableModel) {

				attachDescriptionRenderer(DatasetAnalysisTableModel.COL_QA_STATE);
//				attachDescriptionRenderer(DatasetAnalysisTableModel.COL_DATASET_STATE);
				attachDescriptionRenderer(DatasetAnalysisTableModel.COL_GSA_STATE);
//				attachDescriptionRenderer(DatasetAnalysisTableModel.COL_DATAFLOW);

				lockColumnWidth(DatasetAnalysisTableModel.COL_LABEL, ObslogGUI.COL_WIDTH_LABEL);
				lockColumnWidth(DatasetAnalysisTableModel.COL_FILENAME, ObslogGUI.COL_WIDTH_FILENAME);

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