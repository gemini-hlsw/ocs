//
// $
//

package edu.gemini.spModel.config.injector.obswavelength;

import edu.gemini.spModel.config.injector.ConfigInjectorCalc3;

/**
 * Base class for observing wavelength calculations that require three
 * arguments.
 */
public abstract class ObsWavelengthCalc3<A, B, C> extends ObsWavelengthCalcBase implements ConfigInjectorCalc3<A, B, C, String> {
    public String apply(A a, B b, C c) { return validateOrNull(calcWavelength(a, b, c)); }

    /**
     * Subclasses must implement this method to perform the wavelength
     * calculation.
     */
    protected abstract String calcWavelength(A a, B b, C c);
}
