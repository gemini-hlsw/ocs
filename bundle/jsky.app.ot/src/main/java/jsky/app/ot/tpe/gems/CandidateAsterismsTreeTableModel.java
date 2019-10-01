package jsky.app.ot.tpe.gems;

import edu.gemini.ags.gems.GemsUtils4Java;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.ags.gems.GemsStrehl;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * OT-111: model for {@link CandidateAsterismsTreeTable}
 */
class CandidateAsterismsTreeTableModel extends AbstractTreeTableModel {

    enum Col {
        CHECK("") {
            public Object getValue(Row row) {
                return row.getCheck();
            }
        },

        PRIMARY("") {
            public Object getValue(Row row) {
                return row.getPrimary();
            }
        },

        GUIDE_PROBE("") {
            public Object getValue(Row row) {
                // not displayed in tree table, first column is handled in tree cell renderer
                return row.getGuideProbe();
            }
        },

        ID("Id") {
            public Object getValue(Row row) {
                return row.getId();
            }
        },

        MAG("Mag") {
            public Object getValue(Row row) {
                return row.getMag();
            }
        },

        RA("RA") {
            public Object getValue(Row row) {
                return row.getRA();
            }
        },

        DEC("Dec") {
            public Object getValue(Row row) {
                return row.getDec();
            }
        },
        ;

        private final String displayName;
        Col(final String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
        public abstract Object getValue(Row row);
    }

    static class Row {
        // top level displays the strehl info
        private final GemsGuideStars _gemsGuideStars;

        // child nodes display the guide probe targets
        private final GuideProbeTargets _guideProbeTargets;

        // Not displayed: true if this is a top level tree node row
        private final boolean _isTopLevel;

        // True if the checkbox in the row is selected
        private Boolean _checkBoxSelected;

        // True if this row represents the primary guide group
        private Boolean _primary;

        private final List<Row> _children;

        // Should be final, but must be set after the parent is created.
        // (XXX There might be another way to get the parent node)
        private Row _parent;

        private final Option<Long> _when;

        // A row corresponding to a GemsGuideStars object
        // (top level node, displays the Strehl info and checkbox)
        Row(final GemsGuideStars gemsGuideStars, final List<Row> children, final Option<Long> when) {
            _gemsGuideStars = gemsGuideStars;
            _guideProbeTargets = null;
            _isTopLevel = true;
            _children = children;
            _parent = null;
            _checkBoxSelected = false;
            for(Row row : _children) {
                row._parent = this;
            }
            _when = when;
        }

        // A row corresponding to a GuideProbeTargets object (child node)
        Row(final GemsGuideStars gemsGuideStars, final GuideProbeTargets guideProbeTargets, final Option<Long> when) {
            _gemsGuideStars = gemsGuideStars;
            _guideProbeTargets = guideProbeTargets;
            _isTopLevel = false;
            _children = null;
            _when = when;
        }

        GemsGuideStars getGemsGuideStars() {
            return _gemsGuideStars;
        }

        GuideProbeTargets getGuideProbeTargets() {
            return _guideProbeTargets;
        }

        List<Row> getChildren() {
            return _children;
        }

        Boolean getCheck() {
            if (_gemsGuideStars != null) {
                final GemsStrehl strehl = _gemsGuideStars.strehl();
                if (strehl != null) {
                    return _checkBoxSelected;
                }
            }
            return null;
        }

        Boolean getPrimary() {
            // only checked rows display a value for primary
            if (getCheck()  != null) {
                return _primary;
            }
            return null;
        }

        void setPrimary(final Boolean b) {
            _primary = b;
        }

        String getGuideProbe() {
            return _guideProbeTargets == null ? null : _guideProbeTargets.getGuider().getKey();
        }

        String getId() {
            if (_guideProbeTargets != null) {
                if (_guideProbeTargets.getPrimary().isDefined()) {
                    return _guideProbeTargets.getPrimary().getValue().getName();
                }
            } else if (_gemsGuideStars != null) { // top level displays Strehl values
                final GemsStrehl strehl = _gemsGuideStars.strehl();
                if (strehl != null) {
                    return String.format("%.1f rms", _gemsGuideStars.strehl().rms() * 100);
                }
            }
            return null;
        }

        String getMag() {
            if (_guideProbeTargets != null) {
                if (!_guideProbeTargets.getPrimary().isEmpty()) {
                    return GemsUtils4Java.probeMagnitudeInUse(_guideProbeTargets.getPrimary().getValue().getMagnitudesJava());
                }
            } else if (_gemsGuideStars != null) { // top level displays Strehl values
                final GemsStrehl strehl = _gemsGuideStars.strehl();
                if (strehl != null) {
                    return String.format("%.1f min", _gemsGuideStars.strehl().min() * 100);
                }
            }
            return null;
        }

        SPTarget getTarget() {
            if (_guideProbeTargets != null) {
                return _guideProbeTargets.getPrimary().getValue();
            }
            return null;
        }

        String getRA() {
            if (_guideProbeTargets != null) {
                return getTarget().getRaString(_when).getOrNull();
            } else if (_gemsGuideStars != null) { // top level displays Strehl values
                final GemsStrehl strehl = _gemsGuideStars.strehl();
                if (strehl != null) {
                    return String.format("%.1f max", _gemsGuideStars.strehl().max() * 100);
                }
            }
            return null;
        }

        Object getDec() {
            if (_guideProbeTargets != null) {
                return getTarget().getDecString(_when).getOrNull();
            }
            return null;
        }

