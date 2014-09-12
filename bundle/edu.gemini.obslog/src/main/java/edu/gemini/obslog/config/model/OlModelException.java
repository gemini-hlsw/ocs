//
// Gemini Observatory/AURA
// $Id: OlModelException.java,v 1.1.1.1 2004/11/16 02:56:24 gillies Exp $
//
package edu.gemini.obslog.config.model;

/**
 * An exception thrown when interacting with the configuration model.  Usually
 * indicates the model is not consistent.
 */
public class OlModelException extends Exception {

    public OlModelException(String message) {
        super(message);
    }

    public OlModelException(Exception ex) {
        super(null, ex);
    }
}
