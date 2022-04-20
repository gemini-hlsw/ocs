package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.ags.api.AgsAnalysis;
import edu.gemini.ags.api.AgsGuideQuality;
import edu.gemini.ags.api.AgsMagnitude;
import edu.gemini.ags.api.AgsRegistrar;
import edu.gemini.pot.ModelConverters;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.gemini.ghost.GhostAsterism;
import edu.gemini.spModel.gemini.ghost.GhostAsterism$;
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostTarget;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.guide.ValidatableGuideProbe;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPCoordinates;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.*;
import jsky.app.ot.OT;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


final public class TelescopePosTableModel extends AbstractTableModel {

    // The index of the auto group.
    // This depends on the type of the asterism.
    private int autoGroupIdx;

    // The index of the first target in the auto group, if it exists.
    // This depends on the type of the asterism.
    private int firstAutoTargetIdx;

    private void calcAutoIndices(final Asterism a) {
        switch (a.asterismType()) {
            // The base is the target.
            case Single:
                autoGroupIdx = 1;
                break;

            // A base and a target (which can be the same).
            case GhostSingleTarget:
                autoGroupIdx = 2;
                break;

            // A base, a target (which can be the same), and a sky position.
            case GhostDualTarget:
            case GhostTargetPlusSky:
            case GhostSkyPlusTarget:
            case GhostHighResolutionTargetPlusSky:
                autoGroupIdx = 3;
                break;
            default:
                autoGroupIdx = 1;
        }

        firstAutoTargetIdx = autoGroupIdx + 1;
    }

    // Collection of rows, which may include "subrows", such as rows of a guiding group.
    private List<Row> rows;

    // Total number of rows, including subrows. Precalculated as swing uses this value frequently.
    private int numRows;

    private ImList<MagnitudeBand> bands;
    private ImList<String> columnHeaders;
    private TargetEnvironment env;
    private Option<Coordinates> baseCoords;
    private Option<Long> when;

    // A completely empty set of TableData.
    // Assume this represents a Single-target asterism.
    public TelescopePosTableModel() {
        // We don't have an actual asterism at this point, so set defaults for indices.
        autoGroupIdx       = 1;
        firstAutoTargetIdx = 2;

        env                = null;
        bands              = DefaultImList.create();
        columnHeaders      = computeColumnHeaders(bands);
        baseCoords         = None.instance();
        when               = None.instance();
        rows               = new ArrayList<>();
        numRows            = 0;
    }

    // Create a completely fresh TableData for the given env.
    public TelescopePosTableModel(final Option<ObsContext> ctxOpt,
                           final TargetEnvironment env) {
        calcAutoIndices(env.getAsterism());
        this.env           = env;
        bands              = getSortedBands(env);
        columnHeaders      = computeColumnHeaders(bands);

        // The remaining variables will be initialized by createRows.
        baseCoords         = None.instance();
        when               = None.instance();
        rows               = createRows(ctxOpt);
        numRows            = countRows();
    }

    // We have one method per Asterism for the header rows, and then the remaining
    // rows proceed in a standard way.
    private List<Row> createSingleAsterismRows() {
        // The base position is the science target.
        final List<Row> hdr     = new ArrayList<>();
        final Asterism.Single a = (Asterism.Single) env.getAsterism();
        final SPTarget t        = a.t();

        hdr.add(new BaseTargetRow(t, when, true));
        return hdr;
    }

