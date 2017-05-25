package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.ags.api.*;
import edu.gemini.pot.ModelConverters;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetUtil;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.app.ot.OT;
import jsky.app.ot.OTOptions;
import jsky.util.gui.Resources;
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * An extension of the TableWidget to support telescope target lists.
 */
final class TelescopePosTableWidget extends JTable implements TelescopePosWatcher {
    private static final Icon errorIcon = Resources.getIcon("eclipse/error.gif");

    // Used to format values as strings.
    private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);
    static { nf.setMaximumFractionDigits(2); }

    private static final DecimalFormat MAG_FORMAT = new DecimalFormat("0.0##");

    // Note -- synchronized though I guess this should always be called from
    // gui thread...
    private static synchronized String formatBrightness(Magnitude mag) {
        return MAG_FORMAT.format(mag.value());
    }

    static class TelescopePosTableModel extends AbstractTableModel {
        enum Col {
            TAG("Type Tag") {
                public String getValue(final Row row) { return row.tag(); }
            },

            NAME("Name") {
                public String getValue(final Row row) { return row.name(); }
            },

            RA("RA") {
                public String getValue(final Row row) {
                    return row.target().flatMap(t -> t.getRaString(row.when())).getOrElse("");
                }
            },

            DEC("Dec") {
                public String getValue(final Row row) {
                    return row.target().flatMap(t -> t.getDecString(row.when())).getOrElse("");
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
            default Option<SPTarget> target()         { return None.instance(); }
            default Option<IndexedGuideGroup> group() { return None.instance(); }
            default Option<Double> distance()         { return None.instance(); }
            default List<Row> children()              { return Collections.emptyList(); }
            default Icon getIcon()                    { return null; }
            default boolean editable()                { return true; }
            default boolean movable()                 { return true; }

            default Border border(int col)     {
                return col == 0 ? BorderFactory.createEmptyBorder(0, 5, 0, 0) : null;
            }

            String formatMagnitude(MagnitudeBand band);
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

            Option<Magnitude> getMagnitude(final MagnitudeBand band) {
                return target.flatMap(t -> t.getMagnitudeJava(band));
            }

            @Override public String formatMagnitude(final MagnitudeBand band) {
                return getMagnitude(band).map(TelescopePosTableWidget::formatBrightness).getOrElse("");
            }
        }

        static final class BaseTargetRow extends AbstractRow {
            BaseTargetRow(final SPTarget target, final Option<Long> when) {
                super(true, TargetEnvironment.BASE_NAME, target.getName(), new Some<>(target), when);
            }

            @Override public boolean movable() { return false; }
        }

        static abstract class NonBaseTargetRow extends AbstractRow {
            private final Option<Double> distance;

            NonBaseTargetRow(final boolean enabled, final String tag, final SPTarget target,
                             final Option<Coordinates> baseCoords, final Option<Long> when) {
                super(enabled, tag, target.getName(), new Some<>(target), when);

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
            private final boolean editable;
            private final boolean movable;

            GuideTargetRow(final boolean isActiveGuideProbe, final Option<AgsGuideQuality> quality,
                           final boolean enabled, final boolean editable, final boolean movable,
                           final GuideProbe probe, final int index, final SPTarget target,
                           final Option<Coordinates> baseCoords, final Option<Long> when) {
                super(enabled, String.format("%s (%d)", probe.getKey(), index), target, baseCoords, when);
                this.isActiveGuideProbe = isActiveGuideProbe;
                this.quality = quality;
                this.editable = editable;
                this.movable = movable;
            }

            @Override public Border border(int col)     {
                return col == 0 ? BorderFactory.createEmptyBorder(0, 16, 0, 0) : null;
            }

            @Override public Icon getIcon() {
                return isActiveGuideProbe ?
                        GuidingIcon.apply(quality.getOrElse(AgsGuideQuality.Unusable$.MODULE$), enabled()) :
                        errorIcon;
            }

            @Override public boolean editable() { return editable; }
            @Override public boolean movable()  { return movable;  }
        }

        static final class UserTargetRow extends NonBaseTargetRow {
            UserTargetRow(final int index, final SPTarget target, final Option<Coordinates> baseCoords,
                          final Option<Long> when) {
                super(true, String.format("%s (%d)", TargetEnvironment.USER_NAME, index), target, baseCoords, when);
            }

            @Override public boolean movable() { return false; }
        }

        static final class GroupRow extends AbstractRow {
            private static String extractName(final GuideGroup group) {
                final GuideGrp grp = group.grp();
                final String name;
                if (grp.isAutomatic()) {
                    if (grp instanceof AutomaticGroup.Disabled$) {
                        name = "Auto (Disabled)";
                    } else {
                        name = "Auto";
                    }
                } else {
                    name = group.getName().getOrElse("Manual");
                }
                return name;
            }

            private final Option<IndexedGuideGroup> group;
            private final List<Row> children;
            private final boolean editable;

            GroupRow(final boolean enabled, final boolean editable,
                     final int index, final GuideGroup group, final List<Row> children) {
                super(enabled, extractName(group), "", None.instance(), None.instance());
                this.group    = new Some<>(IndexedGuideGroup$.MODULE$.apply(index, group));
                this.children = Collections.unmodifiableList(children);
                this.editable = editable;
            }

            @Override public Option<IndexedGuideGroup> group() { return group; }
            @Override public List<Row> children()              { return children; }
            @Override public boolean editable()                { return editable; }
            @Override public boolean movable()                 { return false; }
        }

        private static final int FIRST_AUTO_TARGET_IDX = 2;

        // Collection of rows, which may include "subrows".
        private List<Row> rows;

        // Total number of rows, including subrows. Precalculated as swing uses this value frequently.
        private int numRows;

        private ImList<MagnitudeBand> bands;
        private ImList<String> columnHeaders;
        private TargetEnvironment env;

        // A completely empty set of TableData.
        TelescopePosTableModel() {
            rows          = new ArrayList<>();
            numRows       = 0;
            bands         = DefaultImList.create();
            columnHeaders = computeColumnHeaders(bands);
            env           = null;
        }

        // Create a completely fresh TableData for the given env.
        TelescopePosTableModel(final Option<ObsContext> ctxOpt, final TargetEnvironment env) {
            this.env       = env;
            bands          = getSortedBands(env);
            columnHeaders  = computeColumnHeaders(bands);
            rows           = createRows(ctxOpt);
            numRows        = countRows();
        }

        // Create all rows for the table for the current target environment.
        private List<Row> createRows(final Option<ObsContext> ctxOpt) {
            final List<Row> tmpRows = new ArrayList<>();

            // Add the base position first.
            final SPTarget base = env.getArbitraryTargetFromAsterism();
            final Option<Long> when = ctxOpt.flatMap(ObsContext::getSchedulingBlockStart);
            final Option<Coordinates> baseCoords = getCoordinates(base, when);
            tmpRows.add(new BaseTargetRow(base, when));

            // Add all the guide groups and targets.
            final GuideEnvironment ge = env.getGuideEnvironment();
            final ImList<GuideGroup> groups = ge.getOptions();

            // Process each group.
            groups.zipWithIndex().foreach(gtup -> {
                final GuideGroup group = gtup._1();
                final int groupIndex   = gtup._2();
                final GroupRow row     = createGroupRow(ctxOpt, groupIndex, group);
                tmpRows.add(row);
            });

            // Add the user positions.
            env.getUserTargets().zipWithIndex().foreach(tup -> {
                final SPTarget target = tup._1();
                final int index = tup._2() + 1;
                tmpRows.add(new UserTargetRow(index, target, baseCoords, when));
            });

            return tmpRows;
        }

        // Update the auto row of a table.
        // This is called when the auto row should be changed from disabled to initial.
        void enableAutoRow(final TargetEnvironment env) {
            final GroupRow autoGroupRow = new GroupRow(true, false, 0, env.getGuideEnvironment().automaticGroup(),
                    Collections.emptyList());
            rows.set(1, autoGroupRow);
            fireTableRowsUpdated(1, 1);
        }

        // Replace the auto group and return a new TableData.
        // Note that we work under the assumption that there is ALWAYS an auto group, which should be the case,
        // and that it is in row position 1 of the table (directly after the base).
        void replaceAutoGroup(final Option<ObsContext> ctxOpt, final TargetEnvironment newEnv) {
            final List<Row>         oldRows = rows;

            // Recalculate the rows to see if quality has changed based on the new context.
            env = newEnv;
            final Set<Integer> changedRowIndices      = recalculateManualGSQuality(ctxOpt);

            // The auto group is always in group position 0 and row position 1.
            // Recreate it and set it in the new rows.
            final GuideGroup autoGroup  = newEnv.getGuideEnvironment().getOptions().head();
            final GroupRow autoGroupRow = createGroupRow(ctxOpt, 0, autoGroup);
            rows.set(1, autoGroupRow);
            numRows = countRows();

            // Determine the size difference between the old auto group and the new.
            final int oldAutoGroupSize  = oldRows.get(1).children().size();
            final int newAutoGroupSize  = autoGroupRow.children().size();
            final int autoGroupSizeDiff = newAutoGroupSize - oldAutoGroupSize;
            final int autoGroupSizeInt  = Math.min(oldAutoGroupSize, newAutoGroupSize);

            // Fire events indicating the changes in the model.
            if (autoGroupSizeInt > 0) {
                fireTableRowsUpdated(FIRST_AUTO_TARGET_IDX, FIRST_AUTO_TARGET_IDX + autoGroupSizeInt - 1);
            }
            if (oldAutoGroupSize < newAutoGroupSize) {
                fireTableRowsInserted(FIRST_AUTO_TARGET_IDX + oldAutoGroupSize, FIRST_AUTO_TARGET_IDX + newAutoGroupSize - 1);
            } else if (newAutoGroupSize < oldAutoGroupSize) {
                fireTableRowsDeleted(FIRST_AUTO_TARGET_IDX + newAutoGroupSize, FIRST_AUTO_TARGET_IDX + oldAutoGroupSize - 1);
            }

            // Now for each group that was changed, fire update events.
            int tableRowIdx = 0;
            final Stream<Tuple2<Integer,Row>> indexedRows = IntStream.range(0, rows.size()).mapToObj(i -> new Pair<>(i, rows.get(i)));
            for (final Tuple2<Integer, Row> tup2 : indexedRows.collect(Collectors.toList())) {
                final int rowIdx = tup2._1();
                final Row row    = tup2._2();

                if (changedRowIndices.contains(rowIdx)) {
                    // We only care about the child rows (hence + 1), and all rows have been offset by autoGroupSizeDiff.
                    final int lowerIdx = tableRowIdx + 1 + autoGroupSizeDiff;
                    final int upperIdx = lowerIdx + row.children().size() - 1;
                    fireTableRowsUpdated(lowerIdx, upperIdx);
                }

                tableRowIdx += 1 + row.children().size();
            }
        }

        // Given a guide group and its index amongst the set of all groups, create a GroupRow (and all children
        // target subrows) representing it.
        private GroupRow createGroupRow(final Option<ObsContext> ctxOpt, final int groupIdx, final GuideGroup group) {
            final SPTarget base                  = env.getArbitraryTargetFromAsterism();
            final Option<Long> when              = ctxOpt.flatMap(ObsContext::getSchedulingBlockStart);
            final Option<Coordinates> baseCoords = getCoordinates(base, when);
            final GuideEnvironment ge            = env.getGuideEnvironment();
            final boolean isPrimaryGroup         = ge.getPrimaryIndex() == groupIdx;
            final boolean editable               = group.isManual();
            final boolean movable                = group.isManual();
            final Option<Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>> ags = agsForGroup(ctxOpt, group);

            final List<Row> rowList = new ArrayList<>();
            group.getAll().foreach(gpt -> {
                final GuideProbe guideProbe    = gpt.getGuider();
                final boolean isActive         = ctxOpt.exists(ctx -> GuideProbeUtil.instance.isAvailable(ctx, guideProbe));
                final Option<SPTarget> primary = gpt.getPrimary();

                // Add all the targets.
                // The index here is used to generate target names, so begin at 1.
                gpt.getTargets().zipWithIndex().foreach(tup -> {
                    final SPTarget target = tup._1();
                    final int index       = tup._2() + 1;

                    final Option<AgsGuideQuality> quality = guideQuality(ags, guideProbe, target);
                    final boolean enabled = isPrimaryGroup && primary.exists(target::equals);

                    final Row row = new GuideTargetRow(isActive, quality, enabled, editable, movable, guideProbe,
                            index, target, baseCoords, when);
                    rowList.add(row);
                });
            });

            return new GroupRow(isPrimaryGroup, false, groupIdx, group, rowList);
        }

        // Recalculate the quality of all guide stars. If one of them has changed, return a new set of rows.
        // We return a list of row indices of GroupRows indicating which groups have changed.
        private Set<Integer> recalculateManualGSQuality(final Option<ObsContext> ctxOpt) {
            final List<Row>    newRows          = new ArrayList<>();
            final Set<Integer> groupRowsChanged = new HashSet<>();

            IntStream.range(0, rows.size()).mapToObj(i -> new Pair<>(i, rows.get(i))).forEach(tup -> {
                final int rowIdx = tup._1();
                final Row row    = tup._2();

                if (row instanceof GroupRow) {
                    final IndexedGuideGroup igg = row.group().getValue();
                    final int gIdx              = igg.index();
                    final GuideGroup gg         = igg.group();

                    // Recreate the entire group row, and then scan the children to see if any of the qualities are different.
                    final GroupRow gRowNew = createGroupRow(ctxOpt, gIdx, gg);

                    // Zipping Java lists is too obnoxious.
                    final ImList<Row> childRowsOld       = DefaultImList.create(row.children());
                    final ImList<Row> childRowsNew       = DefaultImList.create(gRowNew.children());
                    final boolean groupChanged           = childRowsOld.zip(childRowsNew).exists(tup2 -> {
                        final GuideTargetRow gtrOld = (GuideTargetRow) tup2._1();
                        final GuideTargetRow gtrNew = (GuideTargetRow) tup2._2();
                        return !gtrOld.quality.exists(q -> gtrNew.quality.exists(q::equals));
                    });

                    if (groupChanged) {
                        newRows.add(gRowNew);
                        groupRowsChanged.add(rowIdx);
                    } else {
                        newRows.add(row);
                    }
                } else {
                    newRows.add(row);
                }
            });

            rows = newRows;
            return groupRowsChanged;
        }

        // Return an object used for guide star quality calculations, adjusted by pos angle for the group if one exists.
        private static Option<Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>> agsForGroup(final Option<ObsContext> ctxOpt,
                                                                                           final GuideGroup group) {
            final Option<ObsContext> adjCtxOpt;
            final GuideGrp grp = group.grp();
            if (grp instanceof AutomaticGroup.Active) {
                adjCtxOpt = ctxOpt.map(ctx -> ctx.withPositionAngle(((AutomaticGroup.Active) grp).posAngle()));
            } else {
                adjCtxOpt = ctxOpt;
            }
            return adjCtxOpt.map(ctx -> new Pair<>(ctx, OT.getMagnitudeTable()));
        }

        // Calculate the guide quality for a guide star given a guide probe.
        private static Option<AgsGuideQuality> guideQuality(final Option<Tuple2<ObsContext, AgsMagnitude.MagnitudeTable>> ags,
                                                            final GuideProbe guideProbe, final SPTarget guideStar) {
            return ags.flatMap(tup -> {
                if (guideProbe instanceof ValidatableGuideProbe) {
                    final ObsContext ctx = tup._1();
                    final AgsMagnitude.MagnitudeTable magTable = tup._2();
                    final ValidatableGuideProbe vgp = (ValidatableGuideProbe) guideProbe;
                    return AgsRegistrar.instance().currentStrategyForJava(ctx).map(strategy -> {
                        final Option<AgsAnalysis> agsAnalysis = strategy.analyzeForJava(ctx, magTable, vgp,
                                ModelConverters.toSideralTarget(guideStar, ctx.getSchedulingBlockStart()));
                        return agsAnalysis.map(AgsAnalysis::quality);
                    }).getOrElse(None.instance());
                } else {
                    return None.instance();
                }
            });
        }

        // Pre-compute the number of rows as this will be frequently used.
        private int countRows() {
            return rows.stream().reduce(0,
                    (final Integer sz, final Row r) -> sz + r.children().size() + 1,
                    (i1, i2) -> i1 + i2);
        }

        // Gets all the magnitude bands used by targets in the target
        // environment.
        private ImList<MagnitudeBand> getSortedBands(final TargetEnvironment env) {
            // Keep a sorted set of bands, sorted by the name.
            final Set<MagnitudeBand> bands = new TreeSet<>((b1, b2) -> {
                double w1 = b1.center().toNanometers();
                double w2 = b2.center().toNanometers();
                return (int) (w1 - w2);
            });

            // Extract all the magnitude bands from the environment.
            env.getTargets().foreach(spTarget -> bands.addAll(spTarget.getMagnitudeBandsJava()));

            // Create an immutable sorted list containing the results.
            return DefaultImList.create(bands);
        }

        /**
         * Conversions between SPTarget and row index.
         */
        Option<Integer> rowIndexForTarget(final SPTarget target) {
            if (target == null) return None.instance();

            int index = 0;
            for (final Row row : rows) {
                if (row.target().getOrNull() == target) return new Some<>(index);
                ++index;

                for (final Row row2 : row.children()) {
                    if (row2.target().getOrNull() == target) return new Some<>(index);
                    ++index;
                }
            }
            return None.instance();
        }

        Option<SPTarget> targetAtRowIndex(final int index) {
            return rowAtRowIndex(index).flatMap(Row::target);
        }

        /**
         * Conversions between group index (index of group in list of groups) and row index.
         */
        Option<Integer> rowIndexForGroupIndex(final int gpIdx) {
            final ImList<GuideGroup> groups = env.getGroups();
            if (gpIdx < 0 || gpIdx >= groups.size()) return None.instance();

            int index = 0;
            for (final Row row : rows) {
                if (row.group().exists(igg -> igg.index() == gpIdx))
                    return new Some<>(index);
                index += row.children().size() + 1;
            }
            return None.instance();
        }

        Option<IndexedGuideGroup> groupAtRowIndex(final int index) {
            return rowAtRowIndex(index).flatMap(Row::group);
        }

        /**
         * Get the Row object for a given index.
         */
        Option<Row> rowAtRowIndex(final int index) {
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


        private static ImList<String> computeColumnHeaders(final ImList<MagnitudeBand> bands) {
            // First add the fixed column headers
            final List<String> hdr = Arrays.stream(Col.values()).map(Col::displayName).collect(Collectors.toList());

            // Add each magnitude band name
            hdr.addAll(bands.map(MagnitudeBand::name).toList());
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
            return rowAtRowIndex(rowIndex).map(row ->
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
        return tp.getRaDegrees(when).flatMap(ra ->
                tp.getDecDegrees(when).flatMap(dec ->
                        ImOption.apply(Coordinates.fromDegrees(ra, dec).getOrElse(null))
                )
        );
    }

    // Telescope position list
    private ISPObsComponent _obsComp;
    private TargetObsComp _dataObject;
    private TargetEnvironment _env;
    private TelescopePosTableModel _model;

    // if true, ignore selections
    private boolean _ignoreSelection;

    private final EdCompTargetList owner;

    private final TelescopePosTableDropTarget dropTarget;
    private final TelescopePosTableDragSource dragSource;


    /**
     * Default constructor.
     */
    TelescopePosTableWidget(final EdCompTargetList owner) {
        this.owner = owner;
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setModel(new TelescopePosTableModel());

        getSelectionModel().addListSelectionListener(e -> {
            if (_ignoreSelection) return;
            final TelescopePosTableModel telescopePosTableModel = (TelescopePosTableModel) getModel();
            final int idx = getSelectionModel().getMinSelectionIndex();
            telescopePosTableModel.rowAtRowIndex(idx).foreach(this::notifySelect);
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

                _model.rowAtRowIndex(row).foreach(tableDataRow -> {
                    // If we are in column 0, we treat this differently.
                    if (column == 0) {
                        if (tableDataRow.group().isDefined()) {
                            final String name = tableDataRow.name();
                            final String tag  = tableDataRow.tag();
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
            @Override public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() != 2) return;
                updatePrimaryStar();
            }
        });

        // Add drag and drop features
        dropTarget = new TelescopePosTableDropTarget(this);
        dragSource = new TelescopePosTableDragSource(this);
    }

    @Override public void setEnabled(boolean enabled) {
        // Don't disable, the rest of the controls get disabled
        // but this table should always be enabled to let the user select targets
    }

    @Override public void telescopePosUpdate(final WatchablePos tp) {
        final int index = getSelectedRow();
        _resetTable(_env);

        // Restore the selection without firing new selection events.
        final boolean tmp = _ignoreSelection;
        try {
            _ignoreSelection = true;
            _setSelectedRow(index);
        } finally {
            _ignoreSelection = tmp;
        }
    }

    /**
     * Reinitialize the table.
     */
    public void reinit(final TargetObsComp newTOC, final boolean autoGroupChanged) {
        dragSource.setEditable(false);
        dropTarget.setEditable(false);

        // Remove all target listeners for the old target environment.
        if (_env != null) {
            _env.getTargets().foreach(t -> t.deleteWatcher(TelescopePosTableWidget.this));
        }

        // Stop watching for changes on the obsComp
        stopWatchingSelection();
        stopWatchingEnv();

        // If only the auto group has changed, we simply update the table model instead of rebuilding everything.
        if (autoGroupChanged) {
            _setAutoGroup(newTOC.getTargetEnvironment());
            _obsComp = owner.getContextTargetObsComp();
            _dataObject = newTOC;
        } else {
            final TargetObsComp oldTOC = _dataObject;

            // Determine what, if anything, is selected.
            final Option<Integer> selIndex = ImOption.apply(oldTOC).flatMap(toc -> {
                final TargetEnvironment oldEnv = oldTOC.getTargetEnvironment();
                return ImOption.apply(_model).flatMap(td -> {
                    final Option<SPTarget> tpOpt = TargetSelection.getTargetForNode(oldEnv, _obsComp);
                    final Option<Integer> tpIndex = tpOpt.flatMap(_model::rowIndexForTarget);
                    final Option<IndexedGuideGroup> iggOpt = TargetSelection.getIndexedGuideGroupForNode(oldEnv, _obsComp);
                    final Option<Integer> iggIndex = iggOpt.map(IndexedGuideGroup::index).flatMap(_model::rowIndexForGroupIndex);
                    return tpIndex.orElse(iggIndex);
                });
            });

            _obsComp = owner.getContextTargetObsComp();
            _dataObject = newTOC;

            // Rebuild the entire table from scratch.
            _resetTable(_dataObject.getTargetEnvironment());

            // Set the selection accordingly.
            if (selIndex.isDefined()) {
                selIndex.foreach(this::_setSelectedRow);
            } else {
                selectBasePos();
            }
        }

        // Now we can restart watching the changes as the env has been set and the selection made.
        startWatchingEnv();
        startWatchingSelection();

        _env.getTargets().foreach(t -> t.addWatcher(TelescopePosTableWidget.this));

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

    /**
     * Notify the TargetSelection object when a row in the table has been selected.
     * This can either be a target row, or a group row.
     */
    private void notifySelect(final TelescopePosTableModel.Row row) {
        // Only one of these cases will hold.
        row.target().foreach(target -> {
            stopWatchingSelection();
            try {
                TargetSelection.setTargetForNode(_env, _obsComp, target);
            } finally {
                startWatchingSelection();
            }
        });
        row.group().foreach(igg -> TargetSelection.setGuideGroupByIndex(_env, _obsComp, igg.index()));
    }

    private final PropertyChangeListener selectionListener = evt -> {
        if (_model == null) return;
        TargetSelection.getTargetForNode(_env, _obsComp)
                .flatMap(_model::rowIndexForTarget).foreach(this::_setSelectedRow);
    };

    private final PropertyChangeListener envChangeListener = new PropertyChangeListener() {
        public void propertyChange(final PropertyChangeEvent evt) {
            final TargetEnvironment oldEnv = (TargetEnvironment) evt.getOldValue();
            final TargetEnvironment newEnv = (TargetEnvironment) evt.getNewValue();

            final TargetEnvironmentDiff diff = TargetEnvironmentDiff.all(oldEnv, newEnv);
            final Collection<SPTarget> rmTargets  = diff.getRemovedTargets();
            final Collection<SPTarget> addTargets = diff.getAddedTargets();

            // First update the target listeners according to what was added and removed.
            rmTargets.forEach (t -> t.deleteWatcher(TelescopePosTableWidget.this));
            addTargets.forEach(t -> t.addWatcher(TelescopePosTableWidget.this));

            // Remember what was selected before the reset below.
            final int oldSelIndex               = getSelectedRow();
            final Option<SPTarget> oldSelTarget = _model.rowAtRowIndex(oldSelIndex).flatMap(TelescopePosTableModel.Row::target);

            // Update the table -- setting _tableData ....
            _resetTable(newEnv);

            // Update the selection.
            if (rmTargets.isEmpty() && (addTargets.size() == 1)) {
                // A single position was added, so just select it.
                selectTarget(addTargets.iterator().next());
                return;
            }

            // If obs comp has a selected target, then honor it.
            final Option<SPTarget> newSelectedTarget = TargetSelection.getTargetForNode(_env, _obsComp);
            if (newSelectedTarget.isDefined()) {
                _model.rowIndexForTarget(newSelectedTarget.getValue())
                        .foreach(TelescopePosTableWidget.this::_setSelectedRow);
                return;
            }

            // If a new group was added, select it.
            final Option<IndexedGuideGroup> newGpIdx = TargetSelection.getIndexedGuideGroupForNode(_env, _obsComp);
            if (newGpIdx.isDefined()) {
                final Option<Integer> rowIdx = _model.rowIndexForGroupIndex(newGpIdx.getValue().index());
                if (rowIdx.isDefined()) {
                    _setSelectedRow(rowIdx.getValue());
                    return;
                }
            }

            // Try to select the same target that was selected before, if it is there in the new table.
            if (oldSelTarget.exists(t -> _model.rowIndexForTarget(t).isDefined())) {
                oldSelTarget.foreach(TelescopePosTableWidget.this::selectTarget);
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

    // Just update the auto group and adjust the model accordingly instead of resetting the entire table.
    private void _setAutoGroup(final TargetEnvironment newEnv) {
        _env = newEnv;
        final Option<ObsContext> ctxOpt = ObsContext.create(owner.getContextObservation()).map(c -> c.withTargets(newEnv));
        _model.replaceAutoGroup(ctxOpt, newEnv);
        TableUtil.initColumnSizes(this);
    }

    private void _resetTable(final TargetEnvironment newEnv) {
        _env = newEnv;
        final Option<ObsContext> ctxOpt = ObsContext.create(owner.getContextObservation()).map(c -> c.withTargets(newEnv));
        _model = new TelescopePosTableModel(ctxOpt, newEnv);

        _ignoreSelection = true;
        try {
            setModel(_model);
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

        final TargetEnvironment env = _dataObject.getTargetEnvironment();
        final Option<SPTarget> targetOpt = getSelectedPos();
        final Option<IndexedGuideGroup> iggOpt = targetOpt.flatMap(this::getTargetGroup).orElse(getSelectedGroup());
        final boolean primaryGroupIsSelected = iggOpt.exists(igg -> igg.group().equals(env.getPrimaryGuideGroup()));

        iggOpt.foreach(igg -> {
            final GuideGroup primary = env.getPrimaryGuideGroup();

            // If the auto group is disabled and set to primary, then make it an initial auto group.
            ImOption.apply(env.getGuideEnvironment()).foreach(ge -> {
                final GuideGrp grp = igg.group().grp();
                if (grp instanceof AutomaticGroup.Disabled$) {
                    final TargetEnvironment envNew = env.setGuideEnvironment(ge.setAutomaticGroup(GuideGroup.AutomaticInitial()).setPrimaryIndex(0));
                    _dataObject.setTargetEnvironment(envNew);
                    _model.enableAutoRow(envNew);
                } else if (primary != igg.group() && confirmGroupChange(primary, igg.group())) {
                    _dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.setPrimaryIndex(igg.index())));

                    // If we are switching to an automatic group, we also
                    // possibly need to update the position angle.
                    if (grp instanceof AutomaticGroup.Active) {
                        updatePosAngle(((AutomaticGroup.Active) grp).posAngle());
                    }
                }
            });
        });

        // If we are not the auto group and the update was triggered on a guide star:
        // 1. If the group was originally primary, then toggle the star as primary.
        // 2. If the group wasn't originally primary, then mark the star as primary.
        final boolean autoGroup = targetOpt.flatMap(this::getTargetGroup).exists(igg -> igg.group().isAutomatic());
        if (!autoGroup) {
            targetOpt.foreach(target -> {
                if (primaryGroupIsSelected
                        || iggOpt.exists(igg -> igg.group().getAllContaining(target).forall(gpt -> gpt.getPrimary().forall(pt -> pt != target)))) {
                    PrimaryTargetToggle.instance.toggle(_dataObject, target);
                }
            });
        }
    }

    // Finds the instrument component in the observation that houses the
    // target component being edited and then sets its position angle to the
    // given value.  This is done in response to making the automatic guide
    // group primary.
    private void updatePosAngle(Angle posAngle) {
        ImOption.apply(_obsComp.getContextObservation()).foreach(obs ->
                ImOption.apply(SPTreeUtil.findInstrument(obs)).foreach(oc -> {
                    final SPInstObsComp inst = (SPInstObsComp) oc.getDataObject();
                    final double oldPosAngle = inst.getPosAngleDegrees();
                    final double newPosAngle = posAngle.toDegrees();
                    if (oldPosAngle != newPosAngle) {
                        inst.setPosAngleDegrees(newPosAngle);
                        oc.setDataObject(inst);
                    }
                })
        );
    }

    /**
     * Get the position that is currently selected.
     */
    private Option<SPTarget> getSelectedPos() {
        return TargetSelection.getTargetForNode(_env, _obsComp);
    }

    /**
     * Get the group that is currently selected.
     */
    private Option<IndexedGuideGroup> getSelectedGroup() {
        return TargetSelection.getIndexedGuideGroupForNode(_env, _obsComp);
    }

    /**
     * Get the group that is currently selected or the parent group of the selected node.
     * @param env the target environment to use
     */
    Option<IndexedGuideGroup> getSelectedGroupOrParentGroup(final TargetEnvironment env) {
        final Option<IndexedGuideGroup> groupOpt = getSelectedGroup();
        if (groupOpt.isDefined()) return groupOpt;

        return getSelectedPos()
                .map(target -> env.getGroups().zipWithIndex().find(gg -> gg._1().containsTarget(target)).map(IndexedGuideGroup$.MODULE$::fromReverseTuple))
                .getOrElse(None.instance());
    }

    /**
     * Returns the selected node (currently only single select is allowed).
     */
    Option<TelescopePosTableModel.Row> getSelectedNode() {
        return _model.rowAtRowIndex(getSelectedRow());
    }

    /**
     * Returns true if it is ok to add the given item row to the given parent row.
     * For this to be the case, the item must be movable, the parent must be an editable guide group row.
     */
    private boolean isOkayToAdd(final TelescopePosTableModel.Row item, final TelescopePosTableModel.Row parent) {
        if (item == parent) return false;
        final Option<GuideGroup> groupOpt   = parent.group().map(IndexedGuideGroup::group);
        final Option<SPTarget>   targetOpt  = item.target();
        return item.movable() && (parent instanceof TelescopePosTableModel.GroupRow) && parent.editable()
                && !groupOpt.exists(g -> targetOpt.exists(g::containsTarget));
    }

    /**
     * Returns true if it is ok to move the given row item to the given parent row
     */
    boolean isOkayToMove(TelescopePosTableModel.Row item, TelescopePosTableModel.Row parent) {
        return isOkayToAdd(item, parent);
    }

    /**
     * Moves the given row item to the given parent row.
     * In this case, a guide star to a group.
     */
    public void moveTo(final TelescopePosTableModel.Row item, final TelescopePosTableModel.Row parent) {
        if (item == null) return;

        final Option<IndexedGuideGroup> snkGrpOpt = parent.group();
        final SPTarget target = item.target().getOrNull();
        if (snkGrpOpt.exists(igg -> igg.group().containsTarget(target))) return;

        snkGrpOpt.foreach(snkGrp ->
                getTargetGroup(target).foreach(srcGrp -> {
                    final ImList<GuideProbeTargets> targetList = srcGrp.group().getAllContaining(target);
                    if (targetList.isEmpty()) return;
                    final GuideProbeTargets src = targetList.get(0);

                    final GuideProbe guideprobe = src.getGuider();
                    GuideProbeTargets snk = snkGrp.group().get(guideprobe).getOrNull();
                    if (snk == null) {
                        snk = GuideProbeTargets.create(guideprobe);
                    }

                    final boolean isPrimary = src.getPrimary().getOrNull() == target;
                    final GuideProbeTargets newSrc = src.removeTarget(target);
                    final SPTarget newTarget = target.clone();

                    GuideProbeTargets newSnk = snk.setOptions(snk.getOptions().append(newTarget));
                    if (isPrimary) {
                        newSnk = newSnk.selectPrimary(newTarget);
                    }

                    final GuideEnvironment guideEnv = _env.getGuideEnvironment();
                    final GuideEnvironment newGuideEnv = guideEnv.putGuideProbeTargets(srcGrp.index(), newSrc).putGuideProbeTargets(snkGrp.index(), newSnk);
                    final TargetEnvironment newTargetEnv = _env.setGuideEnvironment(newGuideEnv);

                    _dataObject.setTargetEnvironment(newTargetEnv);
                })
        );
    }

    /**
     * Get the group to which this target belongs, or null.
     */
    private Option<IndexedGuideGroup> getTargetGroup(final SPTarget target) {
        return _env.getGuideEnvironment().getOptions().zipWithIndex().find(gg -> gg._1().containsTarget(target))
                .map(IndexedGuideGroup$.MODULE$::fromReverseTuple);
    }

    /**
     * Updates the TargetSelection's target or group, and selects the relevant row in the table.
     */
    void selectRowAt(final int index) {
        if (_model == null) return;
        final SPTarget target = _model.targetAtRowIndex(index).getOrNull();
        if (target != null) {
            selectTarget(target);
        } else {
            _model.groupAtRowIndex(index).foreach(this::selectGroup);
        }
    }

    /**
     * Select the base position, updating the TargetSelection's target, and set the relevant row in the table.
     */
    private void selectBasePos() {
        selectTarget(_env.getArbitraryTargetFromAsterism());
    }


    /**
     * Updates the TargetSelection's target, and sets the relevant row in the table.
     */
    void selectTarget(final SPTarget tp) {
        TargetSelection.setTargetForNode(_env, _obsComp, tp);
        _model.rowIndexForTarget(tp).foreach(this::_setSelectedRow);
    }

    /**
     * Update the TargetSelection's group, and sets the relevant row in the table.
     */
    void selectGroup(final IndexedGuideGroup igg) {
        TargetSelection.setGuideGroupByIndex(_env, _obsComp, igg.index());
        _model.rowIndexForGroupIndex(igg.index()).foreach(this::_setSelectedRow);
    }

    /**
     * Selects the table row at the given location.
     */
    void setSelectedRow(final Point location) {
        selectRowAt(rowAtPoint(location));
    }

    /**
     * Returns the node at the given location or null if not found.
     */
    public TelescopePosTableModel.Row getNode(final Point location) {
        return _model.rowAtRowIndex(rowAtPoint(location)).getOrNull();
    }

    /**
     * Selects the relevant row in the table.
     */
    private void _setSelectedRow(final int index) {
        if ((index < 0) || (index >= getRowCount())) {
            getSelectionModel().setSelectionInterval(0, 0);
        } else {
            getSelectionModel().setSelectionInterval(index, index);
        }
    }

    void setIgnoreSelection(boolean ignore) {
        _ignoreSelection = ignore;
    }

    public String toString() {
        final String head = getClass().getName() + "[\n";
        String body = "";
        final int numRows = getRowCount();
        final int numCols = getColumnCount();
        for (int row = 0; row < numRows; ++row) {
            for (int col = 0; col < numCols; ++col) {
                body += "\t" + _model.getValueAt(col, row);
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
