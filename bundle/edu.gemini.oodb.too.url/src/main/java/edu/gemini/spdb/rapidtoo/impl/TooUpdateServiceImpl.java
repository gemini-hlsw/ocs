package edu.gemini.spdb.rapidtoo.impl;

import edu.gemini.pot.sp.*;
import static edu.gemini.pot.sp.SPComponentBroadType.INSTRUMENT;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.too.TooConstraintService;
import edu.gemini.spdb.rapidtoo.*;
import edu.gemini.util.security.auth.keychain.KeyService;
import edu.gemini.util.security.permission.PiPermission;
import edu.gemini.util.security.principal.UserPrincipal;
import edu.gemini.util.security.policy.ImplicitPolicyForJava;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class TooUpdateServiceImpl implements TooUpdateService {
    private static final Logger LOG = Logger.getLogger(TooUpdateServiceImpl.class.getName());

    private final KeyService keyservice;

    public TooUpdateServiceImpl(KeyService keyservice) {
        this.keyservice = keyservice;
    }

    private ISPProgram _authenticate(IDBDatabaseService db, TooUpdate update) throws TooUpdateException {
        TooIdentity id = update.getIdentity();

        SPProgramID progId = id.getProgramId();
        if (progId == null) {
            LOG.info("Attempt to log in without a program id");
            throw new AuthenticationException("Incorrect program ID or password");
        }

        ISPProgram prog = db.lookupProgramByID(progId);
        if (prog == null) {
            // using a fine level here, since in the clustered database
            // approach, all but one database will not have the indicated
            // program
            LOG.info("Attempt to log in with unknown program id: " + progId);
            throw new AuthenticationException("Incorrect program ID, email address, or password");
        }

        // Check the password.
        if (!keyservice.asJava().tryUserKey(id.getEmail(), id.getPassword())) {
            LOG.info("Attempt to log in with bad password: prog id=" + progId +
                     ", password=" + id.getPassword());
            throw new AuthenticationException("Incorrect program ID, email address, or password");
        }

        // Check that the user has permission.
        final UserPrincipal up = UserPrincipal.apply(id.getEmail());
        try {
            ImplicitPolicyForJava.checkPermission(db, up, new PiPermission(id.getProgramId()));
        } catch (AccessControlException ace) {
            LOG.info("PiPermission(" + id.getProgramId() + ") denied for user " + id.getEmail());
            throw new AuthenticationException("Insufficient privileges.");
        }

        final SPProgram progDataObj = (SPProgram) prog.getDataObject();
        if (!progDataObj.isActive()) {
            LOG.info("Attempt to update inactive program: prog id=" + progId);
            throw new InactiveProgramException("Program is inactive");
        }
        return prog;
    }

    private ISPObservation _lookupTemplate(IDBDatabaseService db, TooUpdate update, ISPProgram prog)
            throws MissingTemplateException {
        TooIdentity id = update.getIdentity();
        int obsNum = id.getTemplateObsNumber();
        if (obsNum > 0) {
            SPObservationID obsId;
            try {
                obsId = new SPObservationID(prog.getProgramID(), obsNum);
            } catch (SPBadIDException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                throw new RuntimeException("invalid obs id with positive number?: " + obsNum);
            }
            final ISPObservation res = db.lookupObservationByID(obsId);
            if (res == null) throw new MissingTemplateException("Template observation " + obsId + " not found");
            return res;
        }

        String obsName = id.getTemplateObsName();
        if (obsName == null) {
            throw new MissingTemplateException("Template observation not specified");
        }

        //noinspection unchecked
        List<ISPObservation> obsList = prog.getAllObservations();
        if (obsList == null) {
            throw new MissingTemplateException("Program has no observations");
        }

        for (ISPObservation obs : obsList) {
            SPObservation dataObj = (SPObservation) obs.getDataObject();
            if (obsName.equalsIgnoreCase(dataObj.getTitle())) {
                return obs;
            }
        }

        throw new MissingTemplateException("Template observation \"" +
                                                     obsName + "\" not found");
    }

    private ISPGroup _getOrCreateGroup(ISPProgram prog, ISPFactory factory, TooUpdate update)
             {

        String groupName = update.getGroup();
        if (groupName == null) return null;

        //noinspection unchecked
        List<ISPGroup> groupList = prog.getGroups();
        if (groupList == null) {
            groupList = new ArrayList<ISPGroup>(1);
        }

        for (ISPGroup group : groupList) {
            SPGroup dataObj = (SPGroup) group.getDataObject();
            String curGroupName = dataObj.getGroup();
            if (groupName.equals(curGroupName)) return group;
        }

        // Didn't match any existing group, so create one.
        ISPGroup group;
        try {
            group = factory.createGroup(prog, null);
        } catch (SPUnknownIDException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }

        SPGroup dataObj = new SPGroup(groupName);
        group.setDataObject(dataObj);
        groupList.add(group);
        try {
            prog.setGroups(groupList);
        } catch (SPNodeNotLocalException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        } catch (SPTreeStateException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }

        return group;
    }

    private void _addNote(ISPObservation obs, ISPFactory factory, TooUpdate update)
            throws SPException {

        String noteText = update.getNote();
        if (noteText == null) return;

        ISPObsComponent obsComp;
        obsComp = factory.createObsComponent(obs.getProgram(), SPNote.SP_TYPE, null);
        SPNote note = (SPNote) obsComp.getDataObject();
        note.setTitle("Finding Chart");
        note.setNote(noteText);
        obsComp.setDataObject(note);
        obs.addObsComponent(0, obsComp);
    }

    private void _addTimingWindow(ISPObservation obs, ISPFactory factory, TooUpdate update)
            throws SPException {

        SPSiteQuality.TimingWindow win = null;
        TooTimingWindow toowin = update.getTimingWindow();
        if (toowin != null) {
            long start    = toowin.getDate().getTime();
            long duration = toowin.getDuration();
            win = new SPSiteQuality.TimingWindow(start, duration, 0, 0);
        }

        TooConstraintService.addTimingWindow(obs, factory, win);
    }

    private void _addElevationConstraint(ISPObservation obs, ISPFactory factory, TooUpdate update)
            throws SPException {
        TooElevationConstraint cons = update.getElevationConstraint();
        if (cons == null) return;
        TooConstraintService.setElevationConstraint(obs, factory, cons.getType(), cons.getMin(), cons.getMax());
    }

    private ISPObsComponent _findComponent(ISPObservation obs, SPComponentType type)
             {
        //noinspection unchecked
        List<ISPObsComponent> compList = obs.getObsComponents();
        for (ISPObsComponent comp : compList) {
            SPComponentType curType = comp.getType();
            if (curType.equals(type)) return comp;
        }
        return null;
    }

    private void _setTarget(final ISPObservation obs, final TooUpdate update)  {
        final TooTarget tooTarget = update.getBasePosition();
        if (tooTarget == null) {
            LOG.warning("Too update missing target information.");
            return;
        }

        // Set the title of the observation.
        final SPObservation obsDobj = (SPObservation) obs.getDataObject();
        obsDobj.setTitle(tooTarget.getName());

        // Get the target component and update the base position.
        final ISPObsComponent targetComp = _findComponent(obs, TargetObsComp.SP_TYPE);
        if (targetComp == null) {
            LOG.warning("Too update has no target component.");
            return;
        }

        final TargetObsComp targetObsComp = (TargetObsComp) targetComp.getDataObject();
        final TargetEnvironment targetEnv = targetObsComp.getTargetEnvironment();

        final SPTarget base = targetEnv.getBase();
        base.setName(tooTarget.getName());
        base.setRaDecDegrees(tooTarget.getRa(), tooTarget.getDec());
        base.setMagnitudes(tooTarget.getMagnitudes());

        // Set the guide star, if present.
        final TooGuideTarget gs = update.getGuideStar();
        if (gs != null) {
            final TooGuideTarget.GuideProbe tooProbe = gs.getGuideProbe();
            if (tooProbe == null) {
                LOG.warning("Guide star probe not specified.");
            } else {
                // damn
                final GuideProbe probe;
                switch (tooProbe) {
                    case AOWFS:
                        probe = AltairAowfsGuider.instance;
                        break;
                    case OIWFS:
                        probe = getOiwfs(obs);
                        break;
                    case PWFS1:
                        probe = PwfsGuideProbe.pwfs1;
                        break;
                    case PWFS2:
                        probe = PwfsGuideProbe.pwfs2;
                        break;
                    default:
                        probe = null;
                }

                if (probe != null) {
                    final Option<GuideProbeTargets> gtOpt = targetEnv.getPrimaryGuideProbeTargets(probe);
                    final GuideProbeTargets gt = gtOpt.isEmpty() ? GuideProbeTargets.create(probe) : gtOpt.getValue();

                    final Option<SPTarget> targetOpt = gt.getPrimary();
                    final SPTarget target = targetOpt.isEmpty() ? new SPTarget() : targetOpt.getValue();

                    target.setRaDecDegrees(gs.getRa(), gs.getDec());
                    final String name = gs.getName();
                    if (name != null) {
                        target.setName(name);
                    }

                        target.setMagnitudes(gs.getMagnitudes());

                    if (targetOpt.isEmpty()) {
                        final GuideProbeTargets gtNew = gt.withManualTargets(gt.getManualTargets().cons(target)).withExistingPrimary(target);
                        final TargetEnvironment targetEnvNew = targetEnv.putPrimaryGuideProbeTargets(gtNew);
                        targetObsComp.setTargetEnvironment(targetEnvNew);
                    }
                }
            }
        }

        // Store the data object back.
        targetComp.setDataObject(targetObsComp);
    }

    private void _setPosAngle(ISPObservation obs, TooUpdate update)  {

        Double posAngle = update.getPositionAngle();
        if (posAngle == null) return;

        ISPObsComponent inst = getInstrument(obs);
        if (inst == null) {
            LOG.warning("Cannot set position angle for observation since it has no instrument: " +
                         obs.getObservationID());
            return;
        }

        SPInstObsComp dobj = (SPInstObsComp) inst.getDataObject();
        dobj.setPosAngleDegrees(update.getPositionAngle());
        inst.setDataObject(dobj);
    }

    private ISPObsComponent getInstrument(ISPObservation obs)  {
        //noinspection unchecked
        List<ISPObsComponent> obsComps = obs.getObsComponents();
        for (ISPObsComponent obsComp : obsComps) {
            SPComponentType type = obsComp.getType();
            if (INSTRUMENT == type.broadType) {
                return obsComp;
            }
        }
        return null;
    }

    private GuideProbe getOiwfs(ISPObservation obs)  {
        ISPObsComponent inst = getInstrument(obs);
        if (inst == null) return null;

        Object dataObj = inst.getDataObject();
        if (!(dataObj instanceof GuideProbeProvider)) return null;

        Collection<GuideProbe> guiders = ((GuideProbeProvider) dataObj).getGuideProbes();
        if ((guiders != null) && (guiders.size() > 0)) return guiders.iterator().next();
        return null;
    }

    private void _updateObsDataObj(ISPObservation obs, TooUpdate update)  {
        SPObservation dobj = (SPObservation) obs.getDataObject();

        TooTarget tooTarget = update.getBasePosition();
        if (tooTarget != null) {
            dobj.setTitle(tooTarget.getName());
        }

//        dobj.setPriority(SPObservation.Priority.TOO);
        dobj.setPhase2Status(ObsPhase2Status.ON_HOLD);
        obs.setDataObject(dobj);
    }

    private void _markReady(ISPObservation obs)  {
        SPObservation dobj = (SPObservation) obs.getDataObject();
        dobj.setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE);
        obs.setDataObject(dobj);
    }

    private void _updateObservation(ISPObservation obs, ISPFactory factory, TooUpdate update) throws SPException {
        _addNote(obs, factory, update);
        _addElevationConstraint(obs, factory, update);
        _addTimingWindow(obs, factory, update);
        _setTarget(obs, update);
        _setPosAngle(obs, update);
        _updateObsDataObj(obs, update);
    }

    public ISPObservation handleUpdate(IDBDatabaseService db, TooUpdate update) throws TooUpdateException {
        // Get the associated program.
        ISPProgram prog = _authenticate(db, update);

        // Find the template observation.
        ISPObservation template = _lookupTemplate(db, update, prog);

        // Make sure it is on hold.
        SPObservation dataObj =  (SPObservation) template.getDataObject();
        ObsPhase2Status status = dataObj.getPhase2Status();
        if (!(ObsPhase2Status.ON_HOLD == status)) {
            System.out.println("Was not on hold");
            throw new TooUpdateException("Template observation is not 'on hold'.");
        }

        // Clone the template observation.
        ISPFactory factory = db.getFactory();
        ISPObservation res;
        try {
            res = factory.createObservationCopy(prog, template, false);
            _updateObservation(res, factory, update);
            ISPGroup group = _getOrCreateGroup(prog, factory, update);
            if (group == null) {
                prog.addObservation(res);
            } else {
                group.addObservation(res);
            }

            // Set the observation status to ready after adding it, so that the
            // TOO alert is generated.
            if (update.isReady()) _markReady(res);
        } catch (SPException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException("unexpected problem in the middle of an update", ex);
        }

        return res;
    }
}
