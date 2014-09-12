package edu.gemini.obslog.database;

//
// Gemini Observatory/AURA
// $Id: OlPersistenceException.java,v 1.1.1.1 2004/11/16 02:56:25 gillies Exp $
//

/**
 * Class to handle all exceptions related to database access.
 */
public class OlPersistenceException extends RuntimeException {

    public OlPersistenceException(String message) {
        super(message);
    }

    public OlPersistenceException(String message, Exception ex) {
        super(message, ex);
    }

    public OlPersistenceException(Exception ex) {
        super(null, ex);
    }

}
