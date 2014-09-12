package edu.gemini.wdba.glue.api;

/**
 * An exception thrown when a web service has a problem.
 */
public class WdbaGlueException extends Exception {
    /**
     * Use this constructor when only a message should be returned.
     */
    public WdbaGlueException(String message) {
        super(message);
    }

    /**
     * Use this constructor to return a nested exception and message.
     */
    public WdbaGlueException(String message, Exception ex) {
        super(message, ex);
    }
}
