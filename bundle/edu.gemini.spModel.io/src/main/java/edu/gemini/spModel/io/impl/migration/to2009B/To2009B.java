package edu.gemini.spModel.io.impl.migration.to2009B;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Tuple2;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.pio.Container;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Version;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;


import java.util.Map;
import java.util.Set;

/**
 * Ad-hoc code to update a program from the pre-2009B target list to the new
 * {@link TargetEnvironment}.  Includes updates to the offset iterators.  The
 * fixes are applied to each observation that has a target environment with a
 * version number before 2009B.  Otherwise, the observation is not updated.
 *
 * <p>The code works by allowing an observation, its target environment and
 * its offset iterators to be parsed by the normal 2009B code, then explicitly
 * fixing them based on the XML.
 */
public enum To2009B {
    instance;

    private static final Version VERSION_2009B = Version.match("2009B-1");

    public void update(final ISPObservation obs, final Container obsContainer)  {
        // Find the targets container in order to get the version and the
        // ParamSet containing the serialized position list.
        final Container targetsContainer = obsContainer.getContainer("Targets");
        if (targetsContainer == null) return; // no targets anyway

        // Check the version to see if an update is needed.
        final Version v = targetsContainer.getVersion();
        if (v.compareTo(VERSION_2009B) >= 0) return; // up-to-date

        // Get the position list param set.
        final ParamSet posListPset = getPosListPset(targetsContainer);
        if (posListPset == null) return; // no pos list

        // Get the available guiders in the observation.
        final Set<GuideProbe> guiders = GuideProbeUtil.instance.getAvailableGuiders(obs);

        // Parse the old position list into a TargetEnvironment.
        final Tuple2<TargetEnvironment, Map<String, SPTarget>> res;
        res = SPTargetPosListParser.instance.parse(obs.getObservationID(), posListPset, guiders);

        TargetEnvironment env = res._1();
        final Map<String, SPTarget> tagMap = res._2();

        // Update the probe "links" in the offset iterators (if any).  They
        // used to point to specific targets via their tag.  Now they need to
        // be set to the appropriate GuideOption for each probe in question.

        // Note, in addition to the side effect of updating the offset
        // iterators, we get back a map of guide probes to SPTarget tags.  This
        // map indicates which targets should be used as the primary target for
        // each probe.
        final Map<GuideProbe, String> primaryTargetMap = OffsetIteratorUpdater.instance.updateOffsetIterators(obs,
                obsContainer, env.getOrCreatePrimaryGuideGroup().getReferencedGuiders());

        // Use the primaryTargetMap to correct the primary guide star.
        for (final GuideProbe guider : primaryTargetMap.keySet()) {
            final String targetTag = primaryTargetMap.get(guider);
            final SPTarget target = tagMap.get(targetTag);
            if (target != null) {
                final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
                final GuideProbeTargets gt = gtOpt.getValue().selectPrimary(target);
                env = env.putPrimaryGuideProbeTargets(gt);
            }
        }

        // Update the target comp with the fresh 2009B compatible target env.
        updateTargetComponent(obs, env);
    }

    private void updateTargetComponent(final ISPObservation obs, final TargetEnvironment env)  {
        final ISPObsComponent targetNode = getTargetNode(obs);
        if (targetNode == null) return;

        // Update the target component
        final TargetObsComp targetDataObj = (TargetObsComp) targetNode.getDataObject();
        targetDataObj.setTargetEnvironment(env);
        targetNode.setDataObject(targetDataObj);
    }

    private ParamSet getPosListPset(final Container targetsContainer) {
        final ParamSet pset = targetsContainer.getParamSet("Targets");
        if (pset == null) return null;
        return pset.getParamSet("posList");
    }

    private ISPObsComponent getTargetNode(final ISPObservation obs)  {
        return obs.getObsComponents().stream()
                .filter(obsComp -> TargetObsComp.SP_TYPE.equals(obsComp.getType()))
                .findFirst().orElse(null);
    }
}
