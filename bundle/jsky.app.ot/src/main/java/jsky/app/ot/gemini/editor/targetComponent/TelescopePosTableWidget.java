// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TelescopePosTableWidget.java 8534 2008-05-11 05:21:26Z swalker $
//
package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.ags.api.*;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideOptions;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.ValidatableGuideProbe;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.offset.OffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetUtil;
import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.ICoordinate;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.util.Resources;
import jsky.coords.WorldCoords;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;


/**
 * An extension of the TableWidget to support telescope target lists.
 */
public final class TelescopePosTableWidget extends JXTreeTable implements TelescopePosWatcher {

    private static final Icon errorIcon = Resources.getIcon("eclipse/error.gif");
    private static final Icon blankIcon = Resources.getIcon("eclipse/blank.gif");


    // Used to format values as strings.
    private static NumberFormat nf = NumberFormat.getInstance(Locale.US);

    static { nf.setMaximumFractionDigits(2); }

    static class TableData extends AbstractTreeTableModel {

        enum Col {
            TAG("Type Tag") {
                public Object getValue(Row row) { return row.tag(); }
            },

            NAME("Name") {
                public Object getValue(Row row) { return row.name(); }
            },

            RA("RA") {
                public Object getValue(Row row) {
                    return row.target().map(new MapOp<SPTarget, String>() {
                        @Override public String apply(SPTarget t) {
                            return t.getTarget().c1ToString();
                        }
                    }).getOrElse("");
                }
            },

            DEC("Dec") {
                public Object getValue(Row row) {
                    return row.target().map(new MapOp<SPTarget, String>() {
                        @Override public String apply(SPTarget t) {
                            return t.getTarget().c2ToString();
                        }
                    }).getOrElse("");
                }
            },

            DIST("Dist") {
                public Object getValue(Row row) {
                    return row.distance().map(new MapOp<Double, String>() {
                        @Override public String apply(Double dist) {
                            return nf.format(dist);
                        }
                    }).getOrElse("");
                }
            };

            private final String displayName;
            Col(String displayName) { this.displayName = displayName; }
            public String displayName() { return displayName; }
            public abstract Object getValue(Row row);
        }

        interface Row {
            boolean enabled();
            String tag();
            String name();
            Option<SPTarget> target();  // really we want: GuideGroup \/ SPTarget
            Option<GuideGroup> group();
            Option<Double> distance();
            List<Row> children();

            Option<Magnitude> getMagnitude(Magnitude.Band band);
            String formatMagnitude(Magnitude.Band band);

            Icon getIcon();
        }

        static abstract class AbstractRow implements Row {
            final boolean enabled;
            final String tag;
            final String name;
            Option<SPTarget> target;

            AbstractRow(boolean enabled, String tag, String name, Option<SPTarget> target) {
                this.enabled  = enabled;
                this.tag      = tag;
                this.name     = name;
                this.target   = target;
            }

            public boolean enabled()          { return enabled;  }
            public String tag()               { return tag;      }
            public String name()              { return name;     }
            public Option<SPTarget> target()  { return target;   }
            public Option<GuideGroup> group() { return None.instance(); }
            public Option<Double> distance()  { return None.instance(); }
            public List<Row> children()       { return Collections.emptyList(); }

            public Option<Magnitude> getMagnitude(final Magnitude.Band band) {
                return target.flatMap(new MapOp<SPTarget, Option<Magnitude>>() {
                    @Override public Option<Magnitude> apply(SPTarget spTarget) {
                        return spTarget.getTarget().getMagnitude(band);
                    }
                });
            }

            public String formatMagnitude(Magnitude.Band band) {
                return getMagnitude(band).map(new MapOp<Magnitude, String>() {
                    @Override public String apply(Magnitude mag) {
                        return MagnitudeEditor.formatBrightness(mag);
                    }
                }).getOrElse("");
            }

            public Icon getIcon() { return blankIcon; }
        }

        static final class BaseTargetRow extends AbstractRow {
            BaseTargetRow(SPTarget target) {
                super(true, TargetEnvironment.BASE_NAME, target.getTarget().getName(), new Some<>(target));
            }
        }

        static abstract class NonBaseTargetRow extends AbstractRow {
            final Option<Double> distance;

            NonBaseTargetRow(boolean enabled, String tag, SPTarget target, WorldCoords baseCoords) {
                super(enabled, tag, target.getTarget().getName(), new Some<>(target));
                final WorldCoords coords = getWorldCoords(target);
                distance = new Some<>(Math.abs(baseCoords.dist(coords)));
            }

            @Override public Option<Double> distance() { return distance; }
        }

