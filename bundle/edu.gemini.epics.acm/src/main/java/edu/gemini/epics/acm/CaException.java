package edu.gemini.epics.acm;

/**
 * Base class of exceptions thrown by the EPICS ACM library.
 * 
 * @author jluhrs
 *
 */
public final class CaException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1741202190351309479L;

    public CaException() {
    }

    public CaException(String message) {
        super(message);
    }

    public CaException(Throwable cause) {
        super(cause);
    }

    public CaException(String message, Throwable cause) {
        super(message, cause);
    }

    public CaException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
