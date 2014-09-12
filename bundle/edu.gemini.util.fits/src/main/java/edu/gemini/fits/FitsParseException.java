//
// $Id: FitsParseException.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

/**
 * A checked exception generated when a problem parsing a FITS file header
 * is encountered.
 */
public class FitsParseException extends Exception {
    public FitsParseException(String message) {
        super(message);
    }
}
