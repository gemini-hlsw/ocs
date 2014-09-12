// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPTreeStateException.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.sp;


/**
 * The <code>SPTreeStateException</code> is thrown when a requested
 * operation would result in an illegal science program tree.
 */
public class SPTreeStateException extends SPException {

    /**
     * Constructs with no detail message.
     */
    public SPTreeStateException() {
        super();
    }

    /**
     * Constructs with the specified detail message.  A detail message is a
     * String that describes this particular exception.
     *
     * @param message the detail message
     */
    public SPTreeStateException(String message) {
        super(message);
    }

}
