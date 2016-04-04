package edu.gemini.spModel.gemini.parallacticangle;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.inst.ParallacticAngleSupport;
import edu.gemini.spModel.obs.ObsTargetCalculatorService;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.util.skycalc.calc.TargetCalculator;

/**
 * A superclass for instruments that support the parallactic angle feature with most of the implementation in place.
 */
public abstract class ParallacticAngleSupportInst extends SPInstObsComp implements ParallacticAngleSupport {

    /**
     * Constructor and methods for parallactic angle support.
     */
    protected ParallacticAngleSupportInst(SPComponentType type) {
        super(type);
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
