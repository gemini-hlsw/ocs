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

    // The current selection. Can be None if nothing is selected.
    private Option<ImEither<SPTarget, IndexedGuideGroup>> _curSelection = None.instance();

    // A collection of the JMenuItems that allow the addition of guide stars.
    final Map<GuideProbe,Component> _guideStarAdders = new HashMap<>();


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
                SwingUtilities.invokeLater(() ->
                        _curSelection = _curSelection.map(e -> e.map(igg -> {
                            final String newName = _w.guideGroupName.getText();
                            final TargetObsComp toc = getDataObject();
                            final int idx = igg.index();
                            final GuideGroup newGroup = igg.group().setName(newName);
                            final TargetEnvironment oldEnv = toc.getTargetEnvironment();
                            toc.setTargetEnvironment(oldEnv.setGroup(idx, newGroup));
                            _w.guideGroupName.requestFocus();
                            return new IndexedGuideGroup(idx, newGroup);
                        }))
                );
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

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.setEnabled(enabled && inst != null);
        _w.tag.setEnabled(enabled && selectionIsBasePosition());
    }

    private void updateDetailEditorEnabledState(final boolean enabled) {
        // Update enabled state for all detail widgets.
        _w.detailEditor.allEditorsJava().stream().forEach(ed -> updateEnabledState(new Component[]{ed}, enabled));
    }

    /**
     * Auxiliary method to determine if the current selection is the base position.
     */
    private boolean selectionIsBasePosition() {
        return _curSelection.exists(either -> {
            final TargetEnvironment env = getDataObject().getTargetEnvironment();
            return either.swap().exists(env::isBasePosition);
        });
    }

    /**
     * Auxiliary method to determine if the current selection is the auto group.
     */
    private boolean selectionIsAutoGroup() {
        return _curSelection.exists(either -> either.exists(igg -> igg.group().isAutomatic()));
    }

    /**
     * Auxiliary method to determine if the current selection is a target that belongs to the auto group.
     */
    private boolean selectionIsAutoTarget() {
        return _curSelection.exists(sel -> sel.swap().exists(t ->
            getDataObject().getTargetEnvironment().getGuideEnvironment().getOptions()
                    .exists(gg -> gg.isAutomatic() && gg.containsTarget(t))
        ));
    }

    /**
     * Auxiliary method to determine if the current selection is a user target.
     */
    private boolean selectionIsUserTarget() {
        return _curSelection.exists(sel -> sel.swap().exists(t -> getDataObject().getTargetEnvironment().getUserTargets().contains(t)));
    }

    /**
     * Auxiliary method to set the selection to the specified target.
     */
    private void setSelectionToTarget(final SPTarget t) {
        _curSelection = new Some<>(ImEither.left(t));
    }

    /**
     * Update the UI components when a target becomes selected.
     */
    private void updateUIForTarget() {
        final boolean editable     = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        final boolean notBase      = !selectionIsBasePosition();
        final boolean notAuto      = !(selectionIsAutoGroup() || selectionIsAutoTarget());
        final boolean isGuideStar  = _curSelection.exists(ImEither::isLeft) && !(selectionIsBasePosition() || selectionIsUserTarget());

        _w.removeButton.setEnabled (editable && notBase && notAuto);
        _w.primaryButton.setEnabled(editable && isGuideStar && notAuto);
        _w.pasteButton.setEnabled(editable && notAuto);
        _w.duplicateButton.setEnabled(editable && isGuideStar && notAuto);
        updateDetailEditorEnabledState(editable && notAuto);
    }

    /**
     * Update the UI components when a group becomes selected.
     */
    private void updateUIForGroup() {
        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        final boolean notAuto   = !selectionIsAutoGroup();
        _w.removeButton.setEnabled(editable && notAuto);
        _w.primaryButton.setEnabled(editable);
        _w.pasteButton.setEnabled(editable && notAuto);
        _w.duplicateButton.setEnabled(editable);
    }

    /**
     * Update the menu items that allow the addition of guide stars. These must be disabled for the automatic group
     * or if we are currently on the base or a user target.
     */
    private void updateGuideStarAdders() {
        final boolean notBase = !selectionIsBasePosition();
        final boolean notUser = !selectionIsUserTarget();
        final boolean notAuto = !(selectionIsAutoGroup() || selectionIsAutoTarget());
        _guideStarAdders.forEach((gp, comp) -> comp.setEnabled(notBase && notUser && notAuto));
    }

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

        // If current selection is a target, remove the posWatcher.
        _curSelection.foreach(e -> e.swap().foreach(t -> t.deleteWatcher(posWatcher)));

        action.run();

        // If the current selection is a target, then readd the posWatcher and refresh the UI.
        _curSelection.foreach(e -> e.swap().foreach(t -> {
            t.addWatcher(posWatcher);
            refreshAll();
        }));
    }

    @Override public void init() {
        final ISPObsComponent node = getContextTargetObsComp();
        TargetSelection.listenTo(node, selectionListener);

        final TargetObsComp obsComp = getDataObject();
        obsComp.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        obsComp.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);

        final TargetEnvironment env = obsComp.getTargetEnvironment();
        final SPTarget selTarget = TargetSelection.getTargetForNode(env, node).getOrNull();
        manageCurPosIfEnvContainsTarget(selTarget, () -> setSelectionToTarget(selTarget));

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.removeAll();
        _guideStarAdders.clear();

        if (inst == null) {
            _w.newMenu.setEnabled(false);
        } else {
            _w.newMenu.setEnabled(true);
            if (inst.hasGuideProbes()) {
                final List<GuideProbe> guiders = new ArrayList<>(GuideProbeUtil.instance.getAvailableGuiders(getContextObservation()));
                Collections.sort(guiders, GuideProbe.KeyComparator.instance);
                guiders.forEach(probe -> {
                    final JMenuItem guideStarAdder = new JMenuItem(probe.getKey()) {{
                        addActionListener(new AddGuideStarAction(obsComp, probe, _w.positionTable));
                    }};
                    _w.newMenu.add(guideStarAdder);
                    _guideStarAdders.put(probe, guideStarAdder);
                });
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
        final SPTarget      target = TargetSelection.getTargetForNode(env, node).getOrNull();
        _w.detailEditor.gfe().edit(ctx, target, node);
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
        final Option<GuideProbe> illegal = _curSelection.flatMap(either ->
                either.getLeft().flatMap(t -> {
                    final Option<GuideProbe> noProbe = None.instance();
                    return illegalSet.stream().sequential().reduce(noProbe,
                            (illegalOpt, gp) -> {
                                final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(gp);
                                if (gtOpt.getValue().getTargets().contains(t)) {
                                    guiders.add(gp);
                                    return new Some<>(gp);
                                } else return illegalOpt;
                            }, (gp1, gp2) -> gp1);
                }));

        // Sort the list of guiders.
        final List<GuideProbe> guidersList = new ArrayList<>(guiders);
        Collections.sort(guidersList, GuideProbe.KeyComparator.instance);

        // Make a list of PositionTypes that are legal in the current observation context.
        int index = 0;
        final PositionType[] ptA = new PositionType[2 + guiders.size()];
        ptA[index++] = BasePositionType.instance;
        for (GuideProbe guider : guidersList) {
            final boolean guiderNotIllegal = !illegal.exists(guider::equals);
            ptA[index++] = new GuidePositionType(guider, guiderNotIllegal);
        }
        ptA[index] = UserPositionType.instance;

        _w.tag.removeActionListener(_tagListener);
        _w.tag.setModel(new DefaultComboBoxModel<>(ptA));
        _w.tag.setEnabled(isEnabled() && (!selectionIsBasePosition()));
        _w.tag.addActionListener(_tagListener);

        _w.tag.setRenderer(tagRenderer);
        showTargetTag();

        // Update target details
        _curSelection.foreach(e -> e.swap().foreach(t -> _w.detailEditor.edit(getObsContext(env), t, getNode())));

        // Set the status of the buttons and detail editors.
        updateUIForTarget();
        updateGuideStarAdders();
    }

    private void showTargetTag() {
        _curSelection.foreach(either -> either.swap().foreach(t -> {
            final TargetEnvironment env = getDataObject().getTargetEnvironment();
            for (int i = 0; i < _w.tag.getItemCount(); ++i) {
                final PositionType pt = _w.tag.getItemAt(i);
                if (pt.isMember(env, t)) {
                    _w.tag.removeActionListener(_tagListener);
                    _w.tag.setSelectedIndex(i);
                    _w.tag.addActionListener(_tagListener);
                    break;
                }
            }
        }));
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

            // Make sure that a group is selected and that it is not the auto group.
            final Option<IndexedGuideGroup> iggOpt = positionTable.getSelectedGroupOrParentGroup(env);
            if (!iggOpt.exists(igg -> !igg.group().isAutomatic()))
                return;

            Option<GuideProbeTargets> opt = iggOpt.flatMap(igg -> igg.group().get(probe));
            final Integer groupIndex = iggOpt.map(IndexedGuideGroup::index).getOrElse(ge.getPrimaryIndex());

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
            final int newGroupIdx              = oldGroups.size();
            final GuideGroup newGroup          = GuideGroup.create("Manual Group");
            final ImList<GuideGroup> newGroups = oldGroups.append(newGroup);

            // OT-34: make new group primary and select it
            if (!positionTable.confirmGroupChange(primaryGroup, newGroup)) return;
            obsComp.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(newGroups)
                    .setPrimaryIndex(newGroupIdx)));

            // expand new group tree node
            positionTable.selectGroup(IndexedGuideGroup$.MODULE$.apply(newGroupIdx,newGroup));
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
        if (tp != _curSelection.flatMap(ImEither::getLeft).getOrNull()) {
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
            _curSelection.foreach(either -> either.swap().foreach(t -> {
                pt.morphTarget(getDataObject(), t);
                if (getDataObject() != null) {
                    final TargetEnvironment env = getDataObject().getTargetEnvironment();
                    manageCurPosIfEnvContainsTarget(t, () -> _w.detailEditor.edit(getObsContext(env), t, getNode()));
                }
            }));
        }
    };

    private final PropertyChangeListener selectionListener = new PropertyChangeListener() {
        @Override public void propertyChange(final PropertyChangeEvent evt) {
            final ISPObsComponent node = getContextTargetObsComp();
            final TargetEnvironment env = getDataObject().getTargetEnvironment();
            final Option<SPTarget> targetOpt = TargetSelection.getTargetForNode(env, node);

            // We must be careful here to make sure the _curPos \/ _curGroup property is maintained, i.e. exactly
            // one of the two must be defined and the other null.
            if (targetOpt.isDefined()) {
                // _curGroup = null is handled in function.
                targetOpt.foreach(target -> manageCurPosIfEnvContainsTarget(target, () -> setSelectionToTarget(target)));
            } else {
                final Option<IndexedGuideGroup> iggOpt = TargetSelection.getIndexedGuideGroupForNode(env, node);
                iggOpt.filter(igg -> env.getGroups().contains(igg.group())).foreach(igg -> {
                    _curSelection.foreach(either -> either.swap().foreach(t -> t.deleteWatcher(posWatcher)));

                    _curSelection = new Some<>(ImEither.right(igg));

                    _w.guideGroupPanel.setVisible(true);
                    _w.detailEditor.setVisible(false);

                    // N.B. don't trim, otherwise user can't include space in group name
                    final String name;
                    final boolean enabled;
                    if (selectionIsAutoGroup()) {
                        // TODO: Possibly change this for different AutomaticGroup types?
                        name = "Automatic Group";
                        enabled = false;
                    } else {
                        name = igg.group().getName().getOrElse("");
                        enabled = true;
                    }
                    if (!_w.guideGroupName.getValue().equals(name))
                        _w.guideGroupName.setValue(name);
                    _w.guideGroupName.setEnabled(enabled);

                    updateUIForGroup();
                    updateGuideStarAdders();
                });
            }
        }
    };

    // Updates the enabled state of the primary guide target button when the target environment changes.
    private final PropertyChangeListener primaryButtonUpdater = new PropertyChangeListener() {
        @Override public void propertyChange(final PropertyChangeEvent evt) {
            final boolean enabled = _curSelection.map(either -> either.fold(
                    t -> {
                        final TargetEnvironment env = getDataObject().getTargetEnvironment();
                        final ImList<GuideProbeTargets> gtList = env.getOrCreatePrimaryGuideGroup().getAllContaining(t);
                        return gtList.nonEmpty() && !selectionIsAutoTarget();
                    },
                    igg -> true
            )).getOrElse(false);
            _w.primaryButton.setEnabled(enabled && OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation()));
        }
    };

    // Guider panel property change listener to modify status and magnitude limits.
    private final PropertyChangeListener guidingPanelUpdater = evt ->
            updateGuiding((TargetEnvironment) evt.getNewValue());

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener removeListener = evt -> {
        final TargetEnvironment envOld = getDataObject().getTargetEnvironment();

        // If there is a selection, then handle it, separately for targets and guide groups.
        final Option<TargetEnvironment> envNewOpt = _curSelection.flatMap(either -> either.fold(
                // Handle targets.
                t -> {
                    if (selectionIsBasePosition()) {
                        DialogUtil.error("You can't remove the base position.");
                    } else if (selectionIsAutoTarget()) {
                        DialogUtil.error("You can't remove automatic guide stars.");
                    } else {
                        return new Some<>(envOld.removeTarget(t));
                    }
                    return None.<TargetEnvironment>instance();
                },

                // Handle indexed guide groups.
                igg -> {
                    final GuideGroup primary = envOld.getOrCreatePrimaryGuideGroup();
                    if (igg.group() == primary) {
                        DialogUtil.error("You can't remove the primary guide group.");
                    } else if (selectionIsAutoGroup()) {
                        DialogUtil.error("You can't remove the automatic guide group.");
                    } else {
                        final TargetEnvironment envNew = envOld.removeGroup(igg.index());
                        final int groupIndex = envNew.getGuideEnvironment().getPrimaryIndex();
                        final GuideGroup group = envNew.getGuideEnvironment().getPrimary();
                        _curSelection = new Some<>(ImEither.right(new IndexedGuideGroup(groupIndex, group)));
                        return new Some<>(envNew);
                    }
                    return None.<TargetEnvironment>instance();
                })
        );


        // Permitted changes were made only if envNewOpt is defined.
        envNewOpt.foreach(envNew -> {
            getDataObject().setTargetEnvironment(envNew);

            // If we have a selected target, then process it accordingly.
            final Option<SPTarget> selTargetOpt = TargetSelection.getTargetForNode(envNew, getNode());
            selTargetOpt.filter(t -> envNew.getTargets().contains(t)).foreach(selTarget -> {
                // Remove any watchers on the currently selected position.
                _curSelection.foreach(either -> either.swap().foreach(t -> t.deleteWatcher(posWatcher)));

                // Set the current selection to the new target.
                _curSelection = new Some<>(ImEither.left(selTarget));

                // Add the watcher to the currently selected position.
                _curSelection.foreach(either -> either.swap().foreach(t -> t.addWatcher(posWatcher)));
            });

            refreshAll();
        });
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
                // TODO: For BAGS, we do not want to pop open the TPE.
                if (GuideStarSupport.hasGemsComponent(getNode())) {
                    final TelescopePosEditor tpe = TpeManager.open();
                    tpe.reset(getNode());
                    tpe.getImageWidget().autoGuideStarSearch();
                } else {
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

        final Option<SPTarget> targetOpt = TargetSelection.getTargetForNode(dataObject.getTargetEnvironment(), obsComponent);
        if (targetOpt.isDefined()) {
            final SPTarget target = targetOpt.getValue();

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
            final Option<IndexedGuideGroup> iggOpt =
                    TargetSelection.getIndexedGuideGroupForNode(dataObject.getTargetEnvironment(), obsComponent);
            iggOpt.foreach(igg -> {
                final GuideGroup origGroup    = igg.group();

                // This used to be done with origGroup.cloneTargets(), but if origGroup is an auto group, this creates
                // another auto group, which is problematic, so we specifically force a manual group to be created
                // and receive an appropriate name.
                final String newGroupName     = origGroup.getName().filter(s -> !s.isEmpty()).getOrElse("Manual Group");
                final GuideGroup newGroup     = GuideGroup.create(newGroupName, origGroup.getAll()).cloneTargets();

                final TargetEnvironment env   = dataObject.getTargetEnvironment();
                final List<GuideGroup> groups = new ArrayList<>();
                groups.addAll(env.getGroups().toList());
                groups.add(newGroup);

                final TargetEnvironment newEnv = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
                dataObject.setTargetEnvironment(newEnv);
            });
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
            // As long as something is selected, we can paste it.
            _curSelection.foreach(ignored -> pasteSelectedPosition(getNode(), getDataObject()));
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener primaryListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            _w.positionTable.updatePrimaryStar();
        }
    };


    private static final class TargetClipboard {
        private static final class TargetDetails {
            private final ITarget           target;
            private final ImList<Magnitude> mag;

            public TargetDetails(final SPTarget target) {
                this.target = target.getTarget().clone();
                this.mag    = target.getTarget().getMagnitudes();
            }

            public ITarget getTarget() {
                return target;
            }
            public ImList<Magnitude> getMag() {
                return mag;
            }
        }

        private ImEither<TargetDetails,GuideGroup> contents;

        static Option<TargetClipboard> copy(final TargetEnvironment env, final ISPObsComponent obsComponent) {
            if (obsComponent == null) return None.instance();
            return TargetSelection.getTargetForNode(env, obsComponent).map(TargetClipboard::new)
                    .orElse(TargetSelection.getIndexedGuideGroupForNode(env, obsComponent).map(IndexedGuideGroup::group).map(TargetClipboard::new));
        }


        TargetClipboard(final SPTarget target) {
            contents = ImEither.left(new TargetDetails(target));
        }

        TargetClipboard(final GuideGroup group) {
            contents = ImEither.right(group);
        }

        // Groups in their entirety should be copied, pasted, and duplicated by the existing
        // copy, paste, and duplicate buttons.  Disallow pasting a group on top of an individual target.
        // Pasting on top of a group should replace the group contents just as the target paste replaces
        // the coordinates of the selected target.
        void paste(final ISPObsComponent obsComponent, final TargetObsComp dataObject) {
            if ((obsComponent == null) || (dataObject == null)) return;

            contents.biForeach(
                    // Handle targets
                    targetDetails -> {
                        final Option<SPTarget> tOpt = TargetSelection.getTargetForNode(dataObject.getTargetEnvironment(), obsComponent);
                        tOpt.foreach(t -> {
                            t.setTarget(targetDetails.getTarget().clone());
                            t.setMagnitudes(targetDetails.getMag());
                        });
                    },

                    // Handle guide groups.
                    group -> {
                        final Option<IndexedGuideGroup> gpOpt = TargetSelection.getIndexedGuideGroupForNode(dataObject.getTargetEnvironment(), obsComponent);
                        gpOpt.foreach(igg -> {
                            final int idx                    = igg.index();
                            final GuideGroup newGroup        = group.setAll(group.cloneTargets().getAll());
                            final TargetEnvironment env      = dataObject.getTargetEnvironment();
                            final GuideEnvironment ge        = env.getGuideEnvironment();
                            final ImList<GuideGroup> options = ge.getOptions();
                            final ArrayList<GuideGroup> list = new ArrayList<>(options.size());
                            options.zipWithIndex().foreach(tup -> list.add(tup._2() == idx ? newGroup : tup._1()));
                            dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(DefaultImList.create(list))));
                        });
                    }
            );
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