        static final class GuideTargetRow extends NonBaseTargetRow {
            final boolean isActiveGuideProbe;
            final Option<AgsGuideQuality> quality;
            GuideTargetRow(boolean isActiveGuideProbe, Option<AgsGuideQuality> quality, boolean enabled, GuideProbe probe, int index, SPTarget target, WorldCoords baseCoords) {
                super(enabled, String.format("%s (%d)", probe.getKey(), index), target, baseCoords);
                this.isActiveGuideProbe = isActiveGuideProbe;
                this.quality = quality;
            }

            @Override public Icon getIcon() {
                if (!isActiveGuideProbe) return errorIcon;
                else return GuidingIcon.apply(quality.getOrElse(AgsGuideQuality.Unusable$.MODULE$), enabled);
            }
        }

        static final class UserTargetRow extends NonBaseTargetRow {
            UserTargetRow(int index, SPTarget target, WorldCoords baseCoords) {
                super(true, String.format("%s (%d)", TargetEnvironment.USER_NAME, index), target, baseCoords);
            }
        }

        static final class GroupRow extends AbstractRow {
            private final Option<GuideGroup> group;
            private final List<Row> children;

            GroupRow(boolean enabled, int index, GuideGroup group, List<Row> children) {
                super(enabled, group.getName().getOrElse("Guide Group " + index), "", None.<SPTarget>instance());
                this.group    = new Some<>(group);
                this.children = Collections.unmodifiableList(children);
            }

            @Override public Option<GuideGroup> group() { return group; }
            @Override public List<Row> children() { return children; }
        }

        private final ImList<Row> rows;
        private final ImList<Magnitude.Band> bands;
        private final ImList<String> columnHeaders;
        private final TargetEnvironment env;
        private final JXTreeTable treeTable;

        TableData() {
            rows          = DefaultImList.create();
            bands         = DefaultImList.create();
            columnHeaders = computeColumnHeaders(bands);
            env           = null;
            treeTable     = null;
        }

        private static Option<AgsGuideQuality> guideQuality(Option<Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>> ags, final GuideProbe guideProbe, final SPTarget guideStar) {
            return ags.flatMap(new MapOp<Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>, Option<AgsGuideQuality>>() {
                @Override public Option<AgsGuideQuality> apply(Tuple2<ObsContext, AgsMagnitude.MagnitudeTable> tup) {
                    if (guideProbe instanceof ValidatableGuideProbe) {
                        final ValidatableGuideProbe vgp = (ValidatableGuideProbe) guideProbe;
                        final AgsGuideQuality gq = AgsAnalysis$.MODULE$.analysis(tup._1(), tup._2(), vgp, guideStar).quality();
                        return new Some<>(gq);
                    } else {
                        return None.instance();
                    }
                }
            });
        }

        TableData(final Option<ObsContext> ctx, final TargetEnvironment env, JXTreeTable treeTable) {
            super(new ArrayList<Row>());
            @SuppressWarnings("unchecked") final List<Row> tmp = (List<Row>)getRoot();

            this.env       = env;
            this.treeTable = treeTable;
            bands          = getSortedBands(env);
            columnHeaders  = computeColumnHeaders(bands);

            // Add the base position first.
            final SPTarget base = env.getBase();
            final WorldCoords baseCoords = getWorldCoords(base);
            tmp.add(new BaseTargetRow(base));

            // Add all the guide groups and targets.
            final Option<Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>> ags = ctx.map(new MapOp<ObsContext, Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>>() {
                @Override public Tuple2<ObsContext, AgsMagnitude.MagnitudeTable> apply(ObsContext oc) {
                    return new Pair<>(oc, OT.getMagnitudeTable());
                }
            });

            GuideEnvironment ge = env.getGuideEnvironment();
            ImList<GuideGroup> groups = ge.getOptions();
            GuideGroup primaryGroup = env.getOrCreatePrimaryGuideGroup();
            if (groups.size() < 2) {
                for (GuideProbeTargets gt : primaryGroup.getAll()) {
                    final GuideProbe guideProbe = gt.getGuider();
                    final boolean isActive = env.isActive(guideProbe);
                    final Option<SPTarget> primary = gt.getPrimary();
                    gt.getOptions().zipWithIndex().foreach(new ApplyOp<Tuple2<SPTarget, Integer>>() {
                        @Override public void apply(Tuple2<SPTarget, Integer> tup) {
                            final SPTarget target = tup._1();
                            final Option<AgsGuideQuality> quality = guideQuality(ags, guideProbe, target);
                            final boolean isPrimary = !primary.isEmpty() && (primary.getValue() == target);
                            tmp.add(new GuideTargetRow(isActive, quality, isPrimary, guideProbe, tup._2() + 1, target, baseCoords));
                        }
                    });
                }
            } else {
                int groupIndex = 1;
                for (GuideGroup group : groups) {
                    final boolean isPrimaryGroup = group == primaryGroup;
                    final List<Row> rowList = new ArrayList<>();
                    for (GuideProbeTargets gt : group.getAll()) {
                        final GuideProbe guideProbe = gt.getGuider();
                        final boolean isActive = env.isActive(guideProbe);
                        final Option<SPTarget> primary = gt.getPrimary();
                        gt.getOptions().zipWithIndex().foreach(new ApplyOp<Tuple2<SPTarget, Integer>>() {
                            @Override public void apply(Tuple2<SPTarget, Integer> tup) {
                                final SPTarget target = tup._1();
                                final Option<AgsGuideQuality> quality = guideQuality(ags, guideProbe, target);
                                final boolean enabled = isPrimaryGroup && !primary.isEmpty() && (primary.getValue() == tup._1());
                                rowList.add(new GuideTargetRow(isActive, quality, enabled, guideProbe, tup._2() + 1, tup._1(), baseCoords));
                            }
                        });
                    }

                    tmp.add(new GroupRow(isPrimaryGroup, groupIndex++, group, rowList));
                }
            }

            // Add the user positions.
            env.getUserTargets().zipWithIndex().foreach(new ApplyOp<Tuple2<SPTarget, Integer>>() {
                @Override public void apply(Tuple2<SPTarget, Integer> tup) {
                    tmp.add(new UserTargetRow(tup._2(), tup._1(), baseCoords));
                }
            });

            // Finally, initialize the rows
            rows = DefaultImList.create(tmp);
        }

