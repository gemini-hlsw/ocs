package edu.gemini.spModel.template;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.security.UserRolePrivileges;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.util.AsterismEditUtil;

import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.gemini.spModel.template.FunctorHelpers.addIfNotPresent;
import static edu.gemini.spModel.template.FunctorHelpers.findComponentByType;
import static edu.gemini.spModel.template.FunctorHelpers.findInstrument;
import static edu.gemini.spModel.template.FunctorHelpers.lookupNode;

/**
 * Functor that reapplies the originating templates for a set of observations. In the case of science observations
 * (whose templates don't contain targets) the re-application preserves the target environment and position angle
 * (if any). Note that the target node for execute() is ignored.
 */
public final class ReapplicationFunctor extends DBAbstractFunctor {

    private static final Logger LOGGER = Logger.getLogger(ReapplicationFunctor.class.getName());

    private final UserRolePrivileges urps;
    private final Set<ISPObservation> selection = new HashSet<>();

    public ReapplicationFunctor(UserRolePrivileges urps) {
        this.urps = urps;
    }

    /**
     * Add to the set of observations to have their templates reapplied. Note that the observation must have an
     * originating template reference, and this template reference must be valid; if either ends up being false,
     * the template reapplication will fail for the specified node. It is important to check the result to see
     * what happened for each node.
     */
    public void add(ISPObservation obsNode) {
        selection.add(obsNode);
    }

    public void execute(IDBDatabaseService db, ISPNode ignored, Set<Principal> principals) {
        try {
            for (ISPObservation obs : selection)
                reapply(db, obs, urps);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Trouble during template re-application.", e);
            setException(e);
        }
    }

    private static void reapply(IDBDatabaseService db, ISPObservation obs, UserRolePrivileges urps) throws SPException {

        // Get the originating template key, if any
        final SPObservation obsData = (SPObservation) obs.getDataObject();
        final SPNodeKey key = obsData.getOriginatingTemplate();
        if (key == null) {
            throw new IllegalArgumentException("No originating template found for " + obs.getObservationID());
        }

        // Get the template itself
        final ISPProgram prog = db.lookupProgram(obs.getProgramKey());
        final ISPObservation templateObs = (ISPObservation) lookupNode(key, prog);
        if (templateObs == null) {
            throw new IllegalArgumentException("Originating template for " + obs.getObservationID() + " no longer exists.");
        }

        // Check state and privileges
        if (!ReapplicationCheckFunctor.canReapply(db, urps, obs)) {
            throw new IllegalArgumentException("Reapply not allowed for " + obs.getObservationID());
        }

        // Ok, we're good.
        reapply(db, obs, templateObs);

    }

    private static void reapply(IDBDatabaseService db, ISPObservation obs, ISPObservation originalTemplateObs) throws SPException {

        // Get a handle to the existing target and instrument (if any)
        final ISPObsComponent oldTarget = findComponentByType(obs, TargetObsComp.SP_TYPE);
        final ISPObsComponent oldConditions = findComponentByType(obs, SPSiteQuality.SP_TYPE);
        final ISPObsComponent oldInstrument = findInstrument(obs);

        // Remember the observation status.
        SPObservation oldDataObject = (SPObservation) obs.getDataObject();
        final ObsPhase2Status oldStatus = oldDataObject.getPhase2Status();

        // Copy the beef from the template observation into the target observation
        final ISPObservation templateObs = db.getFactory().createObservationCopy(obs.getProgram(), originalTemplateObs, false);

        // Data Object
        final SPObservation dataObject = (SPObservation) templateObs.getDataObject();
        dataObject.setOriginatingTemplate(originalTemplateObs.getNodeKey());
        dataObject.setPhase2Status(oldStatus);
        obs.setDataObject(dataObject);

        // Children
        final List<ISPNode> children = templateObs.getChildren();
        templateObs.setChildren(Collections.<ISPNode>emptyList()); // must detach children first
        obs.setChildren(children);

        // Unless it is a DAY_CAL, restore the target, conditions and position angle.
        // See REL-2165.
        final ObsClass newObsClass = ObsClassService.lookupObsClass(obs);
        if (newObsClass != ObsClass.DAY_CAL) {
            restoreScienceDetails(obs, oldTarget, oldConditions, oldInstrument);
        }
    }

    private static void restoreScienceDetails(final ISPObservation obs, final ISPObsComponent oldTarget, final ISPObsComponent oldConditions, final ISPObsComponent oldInstrument) throws SPNodeNotLocalException, SPTreeStateException {

        // Restore old target and conditions if not already present
        addIfNotPresent(obs, oldTarget);
        AsterismEditUtil.matchAsterismToInstrument(obs);
        addIfNotPresent(obs, oldConditions);

        // Restore the position angle, if any
        if (oldInstrument != null) {
            final ISPObsComponent newInstrument = findInstrument(obs);
            if (newInstrument != null) {
                final SPInstObsComp oldData = (SPInstObsComp) oldInstrument.getDataObject();
                final SPInstObsComp newData = (SPInstObsComp) newInstrument.getDataObject();
                // REL-814 Delegate to the instrument the decision of what data to restore
                newData.restoreScienceDetails(oldData);
                newInstrument.setDataObject(newData);
            }
        }

    }


}
