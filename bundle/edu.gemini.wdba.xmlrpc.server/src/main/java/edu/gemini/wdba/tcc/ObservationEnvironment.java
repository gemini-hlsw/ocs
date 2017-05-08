package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.ext.*;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.wdba.glue.api.WdbaGlueException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public final class ObservationEnvironment {

    private static final Logger LOG = Logger.getLogger(TccConfig.class.getName());

    private final ObservationNode _obs;
    private final Site _site;
    private final TargetEnvironment _targetEnv;

    private final Option<ISPDataObject> _ao;

    private final SPInstObsComp _inst;
    private final ITccInstrumentSupport _is;


    public ObservationEnvironment(ObservationNode obs, Map<SPComponentType, String> supportMap, Site site) throws WdbaGlueException {
        if (obs == null) throw new NullPointerException("null observation");

        _obs  = obs;
        _site = site;

        TargetNode targetNode = _obs.getTarget();
        if (targetNode == null) {
            _logAbort("No TargetEnv found in observation: " + _obs.getObservationId(), null);
            throw new RuntimeException("Did not abort");
        }

        _targetEnv = targetNode.getDataObject().getTargetEnvironment();

        AoNode aoNode = _obs.getAdaptiveOptics();
        if (aoNode == null) {
            _ao = None.instance();
        } else {
            _ao = new Some<>(aoNode.getDataObject());
        }

        ITccInstrumentSupport sup = null;
        InstrumentNode instNode = _obs.getInstrument();
        if (instNode == null) {
            LOG.fine("No instrument in observation: " + _obs.getObservationId());
            _inst = null;
        } else {
            _inst = instNode.getDataObject();
            String className = supportMap.get(_inst.getType());
            if (className != null) {
                try {
                    Class<?> cl = Class.forName(className);
                    LOG.fine("Map class is:" + className);
                    sup = _create(cl);
                } catch (ClassNotFoundException ex) {
                    LOG.warning("Could not locate the support class: " + className);
                }
            }
        }
        if (sup == null) {
            sup = ITccInstrumentSupport.DefaultInstrumentSupport.create(this);
        }
        _is = sup;
    }

    public Set<GuideProbe> getAvailableGuiders() {
        final List<ISPDataObject> dataObjects = new ArrayList<>(3);
        if (_targetEnv != null) {
            dataObjects.add(_obs.getTarget().getDataObject());
        }
        if (_inst != null) {
            dataObjects.add(_obs.getInstrument().getDataObject());
        }
        _ao.foreach(dataObjects::add);
        return GuideProbeUtil.instance.getAvailableGuiders(dataObjects);
    }

    // Create the class instance by calling its create factory method with this OE as an arg
    @SuppressWarnings("unchecked")
    private ITccInstrumentSupport _create(Class<?> cl) {
        // Setup the arguments and their types
        Class<?>[] paramTypes = new Class<?>[]{ObservationEnvironment.class};
        Object[] args = new Object[]{this};
        ITccInstrumentSupport result = null;
        try {
            Method createMethod = cl.getMethod("create", paramTypes);
            result = (ITccInstrumentSupport) createMethod.invoke(this, args);
        } catch (NoSuchMethodException ex) {
            LOG.severe("ITccInstrumentSupport has no create method?" + ex);
        } catch (IllegalAccessException ex) {
            LOG.severe("IllegalAccess while creating ITccInstrumentSupport?" + ex);
        } catch (InvocationTargetException ex) {
            LOG.severe("InvocationTargetException while creating ITccInstrumentSupport?" + ex);
        }
        return result;
    }

    public SPObservationID getObservationID() {
        return _obs.getObservationId();
    }

    public String getObservationTitle() {
        return _obs.getDataObject().getTitle();
    }

    public String getBasePositionName() {
        return _targetEnv.getArbitraryTargetFromAsterism().getName();
    }

    public boolean isNorth() {
        return _site == Site.GN;
    }

    public boolean isSouth() {
        return _site == Site.GS;
    }

    /**
     * Return the instrument component for use by supports.
     */
    public SPInstObsComp getInstrument() {
        return _inst;
    }

    public TargetObsComp getTargetObsComp() {
        return _obs.getTarget().getDataObject();
    }

    public TargetEnvironment getTargetEnvironment() {
        return _targetEnv;
    }

    public SortedSet<GuideProbe> usedGuiders() {
        return _targetEnv.getGuideEnvironment().getPrimaryReferencedGuiders();
    }

    public boolean containsTargets(GuideProbe probe) {
        return usedGuiders().contains(probe);
    }

    public boolean containsTargets(GuideProbe.Type type) {
        return usedGuiders().stream().anyMatch(gp -> gp.getType() == type);
    }

    public enum AoAspect {
        none,
        ngs,
        lgs
    }

    public AoAspect getAoAspect() {
        InstAltair altair = getAltairConfig();
        if (altair != null) {
            return altair.getGuideStarType() == AltairParams.GuideStarType.LGS ? AoAspect.lgs : AoAspect.ngs;
        }

        Gems gems = getGemsConfig();
        return (gems == null) ? AoAspect.none : AoAspect.lgs;
    }

    public boolean isAltair() {
        return getAltairConfig() != null;
    }

    public InstAltair getAltairConfig() {
        if (_ao.isEmpty()) return null;
        if (!_ao.getValue().getType().equals(InstAltair.SP_TYPE)) return null;
        return (InstAltair) _ao.getValue();
    }

    public boolean isGems() {
        return getGemsConfig() != null;
    }

    public Gems getGemsConfig() {
        if (_ao.isEmpty()) return null;
        if (!_ao.getValue().getType().equals(Gems.SP_TYPE)) return null;
        return (Gems) _ao.getValue();
    }

    public ISPObservation getObservation() {
        return _obs.getRemoteNode();
    }

    public ITccInstrumentSupport getInstrumentSupport() {
        return _is;
    }

    public List<SequenceNode> getOffsetNodes() {
        List<SequenceNode> res = new ArrayList<>();
        addOffsetNodes(_obs.getSequence(), res);
        return res;
    }

    private void addOffsetNodes(SequenceNode root, List<SequenceNode> result) {
        if (root == null) return;

        if (root.getDataObject() instanceof SeqRepeatOffsetBase) {
            result.add(root);
        }

        for (SequenceNode child : root.getChildren()) {
            addOffsetNodes(child, result);
        }
    }

    // private method to log and throw and exception
    private void _logAbort(String message, Exception ex) throws WdbaGlueException {
        LOG.severe(message);
        throw new WdbaGlueException(message, (ex != null) ? ex : null);
    }

    // REL-1596: Check if an instrument is using Altair, and if so and the mode is set to LGS_P1, adjust the origin name
    // to reflect this by appending a "_p1".
    public String adjustInstrumentOriginForLGS_P1(String basename) {
        final InstAltair instAltair = getAltairConfig();
        if (instAltair == null || !AltairParams.Mode.LGS_P1.equals(instAltair.getMode())) return basename;
        return basename + "_p1";
    }
}
