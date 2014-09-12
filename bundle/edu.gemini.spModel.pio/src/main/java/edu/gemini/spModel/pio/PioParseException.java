//
// $Id: PioParseException.java 6089 2005-05-12 14:17:55Z shane $
//

package edu.gemini.spModel.pio;

/**
 * An exception that may be thrown by an application parsing a Pio document or
 * document subset when it encounters missing or unhandled information.
 */
public class PioParseException extends Exception {
    public PioParseException(String message) {
        super(message);
    }
    public PioParseException(String message, Exception ex) {
        super(message, ex);
    }
}
