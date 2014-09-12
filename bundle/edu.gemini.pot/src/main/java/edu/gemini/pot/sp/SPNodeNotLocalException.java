// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPNodeNotLocalException.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.sp;


/**
 * The <code>SPNodeNotLocalException</code> is thrown a "local" node is
 * expected but a remote node is provided.  A "local" node is one that
 * was created in the same JVM as the object that contains the operation
 * in question.
 */
public class SPNodeNotLocalException extends SPException {

    /**
     * Constructs with no detail message.
     */
    public SPNodeNotLocalException() {
        super();
    }

    /**
     * Constructs with the specified detail message.  A detail message is a
     * String that describes this particular exception.
     *
     * @param message the detail message
     */
    public SPNodeNotLocalException(String message) {
        super(message);
    }

}
