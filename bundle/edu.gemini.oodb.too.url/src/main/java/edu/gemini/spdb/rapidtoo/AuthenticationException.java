//
// $Id: AuthenticationException.java 259 2006-01-13 20:35:11Z shane $
//

package edu.gemini.spdb.rapidtoo;

/**
 * Exception indicating that the provided program id doesn't correspond to a
 * program in the database, or else the provided password doesn't match.
 */
public class AuthenticationException extends TooUpdateException {
    public AuthenticationException(String message) {
        super(message);
    }
}
