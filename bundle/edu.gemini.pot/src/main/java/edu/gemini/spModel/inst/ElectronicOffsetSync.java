//
// $
//

package edu.gemini.spModel.inst;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;


import java.util.logging.Logger;

/**
 * An event monitor that keeps the electronic offsetting feature provided by
 * some instruments in sync with its environment.  In particular, in order to
 * use electronic offsetting, the observation must contain a target component
 * that has a guide star associated with the instrument's OIWFS.  In addition,
 * there can be no offset iterators in the sequence.
 *
 * <p>This code watches for structure changes to observations because they can
 * result in an observation no longer being able to support electronic
 * offsetting.  For example, when the target component is removed from an
 * observation it no longer has an associated OIWFS. It also watches the target
 * observation component for changes to guide OIWFS guide targets.
 *
 * <p>This code is added in the node initializer for the observation and runs
 * in the database when changes are made.  The API for dealing with the
 * science program declares RemoteExceptions for each method, but these will
 * not be encountered in this code since it is running in the database.
 */
public enum ElectronicOffsetSync implements ISPEventMonitor {
    instance;

    private static final long serialVersionUID = 1l;

    private static final Logger LOG = Logger.getLogger(ElectronicOffsetSync.class.getName());

    /**
     * A ChangeEnv class contains the observation and instrument that will be
     * impacted by the event.
     */
    private static class ChangeEnv {

        /**
         * Creates a {@link Some}<ChangeEnv> provided that the observation
         * contains an instrument which supports
         * {@link ElectronicOffsetProvider} and electronic offsetting is
         * enabled.  Otherwise returns {@link None}.
         *
         * @param node node whose content changed
         *
         * @ should never happen since this code is
         * running in the database
         */
        static Option<ChangeEnv> create(ISPNode node)  {
            ISPObservation obs = getObservation(node);
            if (obs == null) return None.instance();

            ISPObsComponent inst = null;
            ISPObsComponent target = null;

            // Find the target and instrument.
            for (ISPObsComponent obsComp : obs.getObsComponents()) {
                SPComponentType type = obsComp.getType();
                if (TargetObsComp.SP_TYPE.equals(type)) {
                    target = obsComp;
                } else if (SPComponentBroadType.INSTRUMENT.equals(type.broadType)) {
                    inst = obsComp;

                    // Only care about instruments that support e-offseting
                    if (!ElectronicOffsetProvider.class.isInstance(inst.getDataObject())) {
                        return None.instance();
                    }

                    // Only care if electronic offsetting is turned on.
                    ElectronicOffsetProvider prov;
                    prov = (ElectronicOffsetProvider) inst.getDataObject();
                    if (!prov.getUseElectronicOffsetting()) {
                        return None.instance();
                    }
                }
            }

            // If there is no instrument, there is nothing to update.
            if (inst == null) return None.instance();

            // Wrap the target in an Option instance, since it may or may not
            // exist in the observation.
            Option<ISPObsComponent> none = None.instance();
            Option<ISPObsComponent> optTarget = target == null ? none : new Some<ISPObsComponent>(target);

            // Create the ChangeEnv wrapped in an Option
            return new Some<ChangeEnv>(new ChangeEnv(obs, inst, optTarget));
        }

        // Gets the observation in which the node finds itself, if any
        private static ISPObservation getObservation(ISPNode node)  {
            if (node instanceof ISPObservation) return (ISPObservation) node;
            if (node == null) return null;
            return getObservation(node.getParent());
        }

        final ISPObservation obs;
        final ISPObsComponent inst;
        private final Option<ISPObsComponent> target;

        private ChangeEnv(ISPObservation obs, ISPObsComponent inst, Option<ISPObsComponent> target) {
            this.obs    = obs;
            this.inst   = inst;
            this.target = target;
        }


        ElectronicOffsetProvider getOffsetProvider()  {
            return (ElectronicOffsetProvider) inst.getDataObject();
        }

        /**
         * @return <code>true</code> if the target environment associated with
         * this change contains a guide star defined for the OIWFS
         */
        boolean containsElectronicOffsettingGuider()  {
            // If there is no target component, then there is no target
            // defined for this guider.
            if (None.instance().equals(target)) return false;

            // Figure out the guider to use.
            ElectronicOffsetProvider prov;
            prov = (ElectronicOffsetProvider) inst.getDataObject();
            GuideProbe guider = prov.getElectronicOffsetGuider();

            // Get the target environment.
            TargetObsComp targetComp;
            targetComp = (TargetObsComp) target.getValue().getDataObject();
            TargetEnvironment env = targetComp.getTargetEnvironment();

            // Figure out whether it supports the indicated guide star.
            Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            if (gtOpt.isEmpty()) return false;
            return (gtOpt.getValue().getOptions().size() > 0);
        }

        /**
         * @return <code>true</code> if the observation contains offset
         * iterators in addition to the electronic offset provider
         */
        boolean containsAdditionalOffsetIterators()  {
            return containsOffsetIterators(obs.getSeqComponent());
        }

        private boolean containsOffsetIterators(ISPSeqComponent comp)  {
            if (comp == null) return false;
            if (comp.getDataObject() instanceof SeqRepeatOffsetBase) return true;
            for (ISPSeqComponent child : comp.getSeqComponents()) {
                if (containsOffsetIterators(child)) return true;
            }
            return false;
        }
    }

    // Turns off electronic offsetting.
    private void disableElectronicOffsetting(ChangeEnv change)  {
        ElectronicOffsetProvider provider = change.getOffsetProvider();
        if (!provider.getUseElectronicOffsetting()) return; // nothing to do
        provider.setUseElectronicOffsetting(false);
        change.inst.setDataObject((ISPDataObject) provider);
    }

    public void structureChanged(SPStructureChange change) {
//        try {
            Option<ChangeEnv> changeOpt;
            changeOpt = ChangeEnv.create(change.getModifiedNode());
            if (None.instance().equals(changeOpt)) return; // nothing to do

            ChangeEnv changeEnv = changeOpt.getValue();
            if (!changeEnv.containsElectronicOffsettingGuider()) {
                disableElectronicOffsetting(changeEnv);
            }
//        } catch (RemoteException ex) {
//            LOG.log(Level.SEVERE, "RemoteException in local code", ex);
//            throw new RuntimeException(ex);
//        }
    }

    public void propertyChanged(SPCompositeChange change) {
        // Only care about data object updates.
        if (!SPUtil.getDataObjectPropertyName().equals(change.getPropertyName())) return;

        ISPNode node = change.getModifiedNode();
//        try {
            // Ignore updates to objects that aren't to the target obs component
            // or to the instrument.
            Object dataObj = node.getDataObject();
            if (!(dataObj instanceof TargetObsComp) &&
                !(dataObj instanceof SPInstObsComp)) return;

            Option<ChangeEnv> changeOpt;
            changeOpt = ChangeEnv.create(change.getModifiedNode());
            if (None.instance().equals(changeOpt)) return; // nothing to do

            ChangeEnv changeEnv = changeOpt.getValue();
            if (!changeEnv.containsElectronicOffsettingGuider()) {
                disableElectronicOffsetting(changeEnv);
            }

//        } catch (RemoteException ex) {
//            LOG.log(Level.SEVERE, "Remote exception in local method call.", ex);
//            throw new RuntimeException(ex);
//        }
    }
}
