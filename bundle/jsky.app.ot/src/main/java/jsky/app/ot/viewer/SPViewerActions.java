package jsky.app.ot.viewer;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.seqcomp.SeqBase;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import jsky.app.ot.OTOptions;
import jsky.app.ot.nsp.UIInfo;
import jsky.app.ot.util.History;
import jsky.util.gui.Resources;
import jsky.app.ot.viewer.action.*;

import java.util.*;

public final class SPViewerActions {
    private final SPViewer viewer;

    // Individual actions
    public final AbstractViewerAction vcsSyncAction;
    public final AbstractViewerAction syncAllAction;
    public final AbstractViewerAction conflictPrevAction;
    public final AbstractViewerAction conflictNextAction;
    public final AbstractViewerAction resolveConflictsAction;
    public final AbstractViewerAction showProgramManagerAction;
    public final AbstractViewerAction editItemTitleAction;
    public final AbstractViewerAction programAdminAction;
    public final AbstractViewerAction showKeyManagerAction;
    public final AbstractViewerAction setPhase2StatusAction;
    public final AbstractViewerAction setExecStatusAction;
    public final AbstractViewerAction navViewerPrevAction;
    public final AbstractViewerAction navViewerNextAction;
    public final AbstractViewerAction navViewerPrevProgAction;
    public final AbstractViewerAction navViewerNextProgAction;
    public final AbstractViewerAction cutAction;
    public final AbstractViewerAction copyAction;
    public final AbstractViewerAction pasteAction;
    public final AbstractViewerAction getLibAction;
    public final AbstractViewerAction moveUpAction;
    public final AbstractViewerAction moveDownAction;
    public final AbstractViewerAction moveToTopAction;
    public final AbstractViewerAction moveToBottomAction;
    public final AbstractViewerAction expandObsAction;
    public final AbstractViewerAction expandProgAction;
    public final AbstractViewerAction collapseObsAction;
    public final AbstractViewerAction collapseProgAction;
    public final AbstractViewerAction addSiteQualityAction;
    public final AbstractViewerAction addTargetListAction;
    public final AbstractViewerAction showTPEAction;
    public final AbstractViewerAction showElevationPlotAction;
    public final AbstractViewerAction templateApplyAction;
    public final AbstractViewerAction templateReapplyAction;
    public final AbstractViewerAction templateSplitAction;
    public final AbstractViewerAction templateRegenAction;
    public final AbstractViewerAction enqueueAction;
    public final AbstractViewerAction addSequenceAction;
    public final AbstractViewerAction addSchedulingGroupAction;
    public final AbstractViewerAction addOrganizationalFolderAction;
    public final AbstractViewerAction addInfoNoteAction;
    public final AbstractViewerAction addInfoSchedulingNoteAction;
    public final AbstractViewerAction addInfoProgramNoteAction;
    public final AbstractViewerAction purgeEphemerisAction;

    // Groups of actions. In some cases these appear in the list above, in other cases not.
    final List<AbstractViewerAction> templateActions = new ArrayList<AbstractViewerAction>();
    final List<AbstractViewerAction> addGroupActions = new ArrayList<AbstractViewerAction>();
    final List<AddObservationAction> addObservationActions = new ArrayList<AddObservationAction>();
    final List<AddObsCompAction> addNoteActions = new ArrayList<AddObsCompAction>();
    final List<AddObsCompAction> addInstrumentActions = new ArrayList<AddObsCompAction>();
    final List<AddObsCompAction> addAOActions = new ArrayList<AddObsCompAction>();
    final List<AddObsCompAction> addEngineeringActions = new ArrayList<AddObsCompAction>();
    final List<AddSeqCompAction> addInstrumentIteratorActions = new ArrayList<AddSeqCompAction>();
    final List<AddSeqCompAction> addGenericSeqCompActions = new ArrayList<AddSeqCompAction>();


