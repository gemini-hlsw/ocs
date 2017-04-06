package jsky.app.ot.gemini.obslog;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemEntry;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.event.EndDatasetEvent;
import edu.gemini.spModel.event.ObsExecEvent;
import edu.gemini.spModel.event.StartDatasetEvent;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.obslog.ObsQaLog;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Table models used by the Obslog GUI.
 *
 * @author rnorris
 */
public interface ObslogTableModels {

    /**
     * Abstract table model for an array of
     * {@link edu.gemini.spModel.dataset.DatasetRecord}s that handles property
     * change events in QA information by delegating them to the subclass's
     * {@link #propertyChange(int, ObsQaLog.Event)} implementation.
     *
     * @author rnorris
     * @version $Id: ObslogTableModels.java 46728 2012-07-12 16:39:26Z rnorris $
     */
    abstract class AbstractDatasetRecordTableModel extends AbstractTableModel {

        /**
         * The {@link edu.gemini.spModel.dataset.DatasetExecRecord} instances known to this model.
         */
        final ObsLog obsLog;
        final List<DatasetRecord> records;

        /**
         * Constructs a table model that keeps an array of {@link edu.gemini.spModel.dataset.DatasetExecRecord} instances in
         * protected member {@link #records} and registers for change events on each.
         */
        AbstractDatasetRecordTableModel(ObsLog obsLog) {
            this(obsLog, obsLog.getAllDatasetRecords());
        }

        AbstractDatasetRecordTableModel(ObsLog obsLog, List<DatasetRecord> records) {
            this.obsLog  = obsLog;
            this.records = new ArrayList<>(records);

            obsLog.qaLogDataObject.addDatasetQaRecordListener(event -> {
                final List<DatasetRecord> records1 = AbstractDatasetRecordTableModel.this.records;
                final DatasetLabel l = event.newRec.label;
                int row = -1;
                int i = 0;
                for (DatasetRecord r : records1) {
                    if (r.label().equals(l)) {
                        row = i;
                        break;
                    }
                    ++i;
                }
                if (row >= 0) {
                    records1.set(row, records1.get(row).withQa(event.newRec));
                    propertyChange(row, event);
                }
            });
        }

        boolean isUnavailable(int row) {
            final DatasetRecord rec = records.get(row);
            return !obsLog.getExecRecord().inSummitStorage(rec.label());
        }

        public int getRowCount() {
            return records.size();
        }

        /**
         * Called when the dataset QA information in a given row has changed.
         * If this change corresponds to a column in the table model, the
         * model should fire an appropriate table cell modification event.
         */
        protected abstract void propertyChange(int row, ObsQaLog.Event event);

    }

    /**
     * A concrete implementation of AbstractDatasetRecordTableModel that provides the following
     * columns:
     * <ol>
     * <li>label
     * <li>filename
     * <li>qa state (editable if specified in constructor)
     * <li>data flow (editable if specified in constructor)
     * </ol>
     *
     * @author rnorris
     * @version $Id: ObslogTableModels.java 46728 2012-07-12 16:39:26Z rnorris $
     */
    class DatasetAnalysisTableModel extends AbstractDatasetRecordTableModel {

        static final int COL_LABEL    = 0;
        static final int COL_FILENAME = 1;
        static final int COL_QA_STATE = 2;
        static final int COL_STATUS   = 3;

        private static final String[] COL_NAMES = new String[]{
                "Label", "Filename", "QA State", "Dataset Status"
        };

        DatasetAnalysisTableModel(ObsLog log) {
            super(log);
        }

        DatasetAnalysisTableModel(ObsLog log, List<DatasetRecord> records) {
            super(log, records);
        }

        public int getColumnCount() {
            return COL_NAMES.length;
        }

        public String getColumnName(int i) {
            return COL_NAMES[i];
        }

        DatasetRecord getRecordAt(int row) {
            return records.get(row);
        }

        public Object getValueAt(int row, int col) {
            final DatasetRecord rec = records.get(row);
            switch (col) {
                case COL_LABEL:
                    return rec.label();
                case COL_FILENAME:
                    return rec.exec().dataset().getDhsFilename();

                case COL_QA_STATE:
                    final DatasetQaState qs = rec.qa().qaState;
                    if (isUnavailable(row))
                        return qs.displayValue() + "*";
                    return qs;

                case COL_STATUS:
                    return DataflowStatus$.MODULE$.derive(rec);

                default:
                    throw new IllegalArgumentException("Unknown column: " + col);
            }
        }

        public void setValueAt(Object val, int row, int col) {
            final DatasetRecord rec = records.get(row);
            switch (col) {
                case COL_QA_STATE:
                    final DatasetQaState qaState = (DatasetQaState) val;
                    obsLog.qaLogDataObject.setQaState(rec.label(), qaState);
                    break;
                default:
                    throw new IllegalArgumentException("Cannot modify value in column: " + col);
            }
        }

