//
// $Id: BadRequestException.java 259 2006-01-13 20:35:11Z shane $
//

package edu.gemini.spdb.rapidtoo.www;

/**
 * An exception that indicates a problem with the request such that it cannot
 * be completed.
 */
public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super(message);
    }
}
