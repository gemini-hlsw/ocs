//
// $Id: PioReferenceException.java 4937 2004-08-14 21:35:20Z shane $
//

package edu.gemini.spModel.pio;

/**
 * An exception that indicates a problem with following a {@link ParamSet}
 * reference (see {@link ParamSet#getReferenceId}).  This exception is thrown,
 * for example, in situations where a ParamSet reference has been provided, but
 * it does not refer to any ParamSet in the {@link Document}.
 */
public class PioReferenceException extends RuntimeException {
    public PioReferenceException(String cause) {
        super(cause);
    }
}
