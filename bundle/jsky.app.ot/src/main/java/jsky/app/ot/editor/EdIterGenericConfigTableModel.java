//
// $Id: EdIterGenericConfigTableModel.java 8164 2007-10-05 20:23:17Z swalker $
//

package jsky.app.ot.editor;

import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

import javax.swing.table.AbstractTableModel;
import java.beans.PropertyDescriptor;
import java.util.*;

/**
 *
 */
public class EdIterGenericConfigTableModel extends AbstractTableModel {
    private Comparator<PropertyDescriptor> DISPLAY_NAME_COMPARATOR = new Comparator<PropertyDescriptor>() {
        public int compare(PropertyDescriptor p1, PropertyDescriptor p2) {
            return p1.getDisplayName().compareTo(p2.getDisplayName());
        }
    };

    // Contains the data displayed in the table.
    private ISysConfig _config;

    // List of PropertyDescriptor, one for each column in the table, in the
    // order that they should appear in the iterator.
    private List<PropertyDescriptor> _cols = new ArrayList<PropertyDescriptor>();
    private int _rowCount;

    EdIterGenericConfigTableModel(ISysConfig config, Map<String, PropertyDescriptor> props) {
        _config = config;

        int maxRows = -1;
        boolean equalRowSizes = true;

        Set<String> rmProps = new HashSet<String>();
        for (String propName : config.getParameterNames()) {
            // Record the property descriptor.
            PropertyDescriptor pd = props.get(propName);
            if (pd == null) {
                rmProps.add(propName);
                continue;
            }
            _cols.add(pd);

            // See if we need to update the max row size.
            List<Object> vals;
            vals = (List<Object>) config.getParameterValue(propName);
            if (vals.size() > maxRows){
                equalRowSizes = (maxRows == -1);
                maxRows = vals.size();
            }
        }

        // Clean out crap from previous releases that no longer matter.
        for (String propName : rmProps) {
            _config.removeParameter(propName);
        }

        // If necessary, make all the property values be a list of the same
        // size (grow the property lists that are shorter).
        if (!equalRowSizes) {
            for (PropertyDescriptor pd : _cols) {
                List<Object> vals = getColumnValues(pd);
                for (int i=vals.size(); i<maxRows; ++i) {
                    vals.add(null);
                }
            }
        }
        _rowCount = Math.max(maxRows, 0);

        Collections.sort(_cols, DISPLAY_NAME_COMPARATOR);
    }

    public int getColumnIndex(PropertyDescriptor pd) {
        return _cols.indexOf(pd);
    }

    public PropertyDescriptor getPropertyDescriptor(int col) {
        return _cols.get(col);
    }

    private List<Object> getColumnValues(int col) {
        return getColumnValues(getPropertyDescriptor(col));
    }

    private List<Object> getColumnValues(PropertyDescriptor pd) {
        return (List<Object>) _config.getParameterValue(pd.getName());
    }

    ISysConfig getSysConfig() {
        // just returns a reference, which is a bit dangerous since any
        // modifications made externally won't fire events or keep the rows
        // the same size etc.
        return _config;
    }

    public String getColumnName(int index) {
        return _cols.get(index).getDisplayName();
    }

    public int getRowCount() {
        return _rowCount;
    }

    public int getColumnCount() {
        return _cols.size();
    }

    public Object getValueAt(int row, int column) {
        List<Object> vals = getColumnValues(column);
        return vals.get(row);
    }

    public void setValueAt(Object val, int row, int column) {
        List<Object> vals = getColumnValues(column);
        vals.set(row, val);
        fireTableCellUpdated(row, column);
    }

    public void addRow(int row) {
        for (PropertyDescriptor pd : _cols) {
            List<Object> vals = getColumnValues(pd);
            Object rowVal = null;
            if (row != 0) {
                rowVal = vals.get(row-1);
            }
            vals.add(row, rowVal);
        }
        ++_rowCount;
        fireTableRowsInserted(row, row);
    }

    public void removeRow(int row) {
        for (PropertyDescriptor pd : _cols) {
            List<Object> vals = getColumnValues(pd);
            vals.remove(row);
        }
        --_rowCount;
        if (_rowCount > 0) {
            fireTableRowsDeleted(row, row);
        } else {
            _cols.clear();
            _config.removeParameters();
            fireTableStructureChanged();
        }
    }

    public void moveRow(int from, int to) {
        for (PropertyDescriptor pd : _cols) {
            List<Object> vals = getColumnValues(pd);
            vals.add(to, vals.remove(from));  // inefficient but simple
        }

        int begin = from;
        int end   = to;
        if (begin > end) {
            end   = from;
            begin = to;
        }
        fireTableRowsUpdated(begin, end);
    }

    public void addColumn(PropertyDescriptor pd, Object rowValue) {
        List<Object> vals = getColumnValues(pd);
        if (vals != null) return;

        vals = new ArrayList<Object>(_rowCount);
        for (int i=0; i<_rowCount; ++i) vals.add(rowValue);
        IParameter param = DefaultParameter.getInstance(pd.getName(), vals);
        _config.putParameter(param);

        _cols.add(pd);
        Collections.sort(_cols, DISPLAY_NAME_COMPARATOR);
        fireTableStructureChanged();
    }

    public void removeColumn(int column) {
        PropertyDescriptor pd = _cols.get(column);
        _cols.remove(column);
        _config.removeParameter(pd.getName());

        if (_cols.size() == 0) {
            _rowCount = 0;
        }
        fireTableStructureChanged();
    }
}
