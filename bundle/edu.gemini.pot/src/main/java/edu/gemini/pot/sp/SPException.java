// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPException.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.sp;


/**
 * The <code>SPException</code> serves as the base class for exceptions
 * that may occur when dealing with the Science Program.
 */
public class SPException extends Exception {

    /**
     * Constructs with no detail message.
     */
    public SPException() {
        super();
    }

    /**
     * Constructs with the specified detail message.  A detail message is a
     * String that describes this particular exception.
     *
     * @param message the detail message
     */
    public SPException(String message) {
        super(message);
    }

}
