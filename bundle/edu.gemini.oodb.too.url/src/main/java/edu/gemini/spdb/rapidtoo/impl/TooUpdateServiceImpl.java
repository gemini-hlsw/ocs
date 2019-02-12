package edu.gemini.spdb.rapidtoo.impl;

import edu.gemini.pot.sp.*;
import static edu.gemini.pot.sp.SPComponentBroadType.INSTRUMENT;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.ImOption;
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
import edu.gemini.spModel.target.env.*;
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

final class TooUpdateServiceImpl implements TooUpdateService {
    private static final Logger LOG = Logger.getLogger(TooUpdateServiceImpl.class.getName());

    private final KeyService keyservice;

    TooUpdateServiceImpl(KeyService keyservice) {
        this.keyservice = keyservice;
    }

    private ISPProgram _authenticate(final IDBDatabaseService db, final TooUpdate update) throws TooUpdateException {
        final TooIdentity id = update.getIdentity();

        final SPProgramID progId = id.getProgramId();
        if (progId == null) {
            LOG.info("Attempt to log in without a program id");
            throw new AuthenticationException("Incorrect program ID or password");
        }

        final ISPProgram prog = db.lookupProgramByID(progId);
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
        } catch (final AccessControlException ace) {
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

    private ISPObservation _lookupTemplate(final IDBDatabaseService db, final TooUpdate update, final ISPProgram prog)
            throws MissingTemplateException {
        final TooIdentity id = update.getIdentity();
        final int obsNum = id.getTemplateObsNumber();
        if (obsNum > 0) {
            SPObservationID obsId;
            try {
                obsId = new SPObservationID(prog.getProgramID(), obsNum);
            } catch (final SPBadIDException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                throw new RuntimeException("invalid obs id with positive number?: " + obsNum);
            }
            final ISPObservation res = db.lookupObservationByID(obsId);
            if (res == null) throw new MissingTemplateException("Template observation " + obsId + " not found");
            return res;
        }

        final String obsName = id.getTemplateObsName();
        if (obsName == null) {
            throw new MissingTemplateException("Template observation not specified");
        }

        final List<ISPObservation> obsList = prog.getAllObservations();
        if (obsList == null) {
            throw new MissingTemplateException("Program has no observations");
        }

        return obsList.stream().filter(obs -> obsName.equalsIgnoreCase(obs.getDataObject().getTitle()))
                .findFirst()
                .orElseThrow(() -> new MissingTemplateException("Template observation \"" + obsName + "\" not found"));
    }

    private ISPGroup _getOrCreateGroup(final ISPProgram prog, final ISPFactory factory, final TooUpdate update) {
        final String groupName = update.getGroup();
        if (groupName == null) return null;

        final List<ISPGroup> groupList = ImOption.apply(prog.getGroups()).getOrElse(new ArrayList<>(1));

        for (final ISPGroup group : groupList) {
            final SPGroup dataObj = (SPGroup) group.getDataObject();
            final String curGroupName = dataObj.getGroup();
            if (groupName.equals(curGroupName)) return group;
        }

        // Didn't match any existing group, so create one.
        ISPGroup group;
        try {
            group = factory.createGroup(prog, null);
        } catch (final SPUnknownIDException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }

        final SPGroup dataObj = new SPGroup(groupName);
        group.setDataObject(dataObj);
        groupList.add(group);
        try {
            prog.setGroups(groupList);
        } catch (final SPNodeNotLocalException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        } catch (final SPTreeStateException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }

        return group;
    }

    private void _addNote(final ISPObservation obs, final ISPFactory factory, final TooUpdate update)
            throws SPException {

        final String noteText = update.getNote();
        if (noteText == null) return;

        final ISPObsComponent obsComp;
        obsComp = factory.createObsComponent(obs.getProgram(), SPNote.SP_TYPE, null);

        final SPNote note = (SPNote) obsComp.getDataObject();
        note.setTitle("Finding Chart");
        note.setNote(noteText);

        obsComp.setDataObject(note);
        obs.addObsComponent(0, obsComp);
    }

    private void _addTimingWindow(final ISPObservation obs, final ISPFactory factory, final TooUpdate update)
            throws SPException {

        final SPSiteQuality.TimingWindow win;
        final TooTimingWindow toowin = update.getTimingWindow();
        if (toowin != null) {
            long start    = toowin.getDate().getTime();
            long duration = toowin.getDuration();
            win = new SPSiteQuality.TimingWindow(start, duration, 0, 0);
        } else {
            win = null;
        }

        TooConstraintService.addTimingWindow(obs, factory, win);
    }

    private void _addElevationConstraint(final ISPObservation obs, final ISPFactory factory, final TooUpdate update)
            throws SPException {
        final TooElevationConstraint cons = update.getElevationConstraint();
        if (cons == null) return;
        TooConstraintService.setElevationConstraint(obs, factory, cons.getType(), cons.getMin(), cons.getMax());
    }

    private ISPObsComponent _findComponent(final ISPObservation obs, final SPComponentType type) {
        return obs.getObsComponents().stream().filter(comp -> comp.getType().equals(type)).findFirst().orElse(null);
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

        // Create a new sidereal target matching the request.
        final SPTarget base = new SPTarget();
        base.setName(tooTarget.getName());
        base.setRaDecDegrees(tooTarget.getRa(), tooTarget.getDec());
        base.setMagnitudes(tooTarget.getMagnitudes());

        // Create a new target environment using this base position and store
        // it in the target data object.
        final TargetEnvironment env0 = TargetEnvironment.create(base);
        targetObsComp.setTargetEnvironment(env0);

        // Set the guide star, if present.
        update.getGuideStar().foreach(gs -> {
            final TooGuideTarget.GuideProbe tooProbe = gs.getGuideProbe();
            if (tooProbe == null) {
                LOG.warning("Guide star probe not specified.");
            } else {
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
                    // Create a new sidereal guide star matching the request.
                    final SPTarget target = new SPTarget();
                    target.setRaDecDegrees(gs.getRa(), gs.getDec());
                    ImOption.apply(gs.getName()).foreach(target::setName);
                    target.setMagnitudes(gs.getMagnitudes());

                    // Create a simple manual primary guide group with this
                    // guide star tied to the requested probe.
                    final GuideProbeTargets gpt        = GuideProbeTargets.create(probe, target);
                    final GuideGroup gg                = GuideGroup.create(GuideGroup.ManualGroupDefaultName(), gpt);
                    final OptionsList<GuideGroup> opts = OptionsListImpl.create(gg);
                    final GuideEnvironment genv        = GuideEnvironment.create(opts);

                    // Update the target environment to include this guide env.
                    final TargetEnvironment env1       = env0.setGuideEnvironment(genv);
                    targetObsComp.setTargetEnvironment(env1);
                }
            }
        });

        // Store the data object back.
        targetComp.setDataObject(targetObsComp);
    }

