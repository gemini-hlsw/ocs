package edu.gemini.itc.shared;

public class NoSuchParameterException extends Error {

    public java.lang.String parameterName;

    /**
     * Constructs an <code>NoSuchParameterException</code> with the specified detail message.
     *
     * @param parameterName the missing parameter.
     */
    public NoSuchParameterException(String parameterName) {
        super("Parameter " + parameterName + " was not found in request.");
        this.parameterName = parameterName;
    }
}


