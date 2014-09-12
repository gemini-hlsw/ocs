//
// $
//

package edu.gemini.spModel.config.injector.obswavelength;

import edu.gemini.spModel.config.injector.ConfigInjectorCalc1;

/**
 * Base class for observing wavelength calculations that require a single
 * argument.
 */
public abstract class ObsWavelengthCalc1<A> extends ObsWavelengthCalcBase implements ConfigInjectorCalc1<A, String> {
    public final String apply(A a) { return validateOrNull(calcWavelength(a)); }

    /**
     * Subclasses must implement this method to perform the wavelength
     * calculation.
     */
    protected abstract String calcWavelength(A a);
}
