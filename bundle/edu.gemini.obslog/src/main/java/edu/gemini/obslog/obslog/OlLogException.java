package edu.gemini.obslog.obslog;

import edu.gemini.shared.util.GeminiException;

//
// Gemini Observatory/AURA
// $Id: OlLogException.java,v 1.1.1.1 2004/11/16 02:56:25 gillies Exp $
//  


public class OlLogException extends GeminiException {

    public OlLogException(String message) {
        super(message);
    }

    public OlLogException(Exception ex) {
        super(null, ex);
    }
}