    private void _updateInstrument(final ISPObservation obs, final TooUpdate update)  {

        final Double posAngle = update.getPositionAngle();
        if (posAngle == null) return;

        final ISPObsComponent inst = getInstrument(obs);
        if (inst == null) {
            LOG.warning("Cannot set position angle for observation since it has no instrument: " +
                         obs.getObservationID());
            return;
        }

        final SPInstObsComp dobj = (SPInstObsComp) inst.getDataObject();
        dobj.setPosAngleDegrees(update.getPositionAngle());
        update.getExposureTime().foreach(e -> dobj.setExposureTime(e.getSeconds()));
        inst.setDataObject(dobj);
    }

    private ISPObsComponent getInstrument(final ISPObservation obs)  {
        return obs.getObsComponents().stream().filter(obsComp -> INSTRUMENT == obsComp.getType().broadType)
            .findFirst().orElse(null);
    }

    private GuideProbe getOiwfs(final ISPObservation obs)  {
        final ISPObsComponent inst = getInstrument(obs);
        if (inst == null) return null;

        final Object dataObj = inst.getDataObject();
        if (!(dataObj instanceof GuideProbeProvider)) return null;

        final Collection<GuideProbe> guiders = ((GuideProbeProvider) dataObj).getGuideProbes();
        if ((guiders != null) && (guiders.size() > 0)) return guiders.iterator().next();
        return null;
    }

    private void _updateObsDataObj(final ISPObservation obs, final TooUpdate update)  {
        final SPObservation dobj = (SPObservation) obs.getDataObject();

        final TooTarget tooTarget = update.getBasePosition();
        if (tooTarget != null) {
            dobj.setTitle(tooTarget.getName());
        }

        dobj.setPhase2Status(ObsPhase2Status.ON_HOLD);
        obs.setDataObject(dobj);
    }

    private void _markReady(final ISPObservation obs)  {
        final SPObservation dobj = (SPObservation) obs.getDataObject();
        dobj.setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE);
        obs.setDataObject(dobj);
    }

    private void _updateObservation(final ISPObservation obs, final ISPFactory factory, final TooUpdate update)
            throws SPException {
        _addNote(obs, factory, update);
        _addElevationConstraint(obs, factory, update);
        _addTimingWindow(obs, factory, update);
        _setTarget(obs, update);
        _updateInstrument(obs, update);
        _updateObsDataObj(obs, update);
    }

    public ISPObservation handleUpdate(final IDBDatabaseService db, final TooUpdate update) throws TooUpdateException {
        // Get the associated program.
        final ISPProgram prog = _authenticate(db, update);

        // Find the template observation.
        final ISPObservation template = _lookupTemplate(db, update, prog);

        // Make sure it is on hold.
        final SPObservation dataObj =  (SPObservation) template.getDataObject();
        final ObsPhase2Status status = dataObj.getPhase2Status();
        if (!(ObsPhase2Status.ON_HOLD == status)) {
            System.out.println("Was not on hold");
            throw new TooUpdateException("Template observation is not 'on hold'.");
        }

        // Clone the template observation.
        final ISPFactory factory = db.getFactory();
        ISPObservation res;
        try {
            res = factory.createObservationCopy(prog, template, false);
            _updateObservation(res, factory, update);
            final ISPGroup group = _getOrCreateGroup(prog, factory, update);
            if (group == null) {
                prog.addObservation(res);
            } else {
                group.addObservation(res);
            }

            // Set the observation status to ready after adding it, so that the
            // TOO alert is generated.
            if (update.isReady()) _markReady(res);
        } catch (final SPException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException("unexpected problem in the middle of an update", ex);
        }

        return res;
    }
}
