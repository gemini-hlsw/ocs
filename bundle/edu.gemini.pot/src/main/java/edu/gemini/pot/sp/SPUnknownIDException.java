// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPUnknownIDException.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.sp;


/**
 * The <code>SPUnknownIDException</code> is thrown in situations where
 * the code gets an unexpected program or observation ID.
 */
public class SPUnknownIDException extends SPException {

    /**
     * Constructs with no detail message.
     */
    public SPUnknownIDException() {
        super();
    }

    /**
     * Constructs with the specified detail message.  A detail message is a
     * String that describes this particular exception.
     *
     * @param message the detail message
     */
    public SPUnknownIDException(String message) {
        super(message);
    }

}
