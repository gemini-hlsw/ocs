package edu.gemini.wdba.shared;

/**
 * An exception thrown when a problem occurs when converted XML-RPC args
 * Created by Gemini Observatory HLDG
 */
public class WdbaHelperException extends Exception {
    public static WdbaHelperException create(Exception wrapped) {
        if (wrapped instanceof WdbaHelperException) {
            return (WdbaHelperException) wrapped;
        }
        return new WdbaHelperException(wrapped.getMessage(), wrapped);
    }

    public static WdbaHelperException create(String message, Exception wrapped) {
        if (wrapped instanceof WdbaHelperException) {
            return (WdbaHelperException) wrapped;
        }
        return new WdbaHelperException(message, wrapped);
    }

    private WdbaHelperException(String message, Exception wrapped) {
        super(message, wrapped);
    }

    public WdbaHelperException(String message) {
        super(message);
    }

}
