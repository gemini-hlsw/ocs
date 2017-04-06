package jsky.app.ot.gemini.editor.offset;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.guide.DefaultGuideOptions;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosListWatcher;
import edu.gemini.spModel.target.offset.OffsetPosSelection;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * A table model for displaying offset positions in a JTable.
 */
public abstract class AbstractOffsetPosTableModel<P extends OffsetPosBase> extends AbstractTableModel {

    // The state of guide probes for which GuideOptions are linked into the
    // offset positions.
    public enum GuideProbeState {
        unavailable,
        linked,
        unlinked,
        notApplicable
    }

    public interface Column<P extends OffsetPosBase> {
        String getName();
        Object getValue(P pos, int row);
        Class getColumnClass();
        GuideProbeState getGuideProbeState();
    }

    private final Column<P> INDEX_COLUMN = new Column<P>() {
        public String getName() {
            return "Index";
        }

        public Object getValue(P pos, int row) {
            return row;
        }

        public Class getColumnClass() {
            return Integer.class;
        }

        public GuideProbeState getGuideProbeState() {
            return GuideProbeState.notApplicable;
        }
    };

    public static class PColumn<P extends OffsetPosBase> implements Column<P>  {
        public String getName() {
            return "p";
        }

        public Object getValue(P pos, int row) {
            return pos.getXaxis();
        }

        public Class getColumnClass() {
            return Double.class;
        }

        public GuideProbeState getGuideProbeState() {
            return GuideProbeState.notApplicable;
        }
    }

    public static class QColumn<P extends OffsetPosBase> implements Column<P> {
        public String getName() {
            return "q";
        }

        public Object getValue(P pos, int row) {
            return pos.getYaxis();
        }

        public Class getColumnClass() {
            return Double.class;
        }

        public GuideProbeState getGuideProbeState() {
            return GuideProbeState.notApplicable;
        }
    }

    public static class DefaultGuideColumn<P extends OffsetPosBase> implements Column<P> {
        public String getName() { return "Guiding"; }
        public Object getValue(P pos, int row) { return pos.getDefaultGuideOption(); }
        public Class getColumnClass() { return DefaultGuideOptions.Value.class; }
        public GuideProbeState getGuideProbeState() { return GuideProbeState.notApplicable; }
    }

    protected static class LinkColumn<P extends OffsetPosBase> implements Column<P> {

        private final String columnName;
        private final GuideProbe guider;
        private final GuideProbeState state;


        protected LinkColumn(String columnName, GuideProbe guider, GuideProbeState state) {
            this.columnName = columnName;
            this.guider     = guider;
            this.state      = state;
        }

        public String getName() {
            return columnName;
        }

        public Object getValue(P pos, int row) {
            return pos.getLink(guider, guider.getGuideOptions().getDefault());
        }

        public Class getColumnClass() {
            return GuideOption.class;
        }

        public GuideProbeState getGuideProbeState() {
            return state;
        }
    }

    private ISPNode node;
    private OffsetPosList<P> opl;
    private final List<Column<P>> fixedColumns = new ArrayList<Column<P>>();
    private final List<LinkColumn<P>> linkColumns = new ArrayList<LinkColumn<P>>();

    private Set<GuideProbe> referencedGuiders = Collections.emptySet();
    private Set<GuideProbe> availableGuiders  = Collections.emptySet();
    private Set<GuideProbe> noPrimaryGuiders  = Collections.emptySet();

    // should make TelescopePosWatcher parameterize-able
    /**
     * A watcher that updates the table whenever a position changes.
     */
    private final TelescopePosWatcher posWatcher = new TelescopePosWatcher() {
        public void telescopePosUpdate(WatchablePos tp) {
            //noinspection unchecked
            P pos = (P) tp;
            int index = opl.getPositionIndex(pos);
            if (index >= 0) fireTableRowsUpdated(index, index);
        }
    };

    /**
     * A watcher that updates the table whenever the list changes.
     */
    private final OffsetPosListWatcher<P> posListWatcher = new OffsetPosListWatcher<P>() {
        public void posListReset(OffsetPosList<P> tpl) {
            fireTableDataChanged();
        }

        public void posListAddedPosition(OffsetPosList<P> tpl, List<P> newPos) {
            for (P p : newPos) {
                p.addWatcher(posWatcher);
            }
            fireTableDataChanged(); // causes the selection to be lost
            select(newPos); // set selection
        }

        public void posListRemovedPosition(OffsetPosList<P> tpl, List<P> rmPos) {
            for (P p : rmPos) {
                p.deleteWatcher(posWatcher);
            }
            List<P> selList = getAllSelectedPos();
            fireTableDataChanged();  // causes the selection to be lost
            select(selList); // restore selection
        }

        public void posListPropertyUpdated(OffsetPosList<P> tpl, String propertyName, Object oldValue, Object newValue) {
            if (!OffsetPosList.ADVANCED_GUIDING_PROP.equals(propertyName)) return;
            syncAdvancedGuidingColumns();
        }
    };

