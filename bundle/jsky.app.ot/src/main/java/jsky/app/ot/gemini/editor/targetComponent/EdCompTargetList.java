package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.ags.api.AgsRegistrar;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.ags.*;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.NonSiderealTarget;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.core.Target;
import edu.gemini.spModel.gemini.ghost.GhostAsterism;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPCoordinates;
import edu.gemini.spModel.target.SPSkyObject;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import jsky.app.ot.OTOptions;
import jsky.app.ot.ags.*;
import jsky.app.ot.editor.OtItemEditor;
import jsky.app.ot.gemini.editor.targetComponent.tableSelection.*;
import jsky.app.ot.tpe.GuideStarSupport;
import jsky.app.ot.tpe.TelescopePosEditor;
import jsky.app.ot.tpe.TpeManager;
import jsky.util.gui.Resources;
import jsky.util.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.time.Instant;
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

    // The current selection. This will not be defined until after init is run.
    private TableSelection selection;

    // A collection of the JMenuItems that allow the addition of guide stars.
    private final Map<GuideProbe,Component> _guideStarAdders = new HashMap<>();


    public EdCompTargetList() {

        _w = new TelescopeForm(this);

        _w.addComponentListener(new ComponentAdapter() {
            public void componentShown(final ComponentEvent componentEvent) {
                toggleAgsGuiElements();
            }
        });

        _w.removeButton    .addActionListener(removeListener);
        _w.copyButton      .addActionListener(copyListener);
        _w.pasteButton     .addActionListener(pasteListener);
        _w.duplicateButton .addActionListener(duplicateListener);
        _w.primaryButton   .addActionListener(primaryListener);
        _w.linkBaseToTarget.addActionListener(linkedBaseListener);

        _w.guidingControls.manualGuideStarButton().peer().addActionListener(manualGuideStarListener);
        _w.guidingControls.autoGuideStarGuiderSelector().addSelectionListener(strategy ->
                AgsStrategyUtil.setSelection(getContextObservation(), strategy)
        );

        _w.guideGroupName.addWatcher(new TextBoxWidgetWatcher() {
            @Override
            public void textBoxKeyPress(final TextBoxWidget tbwe) {
                SwingUtilities.invokeLater(() -> selectedGroup().map(igg -> {
                            final String newName = _w.guideGroupName.getText();
                            final TargetObsComp toc = getDataObject();
                            final int idx = igg.index();
                            final GuideGroup newGroup = igg.group().setName(newName);
                            final TargetEnvironment oldEnv = toc.getTargetEnvironment();
                            toc.setTargetEnvironment(oldEnv.setGroup(idx, newGroup));
                            _w.guideGroupName.requestFocus();
                            return new IndexedGuideGroup(idx, newGroup);
                        }).foreach(EdCompTargetList.this::setSelectionToGroup)
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

        selection = TableNoSelection.instance;

        BagsManager.addBagsStateListener((key, oldStatus, newStatus) -> {
            if (Optional.ofNullable(getContextObservation()).map(co -> key.equals(co.getNodeKey())).orElse(false)) {
                updateTargetFeedback();
            }
       });
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
            updateCoordinateEditorEnabledState(enabled);
        }

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.setEnabled(enabled && inst != null);

    }

    private void updateDetailEditorEnabledState(final boolean enabled) {
        // Update enabled state for all detail widgets.
        _w.detailEditor.allEditorsJava().forEach(ed -> updateEnabledState(new Component[]{ed}, enabled));
    }

    private void updateCoordinateEditorEnabledState(final boolean enabled) {
        updateEnabledState(new Component[]{_w.coordinateEditor}, enabled);
    }

    /**
     * Simplifications for dealing with the current selection.
     */
    private Option<SPTarget> selectedTarget() {
        if (selection instanceof TableTargetSelection)
            return new Some<>(((TableTargetSelection) selection).getTarget());
        else
            return None.instance();
    }

    private Option<IndexedGuideGroup> selectedGroup() {
        if (selection instanceof TableGroupSelection)
            return new Some<>(((TableGroupSelection) selection).getGroup());
        else
            return None.instance();
    }

    private Option<SPCoordinates> selectedCoordinates() {
        if (selection instanceof TableCoordinateSelection)
            return new Some<>(((TableCoordinateSelection) selection).getCoordinates());
        else
            return None.instance();
    }

    /**
     * Extract the base position out of the asterism.
     * It could be a target or a sky position.
     * Due to type inference issues with map, we have to do this in a horrible way.
     */
    private ImEither<SPTarget, SPCoordinates> basePosition() {
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        final Asterism a            = env.getAsterism();

        switch (a.asterismType()) {
            case Single:
                return new Left<>(((Asterism.Single) a).t());
            case GhostSingleTarget:
                final GhostAsterism.SingleTarget gsa = (GhostAsterism.SingleTarget) a;
                return ImOption.fromScalaOpt(gsa.overriddenBase()).toRight(() -> gsa.target().spTarget());
            case GhostDualTarget:
                // This case is more complicated as it always returns Right.
                final GhostAsterism.DualTarget gda = (GhostAsterism.DualTarget) a;
                if (gda.overriddenBase().isDefined()) return new Right<>(gda.overriddenBase().get());
                else {
                    final Option<Instant> sbi = ((SPObservation)getContextObservation().getDataObject())
                            .getSchedulingBlockStart()
                            .map(Instant::ofEpochMilli);
                    final Option<Coordinates> cOpt = ImOption.fromScalaOpt(gda.basePosition(ImOption.toScalaOpt(sbi)));
                    return new Right<>(cOpt.map(SPCoordinates::new).getOrElse(SPCoordinates::new));
                }
            case GhostTargetPlusSky:
                final GhostAsterism.TargetPlusSky gtsa = (GhostAsterism.TargetPlusSky) a;
                return ImOption.fromScalaOpt(gtsa.overriddenBase()).toRight(() -> gtsa.target().spTarget());
            case GhostSkyPlusTarget:
                final GhostAsterism.SkyPlusTarget gsta = (GhostAsterism.SkyPlusTarget) a;
                return ImOption.fromScalaOpt(gsta.overriddenBase()).toRight(() -> gsta.target().spTarget());
            case GhostHighResolutionTarget:
                final GhostAsterism.HighResolutionTarget ghta = (GhostAsterism.HighResolutionTarget) a;
                return ImOption.fromScalaOpt(ghta.overriddenBase()).toRight(() -> ghta.target().spTarget());
            case GhostHighResolutionTargetPlusSky:
                final GhostAsterism.HighResolutionTargetPlusSky ghtsa = (GhostAsterism.HighResolutionTargetPlusSky) a;
                return ImOption.fromScalaOpt(ghtsa.overriddenBase()).toRight(() -> ghtsa.target().spTarget());
        }

        // We shouldn't get here.
        throw new RuntimeException("The asterism type could not be determined.");
    }

    /**
     * Auxiliary method to determine if the current selection is base target.
     */
    private boolean selectionIsBaseTarget() {
        final ISPObsComponent node = getContextTargetObsComp();
        if (TargetSelection.getIndex(node).forall(i -> i != 0))
            return false;
        final Option<SPTarget> t = basePosition().toOptionLeft();
        return selectedTarget().exists(st -> t.exists(st::equals));
    }

    /**
     * Ensure that there is at most one selection for this node in TargetSelection.
     */
    static void checkTargetSelectionConsistency(final TargetEnvironment env,
                                                final ISPObsComponent node) {
        final Option<SPTarget> tOpt = TargetSelection.getTargetForNode(env, node);
        final Option<SPCoordinates> cOpt = TargetSelection.getCoordinatesForNode(env, node);
        final Option<IndexedGuideGroup> gOpt = TargetSelection.getIndexedGuideGroupForNode(env, node);
        if ((tOpt.isDefined() && (cOpt.isDefined() || gOpt.isDefined()))
                || (cOpt.isDefined() && gOpt.isDefined()))
            throw new RuntimeException("Illegal node state: more than one selection recorded for node");
    }

    /**
     * Ensure that there is at most one selection for the current node in TargetSelection.
     */
    void checkTargetSelectionConsistency() {
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        final ISPObsComponent  node = getNode();
        checkTargetSelectionConsistency(env, node);
    }

    /**
     * Auxiliary method to determine if the current selection is base coordinates.
     */
    private boolean selectionIsBaseCoordinates() {
        final ISPObsComponent node = getContextTargetObsComp();
        return selectionIsCoordinates() && TargetSelection.getIndex(node).exists(i -> i == 0);
    }

    /**
     * Put them together to determine if the base position is selected.
     */
    private boolean selectionIsBasePosition() {
        return selectionIsBaseTarget() || selectionIsBaseCoordinates();
    }

    /**
     * Auxiliary method to determine if the current selection is the auto group.
     */
    private boolean selectionIsAutoGroup() {
        return selectedGroup().exists(igg -> igg.group().isAutomatic());
    }

    /**
     * Auxiliary method to determine if the current selection is a manual group.
     */
    private boolean selectionIsManualGroup() {
        return selectedGroup().exists(igg -> igg.group().isManual());
    }

    /**
     * Auxiliary method to determine if the current selection is a target that belongs to the auto group.
     */
    private boolean selectionIsAutoTarget() {
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        return selectedTarget().exists(t ->
                env.getGuideEnvironment().getOptions().exists(gg -> gg.isAutomatic() && gg.containsTarget(t)));
    }

    /**
     * Auxiliary method to determine if the current selection is a target that belongs to a manual group.
     */
    private boolean selectionIsManualTarget() {
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        return selectedTarget().exists(t ->
                env.getGuideEnvironment().getOptions().exists(gg -> gg.isManual() && gg.containsTarget(t)));
    }

    /**
     * Auxiliary method to determine if the current selection is a guide target.
     */
    private boolean selectionIsGuideTarget() {
        return selectedTarget().isDefined() && !(selectionIsBasePosition() || selectionIsUserTarget());
    }

    /**
     * Auxiliary method to determine if the current selection is a user target.
     */
    private boolean selectionIsUserTarget() {
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        return selectedTarget().exists(env::isUserPosition);
    }

    /**
     * Auxiliary method to determine if the current selection is coordinates.
     */
    private boolean selectionIsCoordinates() {
        return selectedCoordinates().isDefined();
    }

    /**
     * Auxiliary method to determine if the base coordinates are explicitly defined.
     */
    private boolean isGhostAsterismWithExplicitlyDefinedBaseCoordinates() {
        // Editable if coords are sky position, or base is defined.
        final TargetEnvironment env = getDataObject().getTargetEnvironment();
        final Asterism a = env.getAsterism();
        return (a instanceof GhostAsterism) && ((GhostAsterism)a).overriddenBase().isDefined();
    }

    /**
     * Set the selection.
     */
    private void setSelectionToTarget(final SPTarget t) {
        selection = new TableTargetSelection(t);
    }

    private void setSelectionToGroup(final IndexedGuideGroup igg) {
        selection = new TableGroupSelection(igg);
    }

    private void setSelectionToCoordinates(final SPCoordinates c) {
        selection = new TableCoordinateSelection(c);
    }

    /**
     * Update the UI components when a target becomes selected.
     */
    private void updateUIForTarget() {
        if (selectedTarget().isEmpty()) return;

        final boolean editable      = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        final boolean notBase       = !selectionIsBasePosition();
        final boolean notAutoTarget = !selectionIsAutoTarget();
        final boolean isGuideStar   = selectionIsGuideTarget();

        // We allow the primary button to be set for auto targets as it will flag the auto group as primary.
        _w.removeButton.setEnabled (editable && notBase);
        _w.primaryButton.setEnabled(editable && isGuideStar);
        _w.pasteButton.setEnabled(editable && notAutoTarget);
        _w.duplicateButton.setEnabled(editable && notAutoTarget);
        updateDetailEditorEnabledState(editable && notAutoTarget);
        _w.tag.setEnabled(notAutoTarget && notBase);
    }

    /**
     * Update the UI components when a group becomes selected.
     */
    private void updateUIForGroup() {
        if (selectedGroup().isEmpty()) return;

        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        final boolean notAuto   = !selectionIsAutoGroup();
        _w.removeButton.setEnabled(editable && notAuto);
        _w.primaryButton.setEnabled(editable);
        _w.pasteButton.setEnabled(editable && notAuto);
        _w.duplicateButton.setEnabled(editable);
        _w.tag.setEnabled(false);
    }

    /**
     * Update the UI components when coordinates become selected.
     */
    private void updateUIForCoordinates() {
        if (selectedCoordinates().isEmpty()) return;

        final boolean editable = OTOptions.areRootAndCurrentObsIfAnyEditable(getProgram(), getContextObservation());
        _w.removeButton.setEnabled(false);
        _w.primaryButton.setEnabled(false);
        _w.pasteButton.setEnabled(editable);
        _w.duplicateButton.setEnabled(false);
        _w.tag.setEnabled(false);
        updateCoordinateEditorEnabledState(editable &&
                (isGhostAsterismWithExplicitlyDefinedBaseCoordinates() || !selectionIsBaseCoordinates()));
    }

    /**
     * Update the menu items that allow the addition of guide stars. These must be disabled for the automatic group
     * or if we are currently on the base or a user target.
     *
     * REL-2715 similarly demands that if there is only the auto guide group that they be enabled, with a new guide
     * group created automatically.
     */
    private void updateGuideStarAdders() {
        // If there is only one group, then it must be the auto group.
        final boolean onlyAutoGroup = getContextTargetEnv().getGuideEnvironment().manualGroups().isEmpty();
        final boolean enabled       = onlyAutoGroup || selectionIsManualGroup() || selectionIsManualTarget();
        _guideStarAdders.forEach((gp, comp) -> comp.setEnabled(enabled));
    }

    @Override public JPanel getWindow() {
        return _w;
    }

    // Common code to manage the position watcher on the current position around an action that modifies it.
    // We also check for the presence of a specified target in the target environment.
    private boolean manageCurPosIfEnvContainsTarget(final SPTarget target, final Runnable action) {
        final TargetObsComp obsComp = getDataObject();
        if (obsComp == null) return false;

        final TargetEnvironment env = obsComp.getTargetEnvironment();
        if (env == null || !env.getTargets().contains(target)) return false;

        // If current selection is a target or coordinates, remove the posWatcher.
        selectedTarget().foreach(t -> t.deleteWatcher(posWatcher));
        selectedCoordinates().foreach(c -> c.deleteWatcher(posWatcher));

        action.run();

        // If the current selection is a target or coordinates, then readd the posWatcher and refresh the UI.
        selectedTarget().foreach(t -> {
            t.addWatcher(posWatcher);
            refreshAll();
        });
        selectedCoordinates().foreach(c -> {
            c.addWatcher(posWatcher);
            refreshAll();
        });
        return true;
    }

    /**
     * Determine whether or not the auto group has changed for a specific observation.
     */
    private boolean isBagsUpdate(final ISPObsComponent oldObsComp, final TargetObsComp oldTOC) {
        final TargetObsComp newTOC = getDataObject();

        // If either of the TargetObsComps are null, this was not triggered by an auto group change.
        if (oldTOC == null || newTOC == null) {
            return false;
        }

        // If either of the node keys are null, or they are defined but different, this is not an auto group change.
        final SPNodeKey oldNodeKey = ImOption.apply(oldObsComp).map(ISPObsComponent::getNodeKey).getOrNull();
        final SPNodeKey newNodeKey = ImOption.apply(getNode()).map(ISPObsComponent::getNodeKey).getOrNull();
        if (oldNodeKey == null || newNodeKey == null) {
            return false;
        }
        if (!oldNodeKey.equals(newNodeKey)) {
            return false;
        }

        // REL-2822
        // Okay if the scheduling block changed then it isn't just a bags update.
        // Of course we don't know whether the scheduling block changed because
        // that is kept in the observation node itself.  We can however look at
        // the ephemeris for each nonsidereal target.
        // Sorry, this is a terrible hack to make bulk scheduling block updates
        // be considered enough of a change to refresh the target environment
        // editor when it is selected..
        if (!getNonSiderealTargets(oldTOC).equals(getNonSiderealTargets(newTOC))) {
            return false;
        }

        // Check to see if the old and new auto groups contain the same targets, in which case, they are
        // considered equal.
        final GuideGroup oldAutoGroup = oldTOC.getTargetEnvironment().getGuideEnvironment().automaticGroup();
        final GuideGroup newAutoGroup = newTOC.getTargetEnvironment().getGuideEnvironment().automaticGroup();
        final Map<GuideProbe,Set<Target>> oldAutoTargets = extractGroupTargets(oldAutoGroup);
        final Map<GuideProbe,Set<Target>> newAutoTargets = extractGroupTargets(newAutoGroup);
        return !oldAutoTargets.equals(newAutoTargets);
    }

    private static ImList<NonSiderealTarget> getNonSiderealTargets(final TargetObsComp toc) {
        return toc.getTargetEnvironment()
                  .getTargets()
                  .flatMap(spt -> ImOption.fromScalaOpt(spt.getNonSiderealTarget()).toImList());
    }

    /**
     * Extract the targets by guide probe for a given guide group.
     */
    private static Map<GuideProbe,Set<Target>> extractGroupTargets(final GuideGroup group) {
        final Map<GuideProbe,Set<Target>> targets = new HashMap<>();
        group.getAll().foreach(gpt -> {
            final GuideProbe probe = gpt.getGuider();
            final Set<Target> targetSet = new HashSet<>(gpt.getOptions().map(SPTarget::getTarget).toList());
            targets.put(probe, targetSet);
        });
        return targets;
    }

    @Override public void init(final ISPObsComponent oldNode, final TargetObsComp oldTOC) {
        final boolean isBagsUpdate = isBagsUpdate(oldNode, oldTOC);
        final TargetObsComp newTOC = getDataObject();

        // If only the auto group has changed, we do not want to use a completely reset target environment, as this
        // will contain clones of all targets and thus will require the target detail editors to be reset to use these
        // new clones: this is bad as it will reformat the RA and Dec fields and thus any modifications being made
        // to them will be lost as they are repopulated and the caret moved to the end.
        if (isBagsUpdate) {
            // Determine if there was an auto group in the old env and what the primary index was.
            final TargetEnvironment oldEnv     = oldTOC.getTargetEnvironment();
            final GuideEnvironment oldGuideEnv = oldEnv.getGuideEnvironment();
            final int primaryIdx               = oldGuideEnv.getPrimaryIndex();
            final GuideGroup autoGroup         = newTOC.getTargetEnvironment().getGuideEnvironment().automaticGroup();

            final GuideEnvironment newGuideEnv = oldGuideEnv.setAutomaticGroup(autoGroup).setPrimaryIndex(primaryIdx);
            final TargetEnvironment newEnv     = oldEnv.setGuideEnvironment(newGuideEnv);
            newTOC.setTargetEnvironment(newEnv);
        }

        final ISPObsComponent node = getContextTargetObsComp();
        TargetSelection.listenTo(node, selectionListener);

        newTOC.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, primaryButtonUpdater);
        newTOC.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, guidingPanelUpdater);

        // Note that in TPE, when this is issued, the selection does not seem to change in the table.
        final TargetEnvironment env = newTOC.getTargetEnvironment();

        // If more than the auto group has changed, we need to maintain the selection, or default to the base.
        if (!isBagsUpdate) {
            checkTargetSelectionConsistency();
            final boolean targetSet = TargetSelection.getTargetForNode(env, node).map(t ->
                    manageCurPosIfEnvContainsTarget(t, () -> setSelectionToTarget(t))
            ).getOrElse(false);
            final boolean groupSet = TargetSelection.getIndexedGuideGroupForNode(env, node).map(igg -> {
                setSelectionToGroup(igg);
                return true;
            }).getOrElse(false);
            final boolean coordsSet = TargetSelection.getCoordinatesForNode(env, node).map(c -> {
                setSelectionToCoordinates(c);
                return true;
            }).getOrElse(false);
            if (!(targetSet || groupSet || coordsSet)) {
                setSelectionToTarget(env.getAsterism().allSpTargetsJava().head());
            }
        }

        final SPInstObsComp inst = getContextInstrumentDataObject();
        _w.newMenu.removeAll();
        _guideStarAdders.clear();

        if (inst == null) {
            _w.newMenu.setEnabled(false);
        } else {
            _w.newMenu.setEnabled(true);
            if (inst.hasGuideProbes()) {
                final List<GuideProbe> guiders = new ArrayList<>(GuideProbeUtil.instance.getAvailableGuiders(getContextObservation()));
                guiders.sort(GuideProbe.KeyComparator.instance);
                guiders.forEach(probe -> {
                    final JMenuItem guideStarAdder = new JMenuItem(probe.getKey()) {{
                        addActionListener(new AddGuideStarAction(newTOC, probe, _w.positionTable));
                    }};
                    _w.newMenu.add(guideStarAdder);
                    _guideStarAdders.put(probe, guideStarAdder);
                });
                updateGuideStarAdders();
            }

            for (UserTarget.Type t : UserTarget.Type.values()) {
                _w.newMenu.add(new JMenuItem(t.displayName) {{
                    addActionListener(new AddUserTargetAction(t, newTOC, _w.positionTable));
                }});
            }

            if (inst.hasGuideProbes()) {
                _w.newMenu.addSeparator();
                _w.newMenu.add(new JMenuItem("Guide Group") {{
                    addActionListener(new AddGroupAction(newTOC, _w.positionTable));
                }});
            }
        }

        _w.positionTable.reinit(newTOC, isBagsUpdate);


        // The linkBaseToTarget checkbox is:
        // 1. visible if the asterism type is a GHOST asterism; and
        // 2. checked if the base position is None, i.e. linked to the science targets.
        final Asterism a = env.getAsterism();
        _w.linkBaseToTarget.removeActionListener(linkedBaseListener);
        if (a.asterismType() == AsterismType.Single) {
            _w.linkBaseToTarget.setVisible(false);
        } else {
            final GhostAsterism ga = ((GhostAsterism) a);
            _w.linkBaseToTarget.setVisible(true);
            _w.linkBaseToTarget.setSelected(ga.overriddenBase().isEmpty());
        }
        _w.linkBaseToTarget.addActionListener(linkedBaseListener);


        _w.guidingControls.manualGuideStarButton().peer().setVisible(GuideStarSupport.supportsManualGuideStarSelection(getNode()));
        updateGuiding();
        _agsPub.watch(ImOption.apply(getContextObservation()));

        final Option<Site> site = ObsContext.getSiteFromObservation(getContextObservation());
        final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);
        numberFormatter.setMaximumFractionDigits(2);
        numberFormatter.setMaximumIntegerDigits(3);
        _w.schedulingBlock.init(this, site, numberFormatter, () -> {
            final TargetObsComp toc      = getDataObject();
            final TargetEnvironment env1 = toc.getTargetEnvironment();
            updateTargetDetails(env1);
            _w.positionTable.reinit(toc, false);
        });

        // If this update is due to BAGS updating the auto group, then we will
        // skip the target detail editor reset.  Otherwise we could be in the
        // middle of typing in an RA or dec and we'd reset the text field,
        // causing the cursor to jump to the end and generating frustration.
        // If the update comes from anything other than BAGS though, we need to
        // show the target details.  (TODO: Ideally we'd update the target
        // details for a BAGS update too if we're parked on a BAGS target in the
        // target table.)
        if (!isBagsUpdate) {
            updateTargetDetails(newTOC.getTargetEnvironment());
        }
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
        updateTargetFeedback(env);
    }

    private void updateTargetFeedback() {
        updateTargetFeedback(getDataObject().getTargetEnvironment());
    }

    private void updateTargetFeedback(final TargetEnvironment env) {
        final Option<ObsContext> ctx = getObsContext(env);
        final ISPObsComponent node   = getContextTargetObsComp();
        final SPTarget target        = TargetSelection.getTargetForNode(env, node)
                .getOrElse(env.getAsterism().allSpTargets().head());
        _w.detailEditor.targetFeedbackEditor().edit(ctx, target, node);
    }

    private void refreshAll() {
        // We will either have coordinates or a target selected at this point.
        _w.guideGroupPanel.setVisible(false);
        _w.coordinateEditor.setVisible(selectionIsCoordinates());
        _w.detailEditor.setVisible(!selectionIsCoordinates());

        // Get all the legally available guiders in the current context.
        final Set<GuideProbe> avail = GuideProbeUtil.instance.getAvailableGuiders(getContextObservation());
        final Set<GuideProbe> guiders = new HashSet<>(avail);
        final TargetEnvironment env = getDataObject().getTargetEnvironment();

        // Get the set of guiders that are referenced but not legal in this context, if any.  Any
        // "available" guider is legal, anything left over is referenced but not really available.
        final Set<GuideProbe> illegalSet = env.getPrimaryGuideGroup().getReferencedGuiders();
        illegalSet.removeAll(avail);

        GuideProbe illegal = null;
        final Option<SPTarget> tOpt = selectedTarget();
        for (final GuideProbe guider : illegalSet) {
            final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            if (tOpt.exists(gtOpt.getValue().getTargets()::contains)) {
                illegal = guider;
                guiders.add(guider);
            }
        }

        // Sort the list of guiders.
        final List<GuideProbe> guidersList = new ArrayList<>(guiders);
        guidersList.sort(GuideProbe.KeyComparator.instance);

        // Make a list of PositionTypes that are legal in the current observation context.
        int index = 0;
        final PositionType[] ptA = new PositionType[1 + guiders.size() + UserTarget.Type.values().length];
        ptA[index++] = BasePositionType.instance;
        for (GuideProbe guider : guidersList) {
            final boolean guiderNotIllegal = !guider.equals(illegal);
            ptA[index++] = new GuidePositionType(guider, guiderNotIllegal, _w.positionTable);
        }
        for (UserTarget.Type t : UserTarget.Type.values()) {
            ptA[index++] = new UserPositionType(t);
        }

        // We don't allow the tag to be changed for the base position or for coordinates.
        _w.tag.removeActionListener(_tagListener);
        _w.tag.setModel(new DefaultComboBoxModel<>(ptA));
        _w.tag.setEnabled(isEnabled() && !selectionIsCoordinates() && !selectionIsBaseTarget());
        _w.tag.addActionListener(_tagListener);
        _w.tag.setRenderer(tagRenderer);
        showTargetTag();

        // Update the editors for targets and coordinates.
        // This only acts on the one selected.
        if (selectionIsCoordinates())
            updateCoordinateDetails(env);
        else
            updateTargetDetails(env);

        // Set the status of the buttons and detail editors.
        // We do this for both coordinates and targets, as the methods check which is selected.
        updateUIForTarget();
        updateUIForCoordinates();
        updateGuideStarAdders();
    }

    private void updateTargetDetails(final TargetEnvironment env) {
        selectedTarget().foreach(t -> _w.detailEditor.edit(getObsContext(env), t, getNode()));
    }

    private void updateCoordinateDetails(final TargetEnvironment env) {
        selectedCoordinates().foreach(c -> _w.coordinateEditor.edit(getObsContext(env), c, getNode()));
    }

    private void showTargetTag() {
        selectedTarget().foreach(t -> {
            final TargetEnvironment env = getDataObject().getTargetEnvironment();

            // We hide the target tag if we are working with GHOST, as it is
            // unclear how the morph things in that case.
            boolean visible = !(env.getAsterism() instanceof  GhostAsterism);
            _w.tag.setVisible(visible);

            for (int i = 0; i < _w.tag.getItemCount(); ++i) {
                final PositionType pt = _w.tag.getItemAt(i);
                if (pt.isMember(env, t)) {
                    _w.tag.removeActionListener(_tagListener);
                    _w.tag.setSelectedIndex(i);
                    _w.tag.addActionListener(_tagListener);
                    break;
                }
            }
        });
    }


    /**
     * Listeners and watchers.
     */
    private static abstract class AddAction implements ActionListener {
        protected final TargetObsComp obsComp;
        final TelescopePosTableWidget positionTable;

        AddAction(final TargetObsComp obsComp, final TelescopePosTableWidget positionTable) {
            this.obsComp = obsComp;
            this.positionTable = positionTable;
        }
    }

    /**
     * Action that handles adding a new guide star when a probe is picked from the add menu.
     * This should ONLY be permitted if a current non-auto group is selected.
     */
    private final class AddGuideStarAction extends AddAction {
        private final GuideProbe probe;

        AddGuideStarAction(final TargetObsComp obsComp, final GuideProbe probe, final TelescopePosTableWidget positionTable) {
            super(obsComp, positionTable);
            this.probe = probe;
        }

        @Override public void actionPerformed(final ActionEvent actionEvent) {
            final TargetEnvironment env = obsComp.getTargetEnvironment();

            // REL-2715 demands that we allow manual guide groups to be automatically created if only the auto
            // group is present. This requires some ugly hacking.
            final Option<Tuple2<TargetEnvironment,IndexedGuideGroup>> resultOpt = positionTable.getSelectedGroupOrParentGroup(env)
                    .filter(igg -> !igg.group().isAutomatic())
                    .map(igg -> (Tuple2<TargetEnvironment,IndexedGuideGroup>) new Pair<>(env, igg))
                    .orElse(() -> appendNewGroup(obsComp.getTargetEnvironment(), positionTable));

            resultOpt.foreach(result -> {
                final TargetEnvironment newEnv = result._1();
                final IndexedGuideGroup igg    = result._2();
                final SPTarget target          = new SPTarget();
                addTargetToGroup(newEnv, igg, probe, target, obsComp);
                positionTable.selectTarget(target);
                SwingUtilities.invokeLater(EdCompTargetList.this::showTargetTag);
            });
        }
    }

    private static class AddUserTargetAction extends AddAction {
        private final UserTarget.Type userTargetType;

        AddUserTargetAction(final UserTarget.Type utt, final TargetObsComp obsComp, final TelescopePosTableWidget positionTable) {
            super(obsComp, positionTable);
            userTargetType = utt;
        }

        @Override public void actionPerformed(final ActionEvent actionEvent) {
            final TargetEnvironment env = obsComp.getTargetEnvironment();
            final SPTarget target = new SPTarget();
            final UserTarget   ut = new UserTarget(userTargetType, target);
            final TargetEnvironment newEnv = env.setUserTargets(env.getUserTargets().append(ut));
            obsComp.setTargetEnvironment(newEnv);
            positionTable.selectTarget(target);
        }
    }

    private static class AddGroupAction extends AddAction {
        AddGroupAction(final TargetObsComp obsComp, final TelescopePosTableWidget positionTable) {
            super(obsComp, positionTable);
        }

        @Override public void actionPerformed(final ActionEvent actionEvent) {
            final Option<Tuple2<TargetEnvironment,IndexedGuideGroup>> resultOpt =
                    appendNewGroup(obsComp.getTargetEnvironment(), positionTable);
            resultOpt.foreach(result -> {
                final TargetEnvironment newEnv = result._1();
                final IndexedGuideGroup igg    = result._2();
                obsComp.setTargetEnvironment(newEnv);
                positionTable.selectGroup(igg);
            });
        }
    }

    /**
     * Append a guide group to the collection of guide groups and select it as the primary.
     * If the guide group is not able to be appended, returns None.
     */
    static Option<Tuple2<TargetEnvironment,IndexedGuideGroup>> appendNewGroup(final TargetEnvironment env,
                                                                              final TelescopePosTableWidget positionTable) {
        // Ensure we are working with a guide env with a primary group.
        final GuideEnvironment ge     = env.getGuideEnvironment();
        final GuideGroup primaryGroup = ge.getPrimary();

        final ImList<GuideGroup> oldGroups = ge.getOptions();
        final int newGroupIdx              = oldGroups.size();
        final GuideGroup newGroup          = GuideGroup.create(GuideGroup.ManualGroupDefaultName());
        final IndexedGuideGroup igg        = new IndexedGuideGroup(newGroupIdx, newGroup);
        final ImList<GuideGroup> newGroups = oldGroups.append(newGroup);

        // Make and return a new primary group and target environment, if possible.
        final TargetEnvironment         newEnv;
        if (positionTable.confirmGroupChange(primaryGroup, newGroup)) {
            newEnv = env.setGuideEnvironment(ge.setOptions(newGroups).setPrimaryIndex(newGroupIdx));
            return new Some<>(new Pair<>(newEnv, igg));
        } else {
            return None.instance();
        }
    }

    /**
     * Takes a target environment and indexed guide group, and adds the target to the group.
     */
    static void addTargetToGroup(final TargetEnvironment env, final IndexedGuideGroup igg,
                                 final GuideProbe probe, final SPTarget target,
                                 final TargetObsComp obsComp) {
        final Option<GuideProbeTargets> gptOpt = igg.group().get(probe);
        final int groupIndex                   = igg.index();

        final GuideProbeTargets newGpt = gptOpt.map(gpt -> gpt.update(OptionsList.UpdateOps.appendAsPrimary(target)))
                .getOrElse(() -> GuideProbeTargets.create(probe, target));
        final TargetEnvironment newEnv = env.setGuideEnvironment(env.getGuideEnvironment().putGuideProbeTargets(groupIndex, newGpt));
        obsComp.setTargetEnvironment(newEnv);
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
        if (tp != selectedTarget().getOrNull() && tp != selectedCoordinates().getOrNull()) {
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
            selectedTarget().foreach(t -> {
                pt.morphTarget(getDataObject(), t);
                if (getDataObject() != null) {
                    final TargetEnvironment env = getDataObject().getTargetEnvironment();
                    manageCurPosIfEnvContainsTarget(t, () -> _w.detailEditor.edit(getObsContext(env), t, getNode()));
                }
            });
        }
    };

    private void setSelectionFromNode() {
        final ISPObsComponent node = getContextTargetObsComp();
        final TargetEnvironment env = getDataObject().getTargetEnvironment();

        checkTargetSelectionConsistency();
        final Option<SPTarget> targetOpt = TargetSelection.getTargetForNode(env, node);
        final Option<SPCoordinates> coordsOpt = TargetSelection.getCoordinatesForNode(env, node);
        final Option<IndexedGuideGroup> iggOpt = TargetSelection.getIndexedGuideGroupForNode(env, node);

        // If a target, process it.
        targetOpt.foreach(target -> manageCurPosIfEnvContainsTarget(target, () -> setSelectionToTarget(target)));

        // If it is coordinates, process.
        coordsOpt.foreach(coords -> {
            selectedTarget().foreach(t -> t.deleteWatcher(posWatcher));
            selectedCoordinates().foreach(c -> c.deleteWatcher(posWatcher));
            setSelectionToCoordinates(coords);

            _w.guideGroupPanel.setVisible(false);
            _w.coordinateEditor.setVisible(true);
            _w.detailEditor.setVisible(false);

            updateCoordinateEditorEnabledState(isGhostAsterismWithExplicitlyDefinedBaseCoordinates() || !selectionIsBaseCoordinates());
            updateCoordinateDetails(env);
            updateUIForCoordinates();

            selectedCoordinates().foreach(c -> c.addWatcher(posWatcher));
            selectedTarget().foreach(t -> t.addWatcher(posWatcher));
        });

        // If it is a guide group, process it.
        iggOpt.filter(igg -> env.getGroups().contains(igg.group())).foreach(igg -> {
            selectedTarget().foreach(t -> t.deleteWatcher(posWatcher));
            setSelectionToGroup(igg);

            _w.guideGroupPanel.setVisible(true);
            _w.coordinateEditor.setVisible(false);
            _w.detailEditor.setVisible(false);

            // N.B. don't trim, otherwise user can't include space in group name
            final String name;
            final boolean enabled;
            if (selectionIsAutoGroup()) {
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

    private final PropertyChangeListener selectionListener = e -> setSelectionFromNode();

    /**
     * Updates the enabled state of the primary guide target button when the target environment changes.
     * We only allow this if:
     * 1. The selection is a target in a guide group; or
     * 2. The selection is a guide group.
     */
    private final PropertyChangeListener primaryButtonUpdater = new PropertyChangeListener() {
        @Override public void propertyChange(final PropertyChangeEvent evt) {
            final boolean enabled;
            if (selection instanceof TableTargetSelection) {
                final SPTarget t = ((TableTargetSelection) selection).getTarget();
                final TargetEnvironment env = getDataObject().getTargetEnvironment();
                final ImList<GuideProbeTargets> gtList = env.getPrimaryGuideGroup().getAllContaining(t);
                enabled = gtList.nonEmpty() && !selectionIsAutoTarget();
            }
            else
                enabled = selection instanceof TableGroupSelection;

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
        // We don't worry about coordinates as we don't allow them to be removed.
        final Option<TargetEnvironment> envNewOpt = selectedTarget().flatMap(t -> {
            // Handle targets.
            if (selectionIsBasePosition()) {
                DialogUtil.error("You can't remove the base position.");
            } else if (selectionIsAutoTarget()) {
                AgsStrategyUtil.setSelection(
                        getContextObservation(),
                        ImOption.fromScalaOpt(AgsRegistrar.lookup(AgsStrategyKey.OffKey$.MODULE$))
                );
                return new Some<>(envOld.removeTarget(t));
            } else {
                return new Some<>(envOld.removeTarget(t));
            }
            return None.instance();
        }).orElse(() -> selectedGroup().flatMap(igg -> {
            // Handle guide groups.
            final GuideGroup primary = envOld.getPrimaryGuideGroup();
            if (igg.group() == primary) {
                DialogUtil.error("You can't remove the primary guide group.");
            } else if (selectionIsAutoGroup()) {
                DialogUtil.error("You can't remove the automatic guide group.");
            } else {
                final TargetEnvironment envNew = envOld.removeGroup(igg.index());
                final int groupIndex = envNew.getGuideEnvironment().getPrimaryIndex();
                final GuideGroup group = envNew.getGuideEnvironment().getPrimary();
                setSelectionToGroup(new IndexedGuideGroup(groupIndex, group));
                return new Some<>(envNew);
            }
            return None.instance();
        }));

        // Permitted changes were made only if envNewOpt is defined.
        envNewOpt.foreach(envNew -> {
            getDataObject().setTargetEnvironment(envNew);
            setSelectionFromNode();
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
    private final ActionListener duplicateListener = evt -> {
        final ISPObsComponent obsComponent = getNode();
        final TargetObsComp dataObject = getDataObject();
        if ((obsComponent == null) || (dataObject == null)) return;

        // We don't allow this for coordinates.
        checkTargetSelectionConsistency();
        final Option<SPTarget> targetOpt = TargetSelection.getTargetForNode(dataObject.getTargetEnvironment(), obsComponent);
        final Option<IndexedGuideGroup> iggOpt =
                TargetSelection.getIndexedGuideGroupForNode(dataObject.getTargetEnvironment(), obsComponent);
        targetOpt.foreach(target -> {

            // Clone the target.
            final SPTarget newTarget = new SPTarget();
            newTarget.setTarget(target.getTarget());

            // Add it to the environment.  First we have to figure out what it is.
            final TargetEnvironment env = dataObject.getTargetEnvironment();

            // See if it is a guide star and duplicate it in the correct GuideTargets list.
            boolean duplicated = false;
            env.getPrimaryGuideGroup();
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

            final TargetEnvironment newEnv;

            if (duplicated) {
                newEnv = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
            } else {
                // Wasn't a guide star so check which user target needs to
                // be copied.
                final ImList<UserTarget> us = env.getUserTargets();
                newEnv = us.find(u -> u.target.equals(target)).map(u ->
                        env.setUserTargets(us.append(new UserTarget(u.type, newTarget)))
                ).getOrElse(env);
            }
            dataObject.setTargetEnvironment(newEnv);
        });

        iggOpt.foreach(igg -> {
            final GuideGroup origGroup = igg.group();

            // This used to be done with origGroup.cloneTargets(), but if origGroup is an auto group, this creates
            // another auto group, which is problematic, so we specifically force a manual group to be created
            // and receive an appropriate name.
            final String newGroupName = origGroup.getName().filter(s -> !s.isEmpty()).getOrElse("Manual");
            final GuideGroup newGroup = origGroup.toManualGroup().setName(newGroupName);

            final TargetEnvironment env = dataObject.getTargetEnvironment();
            final List<GuideGroup> groups = new ArrayList<>();
            groups.addAll(env.getGroups().toList());
            groups.add(newGroup);

            final TargetEnvironment newEnv = env.setGuideEnvironment(env.getGuideEnvironment().setOptions(DefaultImList.create(groups)));
            dataObject.setTargetEnvironment(newEnv);
        });
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
            if (!(selection instanceof TableNoSelection))
                pasteSelectedPosition(getNode(), getDataObject());
        }
    };

    private final ActionListener primaryListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            _w.positionTable.updatePrimaryStar();
        }
    };

    private final ActionListener linkedBaseListener = new ActionListener() {
        private scala.Option<SPCoordinates> coords(final GhostAsterism ga, final boolean linked) {
                if (linked)
                    return ImOption.scalaNone();

                // Try to get the scheduling block start, convert to Instant, and look up base.
                final Option<Instant> sbi = ((SPObservation)getContextObservation().getDataObject())
                        .getSchedulingBlockStart()
                        .map(Instant::ofEpochMilli);
                return ImOption.toScalaOpt(
                        ImOption.fromScalaOpt(ga.basePosition(ImOption.toScalaOpt(sbi)))
                        .map(SPCoordinates::new)
                );
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final JCheckBox cb = (JCheckBox) e.getSource();

            final TargetObsComp toc = getDataObject();
            if (toc == null) return;
            final TargetEnvironment env = toc.getTargetEnvironment();
            if (env == null) return;
            final Asterism oldA = env.getAsterism();
            if (oldA == null) return;

            // Now depending on the kind of asterism, either make a base SPCoordinates
            // or eliminate one.
            final boolean linked = cb.isSelected();
            final Asterism newA;

            switch (oldA.asterismType()) {
                case GhostSingleTarget:
                    final GhostAsterism.SingleTarget gsa = (GhostAsterism.SingleTarget) oldA;
                    newA = gsa.copy(gsa.target(), coords(gsa, linked));
                    break;
                case GhostDualTarget:
                    final GhostAsterism.DualTarget gda = (GhostAsterism.DualTarget) oldA;
                    newA = gda.copy(gda.target1(), gda.target2(), coords(gda, linked));
                    break;
                case GhostTargetPlusSky:
                    final GhostAsterism.TargetPlusSky gtsa = (GhostAsterism.TargetPlusSky) oldA;
                    newA = gtsa.copy(gtsa.target(), gtsa.sky(), coords(gtsa, linked));
                    break;
                case GhostSkyPlusTarget:
                    final GhostAsterism.SkyPlusTarget gsta = (GhostAsterism.SkyPlusTarget) oldA;
                    newA = gsta.copy(gsta.sky(), gsta.target(), coords(gsta, linked));
                    break;
                case GhostHighResolutionTarget:
                    final GhostAsterism.HighResolutionTarget ghta = (GhostAsterism.HighResolutionTarget) oldA;
                    newA = ghta.copy(ghta.target(), coords(ghta, linked));
                    break;
                case GhostHighResolutionTargetPlusSky:
                    final GhostAsterism.HighResolutionTargetPlusSky ghtsa = (GhostAsterism.HighResolutionTargetPlusSky) oldA;
                    newA = ghtsa.copy(ghtsa.target(), ghtsa.sky(), coords(ghtsa, linked));
                    break;
                default: // This should never happen.
                    newA = oldA;
            }

            if (newA != oldA) {
                final TargetEnvironment newEnv = env.setAsterism(newA);
                toc.setTargetEnvironment(newEnv);
            }
        }
    };


    private static final class TargetClipboard {
        private static final class TargetDetails {
            private final Target target;
            private final ImList<Magnitude> mag;

            TargetDetails(final SPTarget target) {
                this.target = target.getTarget();
                this.mag    = target.getMagnitudesJava();
            }

            public Target getTarget() {
                return target;
            }
            public ImList<Magnitude> getMag() {
                return mag;
            }
        }

        private TableSelection contents = TableNoSelection.instance;

        static Option<TargetClipboard> copy(final TargetEnvironment env, final ISPObsComponent obsComponent) {
            if (obsComponent == null) return None.instance();
            checkTargetSelectionConsistency(env, obsComponent);
            final Option<SPTarget> tOpt = TargetSelection.getTargetForNode(env, obsComponent);
            final Option<SPCoordinates> cOpt = TargetSelection.getCoordinatesForNode(env, obsComponent);
            final Option<IndexedGuideGroup> gOpt = TargetSelection.getIndexedGuideGroupForNode(env, obsComponent);

            if (tOpt.isDefined())
                return new Some<>(new TargetClipboard(tOpt.getValue()));
            if (cOpt.isDefined())
                return new Some<>(new TargetClipboard(cOpt.getValue()));
            if (gOpt.isDefined())
                return new Some<>(new TargetClipboard(gOpt.getValue()));

            return None.instance();
        }


        TargetClipboard(final SPTarget target) {
            contents = new TableTargetSelection(target);
        }
        TargetClipboard(final SPCoordinates coords) {
            contents = new TableCoordinateSelection(coords);
        }
        TargetClipboard(final IndexedGuideGroup group) {
            contents = new TableGroupSelection(group);
        }

        /**
         * Rules for pasting:
         *
         * 1. Groups in their entirety, should be copied, pasted, and duplicated by
         *    the existing copy, paste, and duplicate buttons.
         *
         * 2. Disallow pasting a group on top of an individual target.
         *
         * 3. Pasting on top of a group, target, or coordinate should change all their contents.
         */
        void paste(final ISPObsComponent obsComponent, final TargetObsComp dataObject) {
            if ((obsComponent == null) || (dataObject == null)) return;
            final TargetEnvironment env = dataObject.getTargetEnvironment();

            if (contents instanceof TableTargetSelection) {
                final TableTargetSelection s = (TableTargetSelection) contents;
                final SPTarget tSrc = s.getTarget();

                final Option<SPTarget> tOpt = TargetSelection.getTargetForNode(env, obsComponent);
                tOpt.foreach(t -> {
                    t.setTarget(tSrc.getTarget());
                    t.setMagnitudes(tSrc.getMagnitudesJava());
                });
            }
            else if (contents instanceof TableCoordinateSelection) {
                final TableCoordinateSelection s = (TableCoordinateSelection) contents;
                final Coordinates cSrc = s.getCoordinates().getCoordinates();

                final Option<SPCoordinates> cOpt = TargetSelection.getCoordinatesForNode(env, obsComponent);
                cOpt.foreach(c -> c.setCoordinates(cSrc));
            }
            else if (contents instanceof TableGroupSelection) {
                final TableGroupSelection s = (TableGroupSelection) contents;
                final IndexedGuideGroup iggSrc = s.getGroup();

                final Option<IndexedGuideGroup> gpOpt = TargetSelection.getIndexedGuideGroupForNode(env, obsComponent);
                gpOpt.foreach(igg -> {
                    final int idx                    = igg.index();
                    final GuideGroup newGroup        = igg.group().setAll(iggSrc.group().cloneTargets().getAll());
                    final GuideEnvironment ge        = env.getGuideEnvironment();
                    final ImList<GuideGroup> options = ge.getOptions();
                    final ArrayList<GuideGroup> list = new ArrayList<>(options.size());
                    options.zipWithIndex().foreach(tup -> list.add(tup._2() == idx ? newGroup : tup._1()));
                    dataObject.setTargetEnvironment(env.setGuideEnvironment(ge.setOptions(DefaultImList.create(list))));
                });
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
        // Set the target to the base, and make the existing base a user target.
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;
        env = env.removeTarget(target);

        final SPSkyObject base = env.getSlewPositionObjectFromAsterism();
        if (!(base instanceof SPTarget))
            return;
        final SPTarget baseTarget = (SPTarget) base;

        final GuideEnvironment   genv = env.getGuideEnvironment();
        final ImList<UserTarget> user = env.getUserTargets().append(new UserTarget(UserTarget.Type.other, baseTarget));

        final TargetEnvironment newEnv = TargetEnvironment.create(target, genv, user);
        obsComp.setTargetEnvironment(newEnv);
    }

    @Override public boolean isMember(final TargetEnvironment env, final SPTarget target) {
        return (env.getSlewPositionObjectFromAsterism() == target);
    }

    @Override public String toString() {
        return TargetEnvironment.BASE_NAME;
    }
}

final class GuidePositionType implements PositionType {
    private final GuideProbe guider;
    private final boolean available;
    private final TelescopePosTableWidget positionTable;

    GuidePositionType(final GuideProbe guider, final boolean available, final TelescopePosTableWidget positionTable) {
        this.guider = guider;
        this.available = available;
        this.positionTable = positionTable;
    }

    @Override public boolean isAvailable() {
        return available;
    }

    @Override public void morphTarget(final TargetObsComp obsComp, final SPTarget target) {
        TargetEnvironment env                     = obsComp.getTargetEnvironment();
        final Option<IndexedGuideGroup> oldIggOpt = env.getGroups().zipWithIndex()
                .find(t -> t._1().containsTarget(target))
                .map(t -> new IndexedGuideGroup(t._2(), t._1()));

        // This should never happen: if the target is in the auto group, do nothing.
        if (oldIggOpt.exists(igg -> igg.group().isAutomatic())) return;

        if (isMember(env, target)) return;
        env = env.removeTarget(target);

        // Figure out what group we should add to:
        // If iggOpt is defined, then this is merely a transformation in the group, so add to that.
        // If the primary is manual, add to that.
        // If there is a manual group, add to the first one.
        // Otherwise, create a manual group and add to it.
        final Option<Tuple2<TargetEnvironment,IndexedGuideGroup>> resultOpt;
        if (oldIggOpt.isDefined()) {
            resultOpt = new Some<>(new Pair<>(env, oldIggOpt.getValue()));
        } else if (env.getPrimaryGuideGroup().isManual()) {
            resultOpt = new Some<>(new Pair<>(env, new IndexedGuideGroup(env.getGuideEnvironment().getPrimaryIndex(), env.getPrimaryGuideGroup())));
        } else if (env.getGroups().size() > 1) {
            resultOpt = new Some<>(new Pair<>(env, new IndexedGuideGroup(1, env.getGuideEnvironment().manualGroups().head())));
        } else {
            resultOpt = EdCompTargetList.appendNewGroup(env, positionTable);
        }

        resultOpt.foreach(tup -> {
            final TargetEnvironment envNew = tup._1();
            final IndexedGuideGroup igg    = tup._2();
            EdCompTargetList.addTargetToGroup(envNew, igg, guider, target.clone(), obsComp);
            positionTable.selectTarget(target);
        });
    }

    @Override public boolean isMember(final TargetEnvironment env, final SPTarget target) {
        return env.getGroups().exists(group -> group.get(guider).exists(gt -> gt.getTargets().contains(target)));
    }

    @Override public String toString() {
        return guider.getKey();
    }
}

final class UserPositionType implements PositionType {
    final UserTarget.Type userTargetType;

    UserPositionType(UserTarget.Type t) {
        this.userTargetType = t;
    }

    @Override public boolean isAvailable() {
        return true;
    }

    @Override public void morphTarget(final TargetObsComp obsComp, final SPTarget target) {
        TargetEnvironment env = obsComp.getTargetEnvironment();
        if (isMember(env, target)) return;

        final UserTarget ut = new UserTarget(userTargetType, target);

        final TargetEnvironment newEnv;
        if (env.isUserPosition(target)) {
            // It's a user target that we're morphing so we'll do it in place so
            // that we don't rearrange the order of the user targets.
            final ImList<UserTarget> us = env.getUserTargets();
            final int i = us.indexWhere(u -> u.target.equals(target));
            newEnv = (i < 0) ? env : env.setUserTargets(us.updated(i, ut));
        } else {
            // Otherwise, we'll remove and append.
            newEnv = env.removeTarget(target).setUserTargets(env.getUserTargets().append(ut));
        }
        obsComp.setTargetEnvironment(newEnv);

    }

    @Override public boolean isMember(final TargetEnvironment env, final SPTarget target) {
        return env.getUserTargets().find(u -> u.type == userTargetType && u.target.equals(target)).isDefined();
    }

    @Override public String toString() {
        return userTargetType.displayName;
    }
}
