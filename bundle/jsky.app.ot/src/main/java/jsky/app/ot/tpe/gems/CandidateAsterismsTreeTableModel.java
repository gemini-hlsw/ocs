package jsky.app.ot.tpe.gems;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.ags.gems.GemsGuideStars;
import edu.gemini.ags.gems.GemsStrehl;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import org.jdesktop.swingx.JXTreeTable;
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
        Col(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
        public abstract Object getValue(Row row);
    }

    static class Row {
        // top level displays the strehl info
        private final GemsGuideStars _gemsGuideStars;

        // child nodes display the guide probe targets
        private final GuideProbeTargets _guideProbeTargets;

        // The NIR mag band to display
        private final Magnitude.Band _band;

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

        // A row corresponding to a GemsGuideStars object
        // (top level node, displays the Strehl info and checkbox)
        Row(GemsGuideStars gemsGuideStars, List<Row> children) {
            _gemsGuideStars = gemsGuideStars;
            _guideProbeTargets = null;
            _band = null;
            _isTopLevel = true;
            _children = children;
            _parent = null;
            _checkBoxSelected = false;
            for(Row row : _children) {
                row._parent = this;
            }
        }

        // A row corresponding to a GuideProbeTargets object (child node)
        Row(GemsGuideStars gemsGuideStars, GuideProbeTargets guideProbeTargets, Magnitude.Band band) {
            _gemsGuideStars = gemsGuideStars;
            _guideProbeTargets = guideProbeTargets;
            _band = band;
            _isTopLevel = false;
            _children = null;
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
                GemsStrehl strehl = _gemsGuideStars.getStrehl();
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

        void setPrimary(Boolean b) {
            _primary = b;
        }

        String getGuideProbe() {
            if (_guideProbeTargets != null) {
                return _guideProbeTargets.getGuider().getKey();
            }
            return null;
        }

        String getId() {
            if (_guideProbeTargets != null) {
                if (!_guideProbeTargets.getPrimary().isEmpty()) {
                    return _guideProbeTargets.getPrimary().getValue().getName();
                }
            } else if (_gemsGuideStars != null) { // top level displays Strehl values
                GemsStrehl strehl = _gemsGuideStars.getStrehl();
                if (strehl != null) {
                    return String.format("%.1f rms", _gemsGuideStars.getStrehl().getRms() * 100);
                }
            }
            return null;
        }

        String getMag() {
            if (_guideProbeTargets != null) {
                if (!_guideProbeTargets.getPrimary().isEmpty()) {
                    GuideProbe guideProbe = _guideProbeTargets.getGuider();
                    Magnitude.Band band = _band;
                    if (Canopus.Wfs.Group.instance.getMembers().contains(guideProbe)) {
                        band = Magnitude.Band.R;
                    }
                    Option<Magnitude> magOpt = _guideProbeTargets.getPrimary().getValue().getTarget().getMagnitude(band);
                    if (!magOpt.isEmpty()) {
                        return magOpt.getValue().getBrightness() + " (" + band.name() + ")";
                    }
                }
            } else if (_gemsGuideStars != null) { // top level displays Strehl values
                GemsStrehl strehl = _gemsGuideStars.getStrehl();
                if (strehl != null) {
                    return String.format("%.1f min", _gemsGuideStars.getStrehl().getMin() * 100);
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
                return getTarget().getXaxisAsString();
            } else if (_gemsGuideStars != null) { // top level displays Strehl values
                GemsStrehl strehl = _gemsGuideStars.getStrehl();
                if (strehl != null) {
                    return String.format("%.1f max", _gemsGuideStars.getStrehl().getMax() * 100);
                }
            }
            return null;
        }

        Object getDec() {
            if (_guideProbeTargets != null) {
                return getTarget().getYaxisAsString();
            }
            return null;
        }

        public Magnitude.Band getBand() {
            return _band;
        }

        public boolean isCheckBoxSelected() {
            return _checkBoxSelected != null && _checkBoxSelected;
        }

        public boolean isTopLevel() {
            return _isTopLevel;
        }

        public Row getParent() {
            return _parent;
        }
    }

    // Should be final, a bug seems to prevent it
//    private ImList<Row> _rows;
    private final ImList<String> _columnHeaders;

    CandidateAsterismsTreeTableModel() {
        _columnHeaders = computeColumnHeaders();
    }

    CandidateAsterismsTreeTableModel(List<GemsGuideStars> gemsGuideStarsList, JXTreeTable treeTable, Magnitude.Band band) {
        super(new ArrayList<Row>());
        _columnHeaders = computeColumnHeaders();

        final List<Row> tmp = (List<Row>)getRoot();

        for(GemsGuideStars gemsGuideStars : gemsGuideStarsList) {
            final List<Row> rowList = new ArrayList<Row>();
            for (GuideProbeTargets guideProbeTargets : gemsGuideStars.getGuideGroup().getAll()) {
                rowList.add(new Row(gemsGuideStars, guideProbeTargets, band));
            }
            tmp.add(new Row(gemsGuideStars, rowList));
        }

        if (tmp.size() > 0) {
            Row row = tmp.get(0);
            row._checkBoxSelected = true; // select first item by default
            row._primary = true;          // and make it for the primary group
        }
    }

    private static ImList<String> computeColumnHeaders() {
        List<String> hdr = new ArrayList<String>();
        for (Col c : Col.values()) hdr.add(c.displayName());
        return DefaultImList.create(hdr);
    }

    @Override
    public int getColumnCount() {
        return Col.values().length;
    }

    @Override
    public String getColumnName(int index) {
        return _columnHeaders.get(index);
    }

    @Override
    public Object getValueAt(Object node, int columnIndex) {
        if (node instanceof Row) {
            Row row = (Row) node;
            Col[] cols = Col.values();
            return cols[columnIndex].getValue(row);
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, Object node, int column) {
        if (column == Col.CHECK.ordinal() && node instanceof Row && value instanceof Boolean) {
            ((Row)node)._checkBoxSelected = (Boolean)value;
            // If a checkbox is selected, set primary to false, otherwise null, in which case a new
            // primary node should be set, if one is checked
            if ((Boolean)value) {
                ((Row)node)._primary = (getPrimaryIndex(-1) == -1); // true if no others are marked primary
            } else {
                ((Row)node)._primary = null;
                List<Row> rowList = (List<Row>) getRoot();
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
        // else {
            // other columns are read-only
        // }
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof List) {
            List<Row> rowList = (List<Row>)parent;
            if(rowList.size() > index) {
                return rowList.get(index);
            }
        } else if (parent instanceof Row) {
            Row row = (Row)parent;
            List<Row> rowList = row.getChildren();
            if (rowList != null && rowList.size() > index) {
                return rowList.get(index);
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof List) {
            List<Row> rowList = (List<Row>)parent;
            return rowList.size();
        } else if (parent instanceof Row) {
            Row row = (Row)parent;
            List<Row> rowList = row.getChildren();
            if (rowList != null) {
                return rowList.size();
            }
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
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
    public boolean isCellEditable(Object node, int column) {
        return column == Col.CHECK.ordinal();
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof Row && !((Row)node).isTopLevel();
    }

    @Override
    public int getHierarchicalColumn() {
        return Col.GUIDE_PROBE.ordinal();
    }

    /**
     * Returns a list of GemsGuideStars for the checked asterisms
     */
    public List<GemsGuideStars> getCheckedAsterisms() {
        List<GemsGuideStars> result = new ArrayList<GemsGuideStars>();
        if (getRoot() != null) {
            List<Row> rowList = (List<Row>) getRoot();
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
    public int getPrimaryIndex(int defaultIndex) {
        if (getRoot() != null) {
            int i = 0;
            List<Row> rowList = (List<Row>) getRoot();
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

    void clearPrimary() {
        if (getRoot() != null) {
            List<Row> rowList = (List<Row>) getRoot();
            for (Row row : rowList) {
                if (row.getPrimary() != null && row.getPrimary()) {
                    row.setPrimary(false);
                }
            }
        }
    }

    void setPrimary(Row row) {
        clearPrimary();
        row.setPrimary(true);
    }
}
