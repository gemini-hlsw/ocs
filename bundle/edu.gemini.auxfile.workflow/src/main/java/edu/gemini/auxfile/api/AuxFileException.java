//
// $Id: AuxFileException.java 314 2006-03-31 20:31:51Z shane $
//

package edu.gemini.auxfile.api;

/**
 * An exception thrown when there is a problem using the auxiliary file system.
 */
public class AuxFileException extends Exception {
    public static AuxFileException create(Exception wrapped) {
        if (wrapped instanceof AuxFileException) {
            return (AuxFileException) wrapped;
        }
        return new AuxFileException(wrapped.getMessage(), wrapped);
    }

    public static AuxFileException create(String message, Exception wrapped) {
        if (wrapped instanceof AuxFileException) {
            return (AuxFileException) wrapped;
        }
        return new AuxFileException(message, wrapped);
    }

    private AuxFileException(String message, Exception wrapped) {
        super(message, wrapped);
    }

    public AuxFileException(String message) {
        super(message);
    }
}
