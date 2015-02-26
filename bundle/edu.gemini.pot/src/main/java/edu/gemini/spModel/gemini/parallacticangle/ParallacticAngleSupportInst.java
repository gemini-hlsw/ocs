package edu.gemini.spModel.gemini.parallacticangle;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.inst.ParallacticAngleDuration;
import edu.gemini.spModel.inst.ParallacticAngleSupport;
import edu.gemini.spModel.obs.ObsTargetCalculatorService;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.util.skycalc.calc.TargetCalculator;

import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.TreeMap;


/**
 * A superclass for instruments that support the parallactic angle feature with most of the implementation in place.
 */
public abstract class ParallacticAngleSupportInst extends SPInstObsComp implements ParallacticAngleSupport {
    /**
     * Properties for parallactic angle support.
     *
     * Note that PARALLACTIC_ANGLE_DURATION_PROP is not actually used for PIO, but as an umbrella name for property
     * change events, with the actual PIO being delegated to ParallacticAngleDuration.
     */
    protected static final PropertyDescriptor PARALLACTIC_ANGLE_DURATION_PROP;
    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<String, PropertyDescriptor>();

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, ParallacticAngleSupportInst.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    // Initialize the properties.
    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        PARALLACTIC_ANGLE_DURATION_PROP = initProp("parallacticAngleDuration", query_no, iter_no);
    }

    // The parallactic angle duration information.
    private ParallacticAngleDuration _parallacticAngleDuration = ParallacticAngleDuration.getInstance();

    /**
     * Constructor and methods for parallactic angle support.
     */
    protected ParallacticAngleSupportInst(SPComponentType type) {
        super(type);
    }

    /**
     * Param IO for the instance variables.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);
        ParamSet durationParamSet = ParallacticAngleDuration.toParamSet(factory, getParallacticAngleDuration());
        paramSet.addParamSet(durationParamSet);
        return paramSet;
    }

    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        // Null should never happen here, but just in case.
        ParallacticAngleDuration parallacticAngleDuration = ParallacticAngleDuration.fromParamSet(paramSet);
        if (parallacticAngleDuration != null)
            setParallacticAngleDuration(parallacticAngleDuration);
    }

    @Override
    public ParallacticAngleDuration getParallacticAngleDuration() {
        return _parallacticAngleDuration;
    }

    @Override
    public void setParallacticAngleDuration(ParallacticAngleDuration newValue) {
        if (newValue == null) newValue = ParallacticAngleDuration.getInstance();
        ParallacticAngleDuration oldValue = _parallacticAngleDuration;
        if (oldValue == null) oldValue = ParallacticAngleDuration.getInstance();
        if (!oldValue.equals(newValue)) {
            _parallacticAngleDuration = newValue;
            firePropertyChange(PARALLACTIC_ANGLE_DURATION_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Perform the parallactic angle computation for the observation.
     * Instruments who need specific modifications based on settings should override this, e.g. GMOS.
     */
    @Override
    public Option<Angle> calculateParallacticAngle(ISPObservation obs) {
        final Option<TargetCalculator> targetCalculatorOption = ObsTargetCalculatorService.targetCalculationForJava(obs);
        if (!targetCalculatorOption.isEmpty()) {
            final TargetCalculator targetCalculator = targetCalculatorOption.getValue();

            // Calculate the weighted angle.
            scala.Option<Object> angleOption = targetCalculator.weightedMeanParallacticAngle();
            if (angleOption.nonEmpty()) {
                // Calculate the parallactic angle.
                final double dAngle = (Double) angleOption.get();
                final edu.gemini.skycalc.Angle angle = (new edu.gemini.skycalc.Angle(dAngle, edu.gemini.skycalc.Angle.Unit.DEGREES)).toPositive();
                return new Some<>(angle);
            } else return None.instance();
        } else return None.instance();
    }

    /**
     * By default, assume that the instrument is compatible with the parallactic angle feature unless indicated
     * otherwise.
     */
    @Override
    public boolean isCompatibleWithMeanParallacticAngleMode() {
        return true;
    }
}