    public SPViewerActions(SPViewer viewer) {
        this.viewer = viewer;

        // General Actions
        showProgramManagerAction = new OpenAction(viewer);
        showKeyManagerAction = new OpenKeyManagerAction(viewer);
        getLibAction = new LibraryAction(viewer); // TODO: this will likely go away

        // Navigation Actions

        navViewerPrevProgAction = new NavAction(viewer,  "Prev", "prev_prog.png", "Previous program." ) {
            protected History navigate(History h) {
                return h.prevOrNull();
            }
            protected Boolean isEnabled(History h) {
                return h.hasPrev();
            }
        };


        navViewerPrevAction = new NavAction(viewer,  "Back", "Back24.gif", "Previous node." ) {
            protected History navigate(History h) {
                return h.prevNodeOrNull();
            }
            protected Boolean isEnabled(History h) {
                return h.hasPrevNode();
            }
        };

        navViewerNextAction = new NavAction(viewer,  "Forward", "Forward24.gif", "Next node." ) {
            protected History navigate(History h) {
                return h.nextNodeOrNull();
            }
            protected Boolean isEnabled(History h) {
                return h.hasNextNode();
            }
        };

        navViewerNextProgAction = new NavAction(viewer,  "Next", "next_prog.png", "Next program." ) {
            protected History navigate(History h) {
                return h.nextOrNull();
            }
            protected Boolean isEnabled(History h) {
                return h.hasNext();
            }
        };


        expandObsAction = new ExpandObsAction(viewer);
        expandProgAction = new ExpandProgAction(viewer);
        collapseObsAction = new CollapseObsAction(viewer);
        collapseProgAction = new CollapseProgAction(viewer);
        conflictPrevAction = new VcsShowPrevConflictAction(viewer);
        conflictNextAction = new VcsShowNextConflictAction(viewer);
        resolveConflictsAction = new ResolveConflictsAction(viewer);

        // VCS Actions
        vcsSyncAction = new VcsSyncAction(viewer);
        syncAllAction = new SyncAllAction(viewer);

        // General Edit Actions
        cutAction = new CutAction(viewer);
        copyAction = new CopyAction(viewer);
        pasteAction = new PasteAction(viewer);
        editItemTitleAction = new EditItemTitleAction(viewer);
        moveUpAction = new MoveAction(viewer, MoveAction.Op.UP);
        moveDownAction = new MoveAction(viewer, MoveAction.Op.DOWN);
        moveToTopAction = new MoveAction(viewer, MoveAction.Op.TOP);
        moveToBottomAction = new MoveAction(viewer, MoveAction.Op.BOTTOM);

        // Program-level Actions
        programAdminAction = new ProgramAdminAction(viewer);
        purgeEphemerisAction = new EphemerisPurgeAction(viewer);

        // Group actions
        addSchedulingGroupAction = new AddGroupAction(viewer, SPGroup.GroupType.TYPE_SCHEDULING);
        addOrganizationalFolderAction = new AddGroupAction(viewer, SPGroup.GroupType.TYPE_FOLDER);

        addGroupActions.add(addSchedulingGroupAction);
        addGroupActions.add(addOrganizationalFolderAction);

        // Template actions
        templateApplyAction = new ApplyTemplateAction(viewer);
        templateReapplyAction = new ReapplyTemplateAction(viewer);
        templateSplitAction = new SplitTemplateGroupAction(viewer);
        templateRegenAction = new RegenerateTemplateAction(viewer);

        templateActions.add(templateApplyAction);
        templateActions.add(templateReapplyAction);
        templateActions.add(templateRegenAction);
        templateActions.add(templateSplitAction);

        // Note Actions

        addInfoNoteAction = new AddObsCompAction(viewer, SPComponentType.INFO_NOTE, Resources.getIcon("post-it-note18.gif"));
        addInfoSchedulingNoteAction = new AddObsCompAction(viewer, SPComponentType.INFO_SCHEDNOTE, Resources.getIcon("post-it-note-blue18.gif"));
        addInfoProgramNoteAction = new AddObsCompAction(viewer, SPComponentType.INFO_PROGRAMNOTE, Resources.getIcon("post-it-note-red18.gif")) {
            public boolean computeEnabledState() {
                return super.computeEnabledState() && OTOptions.isStaff(getProgram().getProgramID());
            }
        };

        addNoteActions.add((AddObsCompAction) addInfoNoteAction);
        addNoteActions.add((AddObsCompAction) addInfoSchedulingNoteAction);
        addNoteActions.add((AddObsCompAction) addInfoProgramNoteAction);

        // Observation Actions - Adding observations of various types
        for (UIInfo uiInfo : UIInfoXML.getByType(UIInfo.TYPE_INSTRUMENT)) {
            Set<SPComponentType> typeSet = new HashSet<SPComponentType>();
            for (UIInfo.Id id : uiInfo.getRequires()) {
                UIInfo req = UIInfoXML.getUIInfo(id);
                if (req != null) typeSet.add(req.getSPType());
            }
            addObservationActions.add(new AddObservationAction(viewer, uiInfo.getSPType(), typeSet));
        }
        Collections.sort(addObservationActions);
        addObservationActions.add(0, new AddObservationAction(viewer, null, null)); // empty obs

        // Observation Actions - General
        setPhase2StatusAction = new Phase2StatusAction(viewer);
        setExecStatusAction   = new ExecStatusAction(viewer);
        addSiteQualityAction = new AddObsCompAction(viewer, SPSiteQuality.SP_TYPE);
        addTargetListAction = new AddObsCompAction(viewer, TargetObsComp.SP_TYPE);
        showTPEAction = new ShowTPEAction(viewer);
        showElevationPlotAction = new ShowElevationPlotAction(viewer);
//        enqueueAction = new EnqueueAction(viewer);
        enqueueAction = new SyncAndEnqueueAction(viewer);
        addSequenceAction = new AddSeqCompAction(viewer, SeqBase.SP_TYPE);

        // Observation Actions - Adding Instrument Components
        for (UIInfo uiInfo : UIInfoXML.getByType(UIInfo.TYPE_INSTRUMENT))
            addInstrumentActions.add(new AddObsCompAction(viewer, uiInfo.getSPType()));
        Collections.sort(addInstrumentActions);

        // Observation Actions - Adding AO Components
        for (UIInfo uiInfo : UIInfoXML.getByType(UIInfo.TYPE_AO_INSTRUMENT))
            addAOActions.add(new AddObsCompAction(viewer, uiInfo.getSPType()));

        // Observation Actions - Adding Engineering Components
        for (UIInfo uiInfo : UIInfoXML.getByType(UIInfo.TYPE_ENG_COMP))
            addEngineeringActions.add(new AddObsCompAction(viewer, uiInfo.getSPType()));

        // Observation Actions - Adding Instrument Iterator Components
        for (UIInfo uiInfo : UIInfoXML.getByType(UIInfo.TYPE_ITER_COMP))
            addInstrumentIteratorActions.add(new AddSeqCompAction(viewer, uiInfo.getSPType()));
        Collections.sort(addInstrumentIteratorActions);

        // Observation Actions - Adding General Sequence Components
        for (UIInfo uiInfo : UIInfoXML.getByType(UIInfo.TYPE_ITER_OBS))
            addGenericSeqCompActions.add(new AddSeqCompAction(viewer, uiInfo.getSPType()));

    }


