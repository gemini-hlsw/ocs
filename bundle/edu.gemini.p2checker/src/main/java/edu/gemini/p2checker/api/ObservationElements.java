//
//$Id: ObservationElements.java 47334 2012-08-07 16:53:27Z swalker $
//

package edu.gemini.p2checker.api;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProviderHolder;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.seqcomp.SeqRepeatCbOptions;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The ObservationElements class contains the nodes associated to one observation.
 * Checking an observation using an <code>IRule<code> means to perform specific
 * validations on these nodes.
 * <p/>
 * Besides storing the nodes that conforms an observations, this class also
 * holds the data object associated to every node, so it's easier for
 * client code to perform the validations.
 * <p/>
 * @see edu.gemini.p2checker.api.IRule#check(ObservationElements)
 *
 *
 */
public class ObservationElements implements Serializable {

    private static final Logger LOG = Logger.getLogger(ObservationElements.class.getName());

    private SPProgram _program; //the program that contains the observation
    private ISPProgram _programNode; //the program node
    //nodes that make up an observation
    private ISPObservation _observationNode;
    private ISPObsComponent _instrumentNode;
    private Option<ISPObsComponent> _siteQualityNode = None.instance();
    private Option<ISPObsComponent> _targetEnvNode = None.instance();
    private Option<ISPObsComponent> _aoComponentNode = None.instance();
    private ISPSeqComponent _seqComponentNode;


    //data objects
    private SPInstObsComp _instrument;
    private Option<SPSiteQuality> _siteQuality = None.instance();
    private Option<AbstractDataObject> _aoComponent = None.instance();
    private Option<TargetObsComp> _targetEnv = None.instance();
    private SPObservation _observation;

    private ConfigSequence _sequence;

    /**
     * Constructor. The observation elements must contain
     * at least the <code>ISPObservation</code> node.
     * The constructor will get all
     * the available components within the <code>ISPObservation</code>
     * ready to be used by any {@link edu.gemini.p2checker.api.IRule}.
     *
     * @param obs An <code>ISPObservation</code> observation
     * node whch is the top-level node for any observation
     * within a program.
     */
    public ObservationElements(ISPObservation obs) {
        _observationNode = obs;

        if (obs != null) {
            _observation = (SPObservation) _getDataObject(obs);
            for (ISPObsComponent component : obs.getObsComponents()) {
                SPComponentBroadType type = component.getType().broadType;
                if (type.equals(SPComponentBroadType.INSTRUMENT)) {
                    _setInstrumentNode(component);
                } else if (type.equals(SPSiteQuality.SP_TYPE.broadType)) {
                    _setSiteQualityNode(component);
                } else if (type.equals(TargetObsComp.SP_TYPE.broadType)) {
                    _setTargetObsCompNode(component);
                } else if (type.equals(InstAltair.SP_TYPE.broadType)) {
                    _setAOComponentNode(component);
                }
            }
            _setSequenceComponentNode(obs);
            _setProgram(obs);
        }

    }

    public SPProgram getProgram() {
        return _program;
    }

    public ISPProgram getProgramNode() {
        return _programNode;
    }

    public SPObservation getObservation() {
        return _observation;
    }

    public SPInstObsComp getInstrument() {
        return _instrument;
    }

    public Option<SPSiteQuality> getSiteQuality() {
        return _siteQuality;
    }

    public <T> Option<T> getAOComponent() {
        return (Option<T>)_aoComponent;
    }

    public boolean hasAO(){
        return !getAOComponent().isEmpty();
    }

    public boolean hasAltair(){
        return !_aoComponent.isEmpty() &&  InstAltair.SP_TYPE.equals(_aoComponent.getValue().getType());
    }

    public boolean hasGems(){
        return !_aoComponent.isEmpty() &&  Gems.SP_TYPE.equals(_aoComponent.getValue().getType());
    }

    public Option<TargetObsComp> getTargetObsComp() {
        return _targetEnv;
    }

    public ConfigSequence getSequence() {
        return _sequence;
    }

    public ISPObservation getObservationNode() {
        return _observationNode;
    }

    public ISPSeqComponent getSeqComponentNode() {
        return _seqComponentNode;
    }

    public ISPObsComponent getInstrumentNode() {
        return _instrumentNode;
    }

    public Option<ISPObsComponent> getSiteQualityNode() {
        return _siteQualityNode;
    }

    public Option<ISPObsComponent> getTargetObsComponentNode() {
        return _targetEnvNode;
    }

    public Option<ISPObsComponent> getAOComponentNode() {
        return _aoComponentNode;
    }

    private void _setInstrumentNode(ISPObsComponent inst) {
        _instrumentNode = inst;
        if (inst != null) {
            _instrument = (SPInstObsComp)_getDataObject(inst);
        }
    }

    private void _setSiteQualityNode(ISPObsComponent sq) {
        _siteQualityNode = ImOption.apply(sq);
        if (!_siteQualityNode.isEmpty()) {
            _siteQuality = ImOption.apply((SPSiteQuality)_getDataObject(sq));
        }
    }

    private void _setTargetObsCompNode(ISPObsComponent tenv) {
        _targetEnvNode = ImOption.apply(tenv);
        if (tenv != null) {
            _targetEnv = ImOption.apply((TargetObsComp)_getDataObject(tenv));
        }
    }

    private void _setAOComponentNode(ISPObsComponent aoComp) {
        _aoComponentNode = ImOption.apply(aoComp);
        if (aoComp != null) {
            _aoComponent = ImOption.apply((AbstractDataObject)_getDataObject(aoComp));
        }
    }

    private void _setSequenceComponentNode(ISPObservation obs)  {
        _seqComponentNode = obs.getSeqComponent();
        _sequence = ConfigBridge.extractSequence(obs, getSequenceOptions(), ConfigValMapInstances.IDENTITY_MAP);
    }

    private static Map getSequenceOptions() {
        Map map = new HashMap();
        SeqRepeatCbOptions.setCollapseRepeat(map, true);
        SeqRepeatCbOptions.setAddObsCount(map, true);
        SeqRepeatCbOptions.setCalibrationProvider(map, CalibrationProviderHolder.getProvider());
        //noinspection unchecked
        return Collections.unmodifiableMap(map);
    }


    private Object _getDataObject(ISPNode node) {
        return (node == null) ? null : node.getDataObject();
    }

    /**
     * Traverse the tree to find the program node
     */
    private void _setProgram(ISPNode node) {
        ISPNode parent = node.getParent();

        if (parent == null) { //no parent, this might be the Program
            Object o = node.getDataObject();
            if (o instanceof SPProgram && node instanceof ISPProgram) {
                _program = (SPProgram)node.getDataObject();
                _programNode = (ISPProgram)node;
            }
            return;
        }
        _setProgram(parent);
    }

    /**
     * Creates an observing configuration context for this observation, assuming
     * it has a target component and an instrument.
     *
     * @return ObsContext representing this observation or
     * {@link edu.gemini.shared.util.immutable.None} if there is no target or
     * no instrument
     */
    public Option<ObsContext> getObsContext() {
        return ObsContext.create(_observationNode);
    }

    public boolean isTemplate()  {
        for (ISPNode node = _observationNode; node != null; node = node.getParent())
            if (node instanceof ISPTemplateFolder)
                return true;
        return false;
    }

}