        // Gets all the magnitude bands used by targets in the target
        // environment.
        private ImList<Magnitude.Band> getSortedBands(TargetEnvironment env) {
            // Keep a sorted set of bands, sorted by the name.
            final Set<Magnitude.Band> bands;
            bands = new TreeSet<>(Magnitude.Band.WAVELENGTH_COMPARATOR);

            // An operation that adds a target's bands to the set.
            ApplyOp<SPTarget> op = new ApplyOp<SPTarget>() {
                @Override public void apply(SPTarget spTarget) {
                    bands.addAll(spTarget.getTarget().getMagnitudeBands());
                }
            };

            // Extract all the magnitude bands from the environment.
            bands.addAll(env.getBase().getTarget().getMagnitudeBands());
            for (GuideProbeTargets gt : env.getOrCreatePrimaryGuideGroup()) {
                gt.getOptions().foreach(op);
            }
            env.getUserTargets().foreach(op);

            // Create an immutable sorted list containing the results.
            return DefaultImList.create(bands);
        }

        public int indexOf(final SPTarget target) {
            if (target == null) return -1;

            int index = 0;
            for (Row row : rows) {
                if (row.target().getOrNull() == target) return index;
                final TreePath path = treeTable.getPathForRow(index);
                ++index;
                // don't count collapsed tree nodes!
                if (treeTable.isExpanded(path)) {
                    for (Row row2 : row.children()) {
                        if (row2.target().getOrNull() == target) return index;
                        ++index;
                    }
                }
            }
            return -1;
        }

        public int indexOf(final GuideGroup group) {
            if (group == null) return -1;

            int index = 0;
            for (Row row : rows) {
                if (row.group().getOrNull() == group) return index;
                final TreePath path = treeTable.getPathForRow(index);
                ++index;
                // don't count collapsed tree nodes!
                if (treeTable.isExpanded(path)) index += row.children().size();
            }
            return -1;
        }

        public Option<SPTarget> targetAt(int index) {
            return rowAt(index).flatMap(new MapOp<Row, Option<SPTarget>>() {
                @Override
                public Option<SPTarget> apply(Row row) {
                    return row.target();
                }
            });
        }

        public Option<GuideGroup> groupAt(int index) {
            return rowAt(index).flatMap(new MapOp<Row, Option<GuideGroup>>() {
                @Override public Option<GuideGroup> apply(Row row) {
                    return row.group();
                }
            });
        }

        public Option<Row> rowAt(int index) {
            int i = 0;
            for (Row row : rows) {
                if (i == index) return new Some<>(row);

                final TreePath path = treeTable.getPathForRow(i);
                ++i;
                if (treeTable.isExpanded(path)) {
                    final List<Row> children = row.children();
                    final int cindex = index - i;
                    if (cindex < children.size()) {
                        return new Some<>(children.get(cindex));
                    } else {
                        i += children.size();
                    }
                }
            }
            return None.instance();
        }