    private Row createGhostBaseRow(final GhostAsterism a) {
        final Row row;

        // In the case of DualTarget, the base position is not a science target
        // but simply a set of coordinates. If this is defined independently of
        // the targets, link the row to it; otherwise, just stuff the interpolated
        // coordinates into an SPCoordinates.
        if (a.asterismType() == AsterismType.GhostDualTarget) {
            if (a.overriddenBase().isDefined()) {
                row = new BaseCoordinatesRow(a.overriddenBase().get(), true, "Base");
            }
            else {
                final Coordinates c = Utils.getCoordinates(a, when).getOrElse(Coordinates.zero());
                row = new BaseCoordinatesRow(new SPCoordinates(c), false, "Base");
            }
        }
        else {
            // The logic here is more complicated: we need to determine if the
            // base position corresponds to a target or is set to a sky
            // position, which means checking if a.base (the override) exists.
            if (a.overriddenBase().isDefined())
                row = new BaseCoordinatesRow(a.overriddenBase().get(), true);
            else {
                // This is mildly annoying because all non-dual-target asterisms
                // have their own individual target() member.
                final GhostTarget gt;
                switch (a.asterismType()) {
                    case GhostSingleTarget:
                        gt = ((GhostAsterism.SingleTarget) a).target();
                        break;
                    case GhostTargetPlusSky:
                        gt = ((GhostAsterism.TargetPlusSky) a).target();
                        break;
                    case GhostSkyPlusTarget:
                        gt = ((GhostAsterism.SkyPlusTarget) a).target();
                        break;
                    case GhostHighResolutionTargetPlusSky:
                        gt = ((GhostAsterism.HighResolutionTargetPlusSky) a).target();
                        break;
                    default:
                        // We should never get here, but placate the compiler.
                        gt = null;
                }
                row = new BaseTargetRow(gt.spTarget(), when, false);
            }
        }

        return row;
    }

    private List<Row> createGhostSingleAsterismRows() {
        // Base position and a possibly different science target.
        final List<Row> hdr                = new ArrayList<>();
        final GhostAsterism.SingleTarget a = (GhostAsterism.SingleTarget) env.getAsterism();
        final GhostTarget gt               = a.target();

        hdr.add(createGhostBaseRow(a));
        hdr.add(new GhostTargetRow(GhostAsterism$.MODULE$.SRIFU1(), gt, baseCoords, when));
        return hdr;
    }

    private List<Row> createGhostDualTargetAsterismRows() {
        // Base position and two science targets.
        final List<Row> hdr              = new ArrayList<>();
        final GhostAsterism.DualTarget a = (GhostAsterism.DualTarget) env.getAsterism();
        final GhostTarget gt1            = a.target1();
        final GhostTarget gt2            = a.target2();

        hdr.add(createGhostBaseRow(a));
        hdr.add(new GhostTargetRow(GhostAsterism$.MODULE$.SRIFU1(), gt1, baseCoords, when));
        hdr.add(new GhostTargetRow(GhostAsterism$.MODULE$.SRIFU2(), gt2, baseCoords, when));
        return hdr;
    }

    private List<Row> createGhostTargetPlusSkyAsterismRows() {
        // Base position, a science target, and a sky target.
        final List<Row> hdr                 = new ArrayList<>();
        final GhostAsterism.TargetPlusSky a = (GhostAsterism.TargetPlusSky) env.getAsterism();
        final GhostTarget gt                = a.target();
        final SPCoordinates c               = a.sky();

        hdr.add(createGhostBaseRow(a));
        hdr.add(new GhostTargetRow(GhostAsterism$.MODULE$.SRIFU1(), gt, baseCoords, when));
        hdr.add(new GhostSkyRow(GhostAsterism$.MODULE$.SRIFU2(), c, baseCoords));
        return hdr;
    }

    private List<Row> createGhostSkyPlusTargetAsterismRows() {
        // Base position, a sky target, and a science target.
        final List<Row> hdr                 = new ArrayList<>();
        final GhostAsterism.SkyPlusTarget a = (GhostAsterism.SkyPlusTarget) env.getAsterism();
        final SPCoordinates c               = a.sky();
        final GhostTarget gt                = a.target();

        hdr.add(createGhostBaseRow(a));
        hdr.add(new GhostSkyRow(GhostAsterism$.MODULE$.SRIFU1(), c, baseCoords));
        hdr.add(new GhostTargetRow(GhostAsterism$.MODULE$.SRIFU2(), gt, baseCoords, when));
        return hdr;
    }

    private List<Row> createGhostHighResolutionTargetPlusSkyAsterismRows() {
        // Base position, a science target, and possibly a sky position.
        final List<Row> hdr                               = new ArrayList<>();
        final GhostAsterism.HighResolutionTargetPlusSky a = (GhostAsterism.HighResolutionTargetPlusSky) env.getAsterism();
        final GhostTarget gt                              = a.target();
        final SPCoordinates c                             = a.sky();

        hdr.add(createGhostBaseRow(a));
        hdr.add(new GhostTargetRow(GhostAsterism$.MODULE$.HRIFU(), gt, baseCoords, when));
        hdr.add(new GhostSkyRow(GhostAsterism$.MODULE$.HRSKY(), c, baseCoords));
        return hdr;
    }

