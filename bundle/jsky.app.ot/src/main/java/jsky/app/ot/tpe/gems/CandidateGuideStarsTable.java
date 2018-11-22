package jsky.app.ot.tpe.gems;

import edu.gemini.shared.util.immutable.ImOption;
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
final class CandidateGuideStarsTable extends TableDisplay {

    // Updating the table model changes the state in some inscrutable way in
    // the TPE plotting code that *requires* a replot even if the actual list of
    // candidates has not changed. To keep up with whether a replot is needed,
    // we simply keep a running count of model updates.  Obviously, this is a
    // hack.
    private int _modelVersion = 0;

    private boolean _ignoreSelection;
    private final TablePlotter _plotter;
    private CandidateGuideStarsTableModel _tableModel;

    // Used to select a table row when the symbol is selected
    private SymbolSelectionListener symbolListener = new SymbolSelectionListener() {

        public void symbolSelected(final SymbolSelectionEvent e) {
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

        public void symbolDeselected(final SymbolSelectionEvent e) {
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
            final ListSelectionModel model = getTable().getSelectionModel();
            final int first = e.getFirstIndex();
            final int last = e.getLastIndex();
            if (first != -1 && last != -1) {
                for (int i = first; i <= last; i++) {
                    final int index = getTable().getSortedRowIndex(i);
                    if (model.isSelectedIndex(i)) {
                        _plotter.selectSymbol(getTableQueryResult(), index);
                    } else {
                        _plotter.deselectSymbol(getTableQueryResult(), index);
                    }
                }
            }
        }
    };


    CandidateGuideStarsTable(final TablePlotter plotter) {
        super();
        _plotter = plotter;
        getTable().setSortingAllowed(false); // REL-560: Sorting code doesn't seem to handle editable columns correctly
    }

    public void setTableModel(final CandidateGuideStarsTableModel tableModel) {
        unlink();
        _tableModel = tableModel;
        setModel(_tableModel.getTableQueryResult());

        _modelVersion++;

        // Set up 2-way correspondence between table and TPE plot.  Selections
        // in one update the selection the other.
        getTable().clearSelection(); // do this or add code to keep selections in sync
        _plotter.addSymbolSelectionListener(symbolListener);
        getTable().getSelectionModel().addListSelectionListener(selectionListener);
    }

    /**
     * Obtains the count of times that a call to `setTableModel` has been made,
     * which provides an indication of whether a plot is needed.
     */
    public int getModelVersion() {
        return _modelVersion;
    }

    public CandidateGuideStarsTableModel getTableModel() {
        return _tableModel;
    }

    /**
     * Plot the contents of the table.
     */
    public void plot() {
        try {
            ImOption.apply(getTableQueryResult()).foreach(_plotter::plot);
        } catch (Exception e) {
            DialogUtil.error(this, e);
        }
    }

    /**
     * Remove 2-way correspondence between candidates table and TPE plot symbols.
     */
    private void unlink() {
        _plotter.removeSymbolSelectionListener(symbolListener);
        getTable().getSelectionModel().removeListSelectionListener(selectionListener);
    }

    /**
     * Remove any plot symbols for this table.
     */
    public void unplot() {
        ImOption.apply(getTableQueryResult()).foreach(_plotter::unplot);
    }

    public void clear() {
        unlink();
        getTable().setModel(new DefaultTableModel());
    }
}
