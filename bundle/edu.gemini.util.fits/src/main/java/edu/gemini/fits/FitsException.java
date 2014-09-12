//
// $Id: FitsException.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

/**
 * Runtime exception signifying an illegal value has been specified.  For
 * example, a 9 character keyword or a value that won't fit in a single header
 * "card image".
 */
public class FitsException extends RuntimeException {
    public FitsException() {
    }

    public FitsException(String message) {
        super(message);
    }
}
