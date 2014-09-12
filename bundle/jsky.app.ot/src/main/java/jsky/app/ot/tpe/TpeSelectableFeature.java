// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TpeSelectableFeature.java 4336 2004-01-20 07:57:42Z gillies $
//
package jsky.app.ot.tpe;


/**
 * This is an interface supported by TpeImageFeatures that support
 * selecting individual items (such as target positions).
 */
public interface TpeSelectableFeature {
    /**
     * Select an item, returning it if successful.  Return null if nothing
     * is selected.
     */
    public Object select(TpeMouseEvent evt);
}

