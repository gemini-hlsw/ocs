//
// $Id: GeminiRuntimeException.java 4392 2004-01-30 06:40:18Z gillies $
//

package edu.gemini.shared.util;

import java.io.*;

public class GeminiRuntimeException extends RuntimeException {

    private Exception nestedException;

    /**
     * Creates a new exception using the specified nested exception.  The
     * static "constructor" is used to avoid wrapping a GeminiRuntimeException.
     *
     * @param oldException the source exception (may be null)
     *
     * @return either the oldException if it is an GeminiRuntimeException
     * or a new GeminiRuntimeException with the nested exception in it
     */
    public static GeminiRuntimeException newException(Exception oldException) {
        return newException(null, oldException);
    }

    /**
     * Creates a new exception using the specified message and nested
     * exception.  The static "constructor" is used to avoid wrapping
     * a GeminiRuntimeException.
     *
     * @param message the error message (may be null)
     * @param oldException the source exception (may be null)
     *
     * @return either the oldException if it is an GeminiRuntimeException
     * or a new GeminiRuntimeException with the nested exception in it
     */
    public static GeminiRuntimeException newException(String message, Exception oldException) {
        if (oldException instanceof GeminiRuntimeException) {
            return (GeminiRuntimeException) oldException;
        }
        if (message == null) {
            return new GeminiRuntimeException(oldException);
        }
        return new GeminiRuntimeException(message, oldException);
    }

    /**
     * Creates a new exception using the specified error message string.
     *
     * @param str the error message
     */
    public GeminiRuntimeException(String str) {
        this(str, null);
    }

    /**
     * Creates a new exception using the passed source exception.
     *
     * @param ex the source exception (may be null)
     */
    protected GeminiRuntimeException(Exception ex) {
        this(null, ex);
    }

    /**
     * Creates a new exception using the passed message and source exception.
     *
     * @param message the error message (may be null)
     * @param ex the source exception (may be null)
     */
    protected GeminiRuntimeException(String message, Exception ex) {
        super(((message == null) && (ex != null)) ? ex.getMessage() : message);
        nestedException = ex;
    }

    public void printStackTrace() {
        if (nestedException != null)
            nestedException.printStackTrace();
        super.printStackTrace();
    }

    public void printStackTrace(PrintWriter writer) {
        if (nestedException != null)
            nestedException.printStackTrace(writer);
        super.printStackTrace(writer);
    }

    public void printStackTrace(PrintStream stream) {
        if (nestedException != null)
            nestedException.printStackTrace(stream);
        super.printStackTrace(stream);
    }

    /**
     * Returns the nested exception (if any); <code>null</code> otherwise.
     */
    public Throwable getCause() {
        return nestedException;
    }
}


