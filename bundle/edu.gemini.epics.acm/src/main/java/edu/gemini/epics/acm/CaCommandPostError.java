package edu.gemini.epics.acm;

/**
 * This exception is generated when an inconsistency is detected while
 * monitoring the execution of a command, like another system trying to trigger
 * the same apply record.
 * 
 * @author jluhrs
 *
 */
public final class CaCommandPostError extends Exception {

    CaCommandPostError() {
    }

    CaCommandPostError(String message) {
        super(message);
    }
}
