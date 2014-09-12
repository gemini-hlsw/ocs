//
// $
//

package edu.gemini.spModel.config.injector.obswavelength;

import java.util.regex.Pattern;

import static edu.gemini.spModel.obscomp.InstConstants.OBSERVING_WAVELENGTH_PROP;

/**
 * Base class for observing wavelength calculation.
 */
public abstract class ObsWavelengthCalcBase {
    public final String resultPropertyName() { return OBSERVING_WAVELENGTH_PROP; }

    private static final Pattern PAT = Pattern.compile("\\d+\\.?\\d*");

    /**
     * Validates the given wavelength value, returning a value only if the
     * provided observing wavelength can be parsed as such.
     *
     * @param wl candidate observing wavelength value
     *
     * @return <code>true</code> if considered a valid wavelength stting,
     * <code>false</code> otherwise
     */
    public static boolean isValidWavelength(String wl) {
        if (wl == null) return false;
        return PAT.matcher(wl).matches();
    }

    /**
     * Validates the given wavelength value returning the given argument
     * if valid, or else <code>null</code> if not.
     */
    public static String validateOrNull(String wl) {
        return isValidWavelength(wl) ? wl : null;
    }
}
