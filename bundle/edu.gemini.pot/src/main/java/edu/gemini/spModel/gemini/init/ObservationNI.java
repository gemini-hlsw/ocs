// Copyright 1999-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ObservationNI.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.gemini.init;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.obscomp.GemObservationCB;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.ObsQaState;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obslog.ObsExecLog;
import edu.gemini.spModel.obslog.ObsQaLog;
import edu.gemini.spModel.obsrecord.ObsExecStatus;
import edu.gemini.spModel.seqcomp.InstrumentSequenceSync;
import edu.gemini.spModel.seqcomp.SeqBase;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.telescope.IssPortSync;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Initializes <code>{@link ISPObservation}</code> nodes.
 */
public class ObservationNI implements ISPNodeInitializer<ISPObservation, SPObservation> {
    private static final Logger LOG = Logger.getLogger(ObservationNI.class.getName());

    /**
     * A special instance for use when importing programs from XML
     */
    public static final ObservationNI NO_CHILDREN_INSTANCE = new ObservationNI() {
        protected void addSubnodes(ISPFactory factory, ISPObservation obsNode) {
        }
    };

    @Override
    public SPComponentType getType() {
        return SPComponentType.OBSERVATION_BASIC;
    }

    @Override
    public SPObservation createDataObject() {
        return new SPObservation();
    }

    @Override
    public void initNode(ISPFactory factory, ISPObservation obsNode) {

        obsNode.setDataObject(createDataObject());

        // Add the standard subnodes
        addSubnodes(factory, obsNode);

        // Set the configuration builder
        updateNode(obsNode);
    }

    /**
     * Add the standard observation subnodes.
     */
    protected void addSubnodes(ISPFactory factory, ISPObservation obsNode) {
        // All observations come with:
        // 1. A SiteQuality obscomp
        // 2. A target list
        // 3. Obslog
        // 4. An empty seq component "folder"

        // 1,2. site quality and target
        ISPObsComponent siteQual;
        ISPObsComponent targetEnv;
        try {
            siteQual = factory.createObsComponent(obsNode.getProgram(),
                    SPSiteQuality.SP_TYPE, null);

            targetEnv = factory.createObsComponent(obsNode.getProgram(),
                    TargetObsComp.SP_TYPE, null);
            if (siteQual != null && targetEnv != null) {
                List<ISPObsComponent> l = new ArrayList<ISPObsComponent>();
                l.add(siteQual);
                l.add(targetEnv);
                obsNode.setObsComponents(l);
            }
        } catch (SPException ex) {
            // If this fails, log a message.
            LOG.log(Level.SEVERE, "Failed while initializing observation conditions and target env.", ex);
        }

        // 3. obslog
        try {
            obsNode.setObsExecLog(factory.createObsExecLog(obsNode.getProgram(), null));
            obsNode.setObsQaLog(factory.createObsQaLog(obsNode.getProgram(), null));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed while initializing obslog.", ex);
            throw new RuntimeException(ex);
        }

        // 4. sequence root
        ISPSeqComponent seqRoot;
        try {
            seqRoot = factory.createSeqComponent(obsNode.getProgram(),
                    SeqBase.SP_TYPE, null);
            if (seqRoot != null) {
                obsNode.setSeqComponent(seqRoot);
            }
        } catch (SPException ex) {
            // If this fails, log a message.
            LOG.log(Level.SEVERE, "Failed while initializing an observation.");
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void updateNode(ISPObservation obs) {
        // Set the configuration builder
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new GemObservationCB(obs));
        obs.putClientData(InstrumentSequenceSync.USER_OBJ_KEY, InstrumentSequenceSync.instance);

        // Turn off electronic offset sync for now.  The only usage is
        // currently F2 and it has been modified to no longer support its own
        // offset positions.
//        node.putClientData("ElectronicOffsetSync", ElectronicOffsetSync.instance);

        obs.putClientData("IssPortSync", IssPortSync.instance);
    }

    public static void reset(ISPObservation obs, boolean sameProgram) {
        SPObservation spObs = (SPObservation) obs.getDataObject();

        // SCT-211: make sure that the target program TOO type is compatible
        // with the current setting of the observation TOO type.
        final TooType progTooType = Too.get(obs.getProgram());
        switch (progTooType) {
            case none:
            case standard:
                spObs.setOverrideRapidToo(false);
                break;
            case rapid:
                // leave the override alone (i.e., keep if it exists)
                break;
        }

        // reset the time correction log and status in the copy
        // (SCT-211: but don't reset "on hold" TOO observations)
        // REL-373: On hold observations must revert to phase 2 when copied to another program
        if (!(sameProgram && Too.isToo(obs) && (spObs.getPhase2Status() == ObsPhase2Status.ON_HOLD))) {
            spObs.setPhase2Status(ObsPhase2Status.PI_TO_COMPLETE);
            spObs.setExecStatusOverride(None.<ObsExecStatus>instance());
        }
        spObs.setOverriddenObsQaState(ObsQaState.UNDEFINED);
        spObs.setOverrideQaState(false);
        spObs.resetObsTimeCorrections();

        // store changes
        obs.setDataObject(spObs);

        // Reset the observing log.
        final ISPObsQaLog qa = obs.getObsQaLog();
        if (qa != null) qa.setDataObject(new ObsQaLog());
        final ISPObsExecLog exec = obs.getObsExecLog();
        if (exec != null) exec.setDataObject(new ObsExecLog());

        // Reset disabled automatic groups.
        for (ISPObsComponent oc : obs.getObsComponents()) {
            if (oc.getType() == SPComponentType.TELESCOPE_TARGETENV) {
                final TargetObsComp     toc = (TargetObsComp) oc.getDataObject();
                final TargetEnvironment env = toc.getTargetEnvironment();
                final GuideEnvironment genv = env.getGuideEnvironment();

                if (genv.guideEnv().auto() instanceof AutomaticGroup.Disabled$) {
                    toc.setTargetEnvironment(
                        TargetEnv.auto().set(env, AutomaticGroup.Initial$.MODULE$)
                    );
                    oc.setDataObject(toc);
                }
            }
        }
    }
}
