//
// $Id: MissingTemplateException.java 259 2006-01-13 20:35:11Z shane $
//

package edu.gemini.spdb.rapidtoo;

/**
 * An exception that indicates a TOO update request for which the template
 * observation to clone and update cannot be located.
 */
public class MissingTemplateException extends TooUpdateException {
    public MissingTemplateException(String message) {
        super(message);
    }
}
