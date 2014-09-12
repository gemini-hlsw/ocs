//
// $Id: SPBadIDException.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.spModel.core;

/**
 * An exception thrown when an illegal id is specified.
 */
public class SPBadIDException extends Exception {

    public SPBadIDException(String message) {
        super(message);
    }
}

