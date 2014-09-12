package edu.gemini.p1monitor.fetch;

/**
 * An exception class that marks a problem providing a response to a fetch
 * request.
 */
public final class FetchException extends Exception {
    public FetchException(String message) {
        super(message);
    }
}
