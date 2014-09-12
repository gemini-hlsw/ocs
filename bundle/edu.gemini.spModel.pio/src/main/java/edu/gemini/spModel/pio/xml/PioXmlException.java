//
// $Id: PioXmlException.java 4888 2004-08-02 22:56:25Z shane $
//
package edu.gemini.spModel.pio.xml;

/**
 * An exception that indicates a problem reading or writing a Science Program
 * XML document.
 */
public class PioXmlException extends Exception {
    public static PioXmlException newException(Exception nestedCause) {
        if (nestedCause instanceof PioXmlException) {
            return (PioXmlException) nestedCause;
        }
        return new PioXmlException(nestedCause);
    }

    public static PioXmlException newException(String message, Exception nestedCause) {
        if (nestedCause instanceof PioXmlException) {
            return (PioXmlException) nestedCause;
        }
        return new PioXmlException(message, nestedCause);
    }

    public PioXmlException(String message) {
        super(message);
    }

    private PioXmlException(Exception nestedCause) {
        super(nestedCause);
    }

    private PioXmlException(String message, Exception nestedCause) {
        super(message, nestedCause);
    }
}
