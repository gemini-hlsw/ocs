package edu.gemini.epics.acm;

/**
 * This Exception class is used to indicate that a remote EPICS systems has
 * completed a command with an error. It allows to retrieve the error message
 * provided by remote EPICS system.
 * 
 * @author jluhrs
 *
 */
public final class CaCommandError extends Exception {

    CaCommandError() {
    }

    CaCommandError(String message) {
        super(message);
    }
}
