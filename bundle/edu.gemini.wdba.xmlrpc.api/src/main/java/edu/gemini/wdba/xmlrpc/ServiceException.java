package edu.gemini.wdba.xmlrpc;

/**
 * An exception thrown when a web service has a problem.
 */
public class ServiceException extends Exception {
    public static ServiceException create(Exception wrapped) {
        if (wrapped instanceof ServiceException) {
            return (ServiceException)wrapped;
        }
        return new ServiceException(wrapped.getMessage(), wrapped);
    }

    public static ServiceException create(String message, Exception wrapped) {
        if (wrapped instanceof ServiceException) {
            return (ServiceException)wrapped;
        }
        return new ServiceException(message, wrapped);
    }

    /**
     * Use this constructor when only a message should be returned.
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Use this constructor to return a nested exception and message.
     */
    public ServiceException(String message, Exception ex) {
        super(message, ex);
    }
}
