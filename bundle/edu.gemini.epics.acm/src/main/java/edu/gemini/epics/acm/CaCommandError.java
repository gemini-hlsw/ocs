package edu.gemini.epics.acm;

/**
 * This Exception class is used to indicate that a remote EPICS systems has
 * completed a command with an error. It allows to retrieve the error message
 * provided by remote EPICS system.
 * 
 * @author jluhrs
 *
 */
public class CaCommandError extends Exception {

    public CaCommandError() {
    }

    public CaCommandError(String message) {
        super(message);
    }
}