        protected void propertyChange(int row, ObsQaLog.Event event) {
            if (event.isQaStateUpdated())  fireTableRowsUpdated(row, row);
        }

    }

    /**
     * A concrete implementation of AbstractDatasetRecordTableModel that provides the following
     * columns, all read-only:
     * <ol>
     * <li>label
     * <li>filename
     * <li>comment
     * </ol>
     *
     * @author rnorris
     * @version $Id: ObslogTableModels.java 46728 2012-07-12 16:39:26Z rnorris $
     */
    class CommentTableModel extends AbstractDatasetRecordTableModel {

        static final int COL_LABEL = 0;
        static final int COL_FILENAME = 1;
        static final int COL_COMMENT = 2;

        private static final String[] COL_NAMES = new String[]{
                "Label", "Filename", "Comment",
        };

        CommentTableModel(ObsLog log) {
            super(log);
        }

        public int getColumnCount() {
            return COL_NAMES.length;
        }

        public String getColumnName(int i) {
            return COL_NAMES[i];
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Object getValueAt(int row, int col) {
            final DatasetRecord rec = records.get(row);
            switch (col) {
                case COL_LABEL:
                    return rec.label();
                case COL_FILENAME:
                    return rec.exec().dataset().getDhsFilename();
                case COL_COMMENT:
                    return rec.qa().comment;
                default:
                    throw new IllegalArgumentException("Unknown column: " + col);
            }
        }

        public void setValueAt(Object value, int row, int col) {
            final DatasetRecord rec = records.get(row);
            switch (col) {
                case COL_COMMENT:
                    obsLog.qaLogDataObject.setComment(rec.label(), (String) value);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown or immutable column: " + col);
            }
        }

        protected void propertyChange(int row, ObsQaLog.Event event) {
            if (event.isCommentUpdated()) fireTableCellUpdated(row, COL_COMMENT);
        }
    }

    /**
     * Table model for a {@link Config} object, with read-only columns:
     * <ol>
     * <li>parameter name
     * <li>value
     * </ol>
     *
     * @author rnorris
     * @version $Id: ObslogTableModels.java 46728 2012-07-12 16:39:26Z rnorris $
     */
    class ConfigTableModel extends AbstractTableModel {

        static final int COL_PARAMETER = 0;
        static final int COL_VALUE = 1;

        private static final String[] COL_NAMES = new String[]{
                "Parameter", "Value",
        };

        private final Config config;

        ConfigTableModel(Config config) {
            this.config = config;
        }

        public int getRowCount() {
            return config.size();
        }

        public int getColumnCount() {
            return COL_NAMES.length;
        }

        public String getColumnName(int i) {
            return COL_NAMES[i];
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Object getValueAt(int row, int col) {
            final ItemEntry e = config.itemEntries()[row];
            switch (col) {
                case COL_PARAMETER:
                    return e.getKey();
                case COL_VALUE:
                    return e.getItemValue();
                default:
                    throw new IllegalArgumentException("Unknown column: " + col);
            }
        }

    }

    /**
     * Table model for (@link ObsExecEvent}s that provides the following read-only columns:
     * <ol>
     * <li>time
     * <li>event
     * <li>dataset
     * </ol>
     *
     * @author rnorris
     * @version $Id: ObslogTableModels.java 46728 2012-07-12 16:39:26Z rnorris $
     */
    class EventTableModel extends AbstractTableModel {

        static final int COL_TIME = 0;
        static final int COL_EVENT = 1;
        static final int COL_DATASET = 2;

        private static final String[] COL_NAMES = new String[]{
                "Time", "Event", "Dataset",
        };

        private final ObsExecEvent[] events;

        EventTableModel(ObsExecEvent[] events) {
            this.events = events;
        }

        public int getRowCount() {
            return events.length;
        }

        public int getColumnCount() {
            return COL_NAMES.length;
        }

        public String getColumnName(int i) {
            return COL_NAMES[i];
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Object getValueAt(int row, int col) {
            final ObsExecEvent e = events[row];
            switch (col) {

                case COL_TIME:
                    return ObslogGUI.OBSLOG_DATE_FORMAT.format(new Date(e.getTimestamp()));

                case COL_EVENT:
                    return e.getName();

                case COL_DATASET:

                    // Only exists for dataset events
                    if (e instanceof StartDatasetEvent) {
                        return ((StartDatasetEvent) e).getDataset().getLabel();
                    } else if (e instanceof EndDatasetEvent) {
                        return ((EndDatasetEvent) e).getDatasetLabel();
                    } else {
                        return null;
                    }

                default:
                    throw new IllegalArgumentException("Unknown column: " + col);
            }
        }
    }
}





