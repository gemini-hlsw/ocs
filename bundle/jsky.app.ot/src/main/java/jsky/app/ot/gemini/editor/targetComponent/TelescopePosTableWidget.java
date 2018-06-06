package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.gemini.ghost.GhostAsterism;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPCoordinates;
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

import jsky.app.ot.OTOptions;
import jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel.*;
import jsky.util.gui.TableUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

/**
 * An extension of the TableWidget to support telescope target lists.
 */
final class TelescopePosTableWidget extends JTable implements TelescopePosWatcher {

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
            System.out.println("TelescopePosTableWidget.selectionChanged!");
            if (_ignoreSelection) System.out.println("\tIgnored: idx=" + getSelectionModel().getMinSelectionIndex());
            if (_ignoreSelection) return;
            final TelescopePosTableModel telescopePosTableModel = (TelescopePosTableModel) getModel();
            final int idx = getSelectionModel().getMinSelectionIndex();
            System.out.println("\tRow at idx " + idx +  " " + telescopePosTableModel.rowAtRowIndex(idx));
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
                        if (tableDataRow instanceof GroupRow) {
                            final String name = tableDataRow.name();
                            final String tag  = tableDataRow.tag();
                            label.setText(name != null && !name.equals("") ? name : tag);
                        } else {
                            label.setText(tableDataRow.tag());
                        }

                        label.setIcon(tableDataRow.icon());
                        label.setDisabledIcon(tableDataRow.icon());
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
            _env.getCoordinates().foreach(c -> c.deleteWatcher(TelescopePosTableWidget.this));
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
            System.out.println("TelescopePosTableWidget.reinit lookup");
            final Option<Integer> selIndex = ImOption.apply(oldTOC).flatMap(toc -> {
                final TargetEnvironment oldEnv = oldTOC.getTargetEnvironment();
                return ImOption.apply(_model).flatMap(td -> {
                    final Option<SPTarget> tpOpt = TargetSelection.getTargetForNode(oldEnv, _obsComp);
                    final Option<Integer> tpIndex = tpOpt.flatMap(_model::rowIndexForTarget);
                    final Option<SPCoordinates> cOpt = TargetSelection.getCoordinatesForNode(oldEnv, _obsComp);
                    final Option<Integer> cIndex = cOpt.flatMap(_model::rowIndexForCoordinates);
                    final Option<IndexedGuideGroup> iggOpt = TargetSelection.getIndexedGuideGroupForNode(oldEnv, _obsComp);
                    final Option<Integer> iggIndex = iggOpt.map(IndexedGuideGroup::index).flatMap(_model::rowIndexForGroupIndex);
                    return tpIndex.orElse(cIndex).orElse(iggIndex);
                });
            });
            System.out.println("TelescopePosTableWidget.reinit lookup results: selIndex=" + selIndex + ", val=" + selIndex.getOrNull());
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
        _env.getCoordinates().foreach(c -> c.addWatcher(TelescopePosTableWidget.this));

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
    private void notifySelect(final Row row) {
        if (row instanceof TargetRow) {
            final TargetRow tRow = (TargetRow) row;
            stopWatchingSelection();
            try {
                TargetSelection.setTargetForNode(_env, _obsComp, tRow.target());
            } finally {
                startWatchingSelection();
            }
        } else if (row instanceof CoordinatesRow) {
            final CoordinatesRow cRow = (CoordinatesRow) row;
            TargetSelection.setCoordinatesForNode(_env, _obsComp, cRow.coordinates());
        } else if (row instanceof GroupRow) {
            final GroupRow gRow = (GroupRow) row;
            TargetSelection.setGuideGroupByIndex(_env, _obsComp, gRow.indexedGuideGroup().index());
        }
    }

    private final PropertyChangeListener selectionListener = evt -> {
        if (_model == null) return;
        System.out.println("TelescopePosTableWidget.selectionListener, old=" + evt.getOldValue() + " new=" + evt.getNewValue());
        TargetSelection.getTargetForNode(_env, _obsComp)
                .flatMap(_model::rowIndexForTarget).foreach(this::_setSelectedRow);
        TargetSelection.getCoordinatesForNode(_env, _obsComp)
                .flatMap(_model::rowIndexForCoordinates).foreach(this::_setSelectedRow);
    };