    // Create all rows for the table for the current target environment.
    // This depends on the asterism type and uses the individual methods for each.
    private List<Row> createRows(final Option<ObsContext> ctxOpt) {
        final List<Row> tmpRows = new ArrayList<>();
        when = ctxOpt.flatMap(ObsContext::getSchedulingBlockStart);
        baseCoords = Utils.getBaseCoordinates(env.getAsterism(), when);

        // Add the science positions first.
        final Asterism a = env.getAsterism();
        final List<Row> asterismRows;
        switch (a.asterismType()) {
            case Single:
                asterismRows = createSingleAsterismRows();
                break;
            case GhostSingleTarget:
                asterismRows = createGhostSingleAsterismRows();
                break;
            case GhostDualTarget:
                asterismRows = createGhostDualTargetAsterismRows();
                break;
            case GhostTargetPlusSky:
                asterismRows = createGhostTargetPlusSkyAsterismRows();
                break;
            case GhostSkyPlusTarget:
                asterismRows = createGhostSkyPlusTargetAsterismRows();
                break;
            case GhostHighResolutionTargetPlusSky:
                asterismRows = createGhostHighResolutionTargetPlusSkyAsterismRows();
                break;
            default:
                // This will never happen, but make the compiler happy.
                asterismRows = Collections.emptyList();
        }
        tmpRows.addAll(asterismRows);

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
            final UserTarget t = tup._1();
            final int    index = tup._2() + 1;
            tmpRows.add(new UserTargetRow(index, t, baseCoords, when));
        });

        return tmpRows;
    }

    // Update the auto row of a table.
    // This is called when the auto row should be changed from disabled to initial.
    public void enableAutoRow(final TargetEnvironment env) {
        final GroupRow autoGroupRow = new GroupRow(true, false, 0, env.getGuideEnvironment().automaticGroup(),
                Collections.emptyList());
        rows.set(1, autoGroupRow);
        fireTableRowsUpdated(1, 1);
    }

    // Replace the auto group and return a new TableData.
    // Note that we work under the assumption that there is ALWAYS an auto group, which should be the case,
    // and that it is in row position 1 of the table (directly after the base).
    public void replaceAutoGroup(final Option<ObsContext> ctxOpt, final TargetEnvironment newEnv) {
        final List<Row> oldRows = rows;

        // Recalculate the rows to see if quality has changed based on the new context.
        env = newEnv;
        final Set<Integer> changedRowIndices = recalculateManualGSQuality(ctxOpt);

        // The auto group is in group position 0 and row position autoGroupIdx.
        // Recreate it and set it in the new rows.
        final GuideGroup autoGroup  = newEnv.getGuideEnvironment().getOptions().head();
        final GroupRow autoGroupRow = createGroupRow(ctxOpt, 0, autoGroup);
        rows.set(autoGroupIdx, autoGroupRow);
        numRows = countRows();

        // Determine the size difference between the old auto group and the new.
        final int oldAutoGroupSize  = ((GroupRow)oldRows.get(autoGroupIdx)).children().size();
        final int newAutoGroupSize  = autoGroupRow.children().size();
        final int autoGroupSizeDiff = newAutoGroupSize - oldAutoGroupSize;
        final int autoGroupSizeInt  = Math.min(oldAutoGroupSize, newAutoGroupSize);

        // Fire events indicating the changes in the model.
        if (autoGroupSizeInt > 0) {
            fireTableRowsUpdated(firstAutoTargetIdx, firstAutoTargetIdx + autoGroupSizeInt - 1);
        }
        if (oldAutoGroupSize < newAutoGroupSize) {
            fireTableRowsInserted(firstAutoTargetIdx + oldAutoGroupSize, firstAutoTargetIdx + newAutoGroupSize - 1);
        } else if (newAutoGroupSize < oldAutoGroupSize) {
            fireTableRowsDeleted(firstAutoTargetIdx + newAutoGroupSize, firstAutoTargetIdx + oldAutoGroupSize - 1);
        }

        // Now for each group that was changed, fire update events.
        int tableRowIdx = 0;
        final Stream<Tuple2<Integer,Row>> indexedRows = IntStream.range(0, rows.size()).mapToObj(i -> new Pair<>(i, rows.get(i)));
        for (final Tuple2<Integer, Row> tup2 : indexedRows.collect(Collectors.toList())) {
            final int rowIdx    = tup2._1();
            final Row row       = tup2._2();

            final int children;
            if (row instanceof GroupRow)
                children = ((GroupRow) row).children().size();
            else
                children = 0;

            if (changedRowIndices.contains(rowIdx)) {
                // We only care about the child rows (hence + 1), and all rows have been offset by autoGroupSizeDiff.
                final int lowerIdx = tableRowIdx + 1 + autoGroupSizeDiff;
                final int upperIdx = lowerIdx + children - 1;
                fireTableRowsUpdated(lowerIdx, upperIdx);
            }

            tableRowIdx += 1 + children;
        }
    }

    // Given a guide group and its index amongst the set of all groups, create a GroupRow (and all children
    // target subrows) representing it.
    private GroupRow createGroupRow(final Option<ObsContext> ctxOpt, final int groupIdx, final GuideGroup group) {
        final Asterism a                     = env.getAsterism();
        final Option<Long> when              = ctxOpt.flatMap(ObsContext::getSchedulingBlockStart);
        final Option<Coordinates> baseCoords = Utils.getBaseCoordinates(a, when);

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
            // TODO: Is 1 still right here? I think so, but if anything crashes, this might be a good place to start looking.
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
                final GroupRow gRow         = (GroupRow) row;
                final IndexedGuideGroup igg = gRow.indexedGuideGroup();
                final int gIdx              = igg.index();
                final GuideGroup gg         = igg.group();

                // Recreate the entire group row, and then scan the children to see if any of the qualities are different.
                final GroupRow gRowNew = createGroupRow(ctxOpt, gIdx, gg);

                // Zipping Java lists is too obnoxious.
                final ImList<Row> childRowsOld       = DefaultImList.create(gRow.children());
                final ImList<Row> childRowsNew       = DefaultImList.create(gRowNew.children());
                final boolean groupChanged           = childRowsOld.zip(childRowsNew).exists(tup2 -> {
                    final GuideTargetRow gtrOld = (GuideTargetRow) tup2._1();
                    final GuideTargetRow gtrNew = (GuideTargetRow) tup2._2();
                    return !gtrOld.quality().exists(q -> gtrNew.quality().exists(q::equals));
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

                final Option<AgsAnalysis> agsAnalysis =
                  AgsRegistrar
                      .currentStrategyForJava(ctx)
                      .flatMap(s ->
                          s.analyzeForJava(
                              ctx,
                              magTable,
                              vgp,
                              ModelConverters.toSideralTarget(guideStar, ctx.getSchedulingBlockStart())
                          )
                      );

                return agsAnalysis.map(AgsAnalysis::quality);

            } else {
                return None.instance();
            }
        });
    }

    // Pre-compute the number of rows as this will be frequently used.
    private int countRows() {
        return rows.stream().reduce(0,
                (final Integer sz, final Row r) -> {
                    if (r instanceof GroupRow)
                        return sz + ((GroupRow) r).children().size() + 1;
                    else
                        return sz + 1;
                },
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
     * Conversions between SPTarget and row index. Given a target, find its index.
     * Note that we want to compare REFERENCES here: this is not an error.
     */
    public Option<Integer> rowIndexForTarget(final SPTarget target) {
        if (target == null) return None.instance();
        int index = 0;
        for (final Row row : rows) {
            // Special case: skip the base row if it is disabled. This will mean
            // that the base is dependent on another target, and we want to pick
            // that target.
            if ((index != 0 || row.enabled()) &&
                    row instanceof TargetRow && ((TargetRow) row).target() == target) {
                return new Some<>(index);
            }
            ++index;

            if (row instanceof GroupRow) {
                final GroupRow gRow = (GroupRow) row;
                for (final Row row2 : gRow.children()) {
                    if (row2 instanceof TargetRow && ((TargetRow) row2).target() == target) {
                        return new Some<>(index);
                    }
                    ++index;
                }
            }
        }
        return None.instance();
    }

    public Option<SPTarget> targetAtRowIndex(final int index) {
        return rowAtRowIndex(index).flatMap(r -> {
            if (r instanceof TargetRow)
                return new Some<>(((TargetRow) r).target());
            else
                return None.instance();
        });
    }

    /**
     * Conversions between Coordinates and row index.
     */
    public Option<Integer> rowIndexForCoordinates(final SPCoordinates coords) {
        if (coords == null) return None.instance();
        int index = 0;
        for (final Row row : rows) {
            if (row instanceof CoordinatesRow && ((CoordinatesRow) row).coordinates() == coords)
                return new Some<>(index);
            ++index;

            if (row instanceof GroupRow)
                index += ((GroupRow) row).children().size();
        }
        return None.instance();
    }

    public Option<SPCoordinates> coordinatesAtRowIndex(final int index) {
        return rowAtRowIndex(index).flatMap(r -> {
            if (r instanceof CoordinatesRow)
                return new Some<>(((CoordinatesRow) r).coordinates());
            else
                return None.instance();
        });
    }

    /**
     * Conversions between group index (index of group in list of groups) and row index.
     */
    public Option<Integer> rowIndexForGroupIndex(final int gpIdx) {
        final ImList<GuideGroup> groups = env.getGroups();
        if (gpIdx < 0 || gpIdx >= groups.size()) return None.instance();

        int index = 0;
        for (final Row row : rows) {
            // We are only interested in group rows.
            if (row instanceof GroupRow) {
                final GroupRow gRow = (GroupRow) row;
                if (gRow.indexedGuideGroup().index() == gpIdx)
                    return new Some<>(index);
                index += gRow.children().size() + 1;
            } else
                ++index;
        }
        return None.instance();
    }

    public Option<IndexedGuideGroup> groupAtRowIndex(final int index) {
        return rowAtRowIndex(index).flatMap(r -> {
                if (r instanceof GroupRow)
                    return new Some<>(((GroupRow) r).indexedGuideGroup());
                else
                    return None.instance();
        });
    }

    /**
     * Get the Row object for a given index.
     * We iterate over rows and the contents of GroupRows until we get to
     * the index of the row that we want.
     */
    public Option<Row> rowAtRowIndex(final int index) {
        if (index >= 0) {
            int i = 0;
            for (final Row row : rows) {
                if (i == index)
                    return new Some<>(row);
                ++i;

                if (row instanceof GroupRow) {
                    final GroupRow gRow = (GroupRow) row;
                    final List<Row> children = gRow.children();
                    final int cindex = index - i;
                    if (cindex < children.size()) {
                        return new Some<>(children.get(cindex));
                    } else {
                        i += children.size();
                    }
                }
            }
        }
        return None.instance();
    }


    private static ImList<String> computeColumnHeaders(final ImList<MagnitudeBand> bands) {
        // First add the fixed column headers
        final List<String> hdr = Arrays.stream(Column.values()).map(Column::displayName).collect(Collectors.toList());

        // Add each magnitude band name
        hdr.addAll(bands.map(MagnitudeBand::name).toList());
        return DefaultImList.create(hdr);
    }

    @Override public int getColumnCount() {
        return Column.values().length + bands.size();
    }

    @Override public String getColumnName(final int index) {
        return columnHeaders.get(index);
    }

    @Override public Object getValueAt(final int rowIndex, final int columnIndex) {
        if (rowIndex < 0 || rowIndex >= numRows)
            return null;

        final Column[] columns = Column.values();
        return rowAtRowIndex(rowIndex).map(row -> {
            // Is this a non-mag column?
            if (columnIndex < columns.length)
                return columns[columnIndex].getValue(row);

            // Is this a target row? If so, it has magnitudes; otherwise, it doesn't,
            // so return the empty string.
            else if (row instanceof TargetRow)
                return ((TargetRow) row).formatMagnitude(bands.get(columnIndex - columns.length));
            else
                return "";
        }).getOrNull();
    }

    @Override public int getRowCount() {
        return numRows;
    }
}
