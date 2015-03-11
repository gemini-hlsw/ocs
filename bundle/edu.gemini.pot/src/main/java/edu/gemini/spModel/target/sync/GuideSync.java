//
// $
//

package edu.gemini.spModel.target.sync;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.target.env.GuideEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;


import java.util.*;

/**
 * An event monitor that keeps guider information up-to-date in the target
 * environment observation component. This code watches for structure changes
 * to observations because they can result in new guiders becoming available or
 * existing guiders being removed.  For example, when an instrument is added to
 * an observation it can have associated on-instrument guiders.
 *
 * <p>This code is added in the node initializer for the observation and runs
 * in the database when changes are made.
 */
public enum GuideSync implements ISPEventMonitor {
    instance;

    private static final long serialVersionUID = 2l;

    // An inner class that handles collecting the relevant information and
    // performing the updates.
    private static class Updater {
        private final ISPObsComponent targetObsNode;
        private final TargetObsComp targetObsComp;
        private final TargetEnvironment targetEnvironment;

        private final Set<GuideProbe> availableGuiders;

        Updater(ISPObservation obs)  {
            // Initialize target obs comp fields.
            targetObsNode = findTargetObsNode(obs);
            if (targetObsNode == null) {
                targetObsComp = null;
                targetEnvironment = null;
            } else {
                targetObsComp = (TargetObsComp) targetObsNode.getDataObject();
                targetEnvironment = targetObsComp.getTargetEnvironment();
            }

            availableGuiders = Collections.unmodifiableSet(GuideProbeUtil.instance.getAvailableGuiders(obs));
        }

        // Synchronizes the enabled/disabled state of guide targets according to
        // the context of the observation.
        // Returns true if any changes are made, false otherwise
        boolean updateTargetEnvironment()  {
            // If there is no target environment, there is nothing to update.
            TargetEnvironment env = targetEnvironment;
            if (env == null) return false;

            boolean updated = false;

            GuideEnvironment guideEnv = env.getGuideEnvironment();
            Set<GuideProbe> curActive = guideEnv.getActiveGuiders();
            if (!curActive.equals(availableGuiders)) {
                updated = true;
                env = env.setGuideEnvironment(guideEnv.setActiveGuiders(availableGuiders));
            }

            if (updated) {
                targetObsComp.setTargetEnvironment(env);
                targetObsNode.setDataObject(targetObsComp);
            }
            return updated;
        }


        // Searches the observation's components for the target component.
        private static ISPObsComponent findTargetObsNode(ISPObservation obs)  {
            for (ISPObsComponent obsComp : obs.getObsComponents()) {
                if (TargetObsComp.SP_TYPE.equals(obsComp.getType())) return obsComp;
            }
            return null;
        }
    }

    public void structureChanged(SPStructureChange change) {
        if (ignoreStructureChange(change)) return;
        update((ISPObservation) change.getModifiedNode());
    }

    @SuppressWarnings({"unchecked"})
    private boolean ignoreStructureChange(SPStructureChange change)  {
        // We only care about structure changes to observations in which
        // obs components are involved.
        if (!ISPObservation.OBS_COMPONENTS_PROP.equals(change.getPropertyName())) return true;
        ISPNode node = change.getModifiedNode();
        if (!(node instanceof ISPObservation)) return true;

        // More specifically, we only care if the obs component that has been
        // added or removed are guide probe providers.
        Set<ISPObsComponent> oldSet = new HashSet<ISPObsComponent>((Collection<ISPObsComponent>) change.getOldValue());
        Set<ISPObsComponent> newSet = new HashSet<ISPObsComponent>((Collection<ISPObsComponent>) change.getNewValue());

        // See if we removed a guide probe provider.
        Set<ISPObsComponent> rmSet = new HashSet<ISPObsComponent>(oldSet);
        rmSet.removeAll(newSet);
        for (ISPObsComponent obsComp : rmSet) {
            Object dataObj = obsComp.getDataObject();
            if (dataObj instanceof GuideProbeProvider) return false;
        }

        // See if we added a guide probe provider.
        Set<ISPObsComponent> addedSet = new HashSet<ISPObsComponent>(newSet);
        addedSet.removeAll(oldSet);
        for (ISPObsComponent obsComp : addedSet) {
            Object dataObj = obsComp.getDataObject();
            if (dataObj instanceof GuideProbeProvider) return false;
        }

        return true;
    }

    public void propertyChanged(SPCompositeChange change) {
        if (!SPUtil.getDataObjectPropertyName().equals(change.getPropertyName())) return;

        final ISPNode node = change.getModifiedNode();
        // Ignore updates to objects that aren't to the target obs component
        // or (REL-542) Altair ... Should be done through a marker interface ...
        final Object dataObj = node.getDataObject();
        if (!(dataObj instanceof TargetObsComp) && !(dataObj instanceof InstAltair)) return;

        // Ignore updates to objects that aren't in an observation.  This can
        // happen for example if the component is in a conflict folder.
        final ISPNode parent = node.getParent();
        if (parent instanceof ISPObservation) {
            update((ISPObservation) parent);
        }
    }

    /**
     * Updates the target or offset position guide options as necessary
     * according to the current set of guiders available in the observation
     * and the set referenced by the target component and offset positions.
     *
     * @param obs observation whose target and offsets should be updated
     *
     * @ is required by the API and can be thrown if
     * called remotely, but in general this method will run inside of the
     * ODB and the RemoteException will not happen
     */
    private static void update(ISPObservation obs)  {
        if (obs == null) return;
        Updater up = new Updater(obs);
        up.updateTargetEnvironment();
    }
}