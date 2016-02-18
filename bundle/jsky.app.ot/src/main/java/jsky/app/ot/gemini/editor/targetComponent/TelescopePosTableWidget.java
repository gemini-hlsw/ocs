package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.ags.api.*;
import edu.gemini.pot.ModelConverters;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetUtil;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.app.ot.util.Resources;
import jsky.util.gui.TableUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An extension of the TableWidget to support telescope target lists.
 */
public final class TelescopePosTableWidget extends JTable implements TelescopePosWatcher {
    private static final Icon errorIcon = Resources.getIcon("eclipse/error.gif");

    // Used to format values as strings.
    private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);
    static { nf.setMaximumFractionDigits(2); }


    static class TableData extends AbstractTableModel {
        enum Col {
            TAG("Type Tag") {
                public String getValue(final Row row) { return row.tag(); }
            },

            NAME("Name") {
                public String getValue(final Row row) { return row.name(); }
            },

            RA("RA") {
                public String getValue(final Row row) {
                    return row.target().flatMap(t -> t.getTarget().getRaString(row.when())).getOrElse("");
                }
            },

            DEC("Dec") {
                public String getValue(final Row row) {
                    return row.target().flatMap(t -> t.getTarget().getDecString(row.when())).getOrElse("");
                }
            },

            DIST("Dist") {
                public String getValue(final Row row) {
                    return row.distance().map(nf::format).getOrElse("");
                }
            };

            private final String displayName;
            Col(final String displayName) { this.displayName = displayName; }
            public String displayName() { return displayName; }
            public abstract String getValue(final Row row);
        }

        interface Row {
            boolean enabled();
            String tag();
            String name();

            // really we want: GuideGroup \/ SPTarget
            default Option<SPTarget> target()  { return None.instance(); }
            default Option<GuideGroup> group() { return None.instance(); }

            default Option<Double> distance()  { return None.instance(); }
            default List<Row> children()       { return Collections.emptyList(); }
            default Icon getIcon()             { return null; }

            default Border border(int col)     {
                return col == 0 ? BorderFactory.createEmptyBorder(0, 5, 0, 0) : null;
            }

            String formatMagnitude(Magnitude.Band band);
            Option<Long> when();
        }

        static abstract class AbstractRow implements Row {
            private final boolean enabled;
            private final String tag;
            private final String name;
            private final Option<SPTarget> target;
            private final Option<Long> when;

            AbstractRow(final boolean enabled, final String tag, final String name, final Option<SPTarget> target,
                        final Option<Long> when) {
                this.enabled  = enabled;
                this.tag      = tag;
                this.name     = name;
                this.target   = target;
                this.when     = when;
            }

            @Override public boolean enabled()          { return enabled;  }
            @Override public String tag()               { return tag;      }
            @Override public String name()              { return name;     }
            @Override public Option<SPTarget> target()  { return target;   }
            @Override public Option<Long> when()        { return when; }

            public Option<Magnitude> getMagnitude(final Magnitude.Band band) {
                return target.flatMap(t -> t.getMagnitude(band));
            }

            @Override public String formatMagnitude(final Magnitude.Band band) {
                return getMagnitude(band).map(MagnitudeEditor::formatBrightness).getOrElse("");
            }
        }

        static final class BaseTargetRow extends AbstractRow {
            BaseTargetRow(final SPTarget target, final Option<Long> when) {
                super(true, TargetEnvironment.BASE_NAME, target.getTarget().getName(), new Some<>(target), when);
            }
        }

        static abstract class NonBaseTargetRow extends AbstractRow {
            private final Option<Double> distance;

            NonBaseTargetRow(final boolean enabled, final String tag, final SPTarget target,
                             final Option<Coordinates> baseCoords, final Option<Long> when) {
                super(enabled, tag, target.getTarget().getName(), new Some<>(target), when);

                final Option<Coordinates> coords = getCoordinates(target, when);
                distance = baseCoords.flatMap(bc ->
                        coords.map(c ->
                                bc.angularDistance(c).toArcmins()
                        )
                );
            }

            @Override public Option<Double> distance() { return distance; }
        }

        static final class GuideTargetRow extends NonBaseTargetRow {
            private final boolean isActiveGuideProbe;
            private final Option<AgsGuideQuality> quality;

            GuideTargetRow(final boolean isActiveGuideProbe, final Option<AgsGuideQuality> quality,
                           final boolean enabled, final GuideProbe probe, final int index, final SPTarget target,
                           final Option<Coordinates> baseCoords, final Option<Long> when) {
                super(enabled, String.format("%s (%d)", probe.getKey(), index), target, baseCoords, when);
                this.isActiveGuideProbe = isActiveGuideProbe;
                this.quality = quality;
            }

            @Override public Border border(int col)     {
                return col == 0 ? BorderFactory.createEmptyBorder(0, 16, 0, 0) : null;
            }

            @Override public Icon getIcon() {
                return isActiveGuideProbe ?
                        GuidingIcon.apply(quality.getOrElse(AgsGuideQuality.Unusable$.MODULE$), enabled()) :
                        errorIcon;
            }
        }

        static final class UserTargetRow extends NonBaseTargetRow {
            UserTargetRow(final int index, final SPTarget target, final Option<Coordinates> baseCoords,
                          final Option<Long> when) {
                super(true, String.format("%s (%d)", TargetEnvironment.USER_NAME, index), target, baseCoords, when);
            }
        }

        static final class GroupRow extends AbstractRow {
            private final Option<GuideGroup> group;
            private final List<Row> children;

            GroupRow(final boolean enabled, final int index, final GuideGroup group, final List<Row> children) {
                super(enabled, group.getName().getOrElse("Guide Group " + index), "", None.instance(), None.instance());
                this.group    = new Some<>(group);
                this.children = Collections.unmodifiableList(children);
            }

            @Override public Option<GuideGroup> group() { return group; }
            @Override public List<Row> children()       { return children; }
        }

        // Collection of rows, which may include "subrows".
        private final ImList<Row> rows;

        // Total number of rows, including subrows. Precalculated as swing uses this value frequently.
        private final int numRows;

        private final ImList<Magnitude.Band> bands;
        private final ImList<String> columnHeaders;
        private final TargetEnvironment env;

        TableData() {
            rows          = DefaultImList.create();
            numRows       = 0;
            bands         = DefaultImList.create();
            columnHeaders = computeColumnHeaders(bands);
            env           = null;
        }

        private static Option<AgsGuideQuality> guideQuality(final Option<Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>> ags,
                                                            final GuideProbe guideProbe, final SPTarget guideStar) {
            return ags.flatMap(tup -> {
                if (guideProbe instanceof ValidatableGuideProbe) {
                    final ObsContext ctx = tup._1();
                    final AgsMagnitude.MagnitudeTable magTable = tup._2();
                    final ValidatableGuideProbe vgp = (ValidatableGuideProbe) guideProbe;
                    return AgsRegistrar.instance().currentStrategyForJava(ctx).map(strategy -> {
                        final Option<AgsAnalysis> agsAnalysis = strategy.analyzeForJava(ctx, magTable, vgp,
                                ModelConverters.toSideralTarget(guideStar));
                        return agsAnalysis.map(AgsAnalysis::quality);
                    }).getOrElse(None.instance());
                } else {
                    return None.instance();
                }
            });
        }

        TableData(final Option<ObsContext> ctx, final TargetEnvironment env) {
            this.env       = env;
            bands          = getSortedBands(env);
            columnHeaders  = computeColumnHeaders(bands);

            final List<Row> rowList = createRows(ctx);
            rows                    = DefaultImList.create(rowList);
            numRows                 = countRows();
        }

        // Create rows for all the guide groups and targets, and keep track of the number of rows created.
        private List<Row> createRows(final Option<ObsContext> ctx) {
            final List<Row> tmpRows = new ArrayList<>();

            // Add the base position first.
            final SPTarget base = env.getBase();
            final Option<Long> when = ctx.flatMap(c -> c.getSchedulingBlock().map(SchedulingBlock::start));
            final Option<Coordinates> baseCoords = getCoordinates(base, when);
            tmpRows.add(new BaseTargetRow(base, when));

            // Add all the guide groups and targets.
            final Option<Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>> ags = ctx.map(oc -> new Pair<>(oc, OT.getMagnitudeTable()));
            final GuideEnvironment ge = env.getGuideEnvironment();
            final ImList<GuideGroup> groups = ge.getOptions();
            final GuideGroup primaryGroup = env.getOrCreatePrimaryGuideGroup();

            // Process each group.
            groups.zipWithIndex().foreach(gtup -> {
                final GuideGroup group = gtup._1();
                final int groupIndex = gtup._2() + 1;

                final boolean isPrimaryGroup = groups.size() < 2 || group == primaryGroup;
                final List<Row> rowList = new ArrayList<>();

                // Process the guide probe targets for this group.
                group.getAll().foreach(gpt -> {
                    final GuideProbe guideProbe = gpt.getGuider();
                    final boolean isActive = ctx.exists(c -> GuideProbeUtil.instance.isAvailable(c, guideProbe));
                    final Option<SPTarget> primary = gpt.getPrimary();

                    // Add all the targets.
                    gpt.getTargets().zipWithIndex().foreach(tup -> {
                        final SPTarget target = tup._1();
                        final int index = tup._2() + 1;

                        final Option<AgsGuideQuality> quality = guideQuality(ags, guideProbe, target);
                        final boolean enabled = isPrimaryGroup && primary.exists(target::equals);

                        final Row row = new GuideTargetRow(isActive, quality, enabled, guideProbe, index, target,
                                baseCoords, when);
                        rowList.add(row);
                    });
                });
                tmpRows.add(new GroupRow(isPrimaryGroup, groupIndex, group, rowList));
            });

            // Add the user positions.
            env.getUserTargets().zipWithIndex().foreach(tup -> {
                final SPTarget target = tup._1();
                final int index = tup._2() + 1;
                tmpRows.add(new UserTargetRow(index, target, baseCoords, when));
            });

            return tmpRows;
        }

        // Pre-compute the number of rows as this will be frequently used.
        private int countRows() {
            return rows.foldLeft(0, (numRows, row) -> numRows + row.children().size() + 1);
        }

        // Gets all the magnitude bands used by targets in the target
        // environment.
        private ImList<Magnitude.Band> getSortedBands(final TargetEnvironment env) {
            // Keep a sorted set of bands, sorted by the name.
            final Set<Magnitude.Band> bands = new TreeSet<>(Magnitude.Band.WAVELENGTH_COMPARATOR);

            // Extract all the magnitude bands from the environment.
            env.getTargets().foreach(spTarget -> bands.addAll(spTarget.getTarget().getMagnitudeBands()));

            // Create an immutable sorted list containing the results.
            return DefaultImList.create(bands);
        }

        public int indexOf(final SPTarget target) {
            if (target == null) return -1;

            int index = 0;
            for (final Row row : rows) {
                if (row.target().getOrNull() == target) return index;
                ++index;

                for (final Row row2 : row.children()) {
                    if (row2.target().getOrNull() == target) return index;
                    ++index;
                }
            }
            return -1;
        }

        public int indexOf(final GuideGroup group) {
            if (group == null) return -1;

            int index = 0;
            for (final Row row : rows) {
                if (row.group().getOrNull() == group) return index;
                index += row.children().size() + 1;
            }
            return -1;
        }

        public Option<SPTarget> targetAt(final int index) {
            return rowAt(index).flatMap(Row::target);
        }

        public Option<GuideGroup> groupAt(final int index) {
            return rowAt(index).flatMap(Row::group);
        }

        public Option<Row> rowAt(final int index) {
            if (index >= 0) {
                int i = 0;
                for (final Row row : rows) {
                    if (i == index)
                        return new Some<>(row);
                    ++i;

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


        private static ImList<String> computeColumnHeaders(final ImList<Magnitude.Band> bands) {
            // First add the fixed column headers
            final List<String> hdr = Arrays.stream(Col.values()).map(Col::displayName).collect(Collectors.toList());

            // Add each magnitude band name
            hdr.addAll(bands.map(Magnitude.Band::name).toList());
            return DefaultImList.create(hdr);
        }

        @Override public int getColumnCount() {
            return Col.values().length + bands.size();
        }

        @Override public String getColumnName(final int index) {
            return columnHeaders.get(index);
        }

        @Override public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (rowIndex < 0 || rowIndex >= numRows)
                return null;

            final Col[] cols = Col.values();
            return rowAt(rowIndex).map(row ->
                    (columnIndex < cols.length) ?
                    cols[columnIndex].getValue(row) :
                    row.formatMagnitude(bands.get(columnIndex - cols.length))
            ).getOrNull();
        }

        @Override public int getRowCount() {
            return numRows;
        }
    }

    // Return the world coordinates for the given target
    private static Option<Coordinates> getCoordinates(final SPTarget tp, final Option<Long> when) {
        final ITarget target = tp.getTarget();
        return target.getRaDegrees(when).flatMap(ra ->
                target.getDecDegrees(when).flatMap(dec ->
                    ImOption.apply(Coordinates.fromDegrees(ra, dec).getOrElse(null))
                )
        );
    }

    // Telescope position list
    private ISPObsComponent _obsComp;
    private TargetObsComp _dataObject;
    private TargetEnvironment _env;
    private TableData _tableData;

    // if true, ignore selections
    private boolean _ignoreSelection;

    private final EdCompTargetList owner;

    private final TelescopePosTableDropTarget dropTarget;
    private final TelescopePosTableDragSource dragSource;


    /**
     * Default constructor.
     */
    public TelescopePosTableWidget(final EdCompTargetList owner) {
        this.owner = owner;
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // disable editing by default
        setModel(new TableData());

        getSelectionModel().addListSelectionListener(e -> {
            if (_ignoreSelection) return;
            final TableData tableData = (TableData) getModel();
            final int idx = getSelectionModel().getMinSelectionIndex();
            tableData.rowAt(idx).foreach(this::notifySelect);
        });

        getTableHeader().setReorderingAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowHorizontalLines(false);
        setShowVerticalLines(true);
        getColumnModel().setColumnMargin(1);
        setRowSelectionAllowed(true);
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color ODD_ROW_COLOR = new Color(248, 247, 255);
            @Override public Component getTableCellRendererComponent(final JTable table,
                                                                     final Object value,
                                                                     final boolean isSelected,
                                                                     final boolean hasFocus,
                                                                     final int row,
                                                                     final int column) {
                final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                _tableData.rowAt(row).foreach(tableDataRow -> {
                    // If we are in column 0, we treat this differently.
                    if (column == 0) {
                        if (!tableDataRow.group().isEmpty()) {
                            final String tag = tableDataRow.tag();
                            final String name = tableDataRow.name();
                            label.setText(name != null && !name.equals("") ? name : tag);
                        } else {
                            label.setText(tableDataRow.tag());
                        }
                        label.setIcon(tableDataRow.getIcon());
                        label.setDisabledIcon(tableDataRow.getIcon());
                    } else {
                        label.setIcon(null);
                        label.setDisabledIcon(null);
                    }

                    label.setBorder(tableDataRow.border(column));

                    final int style = tableDataRow.enabled() ? Font.PLAIN : Font.ITALIC;
                    final Font font = label.getFont().deriveFont(style);
                    label.setFont(font);

                    final Color c = tableDataRow.enabled() ? Color.BLACK : Color.GRAY;
                    label.setForeground(c);

                    label.setEnabled(tableDataRow.enabled());

                    // If the row is not selected, set the background to alternating stripes.
                    if (!isSelected) {
                        label.setBackground(row % 2 == 0 ? Color.WHITE : ODD_ROW_COLOR);
                    }
                });

                return label;
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() != 2) return;
                updatePrimaryStar();
            }
        });

        // Add drag and drop features
        dropTarget = new TelescopePosTableDropTarget(this);
        dragSource = new TelescopePosTableDragSource(this);
    }

    public void telescopePosUpdate(final WatchablePos tp) {
        final int index = getSelectedRow();
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
    public void reinit(final TargetObsComp dataObject) {
        dragSource.setEditable(false);
        dropTarget.setEditable(false);

        // Remove all target listeners for the old target environment.
        if (_env != null) {
            _env.getTargets().foreach(t -> t.deleteWatcher(TelescopePosTableWidget.this));
        }

        // Stop watching for changes on the obsComp
        stopWatchingSelection();
        stopWatchingEnv();

        // Watch the new list, and add all of its positions at once
        _obsComp    = owner.getContextTargetObsComp();
        _dataObject = dataObject;

        startWatchingEnv();
        startWatchingSelection();

        final TargetEnvironment env = _dataObject.getTargetEnvironment();
        env.getTargets().foreach(t -> t.addWatcher(TelescopePosTableWidget.this));

        final SPTarget tp = TargetSelection.get(_dataObject.getTargetEnvironment(), _obsComp);
        _resetTable(_dataObject.getTargetEnvironment());

        final int index = _tableData.indexOf(tp);
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

    private void notifySelect(final TableData.Row row) {
        final SPTarget target = row.target().getOrNull();
        if (target != null) {
            stopWatchingSelection();
            try {
                TargetSelection.set(_env, _obsComp, target);
            } finally {
                startWatchingSelection();
            }
        } else {
            final GuideGroup group = row.group().getOrNull();
            if (group != null) TargetSelection.setGuideGroup(_env, _obsComp, group);
        }
    }

    private final PropertyChangeListener selectionListener = evt -> {
        if (_tableData == null) return;
        final SPTarget target = TargetSelection.get(_env, _obsComp);
        if (target != null) {
            final int index = _tableData.indexOf(target);
            _setSelectedRow(index);
        }
    };

    private final PropertyChangeListener envChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            final TargetEnvironment oldEnv = (TargetEnvironment) evt.getOldValue();
            final TargetEnvironment newEnv = (TargetEnvironment) evt.getNewValue();

            final TargetEnvironmentDiff diff = TargetEnvironmentDiff.all(oldEnv, newEnv);
            final Collection<SPTarget> rmTargets  = diff.getRemovedTargets();
            final Collection<SPTarget> addTargets = diff.getAddedTargets();

            // First update the target listeners according to what was added
            // and removed.
            rmTargets.forEach (t -> t.deleteWatcher(TelescopePosTableWidget.this));
            addTargets.forEach(t -> t.addWatcher(TelescopePosTableWidget.this));

            // Remember what was selected before the reset below.
            final int oldSelIndex       = getSelectedRow();
            final SPTarget oldSelTarget = _tableData.rowAt(oldSelIndex).flatMap(TableData.Row::target).getOrNull();

            // Update the table -- setting _tableData ....
            _resetTable(newEnv);

            // Update the selection.
            if (rmTargets.isEmpty() && (addTargets.size() == 1)) {
                // A single position was added, so just select it.
                selectPos(addTargets.iterator().next());
                return;
            }

            // If obs comp has a selected target, then honor it.
            final SPTarget newSelectedTarget = TargetSelection.get(_env, _obsComp);
            if (newSelectedTarget != null) {
                final int index = _tableData.indexOf(newSelectedTarget);
                _setSelectedRow(index);
                return;
            }

            // If a new group was added, select it
            final GuideGroup newSelectedGroup = TargetSelection.getGuideGroup(_env, _obsComp);
            if (newSelectedGroup != null) {
                final int index = _tableData.indexOf(newSelectedGroup);
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
            final int newSelIndex = (oldSelIndex >= getRowCount()) ? getRowCount() - 1 : oldSelIndex;
            if (newSelIndex >= 0) {
                selectRowAt(newSelIndex);
            } else {
                selectBasePos();
            }
        }
    };

    private void _resetTable(final TargetEnvironment env) {
        _env = env;
        final Option<ObsContext> ctx = ObsContext.create(owner.getContextObservation()).map(c -> c.withTargets(env));
        _tableData = new TableData(ctx, env);

        _ignoreSelection = true;
        try {
            setModel(_tableData);
        } finally {
            _ignoreSelection = false;
        }
        TableUtil.initColumnSizes(this);
    }

    /**
     * Update the primary star in a set of guide targets to the currently selected target.
     */
    void updatePrimaryStar() {
        if (_env == null || !OTOptions.isEditable(_obsComp.getProgram(), _obsComp.getContextObservation())) return;
        final SPTarget target = getSelectedPos();
        if (target != null) {
            PrimaryTargetToggle.instance.toggle(_dataObject, target);
        } else {
            final GuideGroup group = getSelectedGroup();
            if (group != null) {
                final TargetEnvironment env = _dataObject.getTargetEnvironment();
                final GuideGroup primary = env.getOrCreatePrimaryGuideGroup();
                if (primary != group && confirmGroupChange(primary, group)) {
                    final GuideEnvironment ge = env.getGuideEnvironment();
                    if (ge != null) {
                        _dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.selectPrimary(group)));
                    }
                }
            }
        }
    }

    /**
     * Get the position that is currently selected.
     */
    private SPTarget getSelectedPos() {
        return TargetSelection.get(_env, _obsComp);
    }

    /**
     * Get the group that is currently selected.
     */
    private GuideGroup getSelectedGroup() {
        return TargetSelection.getGuideGroup(_env, _obsComp);
    }

    /**
     * Get the group that is currently selected or the parent group of the selected node.
     * @param env the target environment to use
     */
    public GuideGroup getSelectedGroupOrParentGroup(final TargetEnvironment env) {
        final GuideGroup group = getSelectedGroup();
        if (group != null) return group;

        final SPTarget target = getSelectedPos();
        if (target == null) return null;

        return env.getGroups().find(gg -> gg.containsTarget(target)).getOrNull();
    }

    /**
     * Returns the selected node (currently only single select is allowed).
     */
    public Option<TableData.Row> getSelectedNode() {
        return _tableData.rowAt(getSelectedRow());
    }

    /**
     * Returns true if it is ok to add the given item row to the given parent row
     * (parent must be a guide group row and item a suitable guide star object).
     */
    public boolean isOkayToAdd(final TableData.Row item, final TableData.Row parent) {
        if (item == parent) return false;
        final GuideGroup group = parent.group().getOrNull();
        final SPTarget target = item.target().getOrNull();
        return !(group == null || target == null || group.containsTarget(target))
                && !(item instanceof TableData.BaseTargetRow) && !(item instanceof TableData.UserTargetRow);
    }

    /**
     * Returns true if it is ok to move the given row item to the given parent row
     */
    public boolean isOkayToMove(TableData.Row item, TableData.Row parent) {
        return isOkayToAdd(item, parent);
    }

    /**
     * Moves the given row item to the given parent row.
     * In this case, a guide star to a group.
     */
    public void moveTo(final TableData.Row item, final TableData.Row parent) {
        if (item == null) return;
        final GuideGroup snkGrp = parent.group().getOrNull();
        final SPTarget target = item.target().getOrNull();
        if (snkGrp.containsTarget(target)) return;
        final GuideGroup srcGrp = getTargetGroup(target);
        if (srcGrp == null) return;

        final ImList<GuideProbeTargets> targetList = srcGrp.getAllContaining(target);
        if (targetList.isEmpty()) return;
        final GuideProbeTargets src = targetList.get(0);

        final GuideProbe guideprobe = src.getGuider();
        GuideProbeTargets snk = snkGrp.get(guideprobe).getOrElse(null);
        if (snk == null) {
            snk = GuideProbeTargets.create(guideprobe);
        }

        final boolean isPrimary = src.getPrimary().getOrElse(null) == target;
        final GuideProbeTargets newSrc = src.removeTarget(target);
        final SPTarget newTarget = target.clone();

        GuideProbeTargets newSnk = snk.setOptions(snk.getOptions().append(newTarget));
        if (isPrimary) {
            newSnk = newSnk.selectPrimary(newTarget);
        }

        final GuideEnvironment guideEnv = _env.getGuideEnvironment();
        final GuideEnvironment newGuideEnv = guideEnv.putGuideProbeTargets(srcGrp, newSrc).putGuideProbeTargets(snkGrp, newSnk);
        final TargetEnvironment newTargetEnv = _env.setGuideEnvironment(newGuideEnv);

        _dataObject.setTargetEnvironment(newTargetEnv);
    }

    /**
     * Get the group to which this target belongs, or null.
     */
    private GuideGroup getTargetGroup(final SPTarget target) {
        return _env.getGuideEnvironment().getOptions().find(gg -> gg.containsTarget(target)).getOrNull();
    }

    /**
     * Updates the TargetSelection's target or group, and selects the relevant row in the table.
     */
    public void selectRowAt(final int index) {
        if (_tableData == null) return;
        final SPTarget target = _tableData.targetAt(index).getOrNull();
        if (target != null) {
            selectPos(target);
        } else {
            final GuideGroup group = _tableData.groupAt(index).getOrNull();
            if (group != null) {
                selectGroup(group);
            }
        }
    }

    /**
     * Select the base position, updating the TargetSelection's target, and set the relevant row in the table.
     */
    void selectBasePos() {
        selectPos(_env.getBase());
    }


    /**
     * Updates the TargetSelection's target, and sets the relevant row in the table.
     */
    void selectPos(final SPTarget tp) {
        TargetSelection.set(_env, _obsComp, tp);
        _setSelectedRow(_tableData.indexOf(tp));
    }

    /**
     * Update the TargetSelection's group, and sets the relevant row in the table.
     */
    void selectGroup(final GuideGroup group) {
        TargetSelection.setGuideGroup(_env, _obsComp, group);
        _setSelectedRow(_tableData.indexOf(group));
    }

    /**
     * Selects the table row at the given location.
     */
    public void setSelectedRow(final Point location) {
        selectRowAt(rowAtPoint(location));
    }

    /**
     * Returns the node at the given location or null if not found.
     */
    public TableData.Row getNode(final Point location) {
        return _tableData.rowAt(rowAtPoint(location)).getOrNull();
    }

    /**
     * Selects the relevant row in the table.
     */
    private void _setSelectedRow(final int index) {
        if ((index < 0) || (index >= getRowCount())) return;
        getSelectionModel().setSelectionInterval(index, index);
    }

    public void setIgnoreSelection(boolean ignore) {
        _ignoreSelection = ignore;
    }

    /**
     */
    public String toString() {
        final String head = getClass().getName() + "[\n";
        String body = "";
        final int numRows = getRowCount();
        final int numCols = getColumnCount();
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
    boolean confirmGroupChange(final GuideGroup oldPrimary, final GuideGroup newPrimary) {
        final List<OffsetPosList<OffsetPosBase>> posLists = OffsetUtil.allOffsetPosLists(owner.getContextObservation());
        if (!posLists.isEmpty()) {
            final SortedSet<GuideProbe> oldGuideProbes = oldPrimary.getReferencedGuiders();
            final SortedSet<GuideProbe> newGuideProbes = newPrimary.getReferencedGuiders();
            final Set<String> warnSet = new TreeSet<>();
            for (OffsetPosList<OffsetPosBase> posList : posLists) {
                for (OffsetPosBase offsetPos : posList.getAllPositions()) {
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
            if (!warnSet.isEmpty()) {
                return confirmGroupChangeDialog(warnSet);
            }
        }
        return true;
    }

    // OT-32: Displays a warning/confirmation dialog when the given guide probe configs are impacted
    private boolean confirmGroupChangeDialog(final Set<String> warnSet) {
        final StringBuilder msg = new StringBuilder();
        msg.append("<html>Changing the primary group will result in losing the existing "
                + "offset iterator guiding configuration for:<ul>");
        warnSet.forEach(key -> msg.append("<li>").append(key).append("</li>"));
        msg.append("</ul><p>Continue?");

        return JOptionPane.showConfirmDialog(this, msg.toString(), "Warning", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }
}
