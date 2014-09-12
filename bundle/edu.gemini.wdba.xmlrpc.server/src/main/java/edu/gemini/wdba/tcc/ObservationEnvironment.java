package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.ext.*;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.wdba.glue.api.WdbaGlueException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            _ao = new Some<ISPDataObject>(aoNode.getDataObject());
        }

        ITccInstrumentSupport sup = null;
        InstrumentNode instNode = _obs.getInstrument();
        if (instNode == null) {
            LOG.info("No instrument in observation: " + _obs.getObservationId());
            _inst = null;
        } else {
            _inst = instNode.getDataObject();
            String className = supportMap.get(_inst.getType());
            if (className != null) {
                try {
                    Class cl = Class.forName(className);
                    LOG.info("Map class is:" + className);
                    sup = _create(cl);
                } catch (ClassNotFoundException ex) {
                    LOG.info("Could not locate the support class: " + className);
                }
            }
        }
        if (sup == null) {
            sup = ITccInstrumentSupport.DefaultInstrumentSupport.create(this);
        }
        _is = sup;
    }


    // Create the class instance by calling its create factory method with this OE as an arg
    private ITccInstrumentSupport _create(Class cl) {
        // Setup the arguments and their types
        Class[] paramTypes = new Class[]{ObservationEnvironment.class};
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

    public SPProgramID getProgramID() {
        SPObservationID obsId = _obs.getObservationId();
        return obsId == null ? null : obsId.getProgramID();
    }

    public String getObservationTitle() {
        return _obs.getDataObject().getTitle();
    }

    public String getBasePositionName() {
        return _targetEnv.getBase().getName();
    }

    public boolean isNorth() {
        return _site == Site.GN;
    }

    public boolean isSouth() {
        return _site == Site.GS;
    }

    /**
     * Allows an instrument support implementation to check to see if the observation's
     * instrument is the one it wants.
     * @param type the type for checking
     * @return true if the types match and there is an instrument else false
     */
    public boolean isMyInstrument(SPComponentType type) {
        if (_inst == null) return false;
        return _inst.getType().equals(type);
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

    public boolean containsTargets(GuideProbe probe) {
        Option<GuideProbeTargets> gtOpt = _targetEnv.getPrimaryGuideProbeTargets(probe);
        return (!gtOpt.isEmpty() && (gtOpt.getValue().getOptions().size() > 0));
    }

    public Option<SPTarget> getPrimaryTarget(GuideProbe probe) {
        Option<GuideProbeTargets> gtOpt = _targetEnv.getPrimaryGuideProbeTargets(probe);
        Option<SPTarget> none = None.instance();

        // TODO: GuideProbeTargets.isEnabled
        if (!_targetEnv.isActive(probe)) return none;
        return gtOpt.isEmpty() ? none : gtOpt.getValue().getPrimary();
    }

    public boolean containsTargets(GuideProbe.Type type) {
        GuideGroup grp = _targetEnv.getOrCreatePrimaryGuideGroup();
        ImList<GuideProbeTargets> gtList = grp.getAllMatching(type);
        return gtList.exists(new PredicateOp<GuideProbeTargets>() {
            @Override public Boolean apply(GuideProbeTargets gt) {
                return gt.getOptions().size() > 0;
            }
        });
    }

    public Option<SPTarget> getPrimaryTarget(GuideProbe.Type type) {
        GuideGroup grp = _targetEnv.getOrCreatePrimaryGuideGroup();
        ImList<GuideProbeTargets> gtList = grp.getAllMatching(type);

        Option<GuideProbeTargets> gtOpt = gtList.find(new PredicateOp<GuideProbeTargets>() {
            @Override public Boolean apply(GuideProbeTargets gt) {
                if (!_targetEnv.isActive(gt.getGuider())) return false;
                Option<SPTarget> target = gt.getPrimary();
                return !target.isEmpty();
            }
        });

        Option<SPTarget> none = None.instance();
        return gtOpt.isEmpty() ? none : gtOpt.getValue().getPrimary();
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
        List<SequenceNode> res = new ArrayList<SequenceNode>();
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
}
