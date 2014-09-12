//
// $Id: SessionQueueTableModel.java 7892 2007-06-04 20:48:36Z gillies $
//

package jsky.app.ot.session;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.wdba.shared.QueuedObservation;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A table model for the session queue.  Contains QueuedObservation objects.
 */
public class SessionQueueTableModel extends AbstractTableModel {

    private enum Col {
        id("Id", SPObservationID.class) {
            Object getValue(QueuedObservation obs) {
                return obs.getId();
            }
        },
        title("Title", String.class) {
            Object getValue(QueuedObservation obs) {
                return obs.getTitle();
            }
        },
        ;

        private String _name;
        private Class _class;

        Col(String name, Class c) {
            _name  = name;
            _class = c;
        }

        String getName() {
            return _name;
        }

        Class getColumnClass() {
            return _class;
        }

        abstract Object getValue(QueuedObservation obs);
    }

    private List<QueuedObservation> _queue = new ArrayList<QueuedObservation>();

    public int findColumn(String columnName) {
        for (Col c : Col.values()) {
            if (columnName.equals(c.getName())) return c.ordinal();
        }
        return -1;
    }

    public String getColumnName(int col) {
        return Col.values()[col].getName();
    }

    public Class<?> getColumnClass(int col) {
        return Col.values()[col].getColumnClass();
    }

    public int getRowCount() {
        return _queue.size();
    }

    public int getColumnCount() {
        return Col.values().length;
    }

    public Object getValueAt(int row, int col) {
        return Col.values()[col].getValue(_queue.get(row));
    }

    public QueuedObservation getQueuedObservation(int row) {
        return _queue.get(row);
    }

    public SPObservationID getId(int row) {
        return _queue.get(row).getId();
    }

    public String getTitle(int row) {
        return _queue.get(row).getTitle();
    }

    void setQueuedObservations(Collection<QueuedObservation> obsCollection) {
        _queue.clear();
        _queue.addAll(obsCollection);
        fireTableDataChanged();
    }
}