    protected AbstractOffsetPosTableModel(List<Column<P>> fixedColumns) {
        this.fixedColumns.addAll(fixedColumns);
    }

    protected List<P> getAllSelectedPos() {
        return OffsetPosSelection.apply(node).selectedPositions(opl);
    }

    protected void select(List<P> posList) {
        OffsetPosSelection.select(opl, posList).commit(node);
    }

    public void setPositionList(ISPNode node, OffsetPosList<P> posList) {
        if (opl != null) {
            opl.deleteWatcher(posListWatcher);
            for (P op : opl.getAllPositions()) {
                op.deleteWatcher(posWatcher);
            }
        }

        this.node = node;
        opl = posList;

        opl.addWatcher(posListWatcher);
        for (P op : opl.getAllPositions()) {
            op.addWatcher(posWatcher);
        }

        syncAdvancedGuidingColumns();
    }

    private void syncAdvancedGuidingColumns() {
        // Need to set up the link columns to match the advanced guiding
        // overrides in the position list.  Initially set the state to
        // "not applicable" because we don't know anything about the target
        // environment here.  This will be updated later.  Try to fire the
        // least disruptive type of change event.

        boolean structureChanged = false;
        Set<GuideProbe> advanced = opl.getAdvancedGuiding();
        if (linkColumns.size() != advanced.size()) {
            structureChanged = true;
        } else {
            for (LinkColumn<P> col : linkColumns) {
                structureChanged = structureChanged || !advanced.contains(col.guider);
            }
        }

        linkColumns.clear();
        for (GuideProbe gp : advancedGuiding()) {
            linkColumns.add(new LinkColumn<P>(gp.getKey(), gp, GuideProbeState.notApplicable));
        }

        if (structureChanged) {
            fireTableStructureChanged();
        } else {
            fireTableDataChanged();
        }
        syncGuideState();
    }

    private List<GuideProbe> advancedGuiding() {
        List<GuideProbe> lst = new ArrayList<GuideProbe>(opl.getAdvancedGuiding());
        Collections.sort(lst, GuideProbe.KeyComparator.instance);
        return lst;
    }


    public void syncGuideState(Set<GuideProbe> referencedGuiders, Set<GuideProbe> availableGuiders, Set<GuideProbe> noPrimaryGuiders) {
        this.referencedGuiders = Collections.unmodifiableSet(new HashSet<GuideProbe>(referencedGuiders));
        this.availableGuiders  = Collections.unmodifiableSet(new HashSet<GuideProbe>(availableGuiders));
        this.noPrimaryGuiders  = Collections.unmodifiableSet(new HashSet<GuideProbe>(noPrimaryGuiders));
        syncGuideState();
    }

    private void syncGuideState() {
        boolean updated = false;
        final ListIterator<LinkColumn<P>> lit = linkColumns.listIterator();
        while (lit.hasNext()) {
            final LinkColumn<P> col = lit.next();
            final GuideProbe guider = col.guider;
            final GuideProbeState cur = col.getGuideProbeState();
            final GuideProbeState nex;
            if (!availableGuiders.contains(guider)) {
                nex = GuideProbeState.unavailable;
            } else if (noPrimaryGuiders.contains(guider) || !referencedGuiders.contains(guider)) {
                nex = GuideProbeState.unlinked;
            } else {
                nex = GuideProbeState.linked;
            }
            if (cur != nex) {
                lit.set(new LinkColumn<P>(guider.getKey(), guider, nex));
                updated = true;
            }
        }
        if (updated) {
            List<P> selList = getAllSelectedPos();
            fireTableDataChanged();
            select(selList);
        }
    }

    public int getRowCount() {
        return opl.size();
    }

    public int getColumnCount() {
        return 1 + fixedColumns.size() + linkColumns.size();
    }

    private Column<P> getColumn(int columnIndex) {
        if (columnIndex == 0) return INDEX_COLUMN;
        --columnIndex;
        if (columnIndex < fixedColumns.size()) {
            return fixedColumns.get(columnIndex);
        }
        columnIndex -= fixedColumns.size();
        if (columnIndex < linkColumns.size()) {
            return linkColumns.get(columnIndex);
        }
        return null;
    }

    public String getColumnName(int columnIndex) {
        Column<P> col = getColumn(columnIndex);
        return (col == null) ? "" : col.getName();
    }

    public Class getColumnClass(int columnIndex) {
        Column<P> col = getColumn(columnIndex);
        return (col == null) ? Object.class : col.getClass();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        P pos = opl.getPositionAt(rowIndex);
        Column<P> col = getColumn(columnIndex);
        return (col == null) ? null : col.getValue(pos, rowIndex);
    }

    /**
     * Returns the state of the guide probe associated with this column,
     * assuming this is a column which displays guide options.  Will return
     * {@link GuideProbeState#notApplicable} otherwise.
     */
    public GuideProbeState getGuideProbeState(int columnIndex) {
        Column<P> col = getColumn(columnIndex);
        return (col == null) ? GuideProbeState.notApplicable : col.getGuideProbeState();
    }
}