        private static ImList<String> computeColumnHeaders(ImList<Magnitude.Band> bands) {
            // First add the fixed column headers
            final List<String> hdr = new ArrayList<>();
            for (Col c : Col.values()) hdr.add(c.displayName());

            // Add each magnitude band name
            hdr.addAll(bands.map(new MapOp<Magnitude.Band, String>() {
                @Override public String apply(Magnitude.Band band) {
                    return band.name();
                }
            }).toList());

            return DefaultImList.create(hdr);
        }

        @SuppressWarnings("unchecked")
        private List<Row> parentToList(Object parent) {
            if (parent instanceof List) return (List<Row>) parent;
            else if (parent instanceof Row) return ((Row) parent).children();
            return Collections.emptyList();
        }

        @Override
        public Object getChild(Object parent, int index) {
            final List<Row> rowList = parentToList(parent);
            return (index < rowList.size()) ? rowList.get(index) : null;
        }

        @Override
        public int getChildCount(Object parent) {
            return parentToList(parent).size();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            final List<Row> rowList = parentToList(parent);
            for (int i=0; i<rowList.size(); ++i) {
                if (rowList.get(i) == child) return i;
            }
            return -1;
        }

        @Override
        public boolean isLeaf(Object node) {
            return (node instanceof Row) && !(node instanceof GroupRow);
        }

        @Override
        public int getColumnCount() {
            return Col.values().length + bands.size();
        }

        @Override
        public String getColumnName(int index) {
            return columnHeaders.get(index);
        }

        @Override
        public Object getValueAt(Object node, int columnIndex) {
            if (!(node instanceof Row)) return null;
            final Row row    = (Row) node;
            final Col[] cols = Col.values();
            return (columnIndex < cols.length) ?
                      cols[columnIndex].getValue(row) :
                      row.formatMagnitude(bands.get(columnIndex - cols.length));
        }
    }

    // Return the world coordinates for the given target
    private static WorldCoords getWorldCoords(SPTarget tp) {
        ITarget target = tp.getTarget();
        ICoordinate c1 = target.getC1();
        ICoordinate c2 = target.getC2();
        double x = c1.getAs(Units.DEGREES);
        double y = c2.getAs(Units.DEGREES);
        return new WorldCoords(x, y, 2000.);
    }

    private static void styleRendererLabel(JLabel lab, TableData.Row row) {
        final Color c;
        int style;
        if (row.enabled()) {
            c     = Color.black;
            style = Font.PLAIN;
        } else {
            c     = Color.gray;
            style = Font.ITALIC;
        }
        final Font f = lab.getFont().deriveFont(style);
        lab.setFont(f);
        lab.setForeground(c);
        lab.setEnabled(row.enabled());
    }

