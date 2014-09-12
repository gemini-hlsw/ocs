package edu.gemini.wdba.glue.api;

/**
 * Gemini Observatory/AURA
 * $Id: DatabaseUnavailableException.java 756 2007-01-08 18:01:24Z gillies $
 */
public class DatabaseUnavailableException extends WdbaGlueException {

    public DatabaseUnavailableException() {
        super("Database is currently unavailable.");
    }
}
