package jsky.app.ot.gemini.gmos;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.StandardGuideOptions;
import edu.gemini.spModel.target.*;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosListWatcher;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import jsky.util.gui.TableUtil;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * An extension of the TableWidget to support telescope offset lists.
 */
public class GmosOffsetPosTableWidget<P extends OffsetPosBase> extends jsky.util.gui.TableWidget
        implements TelescopePosWatcher, OffsetPosListWatcher<P>, PropertyChangeListener {

    private ISPNode node;

    // List of offset positions
    private OffsetPosList<P> _opl;

    // Array of offset positions
    private List<P> _tpList;

    // If true, ignore selection events
    private boolean _ignoreSelection = false;

    /**
     * Constructor.
     */
    public GmosOffsetPosTableWidget() {
        super();

        // Allow multiple, disjoint selections, so that menu items in the GUI can
        // operate on multiple offset positions at once
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // keep the offset position list selections in sync with the table selections
        getSelectionModel().addListSelectionListener(e -> {
            if (_opl == null || _ignoreSelection || e.getValueIsAdjusting())
                return;
            _updatePosListSelection();
        });
    }

    public void telescopePosUpdate(final WatchablePos tp) {
        if (!(tp instanceof OffsetPosBase)) {
            // This shouldn't happen ...
            System.out.println(getClass().getName() + ": received a position " +
                    " update for a non-offset position: " + tp);
            return;
        }
        _updatePos((P) tp);
    }

    /**
     * Reinitialize the table.
     */
    public void reinit(final ISPNode node, final OffsetPosList<P> opl) {

        OffsetPosSelection.deafTo(this.node, this);

        if ((_opl != opl) && (_opl != null)) {
            // Quit watching previous positions
            _opl.deleteWatcher(this);

            final List<P> allList = _opl.getAllPositions();
            for (final P op : allList) {
                op.deleteWatcher(this);
            }
        }

        // Watch the new list, and add all of its positions at once
        this.node = node;
        _opl = opl;

        OffsetPosSelection.listenTo(this.node, this);

        if (_opl != null) {
            _opl.addWatcher(this);
            _tpList = _opl.getAllPositions();
            _insertAllPos(_tpList);
        }
    }

    /**
     * A position has been selected so select the corresponding table row.
     */
    public void propertyChange(PropertyChangeEvent evt) {
      if (!OffsetPosSelection.PROP.equals(evt.getPropertyName())) return;
      _updateTableSelection();
    }

    // Update the table selection to reflect the position list selection settings
    private void _updateTableSelection() {
        _ignoreSelection = true;
        try {
            ListSelectionModel lsm = getSelectionModel();
            lsm.clearSelection();
            List<P> selList = OffsetPosSelection.apply(node).selectedPositions(_opl);
            for (P pos : selList) {
                int index = _opl.getPositionIndex(pos);
                lsm.addSelectionInterval(index, index);
            }
        } finally {
            _ignoreSelection = false;
        }
    }

    // Update the position list selection settings to reflect the current table selection
    private void _updatePosListSelection() {
        _ignoreSelection = true;
        try {
            ListSelectionModel lsm = getSelectionModel();
            if (!lsm.isSelectionEmpty()) {
                int n1 = lsm.getMinSelectionIndex();
                int n2 = lsm.getMaxSelectionIndex();
                List<P> selList = new ArrayList<>();
                for (int i = n1; i <= n2; i++) {
                    if (lsm.isSelectedIndex(i)) {
                        P tp = _opl.getPositionAt(i);
                        if (tp != null) {
                            selList.add(tp);
                        }
                    }
                }
                OffsetPosSelection.select(_opl, selList).commit(node);
            }
        } finally {
            _ignoreSelection = false;
        }
    }

    public void posListReset(OffsetPosList<P> tpl) {
        if (tpl != _opl) return;
        _tpList = tpl.getAllPositions();
        _insertAllPos(_tpList);
    }

    public void posListAddedPosition(OffsetPosList<P> tpl, List<P> tp) {
        posListReset(tpl);
    }

    public void posListRemovedPosition(OffsetPosList<P> tpl, List<P> tp) {
        posListReset(tpl);
    }

    public void posListPropertyUpdated(OffsetPosList<P> tpl, String propertyName, Object oldValue, Object newValue) {
        // ignore, irrelevant
    }

    /**
     * Return a vector for a row in the table corresponding to the given offset position.
     */
    private Vector<String> _createPosRow(P op, int index) {
        Vector<String> v = new Vector<>(7);
        v.addElement(String.valueOf(index));
        v.addElement(op.getXAxisAsString());
        v.addElement(op.getYAxisAsString());
        v.addElement(getGuideOption(op).name());
        return v;
    }


    /**
     * Return the tag for the given WFS and offset position,
     * or "park" for no change.
     *
     * @param op the offset position
     */
    public GuideOption getGuideOption(P op) {
        GuideOption opt = op.getLink(GmosOiwfsGuideProbe.instance);
        return (opt == null) ? StandardGuideOptions.Value.park : opt;
    }


    /**
     * Add all of the offset positions to the table.
     */
    private void _insertAllPos(List<P> allList) {
        Vector[] dataV = new Vector[allList.size()];
        for (int i = 0; i < allList.size(); ++i) {
            P op = allList.get(i);
            op.addWatcher(this);
            dataV[i] = _createPosRow(op, i);
        }
        setRows(dataV);

        // restore the selection
        _updateTableSelection();

        TableUtil.initColumnSizes(this);
    }


    /**
     * Update the table row for the given offset position.
     */
    private void _updatePos(P op) {
        int index = _opl.getPositionIndex(op);
        if (index == -1) {
            //System.out.println("_updatePos: couldn't find " + op.tag);
            return;
        }

        Vector v = _createPosRow(op, index);
        int n = Math.min(v.size(), getModel().getColumnCount());
        for (int i = 0; i < n; i++) {
            setCell(v.elementAt(i), i, index);
        }
    }


    /**
     * Get the position that is currently selected.
     */
    public P getSelectedPos() {
        int[] rows = getSelectedRowIndexes();
        if (rows.length == 0) {
            return null;
        }

        return _opl.getPositionAt(rows[0]);
    }


    /**
     * Select a given position.
     */
    public void selectPos(P tp) {
        OffsetPosSelection.select(_opl, tp).commit(node);
    }
}

