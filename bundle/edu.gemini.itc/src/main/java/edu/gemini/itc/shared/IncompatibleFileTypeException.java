// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

/*
 * IncompatableFileTypeException.java
 *
 * Created on November 30, 2001, 11:18 AM
 */

package edu.gemini.itc.shared;

/**
 * @author bwalls
 */
public class IncompatibleFileTypeException extends java.lang.Exception {

    /**
     * Creates new <code>IncompatableFileTypeException</code> without detail message.
     */
    public IncompatibleFileTypeException() {
    }


    /**
     * Constructs an <code>IncompatableFileTypeException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public IncompatibleFileTypeException(String msg) {
        super(msg);
    }
}