    /**
     * Update the enable states of toolbar actions based on the state of the viewer.
     */
    void _updateEnabledStates() {
//        final Map<String, Long> m = new TreeMap<String, Long>();
//        final long start = System.currentTimeMillis();
        for (AbstractViewerAction a : AbstractViewerAction.getInstances(viewer)) {
            try {
//                final long s = System.currentTimeMillis();
                a.setEnabled(a.computeEnabledState());
//                m.put(a.getValue(Action.NAME).toString(), System.currentTimeMillis() - s);
            } catch (Exception e) {
                e.printStackTrace();
                a.setEnabled(false);
            }
        }
//        final long end = System.currentTimeMillis();
//        final StringBuffer buf = new StringBuffer();
//        long sum = 0;
//        Map.Entry<String, Long> max = m.entrySet().iterator().next();
//        for (Map.Entry<String, Long> me : m.entrySet()) {
//            buf.append(String.format("\t%4d - %s\n", me.getValue(), me.getKey()));
//            if (me.getValue() > max.getValue()) max = me;
//            sum = sum + me.getValue();
//        }
//        System.out.print(buf.toString());
//        System.out.println("## TOTAL UPDATE TIME " + (end - start));
//        System.out.println("   sum = " + sum + ", avg = " + (sum/m.size()) +  ", max = " + max.getValue() + " (" + max.getKey() + ")\n");
    }

    void close() {
        AbstractViewerAction.forgetInstances(viewer);
    }
}
