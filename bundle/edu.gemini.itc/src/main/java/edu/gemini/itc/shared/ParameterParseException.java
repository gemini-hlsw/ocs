// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

/*
 * ParameterParseException.java
 *
 * Created on November 30, 2001, 11:40 AM
 */

package edu.gemini.itc.shared;

/**
 *
 * @author  bwalls
 * @version
 */
public class ParameterParseException extends java.lang.Exception {

    public String parameterName;

    /**
     * Creates new <code>ParameterParseException</code> without detail message.
     */
    public ParameterParseException() {
    }


    /**
     * Constructs an <code>ParameterParseException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ParameterParseException(String msg, String parameterName) {
        super(msg);
        this.parameterName = parameterName;
    }
}


