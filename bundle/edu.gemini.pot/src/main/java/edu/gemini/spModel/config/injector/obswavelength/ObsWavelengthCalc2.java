//
// $
//

package edu.gemini.spModel.config.injector.obswavelength;

import edu.gemini.spModel.config.injector.ConfigInjectorCalc2;

/**
 * Base class for observing wavelength calculations that require two
 * arguments.
 */
public abstract class ObsWavelengthCalc2<A, B> extends ObsWavelengthCalcBase implements ConfigInjectorCalc2<A, B, String> {
    public String apply(A a, B b) { return validateOrNull(calcWavelength(a, b)); }

    /**
     * Subclasses must implement this method to perform the wavelength
     * calculation.
     */
    protected abstract String calcWavelength(A a, B b);
}