    private static final class TelescopePosTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                       Object value,
                                       boolean selected,
                                       boolean expanded,
                                       boolean leaf,
                                       int row,
                                       boolean hasFocus) {
            JLabel lab = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof TableData.Row) {
                TableData.Row tableDataRow = (TableData.Row) value;

                // OT-17: display group name in tag field if defined
                if (!tableDataRow.group().isEmpty()) {
                    String tag = tableDataRow.tag();
                    String name = tableDataRow.name();
                    lab.setText(name != null && !name.equals("") ? name : tag);
                } else {
                    lab.setText(tableDataRow.tag());
                }

                final Icon i = tableDataRow.getIcon();
                lab.setIcon(i);
                lab.setDisabledIcon(i);
                styleRendererLabel(lab, tableDataRow);
            }

            return lab;
        }
    }


    // A class that updates the primary star in a set of guide targets to the
    // currently selected target.
    final class PrimaryStarUpdater extends MouseAdapter {

        void updatePrimaryStar() {
            if (_env == null || !OTOptions.isEditable(_obsComp.getProgram(), _obsComp.getContextObservation())) return;
            SPTarget target = getSelectedPos();
            if (target != null) {
                PrimaryTargetToggle.instance.toggle(_dataObject, target);
            } else {
                GuideGroup group = getSelectedGroup();
                if (group != null) {
                    TargetEnvironment env = _dataObject.getTargetEnvironment();
                    GuideGroup primary = env.getOrCreatePrimaryGuideGroup();
                    if (primary != group && confirmGroupChange(primary, group)) {
                        GuideEnvironment ge = env.getGuideEnvironment();
                        _dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.selectPrimary(group)));
                    }
                }
            }
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2) return;
            updatePrimaryStar();
        }
    }

    // Telescope position list
    private ISPObsComponent _obsComp;
    private TargetObsComp _dataObject;
    private TargetEnvironment _env;
    private TableData _tableData;

    // saved tree state: contains row tags of expanded tree nodes
    private Set<String> _expandedTags;

    // if true, ignore selections
    private boolean _ignoreSelection;

    private PrimaryStarUpdater _primaryStarUpdater;

    private final EdCompTargetList owner;

    private final TelescopePosTableDropTarget dropTarget;
    private final TelescopePosTableDragSource dragSource;

    private final TableCellRenderer defaultCellRenderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int col) {
            final JLabel lab = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            final TableData.Row tdRow = _tableData.rowAt(row).getOrNull();
            if (tdRow != null) styleRendererLabel(lab, tdRow);
            return lab;
        }
    };

    /**
     * Default constructor.
     */
    public TelescopePosTableWidget(EdCompTargetList owner) {
        this.owner = owner;
        // disable editing by default
        setTreeTableModel(new TableData());
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        getTableHeader().setReorderingAllowed(false);

        addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (_ignoreSelection) return;
                Object o = e.getPath().getLastPathComponent();
                if (o instanceof TableData.Row) {
                    _notifySelect((TableData.Row) o);
                }
            }
        });
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowHorizontalLines(false);
        setShowVerticalLines(true);
        setColumnMargin(1);
        addHighlighter(HighlighterFactory.createAlternateStriping(Color.WHITE, new Color(248, 247, 255)));

        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);

        _primaryStarUpdater = new PrimaryStarUpdater();
        addMouseListener(_primaryStarUpdater);
        setTreeCellRenderer(new TelescopePosTreeCellRenderer());

        // Add drag and drop features
        dropTarget = new TelescopePosTableDropTarget(this);
        dragSource = new TelescopePosTableDragSource(this);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int col) {
        if (col == 0) return super.getCellRenderer(row, col);
        return defaultCellRenderer;
    }

    // Called to update the primary target or group to be the one selected in the tree
    void updatePrimaryStar() {
        _primaryStarUpdater.updatePrimaryStar();
    }


    public void telescopePosUpdate(WatchablePos tp) {
        int index = getSelectedRow();
        _resetTable(_env);

        // Restore the selection without firing new selection events.
        if ((index >= 0) && (index < getRowCount())) {
            final boolean ignore = _ignoreSelection;
            try {
                _ignoreSelection = true;
                _setSelectedRow(index);
            } finally {
                _ignoreSelection = ignore;
            }
        }
    }

    /**
     * Reinitialize the table.
     */
    public void reinit(TargetObsComp dataObject) {
        dragSource.setEditable(false);
        dropTarget.setEditable(false);

        // Remove all target listeners for the old target environment.
        if (_env != null) {
            _env.getTargets().foreach(new ApplyOp<SPTarget>() {
                public void apply(SPTarget spTarget) {
                    spTarget.deleteWatcher(TelescopePosTableWidget.this);
                }
            });
        }

        // Stop watching for changes on the obsComp
        stopWatchingSelection();
        stopWatchingEnv();

        // Watch the new list, and add all of its positions at once
        _obsComp    = owner.getContextTargetObsComp();
        _dataObject = dataObject;

        startWatchingEnv();
        startWatchingSelection();

        TargetEnvironment env = _dataObject.getTargetEnvironment();
        env.getTargets().foreach(new ApplyOp<SPTarget>() {
            public void apply(SPTarget spTarget) {
                spTarget.addWatcher(TelescopePosTableWidget.this);
            }
        });

        SPTarget tp = TargetSelection.get(_dataObject.getTargetEnvironment(), _obsComp);
        _resetTable(_dataObject.getTargetEnvironment());

        int index = -1;
        if (tp != null) index = _tableData.indexOf(tp);

        if (index >= 0) {
            _setSelectedRow(index);
        } else {
            selectBasePos();
        }

        final boolean editable = OTOptions.isEditable(owner.getProgram(), owner.getContextObservation());
        dragSource.setEditable(editable);
        dropTarget.setEditable(editable);
    }

    private void stopWatchingSelection() {
        TargetSelection.deafTo(_obsComp, selectionListener);
    }

    private void startWatchingSelection() {
        TargetSelection.listenTo(_obsComp, selectionListener);
    }

    private void stopWatchingEnv() {
        if (_dataObject == null) return;
        _dataObject.removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, envChangeListener);
    }

    private void startWatchingEnv() {
        if (_dataObject == null) return;
        _dataObject.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, envChangeListener);
    }

    protected void _notifySelect(TableData.Row row) {
        final SPTarget target = row.target().getOrNull();
        if (target != null) {
            stopWatchingSelection();
            try {
                TargetSelection.set(_env, _obsComp, target);
            } finally {
                startWatchingSelection();
            }
        } else {
            GuideGroup group = row.group().getOrNull();
            if (group != null) TargetSelection.setGuideGroup(_env, _obsComp, group);
        }
    }

    private final PropertyChangeListener selectionListener = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            if (_tableData == null) return;

            final SPTarget target = TargetSelection.get(_env, _obsComp);
            if (target != null) {
                int index = _tableData.indexOf(target);
                _setSelectedRow(index);
            }
        }
    };

    private final PropertyChangeListener envChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            TargetEnvironment oldEnv = (TargetEnvironment) evt.getOldValue();
            TargetEnvironment newEnv = (TargetEnvironment) evt.getNewValue();

            TargetEnvironmentDiff diff = TargetEnvironmentDiff.all(oldEnv, newEnv);
            Collection<SPTarget> rmTargets  = diff.getRemovedTargets();
            Collection<SPTarget> addTargets = diff.getAddedTargets();

            // First update the target listeners according to what was added
            // and removed.
            for (SPTarget pos : rmTargets) {
                pos.deleteWatcher(TelescopePosTableWidget.this);
            }
            for (SPTarget pos : addTargets) {
                pos.addWatcher(TelescopePosTableWidget.this);
            }

            // Remember what was selected before the reset below.
            int oldSelIndex = getSelectedRow();
            TreePath tp = getTreeSelectionModel().getSelectionPath();
            Object o = tp != null? tp.getLastPathComponent() : null;
            SPTarget oldSelTarget = null;
            if (o instanceof TableData.Row) {
                oldSelTarget = ((TableData.Row)o).target().getOrNull();
            }

            // Update the table -- setting _tableData ....
            _resetTable(newEnv);

            // Update the selection.
            if ((rmTargets.size() == 0) && (addTargets.size() == 1)) {
                // A single position was added, so just select it.
                selectPos(addTargets.iterator().next());
                return;
            }

            // If obs comp has a selected target, then honor it.
            final SPTarget newSelectedTarget = TargetSelection.get(_env, _obsComp);
            if (newSelectedTarget != null) {
                int index = _tableData.indexOf(newSelectedTarget);
                _setSelectedRow(index);
                return;
            }

            // If a new group was added, select it
            final GuideGroup newSelectedGroup = TargetSelection.getGuideGroup(_env, _obsComp);
            if (newSelectedGroup != null) {
                int index = _tableData.indexOf(newSelectedGroup);
                if (index != -1) {
                    _setSelectedRow(index);
                    return;
                }
            }

            // Try to select the same target that was selected before, if it
            // is there in the new table.
            if ((oldSelTarget != null) && (_tableData.indexOf(oldSelTarget) >= 0)) {
                selectPos(oldSelTarget);
                return;
            }

            // Okay, the old selected target or group was removed.  Try to select the
            // target or group at the same position as the old selection.
            if (oldSelIndex >= getRowCount()) oldSelIndex = getRowCount() -1;
            if (oldSelIndex >= 0) {
                selectRowAt(oldSelIndex);
            } else {
                selectBasePos();
            }
        }
    };

    private void _resetTable(final TargetEnvironment env) {
        boolean firstTime = ((TableData)getTreeTableModel()).env == null;
        if (!firstTime) {
            _saveTreeState();
        }
        _env = env;
        final Option<ObsContext> ctx = ObsContext.create(owner.getContextObservation()).map(new MapOp<ObsContext, ObsContext>() {
            @Override public ObsContext apply(ObsContext ctx) {
                return ctx.withTargets(env);
            }
        });
        _tableData = new TableData(ctx, env, this);

        _ignoreSelection = true;
        try {
            setTreeTableModel(_tableData);
        } finally {
            _ignoreSelection = false;
        }

        if (firstTime) {
            // expand group nodes first time
            for(GuideGroup group : _env.getGroups()) {
                expandGroup(group);
            }
        } else {
            _restoreTreeState();
        }
//        initializeColumnWidths();
        packAll();
    }

    // Saves the expanded state of the tree table
    private void _saveTreeState() {
        _expandedTags = new HashSet<>();
        final int numRows = getRowCount();
        for(int i = 0; i < numRows; i++) {
            final TreePath path = getPathForRow(i);
            if (isExpanded(path)) {
                final Object o = path.getLastPathComponent();
                if (o instanceof TableData.Row) {
                    final TableData.Row row = (TableData.Row) o;
                    _expandedTags.add(row.tag());
                }
            }
        }
    }

    // Restores the expanded state of the treetable
    private void _restoreTreeState() {
        final List<TreePath> expandList = new ArrayList<>();
        final int numRows = getRowCount();
        for (int i = 0; i < numRows; i++) {
            final TreePath path = getPathForRow(i);
            final Object o = path.getLastPathComponent();
            if (o instanceof TableData.Row) {
                final TableData.Row row = (TableData.Row) o;
                if (_expandedTags.contains(row.tag())) {
                    expandList.add(path);
                }
            }
        }
        // do this last, since it changes the row indexes
        for(TreePath path : expandList) {
            expandPath(path);
        }
    }

    public void expandGroup(GuideGroup group) {
        int numRows = getRowCount();
        for(int i = 0; i < numRows; i++) {
            TreePath path = getPathForRow(i);
            if (!isExpanded(path)) {
                Object o = path.getLastPathComponent();
                if (o instanceof TableData.Row) {
                    TableData.Row row = (TableData.Row) o;
                    if (row.group().getOrNull() == group) {
                        expandPath(path);
                        getSelectionModel().addSelectionInterval(i, i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get the position that is currently selected.
     */
    public SPTarget getSelectedPos() {
        return TargetSelection.get(_env, _obsComp);
    }

    /**
     * Get the group that is currently selected.
     */
    public GuideGroup getSelectedGroup() {
        return TargetSelection.getGuideGroup(_env, _obsComp);
    }

    /**
     * Get the group that is currently selected or the parent group of the selected node.
     * @param env the target environment to use
     */
    public GuideGroup getSelectedGroupOrParentGroup(TargetEnvironment env) {
        GuideGroup group = getSelectedGroup();
        if (group == null) {
            SPTarget target = getSelectedPos();
            if (target != null) {
                for (GuideGroup g : env.getGroups()) {
                    if (g.containsTarget(target)) {
                        group = g;
                        break;
                    }
                }
            }
        }
        return group;
    }

    /**
     * Returns true if the given path is selected
     */
    public boolean isPathSelected(TreePath path) {
        return getRowForPath(path) == getSelectedRow();
    }

    /**
     * Returns an array of the selected tree nodes (currently only
     * single select is allowed, so the array has length 1).
     */
    public TableData.Row[] getSelectedNodes() {
        int i = getSelectedRow();
        if (i != -1) {
            Object o = getPathForRow(i).getLastPathComponent();
            if (o instanceof TableData.Row) {
                return new TableData.Row[]{(TableData.Row)o};
            }
        }
        return null;
    }


    /**
     * Returns true if it is ok to add the given item row to the given parent row
     * (parent must be a guide group row and item a suitable guide star object).
     */
    public boolean isOkayToAdd(TableData.Row item, TableData.Row parent) {
        if (item == parent) return false;
        GuideGroup group = parent.group().getOrNull();
        SPTarget target = item.target().getOrNull();
        if (group == null || target == null || group.containsTarget(target)) {
            return false;
        }
        return !item.tag().startsWith("Base") && !item.tag().startsWith("User");
    }

    /**
     * Returns true if it is ok to move the given row item to the given parent row
     */
    public boolean isOkayToMove(TableData.Row item, TableData.Row parent) {
        return isOkayToAdd(item, parent);
    }

    /**
     * Moves the given row item(s) to the given parent row.
     * In this case, a guide star to a group.
     */
    public void moveTo(TableData.Row[] items, TableData.Row parent) {
        if (items.length == 0) return;
        GuideGroup snkGrp = parent.group().getOrNull();
        SPTarget target = items[0].target().getOrNull();
        if (snkGrp.containsTarget(target)) return;
        GuideGroup srcGrp = getTargetGroup(target);
        if (srcGrp == null) return;

        ImList<GuideProbeTargets> targetList = srcGrp.getAllContaining(target);
        if (targetList.size() == 0) return;
        GuideProbeTargets src = targetList.get(0);

        GuideProbe guideprobe = src.getGuider();
        GuideProbeTargets snk = snkGrp.get(guideprobe).getOrElse(null);
        if (snk == null) {
            snk = GuideProbeTargets.create(guideprobe);
        }

        boolean isPrimary = src.getPrimary().getOrElse(null) == target;
        GuideProbeTargets newSrc = src.removeTarget(target);
        SPTarget newTarget = target.clone();
        GuideProbeTargets newSnk = snk.setOptions(snk.getOptions().append(newTarget));
        if (isPrimary) {
            newSnk = newSnk.selectPrimary(newTarget);
        }

        GuideEnvironment guideEnv = _env.getGuideEnvironment();
        GuideEnvironment newGuideEnv = guideEnv.putGuideProbeTargets(srcGrp, newSrc).putGuideProbeTargets(snkGrp, newSnk);
        TargetEnvironment newTargetEnv = _env.setGuideEnvironment(newGuideEnv);

        _dataObject.setTargetEnvironment(newTargetEnv);
    }

    private GuideGroup getTargetGroup(SPTarget target) {
        GuideEnvironment ge = _env.getGuideEnvironment();
        ImList<GuideGroup> groups = ge.getOptions();
        for(GuideGroup group : groups) {
            if (group.containsTarget(target)) {
                return group;
            }
        }
        return null;
    }

    private void _setSelectedRow(int index) {
        if ((index < 0) || (index >= getRowCount())) return;
        getSelectionModel().addSelectionInterval(index, index);
    }

    public void selectRowAt(int index) {
        if (_tableData == null) return;

        SPTarget target = _tableData.targetAt(index).getOrNull();
        if (target != null) {
            selectPos(target);
        } else {
            GuideGroup group = _tableData.groupAt(index).getOrNull();
            if (group != null) {
                selectGroup(group);
            }
        }
    }

    /**
     * Select the base position.
     */
    public void selectBasePos() {
        selectPos(_env.getBase());
    }


    /**
     * Select a given position.
     */
    public void selectPos(SPTarget tp) {
        TargetSelection.set(_env, _obsComp, tp);
    }

    /**
     * Select a given guide group.
     */
    public void selectGroup(GuideGroup group) {
        TargetSelection.setGuideGroup(_env, _obsComp, group);
    }

    /**
     * Selects the tree node at the given location
     */
    public void setSelectedNode(Point location) {
        TreePath path = getPathForLocation(location.x, location.y);
        if (path != null) {
            int i = getRowForPath(path);
            if (i != -1) selectRowAt(i);
        }
    }

    /**
     * Returns the node at the given location or null if not found.
     */
    public TableData.Row getNode(Point location) {
        if (location != null) {
            TreePath path = getPathForLocation(location.x, location.y);
            if (path != null) {
                Object o = path.getLastPathComponent();
                if (o instanceof TableData.Row) {
                    return (TableData.Row) o;
                }
            }
        }
        return null;
    }

    public void setIgnoreSelection(boolean ignore) {
        _ignoreSelection = ignore;
    }

    /**
     */
    public String toString() {
        String head = getClass().getName() + "[\n";
        String body = "";
        int numRows = getRowCount();
        int numCols = getColumnCount();
        for (int row = 0; row < numRows; ++row) {
            for (int col = 0; col < numCols; ++col) {
                body += "\t" + _tableData.getValueAt(col, row);
            }
            body += "\n";
        }
        body += "]";
        return head + body;
    }


    // OT-32: Displays a warning/confirmation dialog when guiding config is impacted
    // by changes to primary group.
    // Returns true if the new primary guide group should be set, false to cancel
    // the operation.
    boolean confirmGroupChange(GuideGroup oldPrimary, GuideGroup newPrimary) {
        final List<OffsetPosList<OffsetPosBase>> posLists = OffsetUtil.allOffsetPosLists(owner.getNode());
        if (posLists.size() > 0) {
            final SortedSet<GuideProbe> oldGuideProbes = oldPrimary.getReferencedGuiders();
            final SortedSet<GuideProbe> newGuideProbes = newPrimary.getReferencedGuiders();
            final Set<String> warnSet = new TreeSet<>();
            for (OffsetPosList posList : posLists) {
                for (Object o : posList.getAllPositions()) {
                    final OffsetPos offsetPos = (OffsetPos) o;
                    for (GuideProbe guideProbe : oldGuideProbes) {
                        final GuideOption guideOption = offsetPos.getLink(guideProbe);
                        final GuideOptions options = guideProbe.getGuideOptions();
                        if (guideOption != null
                                && guideOption != options.getDefaultActive()
                                && !newGuideProbes.contains(guideProbe)) {
                            warnSet.add(guideProbe.getKey());
                        }
                    }
                }
            }
            if (warnSet.size() != 0) {
                return confirmGroupChangeDialog(warnSet);
            }
        }
        return true;
    }

    // OT-32: Displays a warning/confirmation dialog when the given guide probe configs are impacted
    private boolean confirmGroupChangeDialog(Set<String> warnSet) {
        StringBuilder msg = new StringBuilder();
        msg.append("<html>Changing the primary group will result in losing the existing "
                         + "offset iterator guiding configuration for:<ul>");
        for(String key : warnSet) {
            msg.append("<li>").append(key).append("</li>");
        }
        msg.append("</ul><p>Continue?");

        return JOptionPane.showConfirmDialog(this, msg.toString(), "Warning", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }
}