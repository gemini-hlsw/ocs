//
// $Id: InactiveProgramException.java 259 2006-01-13 20:35:11Z shane $
//

package edu.gemini.spdb.rapidtoo;

/**
 * An exception that indicates that the program for which a TOO update is
 * desired is not active.  Only active programs are available for updates to
 * observations with TOO priority.
 */
public class InactiveProgramException extends TooUpdateException {
    public InactiveProgramException(String message) {
        super(message);
    }
}
