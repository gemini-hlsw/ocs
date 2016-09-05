package edu.gemini.spModel.gemini.parallacticangle;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.Angle$;
import edu.gemini.spModel.inst.ParallacticAngleSupport;
import edu.gemini.spModel.obs.ObsTargetCalculatorService;
import edu.gemini.spModel.obscomp.SPInstObsComp;

/**
 * A superclass for instruments that support the parallactic angle feature with most of the implementation in place.
 */
public abstract class ParallacticAngleSupportInst extends SPInstObsComp implements ParallacticAngleSupport {

    /**
     * Constructor and methods for parallactic angle support.
     */
    protected ParallacticAngleSupportInst(final SPComponentType type) {
        super(type);
    }

    /**
     * Perform the parallactic angle computation for the observation.
     * Instruments who need specific modifications based on settings should override this, e.g. GMOS.
     */
    @Override
    public Option<Angle> calculateParallacticAngle(final ISPObservation obs) {
        return ImOption.fromScalaOpt(ObsTargetCalculatorService.targetCalculation(obs))
                .flatMap(targetCalculator -> ImOption.fromScalaOpt(targetCalculator.weightedMeanParallacticAngle()))
                .map(angleObj -> Angle$.MODULE$.fromDegrees((double)angleObj));
    }
}