// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ISPCloneable.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.sp;

/**
 * An interface implemented by Science Program user objects and component
 * objects that are cloneable.  This allows an implementation to make a copy
 * of the object, and the object to control how that copy is made.
 */
public interface ISPCloneable extends Cloneable {
    /**
     * Declares an exception-free, public <code>clone</code> method.
     */
    public Object clone();
}
