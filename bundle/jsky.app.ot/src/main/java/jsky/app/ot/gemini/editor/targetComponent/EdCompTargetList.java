package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.OTOptions;
import jsky.app.ot.ags.*;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.tpe.AgsClient;
import jsky.app.ot.tpe.GuideStarSupport;
import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.tpe.TpeManager;
import jsky.app.ot.util.Resources;
import jsky.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

/**
 * This is the editor for the target list component. It is terrible.
 */
public final class EdCompTargetList extends OtItemEditor<ISPObsComponent, TargetObsComp> {

    // Global variables \o/
    private static TargetClipboard clipboard;

    // Instance constants
    private final AgsContextPublisher _agsPub = new AgsContextPublisher();
    private final TelescopeForm _w;

    // Stuff that varies with time
    private SPTarget        _curPos;
    private Tuple2<Integer, GuideGroup> _curGroup;

    public EdCompTargetList() {

        _w = new TelescopeForm(this);

        _w.addComponentListener(new ComponentAdapter() {
            public void componentShown(final ComponentEvent componentEvent) {
                toggleAgsGuiElements();
            }
        });

        _w.removeButton   .addActionListener(removeListener);
        _w.copyButton     .addActionListener(copyListener);
        _w.pasteButton    .addActionListener(pasteListener);
        _w.duplicateButton.addActionListener(duplicateListener);
        _w.primaryButton  .addActionListener(primaryListener);

        _w.guidingControls.autoGuideStarButton().peer().addActionListener(autoGuideStarListener);
        _w.guidingControls.manualGuideStarButton().peer().addActionListener(manualGuideStarListener);

        _w.guidingControls.autoGuideStarGuiderSelector().addSelectionListener(strategy ->
                AgsStrategyUtil.setSelection(getContextObservation(), strategy)
        );

        _w.guideGroupName.addWatcher(new TextBoxWidgetWatcher() {
            @Override public void textBoxKeyPress(final TextBoxWidget tbwe) {
                SwingUtilities.invokeLater(() -> {
                    final String name           = _w.guideGroupName.getText();
                    final GuideGroup newGroup   = _curGroup._2().setName(name);
                    final TargetEnvironment env = getDataObject().getTargetEnvironment();
                    _curGroup                   = new Pair<>(_curGroup._1(), newGroup);
                    getDataObject().setTargetEnvironment(env.setGroup(_curGroup._1(), newGroup));
                    _w.guideGroupName.requestFocus(); // otherwise focus is lost during event handling
                });
            }
        });

        _w.positionTable.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(final KeyEvent event) {
                switch (event.getKeyCode()) {
                    // Make the delete and backspace buttons delete selected positions.
                    case KeyEvent.VK_DELETE:
                    case KeyEvent.VK_BACK_SPACE:
                        _w.removeButton.doClick();
                        break;
                }
            }
        });

        _agsPub.subscribe((obs, oldOptions, newOptions) -> updateGuiding());
    }

    @Override protected void updateEnabledState(final boolean enabled) {
        if (enabled != isEnabled()) {
            setEnabled(enabled);
            updateEnabledState(getWindow().getComponents(), enabled);

            // Update enabled state for all detail widgets.  The current detail
            // editor will have already been updated by the call to
            // updateEnabledState above which updates the hierarchy of widgets
            // rooted in the JPanel containing the target component.  Since the
            // other detail editors are swapped in when the target type changes,
            // update them explicitly so they behave as if they were contained
            // in the panel.
            updateDetailEditorEnabledState(enabled);
        }

        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        _w.tag.setEnabled(enabled && env.isBasePosition(_curPos));

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.setEnabled(enabled && inst != null);
    }

    protected void updateDetailEditorEnabledState(final boolean enabled) {
        // Update enabled state for all detail widgets.
        _w.detailEditor.allEditorsJava().stream().forEach(ed -> updateEnabledState(new Component[]{ed}, enabled));
    }

    /**
     * Update the remove and primary buttons as well as the detail editor.
     */
    private void updateRemovePrimaryButtonsAndDetailEditor() {
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        final boolean curNotBase = !env.isBasePosition(_curPos);

        // We assume that if no group is set, then we are or want to handle the same as the auto group.
        final boolean isAutoGroup = ImOption.apply(_curGroup).exists(cgtup -> cgtup._2().isAutomatic());
        final boolean isAutoTarget = ImOption.apply(_curPos).exists(p -> env.getGuideEnvironment().getOptions()
                .exists(gg -> gg.isAutomatic() && gg.containsTarget(p)));
        final boolean isAuto = isAutoGroup || isAutoTarget;

        _w.removeButton.setEnabled(curNotBase && editable && !isAuto);
        _w.primaryButton.setEnabled(enablePrimary(_curPos, env) && editable);
        updateDetailEditorEnabledState(editable && !isAuto);
    }

    // OtItemEditor
    @Override public JPanel getWindow() {
        return _w;
    }

    // Common code to manage the position watcher on the current position around an action that modifies it.
    // We also check for the presence of a specified target in the target environment.
    private void manageCurPosIfEnvContainsTarget(final SPTarget target, final Runnable action) {
        final TargetObsComp obsComp = getDataObject();
        if (obsComp == null) return;

        final TargetEnvironment env = obsComp.getTargetEnvironment();
        if (env == null || !env.getTargets().contains(target)) return;

        if (_curPos != null)
            _curPos.deleteWatcher(posWatcher);

        action.run();

        if (_curPos != null) {
            _curPos.addWatcher(posWatcher);
            refreshAll();
            updateRemovePrimaryButtonsAndDetailEditor();
        }
    }

    // OtItemEditor
    @Override public void init() {
        final ISPObsComponent node = getContextTargetObsComp();
        TargetSelection.listenTo(node, selectionListener);

        final TargetObsComp obsComp = getDataObject();
        obsComp.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        obsComp.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);

        final TargetEnvironment env = obsComp.getTargetEnvironment();
        final SPTarget selTarget = TargetSelection.get(env, node);
        manageCurPosIfEnvContainsTarget(selTarget, () -> _curPos = selTarget);

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.removeAll();
        if (inst == null) {
            _w.newMenu.setEnabled(false);
        } else {
            _w.newMenu.setEnabled(true);
            if (inst.hasGuideProbes()) {
                final List<GuideProbe> guiders = new ArrayList<>(GuideProbeUtil.instance.getAvailableGuiders(getContextObservation()));
                Collections.sort(guiders, GuideProbe.KeyComparator.instance);
                guiders.forEach(probe ->
                    _w.newMenu.add(new JMenuItem(probe.getKey()) {{
                        addActionListener(new AddGuideStarAction(obsComp, probe, _w.positionTable));
                    }}));
            }

            _w.newMenu.add(new JMenuItem("User") {{
                addActionListener(new AddUserTargetAction(obsComp));
            }});

            if (inst.hasGuideProbes()) {
                _w.newMenu.addSeparator();
                _w.newMenu.add(new JMenuItem("Guide Group") {{
                    addActionListener(new AddGroupAction(obsComp, _w.positionTable));
                }});
            }
        }
        _w.positionTable.reinit(obsComp);
        _w.guidingControls.manualGuideStarButton().peer().setVisible(GuideStarSupport.supportsManualGuideStarSelection(getNode()));
        updateGuiding();
        _agsPub.watch(ImOption.apply(getContextObservation()));
    }

    // OtItemEditor
    @Override protected void cleanup() {
        _agsPub.unwatch();
        TargetSelection.deafTo(getContextTargetObsComp(), selectionListener);
        getDataObject().removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        getDataObject().removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);
        super.cleanup();
    }

    private void toggleAgsGuiElements() {
        final boolean supports = GuideStarSupport.supportsAutoGuideStarSelection(getNode());
        _w.guidingControls.supportsAgs_$eq(supports); // hide the ags related buttons
    }

    private Option<ObsContext> getObsContext(final TargetEnvironment env) {
        return ObsContext.create(getContextObservation()).map(obsContext -> obsContext.withTargets(env));
    }

    private void updateGuiding() {
        updateGuiding(getDataObject().getTargetEnvironment());
    }

    private void updateGuiding(final TargetEnvironment env) {
        toggleAgsGuiElements();
        final Option<ObsContext> ctx = getObsContext(env);
        _w.guidingControls.update(ctx);

        // Update the guiding feedback.
        final ISPObsComponent node = getContextTargetObsComp();
        final SPTarget      target = TargetSelection.get(env, node);
        _w.detailEditor.gfe().edit(ctx, target, node);
    }


    private static boolean enablePrimary(final SPTarget target, final TargetEnvironment env) {
        return env.getBase() != target && !env.getUserTargets().contains(target);
    }

    private void refreshAll() {
        _w.guideGroupPanel.setVisible(false);
        _w.detailEditor.setVisible(true);

        // Get all the legally available guiders in the current context.
        final Set<GuideProbe> avail = GuideProbeUtil.instance.getAvailableGuiders(getContextObservation());
        final Set<GuideProbe> guiders = new HashSet<>(avail);
        final TargetEnvironment env = getDataObject().getTargetEnvironment();

        // Get the set of guiders that are referenced but not legal in this context, if any.  Any
        // "available" guider is legal, anything left over is referenced but not really available.
        final Set<GuideProbe> illegalSet = env.getOrCreatePrimaryGuideGroup().getReferencedGuiders();
        illegalSet.removeAll(avail);

        // Determine whether the current position is one of these illegal guiders.  If so, we add
        // the guide probe to the list of choices so that this target may be selected in order to
        // change its type or delete it.
        GuideProbe illegal = null;
        for (final GuideProbe guider : illegalSet) {
            final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            if (gtOpt.getValue().getTargets().contains(_curPos)) {
                illegal = guider;
                guiders.add(guider);
            }
        }

        // Sort the list of guiders.
        final List<GuideProbe> guidersList = new ArrayList<>(guiders);
        Collections.sort(guidersList, GuideProbe.KeyComparator.instance);

        // Make a list of PositionTypes that are legal in the current observation context.
        int index = 0;
        final PositionType[] ptA = new PositionType[2 + guiders.size()];
        ptA[index++] = BasePositionType.instance;
        for (GuideProbe guider : guidersList) {
            ptA[index++] = new GuidePositionType(guider, guider != illegal);
        }
        ptA[index] = UserPositionType.instance;

        _w.tag.removeActionListener(_tagListener);
        _w.tag.setModel(new DefaultComboBoxModel<>(ptA));
        _w.tag.setEnabled(isEnabled() && (env.getBase() != _curPos));
        _w.tag.addActionListener(_tagListener);

        _w.tag.setRenderer(tagRenderer);
        showTargetTag();

        // Update target details
        _w.detailEditor.edit(getObsContext(env), _curPos, getNode());
    }

    private void showTargetTag() {
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        for (int i = 0; i < _w.tag.getItemCount(); ++i) {
            final PositionType pt = _w.tag.getItemAt(i);
            if (pt.isMember(env, _curPos)) {
                _w.tag.removeActionListener(_tagListener);
                _w.tag.setSelectedIndex(i);
                _w.tag.addActionListener(_tagListener);
                break;
            }
        }
    }


    /**
     * Listeners and watchers.
     */

    /**
     * Action that handles adding a new guide star when a probe is picked from the add menu.
     * This should ONLY be permitted if a current non-auto group is selected.
     */
    private class AddGuideStarAction implements ActionListener {
        private final TargetObsComp obsComp;
        private final GuideProbe probe;
        private final TelescopePosTableWidget positionTable;

        AddGuideStarAction(final TargetObsComp obsComp, final GuideProbe probe, final TelescopePosTableWidget positionTable) {
            this.obsComp = obsComp;
            this.probe = probe;
            this.positionTable = positionTable;
        }

        @Override public void actionPerformed(final ActionEvent actionEvent) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            GuideEnvironment ge = env.getGuideEnvironment();

            // Make sure that a group is selected and that it is not the auto grou.
            final Option<Tuple2<Integer, GuideGroup>> guideGroup = positionTable.getSelectedGroupOrParentGroup(env);
            if (!guideGroup.exists(tup -> !tup._2().isAutomatic()))
                return;

            Option<GuideProbeTargets> opt = guideGroup.flatMap(tup -> tup._2().get(probe));
            //if (opt == null) opt = env.getPrimaryGuideProbeTargets(probe);

            // TODO: This is not returning the group index, but the table row index?
            final Integer groupIndex = guideGroup.map(Tuple2::_1).getOrElse(ge.getPrimaryIndex());

            final SPTarget target = new SPTarget();
            final GuideProbeTargets targets = opt
                    .map(gpt -> gpt.update(OptionsList.UpdateOps.appendAsPrimary(target)))
                    .getOrElse(GuideProbeTargets.create(probe, target));

            obsComp.setTargetEnvironment(env.setGuideEnvironment(
                    env.getGuideEnvironment().putGuideProbeTargets(groupIndex, targets)));

            // XXX OT-35 hack to work around recursive call to TargetObsComp.setTargetEnvironment() in
            // SPProgData.ObsContextManager.update()
            SwingUtilities.invokeLater(EdCompTargetList.this::showTargetTag);
        }
    }

    private static class AddUserTargetAction implements ActionListener {
        private final TargetObsComp obsComp;

        AddUserTargetAction(final TargetObsComp obsComp) {
            this.obsComp = obsComp;
        }

        @Override public void actionPerformed(final ActionEvent actionEvent) {
            final TargetEnvironment env = obsComp.getTargetEnvironment();
            final TargetEnvironment newEnv = env.setUserTargets(env.getUserTargets().append(new SPTarget()));
            obsComp.setTargetEnvironment(newEnv);
        }
    }

    private static class AddGroupAction implements ActionListener {
        private final TargetObsComp obsComp;
        private final TelescopePosTableWidget positionTable;

        AddGroupAction(final TargetObsComp obsComp, final TelescopePosTableWidget positionTable) {
            this.obsComp = obsComp;
            this.positionTable = positionTable;
        }

        @Override public void actionPerformed(final ActionEvent actionEvent) {
            final TargetEnvironment env    = obsComp.getTargetEnvironment();

            // Ensure we are working with a guide env with a primary group.
            final GuideEnvironment ge     = env.getGuideEnvironment();
            final GuideGroup primaryGroup = ge.getPrimary();

            final ImList<GuideGroup> oldGroups = ge.getOptions();
            final GuideGroup newGroup          = GuideGroup.create("Manual Group");
            final ImList<GuideGroup> newGroups = oldGroups.append(newGroup);

            // OT-34: make new group primary and select it
            if (!positionTable.confirmGroupChange(primaryGroup, newGroup)) return;
            obsComp.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(newGroups)
                    .setPrimaryIndex(oldGroups.size())));

            // expand new group tree node
            positionTable.selectGroup(newGroup);
        }
    }

    /**
     * A renderer of target type options.  Shows a guider type that isn't available in the current
     * context with a warning icon.
     */
    private final DefaultListCellRenderer tagRenderer = new DefaultListCellRenderer() {
        private final Icon errorIcon = Resources.getIcon("eclipse/error.gif");
        @Override public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                                final boolean isSelected, final boolean cellHasFocus) {
            final JLabel lab = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final PositionType pt = (PositionType) value;
            if (!pt.isAvailable()) {
                lab.setFont(lab.getFont().deriveFont(Font.ITALIC));
                lab.setIcon(errorIcon);
            }
            return lab;
        }
    };

    private final TelescopePosWatcher posWatcher = tp -> {
        if (tp != _curPos) {
            // This shouldn't happen ...
            System.out.println(getClass().getName() + ": received a position " +
                    " update for a position other than the current one: " + tp);
            return;
        }
        refreshAll();
        updateGuiding();
    };

    private final ActionListener _tagListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            final PositionType pt = (PositionType) _w.tag.getSelectedItem();
            pt.morphTarget(getDataObject(), _curPos);
            if (getDataObject() != null) {
                final TargetEnvironment env = getDataObject().getTargetEnvironment();
                manageCurPosIfEnvContainsTarget(_curPos, () -> _w.detailEditor.edit(getObsContext(env), _curPos, getNode()));
            }
        }
    };

    private final PropertyChangeListener selectionListener = new PropertyChangeListener() {
        @Override public void propertyChange(final PropertyChangeEvent evt) {
            final ISPObsComponent node = getContextTargetObsComp();
            final TargetEnvironment env = getDataObject().getTargetEnvironment();
            final SPTarget target = TargetSelection.get(env, node);

            if (target != null) {
                manageCurPosIfEnvContainsTarget(target, () -> _curPos = target);
            } else {
                final Option<Tuple2<Integer, GuideGroup>> grp = TargetSelection.getIndexedGuideGroup(env, node);
                grp.filter(tup -> env.getGroups().contains(tup._2())).foreach(tup -> {
                    if (_curPos != null) _curPos.deleteWatcher(posWatcher);

                    _curPos = null;
                    _curGroup = tup;

                    _w.guideGroupPanel.setVisible(true);
                    _w.detailEditor.setVisible(false);

                    // N.B. don't trim, otherwise user can't include space in group name
                    final String name = _curGroup._2().getName().getOrElse("");
                    if (!_w.guideGroupName.getValue().equals(name))
                        _w.guideGroupName.setValue(name);

                    final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
                    _w.removeButton.setEnabled(editable);
                    _w.primaryButton.setEnabled(editable);
                });
            }
        }
    };

    // Updates the enabled state of the primary guide target button when the target environment changes.
    private final PropertyChangeListener primaryButtonUpdater = new PropertyChangeListener() {
        @Override public void propertyChange(final PropertyChangeEvent evt) {
            final boolean enabled;
            if (_curPos != null) {
                final TargetEnvironment env = getDataObject().getTargetEnvironment();
                final ImList<GuideProbeTargets> gtList = env.getOrCreatePrimaryGuideGroup().getAllContaining(_curPos);
                enabled = gtList.nonEmpty();
            } else {
                enabled = _curGroup != null;
            }
            _w.primaryButton.setEnabled(enabled && OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation()));
        }
    };

    // Guider panel property change listener to modify status and magnitude limits.
    private final PropertyChangeListener guidingPanelUpdater = evt ->
            updateGuiding((TargetEnvironment) evt.getNewValue());

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener removeListener = evt -> {
        TargetEnvironment env = getDataObject().getTargetEnvironment();
        if (env.isBasePosition(_curPos)) {
            DialogUtil.error("You can't remove the Base Position.");
        } else if (_curPos != null) {
            env = env.removeTarget(_curPos);
        } else if (_curGroup != null) {
            final GuideGroup primary = env.getOrCreatePrimaryGuideGroup();
            if (_curGroup == primary) {
                DialogUtil.error("You can't remove the primary guide group.");
                return;
            }
            env = env.removeGroup(_curGroup._1());
            final int groupIndex = env.getGuideEnvironment().getPrimaryIndex();
            final GuideGroup group = env.getGuideEnvironment().getPrimary();
            _curGroup = new Pair<>(groupIndex, group);
        }
        getDataObject().setTargetEnvironment(env);
        final SPTarget selTarget = TargetSelection.get(env, getNode());
        if (env.getTargets().contains(selTarget)) {
            if (_curPos != null) _curPos.deleteWatcher(posWatcher);
            _curPos = selTarget;
            if (_curPos != null) {
                _curPos.addWatcher(posWatcher);
                refreshAll();
                updateRemovePrimaryButtonsAndDetailEditor();
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener manualGuideStarListener = evt -> {
        try {
            final TelescopePosEditor tpe = TpeManager.open();
            tpe.reset(getNode());
            tpe.getImageWidget().manualGuideStarSearch();
        } catch (final Exception e) {
            DialogUtil.error(e);
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener autoGuideStarListener = new ActionListener() {
        @Override public void actionPerformed (final ActionEvent evt) {
            try {
                if (GuideStarSupport.hasGemsComponent(getNode())) {
                    final TelescopePosEditor tpe = TpeManager.open();
                    tpe.reset(getNode());
                    tpe.getImageWidget().autoGuideStarSearch();
                } else {
                    // In general, we don't want to pop open the TPE just to
                    // pick a guide star.
                    AgsClient.launch(getNode(), _w);
                }
            } catch (final Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener duplicateListener = evt -> {
        final ISPObsComponent obsComponent = getNode();
        final TargetObsComp dataObject = getDataObject();
        if ((obsComponent == null) || (dataObject == null)) return;
        final SPTarget target = TargetSelection.get(dataObject.getTargetEnvironment(), obsComponent);
        if (target != null) {
            // Clone the target.
            final ParamSet ps = target.getParamSet(new PioXmlFactory());
            final SPTarget newTarget = new SPTarget();
            newTarget.setParamSet(ps);

            // Add it to the environment.  First we have to figure out what it is.
            final TargetEnvironment env = dataObject.getTargetEnvironment();

            // See if it is a guide star and duplicate it in the correct GuideTargets list.
            boolean duplicated = false;
            env.getOrCreatePrimaryGuideGroup();
            final List<GuideGroup> groups = new ArrayList<>();
            for (GuideGroup group : env.getGroups()) {
                for (GuideProbeTargets gt : group) {
                    if (gt.getTargets().contains(target)) {
                        group = group.put(gt.update(OptionsList.UpdateOps.append(newTarget)));
                        duplicated = true;
                        break;
                    }
                }
                groups.add(group);
            }

            final TargetEnvironment newEnv = duplicated ?
                    env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups))) :
                    env.setUserTargets(env.getUserTargets().append(newTarget));
            dataObject.setTargetEnvironment(newEnv);
        } else {
            final GuideGroup group = TargetSelection.getGuideGroup(dataObject.getTargetEnvironment(), obsComponent);
            if (group != null) {
                final TargetEnvironment env = dataObject.getTargetEnvironment();
                final List<GuideGroup> groups = new ArrayList<>();
                groups.addAll(env.getGroups().toList());
                groups.add(group.cloneTargets());
                final TargetEnvironment newEnv = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
                dataObject.setTargetEnvironment(newEnv);
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener copyListener = evt -> {
        final Option<TargetClipboard> opt = TargetClipboard.copy(getDataObject().getTargetEnvironment(), getNode());
        opt.foreach(c -> clipboard = c);
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener pasteListener = new ActionListener() {
        private void pasteSelectedPosition(final ISPObsComponent obsComponent, final TargetObsComp dataObject) {
            if (clipboard != null) {
                clipboard.paste(obsComponent, dataObject);
            }
        }
        @Override public void actionPerformed(final ActionEvent e) {
            if (_curPos != null) {
                pasteSelectedPosition(getNode(), getDataObject());
            } else if (_curGroup != null) {
                pasteSelectedPosition(getNode(), getDataObject());
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener primaryListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            _w.positionTable.updatePrimaryStar();
        }
    };


    private static final class TargetClipboard {
        private ITarget target;
        private GuideGroup group;
        private ImList<Magnitude> mag;

        static Option<TargetClipboard> copy(final TargetEnvironment env, final ISPObsComponent obsComponent) {
            if (obsComponent == null) return None.instance();
            return ImOption.apply(TargetSelection.get(env, obsComponent)).map(TargetClipboard::new).orElse(
                    ImOption.apply(TargetSelection.getGuideGroup(env, obsComponent)).map(TargetClipboard::new));
        }


        TargetClipboard(final SPTarget spTarget) {
            this.target = spTarget.getTarget().clone();
            this.mag = spTarget.getTarget().getMagnitudes();
        }

        TargetClipboard(final GuideGroup group) {
            this.group = group;
        }

        // Groups in their entirety should be copied, pasted, and duplicated by the existing
        // copy, paste, and duplicate buttons.  Disallow pasting a group on top of an individual target.
        // Pasting on top of a group should replace the group contents just as the target paste replaces
        // the coordinates of the selected target.
        void paste(final ISPObsComponent obsComponent, final TargetObsComp dataObject) {
            if ((obsComponent == null) || (dataObject == null)) return;

            final SPTarget spTarget = TargetSelection.get(dataObject.getTargetEnvironment(), obsComponent);
            final GuideGroup group = TargetSelection.getGuideGroup(dataObject.getTargetEnvironment(), obsComponent);

            if (spTarget != null && target != null) {
                spTarget.setTarget(target.clone());
                spTarget.setMagnitudes(mag);
            } else if (group != null && this.group != null) {
                final GuideGroup newGroup = group.setAll(this.group.cloneTargets().getAll());
                final TargetEnvironment env = dataObject.getTargetEnvironment();
                final GuideEnvironment ge = dataObject.getTargetEnvironment().getGuideEnvironment();
                final ImList<GuideGroup> options = ge.getOptions();
                final ArrayList<GuideGroup> list = new ArrayList<>(options.size());
                options.foreach(gg -> list.add(gg == group ? newGroup : gg));
                dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(DefaultImList.create(list))));
            }
        }
    }
}

interface PositionType {
    boolean isAvailable();
    void morphTarget(TargetObsComp obsComp, SPTarget target);
    boolean isMember(TargetEnvironment env, SPTarget target);
}

enum BasePositionType implements PositionType {
    instance;

    @Override public boolean isAvailable() {
        return true;
    }

    @Override public void morphTarget(final TargetObsComp obsComp, final SPTarget target) {
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;
        env = env.removeTarget(target);

        final SPTarget base = env.getBase();

        final GuideEnvironment genv = env.getGuideEnvironment();
        final ImList<SPTarget> user = env.getUserTargets().append(base);

        final TargetEnvironment newEnv = TargetEnvironment.create(target, genv, user);
        obsComp.setTargetEnvironment(newEnv);
    }

    @Override public boolean isMember(final TargetEnvironment env, final SPTarget target) {
        return (env.getBase() == target);
    }

    @Override public String toString() {
        return TargetEnvironment.BASE_NAME;
    }
}

class GuidePositionType implements PositionType {
    private final GuideProbe guider;
    private final boolean available;

    GuidePositionType(final GuideProbe guider, final boolean available) {
        this.guider = guider;
        this.available = available;
    }

    @Override public boolean isAvailable() {
        return available;
    }

    @Override public void morphTarget(final TargetObsComp obsComp, final SPTarget target) {
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;
        env = env.removeTarget(target);

        final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
        final GuideProbeTargets gt = gtOpt.map(gpt -> gpt.update(OptionsList.UpdateOps.appendAsPrimary(target)))
                .getOrElse(GuideProbeTargets.create(guider, target));

        final TargetEnvironment newEnv = env.putPrimaryGuideProbeTargets(gt);
        obsComp.setTargetEnvironment(newEnv);
    }

    @Override public boolean isMember(final TargetEnvironment env, final SPTarget target) {
        return env.getGroups().exists(group -> group.get(guider).exists(gt -> gt.getTargets().contains(target)));
    }

    @Override public String toString() {
        return guider.getKey();
    }
}

enum UserPositionType implements PositionType {
    instance;

    @Override public boolean isAvailable() {
        return true;
    }

    @Override public void morphTarget(final TargetObsComp obsComp, final SPTarget target) {
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;
        env = env.removeTarget(target);

        final TargetEnvironment newEnv = env.setUserTargets(env.getUserTargets().append(target));
        obsComp.setTargetEnvironment(newEnv);
    }

    @Override public boolean isMember(final TargetEnvironment env, final SPTarget target) {
        return env.getUserTargets().contains(target);
    }

    @Override public String toString() {
        return TargetEnvironment.USER_NAME;
    }
}
