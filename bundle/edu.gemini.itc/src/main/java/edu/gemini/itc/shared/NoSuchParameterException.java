// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

/*
 * NoSuchParameterException.java
 *
 * Created on November 30, 2001, 11:21 AM
 */

package edu.gemini.itc.shared;

/**
 *
 * @author  bwalls
 * @version
 */
public class NoSuchParameterException extends java.lang.Exception {

    public java.lang.String parameterName;

    /**
     * Creates new <code>NoSuchParameterException</code> without detail message.
     */
    public NoSuchParameterException() {
    }


    /**
     * Constructs an <code>NoSuchParameterException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NoSuchParameterException(String msg, String parameterName) {
        super(msg);
        this.parameterName = parameterName;
    }
}


