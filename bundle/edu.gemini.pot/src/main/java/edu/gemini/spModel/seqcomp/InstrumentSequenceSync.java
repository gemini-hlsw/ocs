package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.Tuple2;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.util.SPTreeUtil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


// SW: Moving code from the OT's SPTreeEditUtil so that it can be used from the
// model itself (particularly during XML import).

// SW: sorry this is all just so very awful

/**
 * Utility for keeping the instrument static component in sync with the first
 * step of the sequence.
 * <p/>
 * <p>This code is added in the node initializer for the observation and runs
 * in the database when changes are made.
 */
public enum InstrumentSequenceSync implements ISPEventMonitor {
    instance;

    public static final String USER_OBJ_KEY = "InstrumentSequenceSync";

    private static final Logger LOG = Logger.getLogger(InstrumentSequenceSync.class.getName());

    @Override
    public void structureChanged(SPStructureChange change) {
        final ISPObservation obs = change.getModifiedNode().getContextObservation();
        if (obs == null) return;

        if (addedFirstInstrumentIterator(change) || addedInstrumentOrEngineeringComponent(change)) {
            syncFromIterator(obs);
        }
    }

    @Override
    public void propertyChanged(SPCompositeChange change) {
        final ISPObservation obs = change.getModifiedNode().getContextObservation();
        if (obs == null) return;

        final ISPNode node = change.getModifiedNode();
        if (isInstrumentOrEngineering(node)) {
            syncFromInstrument(obs);
        } else if (isInstrumentIterator(node)) {
            syncFromIterator(obs);
        }
    }

    @SuppressWarnings({"unchecked"})
    private boolean addedInstrumentOrEngineeringComponent(SPStructureChange change) {
        // filter anything but changes to obs components
        if (!ISPObservation.OBS_COMPONENTS_PROP.equals(change.getPropertyName()))
            return false;

        // What did we add (if anything)
        final Set<ISPObsComponent> oldSet = new HashSet<>((Collection<ISPObsComponent>) change.getOldValue());
        final Set<ISPObsComponent> newSet = new HashSet<>((Collection<ISPObsComponent>) change.getNewValue());
        final Set<ISPObsComponent> addedSet = new HashSet<>(newSet);
        addedSet.removeAll(oldSet);

        // Did we add an instrument or eng component?
        for (ISPObsComponent obsComponent : addedSet) {
            if (isInstrumentOrEngineering(obsComponent)) return true;
        }
        return false; // not adding an instrument or engineering component.
    }

