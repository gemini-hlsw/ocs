package edu.gemini.obslog.config;

//
// Gemini Observatory/AURA
// $Id: OlConfigException.java,v 1.1.1.1 2004/11/16 02:56:24 gillies Exp $
//

/**
 * An exception thrown when constructing the configuration model from the XML
 * configuration file.  Indicates a problem with the XML conversion.
 */
public class OlConfigException extends Exception {

    public OlConfigException(String message) {
        super(message);
    }

    public OlConfigException(Exception ex) {
        super(null, ex);
    }
}
