package jsky.app.ot.tpe.gems;

import jsky.catalog.gui.SymbolSelectionEvent;
import jsky.catalog.gui.SymbolSelectionListener;
import jsky.catalog.gui.TableDisplay;
import jsky.catalog.gui.TablePlotter;
import jsky.util.gui.DialogUtil;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * OT-111: Displays the results of the catalog search.
 */
class CandidateGuideStarsTable extends TableDisplay {

    private boolean _ignoreSelection;
    private final TablePlotter _plotter;
    private CandidateGuideStarsTableModel _tableModel;

    // Used to select a table row when the symbol is selected
    private SymbolSelectionListener symbolListener = new SymbolSelectionListener() {

        public void symbolSelected(SymbolSelectionEvent e) {
            if (!_ignoreSelection && e.getTable() == getTableQueryResult()) {
                _ignoreSelection = true;
                try {
                    selectRow(e.getRow());
                } catch (Exception ex) {
                    // Ignore
                }
                _ignoreSelection = false;
            }
        }

        public void symbolDeselected(SymbolSelectionEvent e) {
            if (!_ignoreSelection && e.getTable() == getTableQueryResult()) {
                _ignoreSelection = true;
                try {
                    deselectRow(e.getRow());
                } catch (Exception ex) {
                    // Ignore
                }
                _ignoreSelection = false;
            }
        }
    };

    // Used to select a plot symbol when the table row is selected
    private ListSelectionListener selectionListener = new ListSelectionListener() {

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting() || _ignoreSelection)
                return;
            ListSelectionModel model = getTable().getSelectionModel();
            int first = e.getFirstIndex();
            int last = e.getLastIndex();
            if (first != -1 && last != -1) {
                for (int i = first; i <= last; i++) {
                    int index = getTable().getSortedRowIndex(i);
                    if (model.isSelectedIndex(i)) {
                        _plotter.selectSymbol(getTableQueryResult(), index);
                    } else {
                        _plotter.deselectSymbol(getTableQueryResult(), index);
                    }
                }
            }
        }
    };


    public CandidateGuideStarsTable(TablePlotter plotter) {
        super();
        _plotter = plotter;
        getTable().setSortingAllowed(false); // REL-560: Sorting code doesn't seem to handle editable columns correctly
    }

    public void setTableModel(CandidateGuideStarsTableModel tableModel) {
        unplot();
        _tableModel = tableModel;
        setModel(_tableModel.getTableQueryResult());
        plot();
    }

    public CandidateGuideStarsTableModel getTableModel() {
        return _tableModel;
    }

    /**
     * Plot the contents of the table.
     */
    public void plot() {
        if (_plotter != null) {
            getTable().clearSelection(); // do this or add code to keep selections in sync
            _plotter.addSymbolSelectionListener(symbolListener);
            getTable().getSelectionModel().addListSelectionListener(selectionListener);
            try {
                _plotter.plot(getTableQueryResult());
            } catch (Exception e) {
                DialogUtil.error(this, e);
            }
        }
    }

    /**
     * Remove any plot symbols for this table.
     */
    public void unplot() {
        if (_plotter != null && getTableQueryResult() != null) {
            _plotter.removeSymbolSelectionListener(symbolListener);
            getTable().getSelectionModel().removeListSelectionListener(selectionListener);
            _plotter.unplot(getTableQueryResult());
        }
    }

    public void clear() {
        unplot();
        getTable().setModel(new DefaultTableModel());
    }
}