    public static ISPSeqComponent firstInstrumentIterator(ISPSeqComponent root) {
        if (root == null) return null;
        if (isInstrumentIterator(root)) return root;
        for (ISPSeqComponent child : root.getSeqComponents()) {
            final ISPSeqComponent first = firstInstrumentIterator(child);
            if (first != null) return first;
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private boolean addedFirstInstrumentIterator(SPStructureChange change) {
        final ISPObservation obs = change.getModifiedNode().getContextObservation();
        if (obs == null) return false;

        // filter anything but changes to seq components
        final String propName = change.getPropertyName();
        if (ISPObservation.SEQ_COMPONENT_PROP.equals(propName)) {
            return firstInstrumentIterator(obs.getSeqComponent()) != null;
        }

        if (ISPSeqComponent.SEQ_COMPONENTS_PROP.equals(propName)) {
            for (ISPSeqComponent sc : ((ISPSeqComponent) change.getModifiedNode()).getSeqComponents()) {
                final ISPSeqComponent first = firstInstrumentIterator(sc);
                if (first != null) {
                    // is the added instrument iterator *the* first instrument iterator?
                    return first == firstInstrumentIterator(obs.getSeqComponent());
                }
            }
        }

        return false;
    }

    private boolean isInstrumentOrEngineering(ISPNode node) {
        if (node instanceof ISPObsComponent) {
            final SPComponentType t = ((ISPObsComponent) node).getType();
            if (SPComponentBroadType.INSTRUMENT.equals(t.broadType))
                return true;
            if (SPComponentBroadType.ENGINEERING.equals(t.broadType))
                return true;
        }
        return false;
    }

    private static boolean isInstrumentIterator(ISPNode node) {
        return node.getDataObject() instanceof SeqConfigComp;
    }

    private static final class SyncContext {
        // Instrument
        final ISPObsComponent instNode;
        final ISPDataObject instObject;

        // (Optional) engineering component
        final ISPObsComponent engNode;
        final ISPDataObject engObject;

        // Iterator
        final ISPSeqComponent seqNode;
        final ISPDataObject seqObject;
        final ISysConfig sysConfig;

        private SyncContext(ISPObsComponent instNode, ISPDataObject instObject,
                            ISPObsComponent engNode, ISPDataObject engObject,
                            ISPSeqComponent seqNode, ISPDataObject seqObject, ISysConfig sysConfig) {
            this.instNode = instNode;
            this.instObject = instObject;
            this.engNode = engNode;
            this.engObject = engObject;
            this.seqNode = seqNode;
            this.seqObject = seqObject;
            this.sysConfig = sysConfig;
        }

        void updateInstrument() {
            instNode.setDataObject(instObject);
        }

        void updateEngineering() {
            if (engNode != null) engNode.setDataObject(engObject);
        }

        void updateSysConfig() {
            ((IConfigProvider) seqObject).setSysConfig(sysConfig);
            seqNode.setDataObject(seqObject);
        }

        static SyncContext apply(ISPObservation obs) {
            final ISPObsComponent instNode = SPTreeUtil.findInstrument(obs);
            if (instNode == null) return null; // nothing to update

            final ISPSeqComponent rootSeqNode = obs.getSeqComponent();
            if (rootSeqNode == null) return null; // nothing to update from

            final ISPObsComponent engNode = SPTreeUtil.findObsComponentByBroadType(obs, SPComponentBroadType.ENGINEERING);
            final ISPDataObject engObject = (engNode == null) ? null : engNode.getDataObject();

            // LORD OF DESTRUCTION: DataObjectManager get without set
            final ISPDataObject instObject = instNode.getDataObject();
            if (!(instObject instanceof PropertyProvider)) return null;
            final String narrowType = instObject.getType().narrowType;
            final ISPSeqComponent seqNode = SPTreeUtil.findSeqComponentByNarrowType(rootSeqNode, narrowType, true);
            if (seqNode == null) return null;

            // LORD OF DESTRUCTION: DataObjectManager get without set
            final ISPDataObject seqObj = seqNode.getDataObject();
            if (!(seqObj instanceof IConfigProvider)) return null;
            final ISysConfig sysConfig = ((IConfigProvider) seqObj).getSysConfig();
            if (sysConfig == null) return null;

            return new SyncContext(instNode, instObject, engNode, engObject, seqNode, seqObj, sysConfig);
        }
    }

    /**
     * Using the values of the first step of the first instrument iterator (if
     * any) as a starting point, bring the static instrument and engineering
     * components (if any) into sync with the iterator.
     */
    public static void syncFromIterator(ISPObservation obs) {
        final SyncContext ctx = SyncContext.apply(obs);
        if (ctx == null) return;  // nothing to update

        boolean updatedInstrument = false;
        if (updateInstrumentFromSysConfig(ctx.instObject, ctx.sysConfig)) {
            updatedInstrument = true;
            ctx.updateInstrument();
        }

        // If there is an engineering component, update it too
        if ((ctx.engNode != null) && updateInstrumentFromSysConfig(ctx.engObject, ctx.sysConfig)) {
            updatedInstrument = true;
            ctx.updateEngineering();
        }

        // Setting some parameters can cause others to be updated
        // automatically.  For example, the Flamingos2 FPU automatically
        // updates the Decker when changed in the static component.
        // In other cases, invalid updates in the first step are ignored in
        // the static component (GNIRS LXD mode when Pixel Scale is 0.15).
        // Having updated the instrument from the iterator first row,
        // update the iterator from the static component to make sure
        // we are displaying and using synchronized values.

        // Note, if we did update the instrument then we'll call
        // updateIteratorFromStatic via the event monitor. If we didn't, it
        // could be that changes were rejected (as in the GNIRS LXD mode with
        // Pixel Scale 0.15 example) so sync sys config here. :/
        if (!updatedInstrument && updateIteratorFromStatic(ctx.sysConfig, ctx.instObject)) {
            ctx.updateSysConfig();
        }
    }

    /**
     * Updates the instrument data object from the sys config if there are
     * differences to apply.
     *
     * @return <code>true</code> if the data object was updated,
     *         <code>false</code> otherwise
     */
    private static boolean updateInstrumentFromSysConfig(ISPDataObject inst, ISysConfig sysConfig) {
        if (!(inst instanceof PropertyProvider)) return false;
        final Map<String, PropertyDescriptor> props = ((PropertyProvider) inst).getProperties();

        final class ChangeRegister implements PropertyChangeListener {
            boolean changeRegistered = false;

            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                changeRegistered = true;
            }
        }

        final ChangeRegister changeReg = new ChangeRegister();
        try {
            inst.addPropertyChangeListener(changeReg);

            // Split the parameter set into (volatile, non-volatile) params.
            // "Volatile" means that they change automatically as a result of
            // updating other parameters.
            Tuple2<Collection<IParameter>, Collection<IParameter>> tup;
            tup = separateVolatileParameters(sysConfig, props);

            // Apply the non-volatile parameters first.  Setting some of these
            // can cause the volatile parameter values to update.
            setInstrumentValues(inst, tup._2(), props);

            // Apply the volatile parameters files next.  This can override
            // automatic updates made by the non-volatile parameters.  We want
            // to do it in this order so that we can avoid having the volatile
            // parameter value display change when the "Save" button is pressed.
            // In other words, to avoid displaying one value when in fact the
            // value has been replaced w/o the user knowing yet.
            setInstrumentValues(inst, tup._1(), props);

        } finally {
            inst.removePropertyChangeListener(changeReg);
        }

        return changeReg.changeRegistered;
    }

    // Separates the set of parameters into a tuple:
    // (collection of volatile params, collection of non-volatile params).
    // "Volatile" means that the parameter might change automatically as a
    // result of updates to other parameters.
    private static Tuple2<Collection<IParameter>, Collection<IParameter>> separateVolatileParameters(
            ISysConfig sysConfig,
            Map<String, PropertyDescriptor> props) {
        final List<IParameter> vParams = new ArrayList<>();
        final List<IParameter> nvParams = new ArrayList<>();

        for (IParameter p : sysConfig.getParameters()) {
            final PropertyDescriptor pd = props.get(p.getName());
            if (pd == null) continue;
            (PropertySupport.isVolatile(pd) ? vParams : nvParams).add(p);
        }

        return new Pair<>(vParams, nvParams);
    }

    // Applies the first row values to the instrument static component.
    private static void setInstrumentValues(
            ISPDataObject dataObject,
            Collection<IParameter> params,
            Map<String, PropertyDescriptor> props) {

        for (IParameter p : params) {
            // don't propagate title (fix for OT-367)
            if (ISPDataObject.TITLE_PROP.equals(p.getName())) continue;

            // noinspection unchecked
            final List<Object> valueList = (List<Object>) p.getValue();
            if ((valueList == null) || (valueList.size() == 0)) continue;

            final PropertyDescriptor pd = props.get(p.getName());
            if (pd == null) continue;

            final Object val = valueList.get(0);
            if (val == null) {
                LOG.warning("Null value in first step of instrument sequence.  type=" + dataObject.getType() + ", property=" + pd.getName());
                // we're supposedly updating the static component here, so
                // don't mess with the sequence node!
                // apply instrument value to blank first row
                // Object obj = getDefaultParamValue(dataObject, pd);
                // if (obj != null) valueList.set(0, obj);
            } else {
                try {
                    pd.getWriteMethod().invoke(dataObject, val);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Return the given parameter value from the given data object.
     *
     * @param dataObject usually an instrument data object
     * @return the parameter value, if found, otherwise null
     */
    private static Object getDefaultParamValue(ISPDataObject dataObject,
                                               PropertyDescriptor pd) {
        if (dataObject == null) return null;

        try {
            return pd.getReadMethod().invoke(dataObject);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * The values in the given (instrument) data object are propagated to the
     * first row of the first instrument iterator.
     */
    public static void syncFromInstrument(ISPObservation obs) {
        final SyncContext ctx = SyncContext.apply(obs);
        if (ctx == null) return;

        if (updateIteratorFromStatic(ctx.sysConfig, ctx.instObject) ||
                updateIteratorFromStatic(ctx.sysConfig, ctx.engObject)) {
            ctx.updateSysConfig();
        }
    }


    /**
     * Update the given SysConfig with the values in the given (instrument or
     * engineering) data object.
     *
     * @return <code>true</code> if changes were applied to the sys config;
     *         <code>false</code> otherwise
     */
    private static boolean updateIteratorFromStatic(ISysConfig sysConfig, ISPDataObject dataObject) {
        if (dataObject == null) return false;

        final Map<String, PropertyDescriptor> props = ((PropertyProvider) dataObject).getProperties();

        boolean updated = false;
        for (IParameter param : sysConfig.getParameters()) {
            // Get a mutable value list.
            //noinspection unchecked
            final List<Object> valueList = new ArrayList<>((List<Object>) param.getValue());
            if (valueList.size() == 0) continue;

            final String propertyName = param.getName();
            final PropertyDescriptor pd = props.get(propertyName);
            if (pd == null) continue;

            final Object val = getDefaultParamValue(dataObject, pd);

            if (!updated) {
                final Object curVal = valueList.get(0);
                updated = (val == null) ? (curVal != null) : !val.equals(curVal);
            }

            valueList.set(0, val);
            param.setValue(valueList);
        }

        return updated;
    }
}
