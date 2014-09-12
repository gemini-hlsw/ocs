package jsky.app.ot.gemini.flamingos2;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;

import javax.swing.table.AbstractTableModel;

/**
 * A helper class for displaying the offset position table in the Flamingos2
 * editor. The model displays a table with 3 columns and 2 rows (one per offset
 * position).  The columns display the index of the position (0 or 1), the
 * offset in p (which is always 0), and the offset in q (which is the only
 * variable).
 */
final class OffsetTableModel extends AbstractTableModel {
    enum Column {
        index() {
            public String getColumnName() {
                return "Index";
            }
            public String getValueAt(int row, Option<Double> offset) {
                if (None.instance().equals(offset)) return "--";
                return String.valueOf(row);
            }
        },
        p() {
            public String getColumnName() {
                return "p";
            }
            public String getValueAt(int row, Option<Double> offset) {
                if (None.instance().equals(offset)) return "--";
                return "0";
            }
        },
        q() {
            public String getColumnName() {
                return "q";
            }
            public String getValueAt(int row, Option<Double> offset) {
                if (None.instance().equals(offset)) return "--";

                double val = offset.getValue();
                if (row == 1) val = -val;
                return String.format("%3.2f", val);
            }
        },;

        public abstract String getColumnName();
        public abstract String getValueAt(int row, Option<Double> offset);
    }

    private Option<Double> offset = None.instance();

    public int getRowCount() {
        return 2;
    }

    public int getColumnCount() {
        return Column.values().length;
    }

    public String getColumnName(int columnIndex) {
        return Column.values()[columnIndex].getColumnName();
    }

    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return Column.values()[columnIndex].getValueAt(rowIndex, offset);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    public void setOffset(Option<Double> offset) {
        this.offset = offset;
        int col = Column.q.ordinal();
        fireTableDataChanged();
    }
}