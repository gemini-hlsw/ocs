// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: Assert.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.util;

/**
 * A simple assertion mechanism for asserting validity of
 * arguments. (Taken from David Geary and the Graphic Java Toolkit.)<p>
 *
 * @version 1.0, Apr 1 1996
 * @author  David Geary
 */
public class Assert {
    static public void notFalse(boolean b) throws IllegalArgumentException {
        if (b == false)
            throw new IllegalArgumentException("boolean expression false");
    }

    static public void notNull(Object obj) throws IllegalArgumentException {
        if (obj == null)
            throw new IllegalArgumentException("null argument");
    }

    static public void notFalse(boolean b, String s) throws IllegalArgumentException {
        if (b == false)
            throw new IllegalArgumentException(s);
    }

    static public void notNull(Object obj, String s) throws IllegalArgumentException {
        if (obj == null)
            throw new IllegalArgumentException(s);
    }
}
