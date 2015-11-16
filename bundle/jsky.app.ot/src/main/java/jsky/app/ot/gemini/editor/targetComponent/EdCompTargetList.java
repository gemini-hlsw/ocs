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
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.ot.OTOptions;
import jsky.app.ot.ags.*;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.tpe.GuideStarSupport;
import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.tpe.TpeManager;
import jsky.app.ot.util.Resources;
import jsky.util.gui.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
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
    private GuideGroup      _curGroup;

    public EdCompTargetList() {

        _w = new TelescopeForm(this);

        _w.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent componentEvent) {
                toggleAgsGuiElements();
            }
        });

        _w.removeButton   .addActionListener(removeListener);
        _w.copyButton     .addActionListener(copyListener);
        _w.pasteButton    .addActionListener(pasteListener);
        _w.duplicateButton.addActionListener(duplicateListener);
        _w.primaryButton  .addActionListener(primaryListener);

        _w.guidingControls.manualGuideStarButton().peer().addActionListener(manualGuideStarListener);

        _w.guidingControls.autoGuideStarGuiderSelector().addSelectionListener(strategy ->
                AgsStrategyUtil.setSelection(getContextObservation(), strategy)
        );

        _w.guideGroupName.addWatcher(new TextBoxWidgetWatcher() {
            public void textBoxKeyPress(TextBoxWidget tbwe) {
                SwingUtilities.invokeLater(() -> {
                    final String name = _w.guideGroupName.getText();
                    final GuideGroup newGroup = _curGroup.setName(name);
                    final TargetEnvironment env = getDataObject().getTargetEnvironment();
                    final GuideEnvironment ge = env.getGuideEnvironment();
                    final ImList<GuideGroup> options = ge.getOptions();
                    final List<GuideGroup> list = new ArrayList<>(options.size());
                    for (GuideGroup g : options) {
                        list.add(g == _curGroup ? newGroup : g);
                    }
                    _curGroup = newGroup;

                    getDataObject().setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(DefaultImList.create(list))));
                    _w.guideGroupName.requestFocus(); // otherwise focus is lost during event handling
                });
            }
            public void textBoxAction(TextBoxWidget tbwe) {
            }
        });

        _w.positionTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
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

    @Override protected void updateEnabledState(boolean enabled) {
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
        _w.tag.setEnabled(enabled && env.getBase() != _curPos);

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.setEnabled(enabled && inst != null);
    }

    protected void updateDetailEditorEnabledState(boolean enabled) {
        // Update enabled state for all detail widgets.
        _w.detailEditor.allEditorsJava().stream().forEach(ed -> updateEnabledState(new Component[]{ed}, enabled));
    }

    /**
     * Update the remove and primary buttons as well as the detail editor.
     */
    private void updateRemovePrimaryButtonsAndDetailEditor(final TargetEnvironment env) {
        final boolean editable   = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        final boolean curNotBags = !env.getGroups().exists(gg -> gg.getAllContaining(_curPos).exists(gpt -> gpt.getBagsResult().targetAsJava().exists(_curPos::equals)));
        final boolean curNotBase = _curPos != env.getBase();
        _w.removeButton.setEnabled(curNotBase && curNotBags && editable);
        _w.primaryButton.setEnabled(enablePrimary(_curPos, env) && editable);
        updateDetailEditorEnabledState(curNotBags && editable);
    }

    private final ActionListener _tagListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            final PositionType pt = (PositionType) _w.tag.getSelectedItem();
            pt.morphTarget(getDataObject(), _curPos);
            if (getDataObject() != null) {
                final TargetEnvironment env = getDataObject().getTargetEnvironment();
                if (env.getTargets().contains(_curPos)) {

                    final Option<ObsContext> ctx = getObsContext(env);

                    if (_curPos != null) _curPos.deleteWatcher(posWatcher);

                    _w.detailEditor.edit(ctx, _curPos, getNode());

                    if (_curPos != null) {
                        _curPos.addWatcher(posWatcher);
                        refreshAll();
                        updateRemovePrimaryButtonsAndDetailEditor(env);
                    }
                }
            }
        }
    };

    /**
     * A renderer of target type options.  Shows a guider type that isn't available in the current
     * context with a warning icon.
     */
    private final DefaultListCellRenderer tagRenderer = new DefaultListCellRenderer() {
        private final Icon errorIcon = Resources.getIcon("eclipse/error.gif");
        @Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel lab = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final PositionType pt = (PositionType) value;
            if (!pt.isAvailable()) {
                lab.setFont(lab.getFont().deriveFont(Font.ITALIC));
                lab.setIcon(errorIcon);
            }
            return lab;
        }
    };

    private static final class TargetClipboard {
        private ITarget target;
        private GuideGroup group;
        private ImList<Magnitude> mag;

        static Option<TargetClipboard> copy(TargetEnvironment env, ISPObsComponent obsComponent) {
            if (obsComponent == null) return None.instance();

            final SPTarget target = TargetSelection.get(env, obsComponent);
            if (target == null) {
                final GuideGroup group = TargetSelection.getGuideGroup(env, obsComponent);
                if (group == null) {
                    return None.instance();
                }
                return new Some<>(new TargetClipboard(group));
            }
            return new Some<>(new TargetClipboard(target));
        }

        TargetClipboard(SPTarget spTarget) {
            this.target = spTarget.getTarget().clone();
            this.mag = spTarget.getTarget().getMagnitudes();
        }

        TargetClipboard(GuideGroup group) {
            this.group = group;
        }

        // Groups in their entirety should be copied, pasted, and duplicated by the existing
        // copy, paste, and duplicate buttons.  Disallow pasting a group on top of an individual target.
        // Pasting on top of a group should replace the group contents just as the target paste replaces
        // the coordinates of the selected target.
        void paste(ISPObsComponent obsComponent, TargetObsComp dataObject) {
            if ((obsComponent == null) || (dataObject == null)) return;

            final SPTarget spTarget = TargetSelection.get(dataObject.getTargetEnvironment(), obsComponent);
            if (spTarget != null) {
                if (target != null) {
                    spTarget.setTarget(target.clone());
                    spTarget.setMagnitudes(mag);
                }
            } else {
                final GuideGroup group = TargetSelection.getGuideGroup(dataObject.getTargetEnvironment(), obsComponent);
                if (group != null) {
                    if (this.group != null) {
                        final GuideGroup newGroup = group.setAll(this.group.cloneTargets().getAll());
                        // XXX TODO: add a helper method in the model to replace a guide group
                        final TargetEnvironment env = dataObject.getTargetEnvironment();
                        final GuideEnvironment ge = dataObject.getTargetEnvironment().getGuideEnvironment();
                        final ImList<GuideGroup> options = ge.getOptions();
                        final ArrayList<GuideGroup> list = new ArrayList<>(options.size());
                        for (GuideGroup gg : options) {
                            list.add(gg == group ? newGroup : gg);
                        }
                        dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(DefaultImList.create(list))));
                    }
                }
            }
        }
    }

    // OtItemEditor
    public JPanel getWindow() {
        return _w;
    }

    // OtItemEditor
    public void init() {

        final ISPObsComponent node = getContextTargetObsComp();
        TargetSelection.listenTo(node, selectionListener);

        getDataObject().addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        getDataObject().addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);

        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        final SPTarget selTarget = TargetSelection.get(env, node);
        if (getDataObject() != null) {
            final TargetEnvironment env2 = getDataObject().getTargetEnvironment();
            if (env2.getTargets().contains(selTarget)) {

                if (_curPos != null) _curPos.deleteWatcher(posWatcher);

                _curPos = selTarget;

                if (_curPos != null) {
                    _curPos.addWatcher(posWatcher);
                    refreshAll();
                    updateRemovePrimaryButtonsAndDetailEditor(env2);
                }
            }
        }

        final TargetObsComp obsComp = getDataObject();
        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.removeAll();
        if (obsComp == null || inst == null) {
            _w.newMenu.setEnabled(false);
        } else {
            _w.newMenu.setEnabled(true);
            if (inst.hasGuideProbes()) {
                final List<GuideProbe> guiders = new ArrayList<>(GuideProbeUtil.instance.getAvailableGuiders(getContextObservation()));
                Collections.sort(guiders, GuideProbe.KeyComparator.instance);

                for (final GuideProbe probe : guiders) {
                    _w.newMenu.add(new JMenuItem(probe.getKey()) {{
                        addActionListener(new AddGuideStarAction(obsComp, probe, _w.positionTable));
                    }});
                }
            }

            _w.newMenu.add(new JMenuItem("User") {{
                addActionListener(new AddUserTargetAction(obsComp));
            }});

            if (inst.hasGuideProbes()) {
                _w.newMenu.addSeparator();
                final JMenuItem guideGroupMenu = _w.newMenu.add(new JMenuItem("Guide Group") {{
                    addActionListener(new AddGroupAction(obsComp, _w.positionTable));
                }});

                // OT-34: disable create group menu if no guide stars defined
                _w.newMenu.addMenuListener(new MenuListener() {
                    @Override
                    public void menuSelected(MenuEvent e) {
                        guideGroupMenu.setEnabled(obsComp.getTargetEnvironment().getGuideEnvironment().getTargets().size() != 0);
                    }

                    @Override
                    public void menuDeselected(MenuEvent e) {
                    }

                    @Override
                    public void menuCanceled(MenuEvent e) {
                    }
                });
            }
        }
        _w.positionTable.reinit(getDataObject());
        _w.guidingControls.manualGuideStarButton().peer().setVisible(GuideStarSupport.supportsManualGuideStarSelection(getNode()));
        updateGuiding();
        _agsPub.watch(getContextObservation());
    }

    // OtItemEditor
    protected void cleanup() {
        _agsPub.watch(null);
        TargetSelection.deafTo(getContextTargetObsComp(), selectionListener);
        getDataObject().removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        getDataObject().removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);
        super.cleanup();
    }

    private void toggleAgsGuiElements() {
        final boolean supports = GuideStarSupport.supportsAutoGuideStarSelection(getNode());
        _w.guidingControls.supportsAgs_$eq(supports); // hide the ags related buttons
    }

    // Guider panel property change listener to modify status and magnitude limits.
    private final PropertyChangeListener guidingPanelUpdater = (evt) ->
        updateGuiding((TargetEnvironment) evt.getNewValue());

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
        _w.detailEditor.gfe().edit(getObsContext(env), target, node);
    }

    private final PropertyChangeListener selectionListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final ISPObsComponent node = getContextTargetObsComp();
            final TargetEnvironment env = getDataObject().getTargetEnvironment();
            final SPTarget target = TargetSelection.get(env, node);
            if (target != null) {
                if (getDataObject() != null) {
                    final TargetEnvironment env1 = getDataObject().getTargetEnvironment();
                    if (env1.getTargets().contains(target)) {

                        if (_curPos != null) _curPos.deleteWatcher(posWatcher);
                        _curPos = target;
                        _curPos.addWatcher(posWatcher);
                        refreshAll();
                        updateRemovePrimaryButtonsAndDetailEditor(env1);
                    }
                }
            } else {
                final GuideGroup grp = TargetSelection.getGuideGroup(env, node);
                if (grp != null) if (getDataObject() != null) {
                    final TargetEnvironment env1 = getDataObject().getTargetEnvironment();
                    if (env1.getGroups().contains(grp)) {

                        if (_curPos != null) _curPos.deleteWatcher(posWatcher);

                        _curPos = null;
                        _curGroup = grp;

                        _w.guideGroupPanel.setVisible(true);
                        _w.detailEditor.setVisible(false);

                        // N.B. don't trim, otherwise user can't include space in group name
                        final String name = _curGroup.getName().getOrElse("");
                        _w.guideGroupName.setValue(name);

                        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
                        _w.removeButton.setEnabled(editable);
                        _w.primaryButton.setEnabled(editable);
                    }
                }
            }
        }
    };

    // Updates the enabled state of the primary guide target button when the target environment changes.
    private final PropertyChangeListener primaryButtonUpdater = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            boolean enabled = false;
            if (_curPos != null) {
                final TargetEnvironment env = getDataObject().getTargetEnvironment();
                final ImList<GuideProbeTargets> gtList = env.getOrCreatePrimaryGuideGroup().getAllContaining(_curPos);
                enabled = gtList.size() > 0;
            } else if (_curGroup != null) {
                enabled = true;
            }
            _w.primaryButton.setEnabled(enabled && OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation()));
        }
    };

    // Action that handles adding a new guide star when a probe is picked from the add menu.
    private class AddGuideStarAction implements ActionListener {
        private final TargetObsComp obsComp;
        private final GuideProbe probe;
        private final TelescopePosTableWidget positionTable;

        AddGuideStarAction(TargetObsComp obsComp, GuideProbe probe, TelescopePosTableWidget positionTable) {
            this.obsComp = obsComp;
            this.probe = probe;
            this.positionTable = positionTable;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            GuideEnvironment ge = env.getGuideEnvironment();
            if (ge.getPrimary().isEmpty()) {
                ge = ge.setPrimary(env.getOrCreatePrimaryGuideGroup());
                env = env.setGuideEnvironment(ge);
            }

            // OT-16: add new guide star to selected group, if any, otherwise the primary group
            GuideGroup guideGroup = positionTable.getSelectedGroupOrParentGroup(env);
            final Option<GuideProbeTargets> opt = guideGroup == null ?
                    env.getPrimaryGuideProbeTargets(probe) :
                    guideGroup.get(probe);
            if (guideGroup == null) {
                guideGroup = ge.getPrimary().getValue();
            }

            final GuideProbeTargets targets;
            final SPTarget target = new SPTarget();
            if (opt.isEmpty()) {
                targets = GuideProbeTargets.create(probe, target).withExistingPrimary(target);
            } else {
                targets = opt.getValue().addManualTarget(target).withExistingPrimary(target);
            }
            obsComp.setTargetEnvironment(env.setGuideEnvironment(
                    env.getGuideEnvironment().putGuideProbeTargets(guideGroup, targets)));

            // XXX OT-35 hack to work around recursive call to TargetObsComp.setTargetEnvironment() in
            // SPProgData.ObsContextManager.update()
            SwingUtilities.invokeLater(EdCompTargetList.this::showTargetTag);
        }
    }

    private static class AddUserTargetAction implements ActionListener {
        private final TargetObsComp obsComp;

        AddUserTargetAction(TargetObsComp obsComp) {
            this.obsComp = obsComp;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            TargetEnvironment env = obsComp.getTargetEnvironment();
            env = env.setUserTargets(env.getUserTargets().append(new SPTarget()));
            obsComp.setTargetEnvironment(env);
        }
    }

    private static class AddGroupAction implements ActionListener {
        private final TargetObsComp obsComp;
        private final TelescopePosTableWidget positionTable;

        AddGroupAction(TargetObsComp obsComp, TelescopePosTableWidget positionTable) {
            this.obsComp = obsComp;
            this.positionTable = positionTable;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            final TargetEnvironment env = obsComp.getTargetEnvironment();
            GuideEnvironment ge = env.getGuideEnvironment();
            if (ge.getPrimary().isEmpty()) {
                ge = ge.setPrimary(env.getOrCreatePrimaryGuideGroup());
            }
            final GuideGroup primaryGroup = ge.getPrimary().getValue();
            final ImList<GuideGroup> options = ge.getOptions();
            final GuideGroup group = GuideGroup.create(null);
            final ImList<GuideGroup> groups = options.append(group);

            // OT-34: make new group primary and select it
            if (!positionTable.confirmGroupChange(primaryGroup, group)) return;
            obsComp.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(groups).selectPrimary(group)));
            if (groups.size() == 2) {
                // expand primary group tree node, which was previously hidden (implicit)
                positionTable.expandGroup(options.get(0));
            }
            // expand new group tree node
            positionTable.expandGroup(group);
        }
    }

    private static boolean enablePrimary(SPTarget target, TargetEnvironment env) {
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
        for (GuideProbe guider : illegalSet) {
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
        final PositionType[] ptA;
        ptA = new PositionType[2 + guiders.size()];

        int index = 0;
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

        // TODO: Why is this doing nothing???
        // final boolean isBags = _curPos != null && env.getGroups().exists(gg -> gg.getAllContaining(_curPos).exists(gpt -> gpt.getBagsResult().exists(_curPos::equals)));
        // _w.detailEditor.allEditorsJava().stream().filter(ed -> _w.detailEditor.curDetailEditorJava().forall(cur -> cur != ed)).forEach(ed -> updateEnabledState(new Component[]{ed}, !isBags));
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

    private final TelescopePosWatcher posWatcher = new TelescopePosWatcher() {
        public void telescopePosUpdate(WatchablePos tp) {
            if (tp != _curPos) {
                // This shouldn't happen ...
                System.out.println(getClass().getName() + ": received a position " +
                        " update for a position other than the current one: " + tp);
                return;
            }
            refreshAll();
            updateGuiding();
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener removeListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
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
                env = env.removeGroup(_curGroup);
                _curGroup = primary;
            }
            getDataObject().setTargetEnvironment(env);
            final SPTarget selTarget = TargetSelection.get(env, getNode());
            if (env.getTargets().contains(selTarget)) {
                if (_curPos != null) _curPos.deleteWatcher(posWatcher);
                _curPos = selTarget;
                if (_curPos != null) {
                    _curPos.addWatcher(posWatcher);
                    refreshAll();
                    updateRemovePrimaryButtonsAndDetailEditor(env);
                }
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener manualGuideStarListener = evt -> {
        try {
            final TelescopePosEditor tpe = TpeManager.open();
            tpe.reset(getNode());
            tpe.getImageWidget().manualGuideStarSearch();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener duplicateListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
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
                TargetEnvironment env = dataObject.getTargetEnvironment();

                // See if it is a guide star and duplicate it in the correct GuideTargets list.
                boolean duplicated = false;
                env.getOrCreatePrimaryGuideGroup();
                final List<GuideGroup> groups = new ArrayList<>();
                for (GuideGroup group : env.getGroups()) {
                    for (GuideProbeTargets gt : group) {
                        if (gt.getTargets().contains(target)) {
                            group = group.put(gt.addManualTarget(newTarget));
                            duplicated = true;
                            break;
                        }
                    }
                    groups.add(group);
                }
                if (duplicated) {
                    // Update groups list
                    env = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
                } else {
                    // Add as a user target
                    env = env.setUserTargets(env.getUserTargets().append(newTarget));
                }

                dataObject.setTargetEnvironment(env);
            } else {
                final GuideGroup group = TargetSelection.getGuideGroup(dataObject.getTargetEnvironment(), obsComponent);
                if (group != null) {
                    TargetEnvironment env = dataObject.getTargetEnvironment();
                    final List<GuideGroup> groups = new ArrayList<>();
                    groups.addAll(env.getGroups().toList());
                    groups.add(group.cloneTargets());
                    env = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
                    dataObject.setTargetEnvironment(env);
                    // save/restore tree state will leave last group closed, since there is one more, so expand it here
                    _w.positionTable.expandGroup(groups.get(groups.size() - 1));
                }
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener copyListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            final Option<TargetClipboard> opt = TargetClipboard.copy(getDataObject().getTargetEnvironment(), getNode());
            if (opt.isEmpty()) return;
            clipboard = opt.getValue();
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener pasteListener = new ActionListener() {
        private void pasteSelectedPosition(ISPObsComponent obsComponent, TargetObsComp dataObject) {
            if (clipboard != null) {
                clipboard.paste(obsComponent, dataObject);
            }
        }
        public void actionPerformed(ActionEvent e) {
            if (_curPos != null) {
                pasteSelectedPosition(getNode(), getDataObject());
            } else if (_curGroup != null) {
                pasteSelectedPosition(getNode(), getDataObject());
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ActionListener primaryListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            _w.positionTable.updatePrimaryStar();
        }
    };

}

interface PositionType {
    boolean isAvailable();
    void morphTarget(TargetObsComp obsComp, SPTarget target);
    boolean isMember(TargetEnvironment env, SPTarget target);
}

enum BasePositionType implements PositionType {
    instance;

    public boolean isAvailable() {
        return true;
    }

    public void morphTarget(TargetObsComp obsComp, SPTarget target) {
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;
        env = env.removeTarget(target);

        final SPTarget base = env.getBase();

        final GuideEnvironment genv = env.getGuideEnvironment();
        final ImList<SPTarget> user = env.getUserTargets().append(base);

        env = TargetEnvironment.create(target, genv, user);
        obsComp.setTargetEnvironment(env);
    }

    public boolean isMember(TargetEnvironment env, SPTarget target) {
        return (env.getBase() == target);
    }

    public String toString() {
        return TargetEnvironment.BASE_NAME;
    }
}

class GuidePositionType implements PositionType {
    private final GuideProbe guider;
    private final boolean available;

    GuidePositionType(GuideProbe guider, boolean available) {
        this.guider = guider;
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public void morphTarget(TargetObsComp obsComp, SPTarget target) {
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;
        env = env.removeTarget(target);


        final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
        final GuideProbeTargets gt = gtOpt.map(gpt -> gpt.addManualTarget(target)).
                getOrElse(GuideProbeTargets.create(guider, target)).withExistingPrimary(target);

        env = env.putPrimaryGuideProbeTargets(gt);
        obsComp.setTargetEnvironment(env);
    }

    public boolean isMember(TargetEnvironment env, SPTarget target) {
        return env.getGroups().exists(group -> group.get(guider).exists(gt -> gt.getTargets().contains(target)));
    }

    public String toString() {
        return guider.getKey();
    }
}

enum UserPositionType implements PositionType {
    instance;

    public boolean isAvailable() {
        return true;
    }

    public void morphTarget(TargetObsComp obsComp, SPTarget target) {
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;

        env = env.removeTarget(target);
        env = env.setUserTargets(env.getUserTargets().append(target));
        obsComp.setTargetEnvironment(env);
    }

    public boolean isMember(TargetEnvironment env, SPTarget target) {
        return env.getUserTargets().contains(target);
    }

    public String toString() {
        return TargetEnvironment.USER_NAME;
    }
}