        boolean isCheckBoxSelected() {
            return _checkBoxSelected != null && _checkBoxSelected;
        }

        boolean isTopLevel() {
            return _isTopLevel;
        }

        public Row getParent() {
            return _parent;
        }
    }

    private final ImList<String> _columnHeaders;

    CandidateAsterismsTreeTableModel(final List<GemsGuideStars> gemsGuideStarsList, final Option<Long> when) {
        super(new ArrayList<Row>());
        _columnHeaders = computeColumnHeaders();

        final List<Row> tmp = getRows();

        for(GemsGuideStars gemsGuideStars : gemsGuideStarsList) {
            final List<Row> rowList = new ArrayList<>();
            for (GuideProbeTargets guideProbeTargets : gemsGuideStars.guideGroup().getAll()) {
                rowList.add(new Row(gemsGuideStars, guideProbeTargets, when));
            }
            tmp.add(new Row(gemsGuideStars, rowList, when));
        }

        if (tmp.size() > 0) {
            final Row row = tmp.get(0);
            row._checkBoxSelected = true; // select first item by default
            row._primary = true;          // and make it for the primary group
        }
    }

    private static ImList<String> computeColumnHeaders() {
        final List<String> hdr = new ArrayList<>();
        for (Col c : Col.values()) hdr.add(c.displayName());
        return DefaultImList.create(hdr);
    }

    @Override
    public int getColumnCount() {
        return Col.values().length;
    }

    @Override
    public String getColumnName(final int index) {
        return _columnHeaders.get(index);
    }

    @Override
    public Object getValueAt(final Object node, final int columnIndex) {
        if (node instanceof Row) {
            final Row row = (Row) node;
            final Col[] cols = Col.values();
            return cols[columnIndex].getValue(row);
        }
        return null;
    }

    @Override
    public void setValueAt(final Object value, final Object node, final int column) {
        if (column == Col.CHECK.ordinal() && node instanceof Row && value instanceof Boolean) {
            ((Row)node)._checkBoxSelected = (Boolean)value;
            // If a checkbox is selected, set primary to false, otherwise null, in which case a new
            // primary node should be set, if one is checked
            if ((Boolean)value) {
                ((Row)node)._primary = (getPrimaryIndex(-1) == -1); // true if no others are marked primary
            } else {
                ((Row)node)._primary = null;
                final List<Row> rowList = getRows();
                for (Row row : rowList) {
                    if (row.isCheckBoxSelected()) {
                        row.setPrimary(true);
                        break;
                    }
                }
            }
        } else if (column == Col.PRIMARY.ordinal() && node instanceof Row && value instanceof Boolean) {
            ((Row)node)._primary = (Boolean)value;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getChild(final Object parent, final int index) {
        if (parent instanceof List) {
            final List<Row> rowList = (List<Row>)parent;
            if (rowList.size() > index) {
                return rowList.get(index);
            }
        } else if (parent instanceof Row) {
            final Row row = (Row)parent;
            List<Row> rowList = row.getChildren();
            if (rowList != null && rowList.size() > index) {
                return rowList.get(index);
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getChildCount(final Object parent) {
        if (parent instanceof List) {
            final List<Row> rowList = (List<Row>)parent;
            return rowList.size();
        } else if (parent instanceof Row) {
            final Row row = (Row)parent;
            List<Row> rowList = row.getChildren();
            if (rowList != null) {
                return rowList.size();
            }
        }
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getIndexOfChild(final Object parent, final Object child) {
        List<Row> rowList = null;
        if (parent instanceof List) {
            rowList = (List<Row>)parent;
        } else if (parent instanceof Row) {
            Row row = (Row)parent;
            rowList = row.getChildren();
        }
        if (rowList != null) {
            for(int i = 0; i < rowList.size(); i++) {
                if (rowList.get(i) == child) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean isCellEditable(final Object node, final int column) {
        return column == Col.CHECK.ordinal();
    }

    @Override
    public boolean isLeaf(final Object node) {
        return node instanceof Row && !((Row)node).isTopLevel();
    }

    @Override
    public int getHierarchicalColumn() {
        return Col.GUIDE_PROBE.ordinal();
    }

    /**
     * Returns a list of GemsGuideStars for the checked asterisms
     */
    List<GemsGuideStars> getCheckedAsterisms() {
        final List<GemsGuideStars> result = new ArrayList<>();
        if (getRoot() != null) {
            final List<Row> rowList = getRows();
            for (Row row : rowList) {
                if (row.isCheckBoxSelected()) {
                    result.add(row.getGemsGuideStars());
                }
            }
        }
        return result;
    }

    /**
     * Returns the index of the row marked to be the primary guide group, or defaultIndex if none are marked
     */
    int getPrimaryIndex(final int defaultIndex) {
        if (getRoot() != null) {
            int i = 0;
            final List<Row> rowList = getRows();
            for (Row row : rowList) {
                if (row.isCheckBoxSelected()) {
                    if (row.getPrimary() != null && row.getPrimary()) {
                        return i;
                    }
                    i++;
                }
            }
        }
        return defaultIndex;
    }

    private void clearPrimary() {
        if (getRoot() != null) {
            final List<Row> rowList = getRows();
            for (Row row : rowList) {
                if (row.getPrimary() != null && row.getPrimary()) {
                    row.setPrimary(false);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Row> getRows() {
        return (List<Row>) getRoot();
    }

    void setPrimary(final Row row) {
        clearPrimary();
        row.setPrimary(true);
    }
}