    private final PropertyChangeListener envChangeListener = new PropertyChangeListener() {
        public void propertyChange(final PropertyChangeEvent evt) {
            final TargetEnvironment oldEnv = (TargetEnvironment) evt.getOldValue();
            final TargetEnvironment newEnv = (TargetEnvironment) evt.getNewValue();

            final TargetEnvironmentDiff diff = TargetEnvironmentDiff.all(oldEnv, newEnv);
            final Collection<SPTarget> rmTargets  = diff.getRemovedTargets();
            final Collection<SPTarget> addTargets = diff.getAddedTargets();
            final Collection<SPCoordinates> rmCoords  = diff.getRemovedCoordinates();
            final Collection<SPCoordinates> addCoords = diff.getAddedCoordinates();

            // First update the target listeners according to what was added and removed.
            rmTargets.forEach (t -> t.deleteWatcher(TelescopePosTableWidget.this));
            addTargets.forEach(t -> t.addWatcher(TelescopePosTableWidget.this));
            rmCoords.forEach(t -> t.deleteWatcher(TelescopePosTableWidget.this));
            addCoords.forEach(t -> t.addWatcher(TelescopePosTableWidget.this));

            // Remember what was selected before the reset below.
            final int oldSelIndex               = getSelectedRow();
            final Option<SPTarget> oldSelTarget = _model.rowAtRowIndex(oldSelIndex).flatMap(r -> {
                if (r instanceof TargetRow)
                    return new Some<>(((TargetRow) r).target());
                else
                    return None.instance();
            });
            final Option<SPCoordinates> oldSelCoords = _model.rowAtRowIndex(oldSelIndex).flatMap(r -> {
                if (r instanceof CoordinatesRow)
                    return new Some<>(((CoordinatesRow) r).coordinates());
                else
                    return None.instance();
            });

            // Update the table -- setting _tableData ....
            _resetTable(newEnv);

            // Update the selection.
            if (rmTargets.isEmpty() && (addTargets.size() == 1)) {
                // A single position was added, so just select it.
                selectTarget(addTargets.iterator().next());
                return;
            }

            // If obs comp has a selected target, then honor it.
            System.out.println("TelescopePosTableWidget.envChangeListener");
            final Option<SPTarget> newSelectedTarget = TargetSelection.getTargetForNode(_env, _obsComp);
            if (newSelectedTarget.isDefined()) {
                _model.rowIndexForTarget(newSelectedTarget.getValue())
                        .foreach(TelescopePosTableWidget.this::_setSelectedRow);
                return;
            }

            // If the obs comp has selected coordinates, then honor them.
            final Option<SPCoordinates> newSelectedCoords = TargetSelection.getCoordinatesForNode(_env, _obsComp);
            if (newSelectedCoords.isDefined()) {
                _model.rowIndexForCoordinates(newSelectedCoords.getValue())
                        .foreach(TelescopePosTableWidget.this::_setSelectedRow);
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
            System.out.println("TelescopePosTableWidget.envChangeListener2");
            if (oldSelTarget.exists(t -> _model.rowIndexForTarget(t).isDefined())) {
                oldSelTarget.foreach(TelescopePosTableWidget.this::selectTarget);
                return;
            }

            // Otherwise, try to select the same coordinates that was selected before.
            if (oldSelCoords.exists(c -> _model.rowIndexForCoordinates(c).isDefined())) {
                oldSelCoords.foreach(TelescopePosTableWidget.this::selectCoordinates);
                return;
            }

            // Okay, the old selected target or group was removed.  Try to select the
            // target, group, or coordinates at the same position as the old selection.
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
    Option<Row> getSelectedNode() {
        return _model.rowAtRowIndex(getSelectedRow());
    }

    /**
     * Returns true if it is ok to add the given item row to the given parent row.
     * For this to be the case:
     * 1. The parent must be an editable group row.
     * 2. The item must be a target row that can be moved and is not already part
     *    of the parent group.
     */
    private boolean isOkayToAdd(final Row item, final Row parent) {
        if (item == parent) return false;
        if (!(parent instanceof GroupRow)) return false;
        if (!(item instanceof TargetRow))  return false;

        final GuideGroup group = ((GroupRow) parent).indexedGuideGroup().group();
        final SPTarget   t     = ((TargetRow) item).target();
        return item.movable() && parent.editable() && !group.containsTarget(t);
    }

    /**
     * Returns true if it is ok to move the given row item to the given parent row
     */
    boolean isOkayToMove(Row item, Row parent) {
        return isOkayToAdd(item, parent);
    }

    /**
     * Moves the given row item to the given parent row.
     * In this case, a guide star to a group.
     * We cannot move coordinates, so don't worry about this.
     */
    public void moveTo(final Row item, final Row parent) {
        if (item == null) return;
        if (!(parent instanceof GroupRow)) return;
        if (!(item instanceof TargetRow)) return;

        final GroupRow gRow  = (GroupRow) parent;
        final TargetRow tRow = (TargetRow) item;

        final IndexedGuideGroup snkGrp = gRow.indexedGuideGroup();
        final SPTarget target          = tRow.target();
        if (snkGrp.group().containsTarget(target)) return;

        getTargetGroup(target).foreach(srcGrp -> {
            final ImList<GuideProbeTargets> targetList = srcGrp.group().getAllContaining(target);
            if (targetList.isEmpty()) return;
            final GuideProbeTargets src = targetList.get(0);

            final GuideProbe guideprobe = src.getGuider();
            GuideProbeTargets snk = snkGrp.group().get(guideprobe).getOrNull();
            if (snk == null)
                snk = GuideProbeTargets.create(guideprobe);

            final boolean isPrimary = src.getPrimary().getOrNull() == target;
            final GuideProbeTargets newSrc = src.removeTarget(target);
            final SPTarget newTarget = target.clone();

            GuideProbeTargets newSnk = snk.setOptions(snk.getOptions().append(newTarget));
            if (isPrimary)
                newSnk = newSnk.selectPrimary(newTarget);

            final GuideEnvironment guideEnv = _env.getGuideEnvironment();
            final GuideEnvironment newGuideEnv = guideEnv.putGuideProbeTargets(srcGrp.index(), newSrc).putGuideProbeTargets(snkGrp.index(), newSnk);
            final TargetEnvironment newTargetEnv = _env.setGuideEnvironment(newGuideEnv);

            _dataObject.setTargetEnvironment(newTargetEnv);
        });
    }

    /**
     * Get the group to which this target belongs, or null.
     */
    private Option<IndexedGuideGroup> getTargetGroup(final SPTarget target) {
        return _env.getGuideEnvironment().getOptions().zipWithIndex().find(gg -> gg._1().containsTarget(target))
                .map(IndexedGuideGroup$.MODULE$::fromReverseTuple);
    }

    /**
     * Updates the TargetSelection's target, group, or coordinates, and selects the relevant row in the table.
     */
    void selectRowAt(final int index) {
        if (_model == null) return;
        _model.targetAtRowIndex(index).foreach(this::selectTarget);
        _model.coordinatesAtRowIndex(index).foreach(this::selectCoordinates);
        _model.groupAtRowIndex(index).foreach(this::selectGroup);
    }

    /**
     * Select the base position, updating the TargetSelection's target, and set the relevant row in the table.
     */
    private void selectBasePos() {
        selectRowAt(0);
    }


    /**
     * Updates the TargetSelection's target, and sets the relevant row in the table.
     */
    void selectTarget(final SPTarget tp) {
        System.out.println("TelescopePosTableWidget.selectTarget");
        TargetSelection.setTargetForNode(_env, _obsComp, tp);
        _model.rowIndexForTarget(tp).foreach(this::_setSelectedRow);
    }

    /**
     * Updates the TargetSelection's coordinates, and sets the relevant row in the table.
     */
    void selectCoordinates(final SPCoordinates coords) {
        TargetSelection.setCoordinatesForNode(_env, _obsComp, coords);
        _model.rowIndexForCoordinates(coords).foreach(this::_setSelectedRow);
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
    public Row getNode(final Point location) {
        return _model.rowAtRowIndex(rowAtPoint(location)).getOrNull();
    }

    /**
     * Selects the relevant row in the table.
     */
    private void _setSelectedRow(final int index) {
        System.out.println("TelescopePosTableWidget:_setSelectedRow: " + index);
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
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("[\n");
        final int numRows = getRowCount();
        final int numCols = getColumnCount();
        for (int row = 0; row < numRows; ++row) {
            for (int col = 0; col < numCols; ++col) {
                sb.append("\t").append(_model.getValueAt(col, row));
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
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
